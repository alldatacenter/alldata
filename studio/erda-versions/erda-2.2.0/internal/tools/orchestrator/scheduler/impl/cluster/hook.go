// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package cluster

import (
	"context"
	"fmt"
	"net/url"
	"path"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	// ErrInvalidAction Illegal clusterevent action
	ErrInvalidAction = errors.New("invalid cluster event action")
	// ErrInvalidClusterType Illegal cluster type
	ErrInvalidClusterType = errors.New("invalid cluster type")
	// ErrEmptyClusterName cluster name is empty
	ErrEmptyClusterName = errors.New("empty cluster name")
	// ErrClusterAddrNotSet cluster addr not setting
	ErrClusterAddrNotSet = errors.New("cluster addr not set")
)

// Hook receive clusterevent， reference `apistructs.ClusterEvent`
func (c *ClusterImpl) Hook(clusterEvent *apistructs.ClusterEvent) error {
	if !strutil.Equal(clusterEvent.Content.Type, clusterTypeDcos, true) {
		if strutil.Equal(clusterEvent.Content.Type, clusterTypeK8S, true) {
			return c.handleK8SCluster(clusterEvent)
		}
		if strutil.Equal(clusterEvent.Content.Type, clusterTypeEdas, true) {
			return c.handleEdasCluster(clusterEvent)
		}
		return errors.Wrap(ErrInvalidClusterType, clusterEvent.Content.Type)
	}
	if clusterEvent.Content.Name == "" {
		return errors.Wrap(ErrEmptyClusterName, fmt.Sprintf("%+v", clusterEvent.Content))
	}
	logrus.Infof("cluster event: %+v, scheduler config: %+v", clusterEvent, *(clusterEvent.Content.SchedConfig))

	if clusterEvent.Action == clusterActionCreate {
		cluster := generateClusterInfoFromEvent(&clusterEvent.Content)
		addr, ok := cluster.Options["ADDR"]
		if !ok {
			return ErrClusterAddrNotSet
		}

		srv := cluster
		if err := createMarathonExecutor(c.js, srv, addr); err != nil {
			return err
		}

		job := cluster
		if err := createMetronomeExecutor(c.js, job, addr); err != nil {
			return err
		}

		logrus.Infof("cluster(%s) %s successfully according to related event", clusterEvent.Action, cluster.ClusterName)
		return nil
	} else if clusterEvent.Action == clusterActionUpdate {
		keys, err := c.FindKeysByCluster(clusterEvent.Content.Name)
		if err != nil {
			return err
		}

		for _, k := range keys {
			cluster := ClusterInfo{}
			if err := c.js.Get(context.Background(), k, &cluster); err != nil {
				content := fmt.Sprintf("get cluster(%s, key: %s) from etcd error: %v", clusterEvent.Content.Name, k, err)
				return errors.New(content)
			}
			if err := patchClusterConfig(&cluster, &(clusterEvent.Content)); err != nil {
				content := fmt.Sprintf("parse cluster(%s, key: %s) url error: %v", clusterEvent.Content.Name, k, err)
				return errors.New(content)
			}
			if err := c.js.Put(context.Background(), k, cluster); err != nil {
				content := fmt.Sprintf("patch cluster(%s, key: %s) to etcd error: %v", clusterEvent.Content.Name, k, err)
				return errors.New(content)
			}
		}
		return nil
	} else if clusterEvent.Action == clusterActionDelete {
		if err := c.deleteExecutor(clusterEvent.Content.Name); err != nil {
			content := fmt.Sprintf("delete cluster(%s) error: %v", clusterEvent.Content.Name, err)
			return errors.New(content)
		}
	}
	return errors.Wrap(ErrInvalidAction, clusterEvent.Action)
}

func (c *ClusterImpl) FindKeysByCluster(name string) ([]string, error) {
	keys := make([]string, 0)
	findKeyBelongedToCluster := func(k string, v interface{}) error {
		c, ok := v.(*ClusterInfo)
		if !ok {
			logrus.Errorf("key(%s) related value(%+v) is not type of ClusterInfo", k, v)
			return nil
		}
		if c.ClusterName == name {
			keys = append(keys, k)
		}
		return nil
	}

	if err := c.js.ForEach(context.Background(), clusterPrefix, ClusterInfo{}, findKeyBelongedToCluster); err != nil {
		content := fmt.Sprintf("cluster(%s) range prefix(%s) from etcd got err: %v", name, clusterPrefix, err)
		return keys, errors.New(content)
	}

	if len(keys) == 0 {
		content := fmt.Sprintf("Cannot find executor belonged to cluster(%s)", name)
		return keys, errors.New(content)
	}
	return keys, nil
}

func setDefaultClusterConfig(ci *ClusterInfo) {
	t := "true"
	if _, ok := ci.Options[labelconfig.ENABLETAG]; !ok {
		ci.Options[labelconfig.ENABLETAG] = t
	}
	if _, ok := ci.Options[labelconfig.ENABLE_ORG]; !ok {
		ci.Options[labelconfig.ENABLE_ORG] = t
	}
	if _, ok := ci.Options[labelconfig.ENABLE_WORKSPACE]; !ok {
		ci.Options[labelconfig.ENABLE_WORKSPACE] = t
	}
	if _, ok := ci.Options[labelconfig.CPU_NUM_QUOTA]; !ok {
		// Here "-1" means to limit the maximum cpu according to the real cpu requested by the user,
		// That is, if the user applies for a 1 core cpu, the quota corresponds to 1 core
		// If set to "0", the cpu quota is unlimited
		// For details, please refer to the implementation in the marathon plugin (in the setFineGrainedCPU function)
		ci.Options[labelconfig.CPU_NUM_QUOTA] = "-1"
	}
}

func create(js jsonstore.JsonStore, key string, c ClusterInfo) error {
	setDefaultClusterConfig(&c)
	if err := js.Put(context.Background(), key, c); err != nil {
		content := fmt.Sprintf("write key(%s) to etcd error: %v", key, err)
		return errors.New(content)
	}

	logrus.Infof("cluster executor(%s) created successfully, content: %+v", c.ExecutorName, c)
	return nil
}

func createEdasExector(js jsonstore.JsonStore, key string, c string) error {
	if err := js.Put(context.Background(), key, c); err != nil {
		content := fmt.Sprintf("write key(%s) to etcd error: %v", key, err)
		return errors.New(content)
	}

	logrus.Infof("cluster executor(%s) created successfully", c)
	return nil
}

func createMarathonExecutor(js jsonstore.JsonStore, cluster ClusterInfo, addr string) error {
	srv := cluster
	srv.Kind = clusterutil.ServiceKindMarathon
	srv.ExecutorName = clusterutil.GenerateExecutorByCluster(cluster.ClusterName, clusterutil.ServiceKindMarathon)
	srv.Options["ADDR"] = addr + marathonAddrSuffix
	// Maximum cpu limit by default (cpu quota)
	if _, ok := srv.Options["CPU_NUM_QUOTA"]; !ok {
		// Here "-1" means to limit the maximum cpu according to the real cpu requested by the user,
		// That is, if the user applies for a 1 core cpu, the quota corresponds to 1 core
		// If set to "0", the cpu quota is unlimited
		// For details, please refer to the implementation in the marathon plugin (in the setFineGrainedCPU function)
		srv.Options["CPU_NUM_QUOTA"] = "-1"
	}

	return create(js, clusterPrefix+cluster.ClusterName+clusterMarathonSuffix, srv)
}

func createMetronomeExecutor(js jsonstore.JsonStore, cluster ClusterInfo, addr string) error {
	job := cluster
	job.Kind = clusterutil.JobKindMetronome
	job.ExecutorName = clusterutil.GenerateExecutorByCluster(cluster.ClusterName, clusterutil.JobKindMetronome)
	job.Options["ADDR"] = addr + metronomeAddrSuffix

	return create(js, clusterPrefix+cluster.ClusterName+clusterMetronomeSuffix, job)
}

func generateClusterInfoFromEvent(eventCluster *apistructs.ClusterInfo) ClusterInfo {
	configCluster := ClusterInfo{
		ClusterName:  eventCluster.Name,
		ExecutorName: clusterutil.GenerateExecutorByCluster(eventCluster.Name, clusterutil.ServiceKindMarathon),
		Kind:         clusterutil.ServiceKindMarathon,
		Options:      make(map[string]string),
	}
	if eventCluster.SchedConfig == nil {
		return configCluster
	}

	configCluster.Options["ADDR"] = eventCluster.SchedConfig.MasterURL
	configCluster.Options["ENABLETAG"] = strconv.FormatBool(eventCluster.SchedConfig.EnableTag)
	if eventCluster.SchedConfig.AuthType == "basic" {
		configCluster.Options["BASICAUTH"] = eventCluster.SchedConfig.AuthUsername + ":" + eventCluster.SchedConfig.AuthPassword
	}
	if len(eventCluster.SchedConfig.CACrt) > 0 {
		configCluster.Options["CA_CRT"] = eventCluster.SchedConfig.CACrt
		configCluster.Options["CLIENT_CRT"] = eventCluster.SchedConfig.ClientCrt
		configCluster.Options["CLIENT_KEY"] = eventCluster.SchedConfig.ClientKey
	}
	return configCluster
}

// TODO: Need to discuss the content that is open to users to modify
// TODO: Support patch refined configuration
func patchClusterConfig(local *ClusterInfo, request *apistructs.ClusterInfo) error {
	if request.SchedConfig != nil {
		if request.SchedConfig.MasterURL != "" {
			u, err := url.Parse(request.SchedConfig.MasterURL)
			if err != nil {
				return errors.Errorf("cluster addr(%s) is invalid url", request.SchedConfig.MasterURL)
			}
			if local.Kind == clusterutil.ServiceKindMarathon {
				u.Path = path.Join(u.Path, marathonAddrSuffix)
				local.Options["ADDR"] = u.String()
			} else if local.Kind == clusterutil.JobKindMetronome {
				u.Path = path.Join(u.Path, metronomeAddrSuffix)
				local.Options["ADDR"] = u.String()
			}
		}
		if request.SchedConfig.AuthUsername != "" && request.SchedConfig.AuthPassword != "" {
			local.Options["BASICAUTH"] = request.SchedConfig.AuthUsername + ":" + request.SchedConfig.AuthPassword
		}
		if request.SchedConfig.CACrt != "" && request.SchedConfig.ClientCrt != "" && request.SchedConfig.ClientKey != "" {
			local.Options["CA_CRT"] = request.SchedConfig.CACrt
			local.Options["CLIENT_CRT"] = request.SchedConfig.ClientCrt
			local.Options["CLIENT_KEY"] = request.SchedConfig.ClientKey
		}
	}
	return nil
}

func (c *ClusterImpl) handleK8SCluster(clusterEvent *apistructs.ClusterEvent) error {
	logrus.Infof("cluster hook of k8s, name: %s", clusterEvent.Content.Name)

	if clusterEvent.Content.SchedConfig == nil {
		return errors.Wrap(ErrInvalidClusterType, clusterEvent.Content.Type)
	}

	if clusterEvent.Action == clusterActionCreate {
		return c.createK8SExecutor(clusterEvent)
	} else if clusterEvent.Action == clusterActionUpdate {
		return c.updateK8SExecutor(clusterEvent)
	} else if clusterEvent.Action == clusterActionDelete {
		return c.deleteExecutor(clusterEvent.Content.Name)
	}
	return nil
}

func (c *ClusterImpl) createK8SExecutor(clusterEvent *apistructs.ClusterEvent) error {
	serviceCfg := ClusterInfo{
		ClusterName:  clusterEvent.Content.Name,
		ExecutorName: clusterutil.GenerateExecutorByCluster(clusterEvent.Content.Name, clusterutil.ServiceKindMarathon),
		Kind:         clusterutil.ServiceKindK8S,
		Options: map[string]string{
			"cluster":                     clusterEvent.Content.Name,
			"ADDR":                        clusterEvent.Content.SchedConfig.MasterURL,
			"DEV_CPU_SUBSCRIBE_RATIO":     clusterEvent.Content.SchedConfig.CPUSubscribeRatio,
			"TEST_CPU_SUBSCRIBE_RATIO":    clusterEvent.Content.SchedConfig.CPUSubscribeRatio,
			"STAGING_CPU_SUBSCRIBE_RATIO": clusterEvent.Content.SchedConfig.CPUSubscribeRatio,
		},
	}
	setDefaultClusterConfig(&serviceCfg)

	svcPath := strutil.Concat(clusterPrefix, clusterEvent.Content.Name, clusterK8SSuffix)
	if err := create(c.js, svcPath, serviceCfg); err != nil {
		logrus.Errorf("failed to create k8s service executor, cluster: %s", clusterEvent.Content.Name)
		return err
	}

	logrus.Infof("succeed to create k8s cluster, name: %s", clusterEvent.Content.Name)
	return nil
}

func (c *ClusterImpl) updateK8SExecutor(clusterEvent *apistructs.ClusterEvent) error {
	keys, err := c.FindKeysByCluster(clusterEvent.Content.Name)
	if err != nil {
		return err
	}
	for _, k := range keys {
		cluster := ClusterInfo{}
		if err := c.js.Get(context.Background(), k, &cluster); err != nil {
			content := fmt.Sprintf("get cluster(%s, key: %s) from etcd error: %v", clusterEvent.Content.Name, k, err)
			return errors.New(content)
		}
		if err := patchK8SConfig(&cluster, &(clusterEvent.Content)); err != nil {
			content := fmt.Sprintf("parse cluster(%s, key: %s) url error: %v", clusterEvent.Content.Name, k, err)
			return errors.New(content)
		}
		if err := c.js.Put(context.Background(), k, cluster); err != nil {
			content := fmt.Sprintf("patch cluster(%s, key: %s) to etcd error: %v", clusterEvent.Content.Name, k, err)
			return errors.New(content)
		}
	}
	return nil
}

// todo: Only support to modify the address at the moment
func patchK8SConfig(local *ClusterInfo, request *apistructs.ClusterInfo) error {
	if request.SchedConfig != nil {
		if request.SchedConfig.MasterURL != "" {
			u, err := url.Parse(request.SchedConfig.MasterURL)
			if err != nil {
				return errors.Errorf("k8s cluster addr is invalid, addr: %s", request.SchedConfig.MasterURL)
			}
			local.Options["ADDR"] = u.String()
		}
		c, err := url.Parse(request.SchedConfig.CPUSubscribeRatio)
		if err != nil {
			return errors.Errorf("k8s cluster addr is invalid, addr: %s", request.SchedConfig.MasterURL)
		}
		local.Options["DEV_CPU_SUBSCRIBE_RATIO"] = c.String()
		local.Options["TEST_CPU_SUBSCRIBE_RATIO"] = c.String()
		local.Options["STAGING_CPU_SUBSCRIBE_RATIO"] = c.String()
	}
	return nil
}

// deleteExecutor delete cluster executor
func (c *ClusterImpl) deleteExecutor(clusterName string) error {
	keys, err := c.FindKeysByCluster(clusterName)
	if err != nil {
		return err
	}

	for _, k := range keys {
		if err := c.js.Remove(context.Background(), k, nil); err != nil {
			content := fmt.Sprintf("delete cluster(%s, key: %s) from etcd error: %v", clusterName, k, err)
			return errors.New(content)
		}
	}

	return nil
}

func (c *ClusterImpl) handleEdasCluster(clusterEvent *apistructs.ClusterEvent) error {
	logrus.Infof("cluster hook of edas, name: %s", clusterEvent.Content.Name)

	if clusterEvent.Content.SchedConfig == nil {
		return errors.Wrap(ErrInvalidClusterType, clusterEvent.Content.Type)
	}

	if clusterEvent.Action == clusterActionCreate {
		return c.createEdasExecutor(clusterEvent)
	} else if clusterEvent.Action == clusterActionUpdate {
		return c.updateEdasExecutor(clusterEvent)
	} else if clusterEvent.Action == clusterActionDelete {
		return c.deleteExecutor(clusterEvent.Content.Name)
	}
	return nil
}

func (c *ClusterImpl) createEdasExecutor(clusterEvent *apistructs.ClusterEvent) error {
	serviceExecutorName := clusterutil.GenerateExecutorByCluster(clusterEvent.Content.Name, clusterutil.ServiceKindEdas)
	jobExecutorName := clusterutil.GenerateExecutorByCluster(clusterEvent.Content.Name, clusterutil.JobKindK8S)
	addonExecutorName := clusterutil.GenerateExecutorByCluster(clusterEvent.Content.Name, clusterutil.ServiceKindK8S)
	//write to edas service excutor
	svcExecutorPath := strutil.Concat(edasExecutorPrefix, clusterEvent.Content.Name, "/", clusterutil.ServiceKindMarathon)
	if err := createEdasExector(c.js, svcExecutorPath, serviceExecutorName); err != nil {
		logrus.Errorf("failed to create edas service executor, executor: %s", serviceExecutorName)
		return err
	}
	//write edas service configure
	serviceCfg := ClusterInfo{
		ClusterName:  clusterEvent.Content.Name,
		ExecutorName: serviceExecutorName,
		Kind:         clusterutil.ServiceKindEdas,
		Options: map[string]string{
			"ADDR":            clusterEvent.Content.SchedConfig.EdasConsoleAddr,
			"ACCESSKEY":       clusterEvent.Content.SchedConfig.AccessKey,
			"ACCESSSECRET":    clusterEvent.Content.SchedConfig.AccessSecret,
			"CLUSTERID":       clusterEvent.Content.SchedConfig.ClusterID,
			"REGIONID":        clusterEvent.Content.SchedConfig.RegionID,
			"LOGICALREGIONID": clusterEvent.Content.SchedConfig.LogicalRegionID,
			"REGADDR":         clusterEvent.Content.SchedConfig.RegAddr,
		},
	}

	svcPath := strutil.Concat(clusterPrefix, clusterEvent.Content.Name, clusterEdasSuffix)
	if err := create(c.js, svcPath, serviceCfg); err != nil {
		logrus.Errorf("failed to create edas service executor, cluster: %s", clusterEvent.Content.Name)
		return err
	}
	//wirte to edas job excutor
	jobExecutorPath := strutil.Concat(edasExecutorPrefix, clusterEvent.Content.Name, "/", clusterutil.JobKindMetronome)
	if err := createEdasExector(c.js, jobExecutorPath, jobExecutorName); err != nil {
		logrus.Errorf("failed to create edas service executor, executor: %s", jobExecutorName)
		return err
	}
	//write to edas job configure
	jobCfg := ClusterInfo{
		ClusterName:  clusterEvent.Content.Name,
		ExecutorName: jobExecutorName,
		Kind:         clusterutil.JobKindK8S,
		Options: map[string]string{
			"ADDR": clusterEvent.Content.SchedConfig.K8sAddr,
		},
	}
	setDefaultClusterConfig(&jobCfg)

	jobPath := strutil.Concat(clusterPrefix, clusterEvent.Content.Name, clusterK8SJobSuffix)
	if err := create(c.js, jobPath, jobCfg); err != nil {
		logrus.Errorf("failed to create edas job executor, cluster: %s", clusterEvent.Content.Name)
		return err
	}

	//write to edas addon excutor
	addonExecutorPath := strutil.Concat(edasExecutorPrefix, clusterEvent.Content.Name, "/", clusterutil.EdasKindInK8s)
	if err := createEdasExector(c.js, addonExecutorPath, addonExecutorName); err != nil {
		logrus.Errorf("failed to create edas service executor, executor: %s", addonExecutorName)
		return err
	}
	//write to edas addon configure
	addonCfg := ClusterInfo{
		ClusterName:  clusterEvent.Content.Name,
		ExecutorName: addonExecutorName,
		Kind:         clusterutil.ServiceKindK8S,
		Options: map[string]string{
			"ADDR":    clusterEvent.Content.SchedConfig.K8sAddr,
			"IS_EDAS": "true",
		},
	}
	setDefaultClusterConfig(&addonCfg)

	addonPath := strutil.Concat(clusterPrefix, clusterEvent.Content.Name, clusterK8SSuffix)
	if err := create(c.js, addonPath, addonCfg); err != nil {
		logrus.Errorf("failed to create edas addon executor, cluster: %s", clusterEvent.Content.Name)
		return err
	}

	logrus.Infof("succeed to create edas cluster, name: %s", clusterEvent.Content.Name)
	return nil
}

func (c *ClusterImpl) updateEdasExecutor(clusterEvent *apistructs.ClusterEvent) error {
	keys, err := c.FindKeysByCluster(clusterEvent.Content.Name)
	if err != nil {
		return err
	}
	for _, k := range keys {
		cluster := ClusterInfo{}
		if err := c.js.Get(context.Background(), k, &cluster); err != nil {
			content := fmt.Sprintf("get cluster(%s, key: %s) from etcd error: %v", clusterEvent.Content.Name, k, err)
			return errors.New(content)
		}
		var pErr error
		if cluster.Kind == clusterutil.ServiceKindEdas {
			pErr = patchEdasConfig(&cluster, &(clusterEvent.Content))
		} else {
			pErr = patchK8SConfig(&cluster, &(clusterEvent.Content))
		}
		if pErr != nil {
			content := fmt.Sprintf("parse cluster(%s, key: %s) url error: %v", clusterEvent.Content.Name, k, err)
			return errors.New(content)
		}
		if err := c.js.Put(context.Background(), k, cluster); err != nil {
			content := fmt.Sprintf("patch cluster(%s, key: %s) to etcd error: %v", clusterEvent.Content.Name, k, err)
			return errors.New(content)
		}
	}
	return nil
}

// update edas executor
func patchEdasConfig(local *ClusterInfo, request *apistructs.ClusterInfo) error {
	if request.SchedConfig != nil {
		local.Options["ADDR"] = request.SchedConfig.EdasConsoleAddr
		local.Options["ACCESSKEY"] = request.SchedConfig.AccessKey
		local.Options["ACCESSSECRET"] = request.SchedConfig.AccessSecret
		local.Options["CLUSTERID"] = request.SchedConfig.ClusterID
		local.Options["REGIONID"] = request.SchedConfig.RegionID
		local.Options["LOGICALREGIONID"] = request.SchedConfig.LogicalRegionID
	}
	return nil
}
