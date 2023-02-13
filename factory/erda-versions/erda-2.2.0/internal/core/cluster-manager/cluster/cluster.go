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
	"encoding/json"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/cluster-manager/cluster/db"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateWithEvent create cluster with event request
func (c *ClusterService) CreateWithEvent(req *pb.CreateClusterRequest) error {
	cluster, err := c.db.GetClusterByName(req.Name)
	if err != nil {
		return err
	}
	if cluster != nil {
		return nil
	}

	if err = c.Create(req); err != nil {
		return err
	}

	clusterInfo, err := c.Get(req.Name)
	if err != nil {
		return err
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:     bundle.ClusterEvent,
			Action:    bundle.CreateAction,
			OrgID:     "-1",
			ProjectID: "-1",
			TimeStamp: time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderClusterManager,
		Content: clusterInfo,
	}

	if err = c.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send cluster create event, (%v)", err)
		return nil
	}

	return nil
}

// Create creates cluster
func (c *ClusterService) Create(req *pb.CreateClusterRequest) error {
	// validate param
	if err := c.checkCreateParam(req); err != nil {
		return err
	}

	if req.SysConfig != nil {
		switch req.Type {
		case apistructs.K8S:
			if req.SysConfig.MainPlatform != nil {
				req.SchedulerConfig.MasterURL = strutil.Concat("inet://", req.WildcardDomain,
					"/insecure-kubernetes.default.svc.cluster.local")
			} else {
				req.SchedulerConfig.MasterURL = "http://insecure-kubernetes.default.svc.cluster.local"
			}
		case apistructs.DCOS:
			if req.SysConfig.MainPlatform != nil {
				req.SchedulerConfig.MasterURL = strutil.Concat("inet://", req.WildcardDomain, "/master.mesos")
			} else {
				req.SchedulerConfig.MasterURL = "http://master.mesos"
			}
		}
	}

	// parse json store
	sysConfig, err := json.MarshalIndent(req.SysConfig, "", "\t")
	if err != nil {
		return err
	}

	schedulerConfig, err := json.MarshalIndent(req.SchedulerConfig, "", "\t")
	if err != nil {
		return err
	}

	opsConfig, err := json.MarshalIndent(req.OpsConfig, "", "\t")
	if err != nil {
		return err
	}

	manageConfig, err := json.MarshalIndent(req.ManageConfig, "", "\t")
	if err != nil {
		return err
	}

	cluster := &db.Cluster{
		OrgID:           uint64(req.OrgID),
		Name:            req.Name,
		DisplayName:     req.DisplayName,
		Description:     req.Description,
		Type:            req.Type,
		CloudVendor:     req.CloudVendor,
		Logo:            req.Logo,
		WildcardDomain:  req.WildcardDomain,
		SysConfig:       string(sysConfig),
		SchedulerConfig: string(schedulerConfig),
		OpsConfig:       string(opsConfig),
		ManageConfig:    string(manageConfig),
	}

	if err = c.db.CreateCluster(cluster); err != nil {
		return err
	}

	return nil
}

// UpdateWithEvent update cluster & sender cluster update event
func (c *ClusterService) UpdateWithEvent(req *pb.UpdateClusterRequest) error {
	if err := c.Update(req); err != nil {
		return err
	}

	cluster, err := c.Get(req.Name)
	if err != nil {
		return err
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:     bundle.ClusterEvent,
			Action:    bundle.UpdateAction,
			OrgID:     "-1",
			ProjectID: "-1",
			TimeStamp: time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderClusterManager,
		Content: cluster,
	}

	if err = c.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send cluster update event, (%v)", err)
		return nil
	}

	return nil
}

// Update update cluster
func (c *ClusterService) Update(req *pb.UpdateClusterRequest) error {
	cluster, err := c.db.GetClusterByName(req.Name)
	if err != nil {
		return err
	}
	if cluster == nil {
		return errors.Errorf("not found")
	}
	logrus.Infof("before updated cluster info: %+v", cluster)

	cluster.DisplayName = req.DisplayName
	if req.Type != "" {
		cluster.Type = req.Type
	}
	cluster.Logo = req.Logo
	cluster.Description = req.Description
	if req.WildcardDomain != "" {
		cluster.WildcardDomain = req.WildcardDomain
	}

	if req.SchedulerConfig != nil {
		schedulerConfig, err := json.MarshalIndent(req.SchedulerConfig, "", "\t")
		if err != nil {
			return err
		}
		cluster.SchedulerConfig = string(schedulerConfig)
	}
	if req.OpsConfig != nil {
		opsConfig, err := json.MarshalIndent(req.OpsConfig, "", "\t")
		if err != nil {
			return err
		}
		cluster.OpsConfig = string(opsConfig)
	}
	if req.SysConfig != nil {
		var newSysConfig *pb.SysConf
		if err := json.Unmarshal([]byte(cluster.SysConfig), &newSysConfig); err != nil {
			return err
		}
		// Check field which change disabled
		if err := c.diffSysConfig(req.SysConfig, newSysConfig); err != nil {
			return err
		}
		sysConfig, err := json.MarshalIndent(req.SysConfig, "", "\t")
		if err != nil {
			return err
		}
		cluster.SysConfig = string(sysConfig)
	}
	if req.ManageConfig != nil {
		manageConfig, err := json.MarshalIndent(req.ManageConfig, "", "\t")
		if err != nil {
			return err
		}
		cluster.ManageConfig = string(manageConfig)
	}
	if req.CloudVendor != "" {
		cluster.CloudVendor = req.CloudVendor
	}

	if err = c.db.UpdateCluster(cluster); err != nil {
		return err
	}

	return nil
}

// PatchWithEvent patch with event
func (c *ClusterService) PatchWithEvent(req *pb.PatchClusterRequest) error {
	cluster, err := c.db.GetClusterByName(req.Name)
	if err != nil {
		return err
	}
	if cluster == nil {
		return errors.Errorf("not found")
	}

	if req.ManageConfig == nil {
		return nil
	}

	cCluster := c.convert(cluster)

	if req.ManageConfig.CredentialSource == "" {
		req.ManageConfig.CredentialSource = cCluster.ManageConfig.CredentialSource
	}

	manageConfig, err := json.MarshalIndent(req.ManageConfig, "", "\t")
	if err != nil {
		return err
	}
	cluster.ManageConfig = string(manageConfig)

	if err = c.db.UpdateCluster(cluster); err != nil {
		return err
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:     bundle.ClusterEvent,
			Action:    bundle.UpdateAction,
			OrgID:     "-1",
			ProjectID: "-1",
			TimeStamp: time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderClusterManager,
		Content: cCluster,
	}

	if err = c.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send cluster update event, (%v)", err)
		return nil
	}

	return nil
}

// DeleteWithEvent delete cluster with delete event
func (c *ClusterService) DeleteWithEvent(clusterName string) error {
	cluster, err := c.Get(clusterName)
	if err != nil {
		return ErrDeleteCluster.InternalError(err)
	}
	if cluster == nil {
		return errors.Errorf("not found")
	}

	logrus.Infof("deleting cluster: %+v", cluster)

	if err := c.DeleteByName(clusterName); err != nil {
		return ErrDeleteCluster.InternalError(err)
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:     bundle.ClusterEvent,
			Action:    bundle.DeleteAction,
			OrgID:     "-1",
			ProjectID: "-1",
			TimeStamp: time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderClusterManager,
		Content: cluster,
	}

	if err = c.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("[alert]failed to send cluster delete event, (%v)", err)
	}

	return nil
}

// DeleteByName delete cluster by name
func (c *ClusterService) DeleteByName(clusterName string) error {
	return c.db.DeleteCluster(clusterName)
}

// Get gets *apistructs.ClusterInfo by name
func (c *ClusterService) Get(idOrName string) (*pb.ClusterInfo, error) {
	var cluster *db.Cluster
	clusterID, err := strutil.Atoi64(idOrName)
	if err == nil {
		cluster, err = c.db.GetClusterByID(clusterID)
	} else {
		cluster, err = c.db.GetClusterByName(idOrName)
	}
	if err != nil {
		return nil, err
	}
	if cluster == nil {
		return nil, errors.Errorf("not found")
	}
	return c.convert(cluster), nil
}

// List lists all cluster
func (c *ClusterService) List() ([]*pb.ClusterInfo, error) {
	clusters, err := c.db.ListCluster()
	if err != nil {
		return nil, err
	}
	clusterInfos := make([]*pb.ClusterInfo, 0, len(*clusters))
	for i := range *clusters {
		item := c.convert(&(*clusters)[i])
		// TODO: Deprecated at future version.
		if item.SchedConfig != nil {
			item.SchedConfig.AuthPassword = ""
			item.SchedConfig.ClientCrt = ""
			item.SchedConfig.CaCrt = ""
			item.SchedConfig.ClientKey = ""
			if item.SchedConfig.CpuSubscribeRatio == "" {
				item.SchedConfig.CpuSubscribeRatio = "1"
			}
		}

		clusterInfos = append(clusterInfos, item)
	}

	return clusterInfos, nil
}

func (c *ClusterService) ListClusterByType(clusterType string) ([]*pb.ClusterInfo, error) {
	var clusterList []*pb.ClusterInfo

	clusters, err := c.db.ListClusterByType(clusterType)
	if err != nil {
		return nil, err
	}

	clusterList = make([]*pb.ClusterInfo, 0, len(*clusters))
	// TODO: Deprecated at future version.
	for i := range *clusters {
		item := c.convert(&(*clusters)[i])
		// TODO: Deprecated at future version.
		if item.SchedConfig != nil {
			item.SchedConfig.AuthPassword = ""
			item.SchedConfig.ClientCrt = ""
			item.SchedConfig.CaCrt = ""
			item.SchedConfig.ClientKey = ""
			if item.SchedConfig.CpuSubscribeRatio == "" {
				item.SchedConfig.CpuSubscribeRatio = "1"
			}
		}

		clusterList = append(clusterList, item)
	}

	return clusterList, nil
}

// checkCreateParam check create param
func (c *ClusterService) checkCreateParam(req *pb.CreateClusterRequest) error {
	if req.Name == "" {
		return errors.Errorf("name is empty")
	}

	switch req.Type {
	case apistructs.DCOS, apistructs.EDAS, apistructs.K8S:
	default:
		return errors.Errorf("type is invalid")
	}
	return nil
}

func (c *ClusterService) diffSysConfig(new, old *pb.SysConf) error {
	if new.Cluster.Name != old.Cluster.Name {
		return errors.Errorf("cluster name mismatch")
	}
	if new.Fps.Host != old.Fps.Host {
		return errors.Errorf("fps host mismatch")
	}
	if new.Platform.WildcardDomain != old.Platform.WildcardDomain {
		return errors.Errorf("wildcard domain mismatch")
	}
	if new.Storage.MountPoint != old.Storage.MountPoint {
		return errors.New("nas mount point mismatch")
	}
	if new.Docker.DataRoot != old.Docker.DataRoot {
		return errors.New("docker data root mismatch")
	}
	newNodes := make(map[string]string, len(new.Nodes))
	for _, node := range new.Nodes {
		newNodes[node.Ip] = node.Type
	}
	for _, node := range old.Nodes {
		if _, ok := newNodes[node.Ip]; !ok {
			return errors.New("nodes info mismatch")
		}
	}
	return nil
}

// convert cluster model to *pb.ClusterInfo
func (c *ClusterService) convert(cluster *db.Cluster) *pb.ClusterInfo {
	var (
		schedulerConfig *pb.ClusterSchedConfig
		opsConfig       *pb.OpsConfig
		manageConfig    *pb.ManageConfig
		sysConfig       *pb.SysConf
		// Deprecated at 1.2
		urls = make(map[string]string)
		cm   = make(map[string]string)
	)

	if cluster.SysConfig != "" {
		if err := json.Unmarshal([]byte(cluster.SysConfig), &sysConfig); err != nil {
			logrus.Warnf("failed to unmarshal sysConfig, (%v)", err)
		}
	}

	if cluster.ManageConfig != "" {
		if err := json.Unmarshal([]byte(cluster.ManageConfig), &manageConfig); err != nil {
			logrus.Warnf("failed to unmarshal manageConfig, (%v)", err)
		}
	}

	if cluster.SchedulerConfig != "" {
		if err := json.Unmarshal([]byte(cluster.SchedulerConfig), &schedulerConfig); err != nil {
			logrus.Warnf("failed to unmarshal schedulerConfig, %v", err)
		}
	}

	if cluster.OpsConfig != "" {
		if err := json.Unmarshal([]byte(cluster.OpsConfig), &opsConfig); err != nil {
			logrus.Warnf("failed to unmarshal opsConfig, (%v)", err)
		}
	}
	// TODO: Deprecated at 1.2, use for edas soldier 1.1 version
	if cluster.URLs != "" {
		if err := json.Unmarshal([]byte(cluster.URLs), &urls); err != nil {
			logrus.Warnf("failed to unmarshal urls, (%v)", err)
		}
	}
	if cluster.ClusterInfo != "" {
		if err := json.Unmarshal([]byte(cluster.ClusterInfo), &cm); err != nil {
			logrus.Warnf("failed to unmarshal clusterInfo, (%v)", err)
		}
	}

	return &pb.ClusterInfo{
		Id:             int32(cluster.ID),
		Name:           cluster.Name,
		DisplayName:    cluster.DisplayName,
		Description:    cluster.Description,
		Type:           cluster.Type,
		CloudVendor:    cluster.CloudVendor,
		Logo:           cluster.Logo,
		WildcardDomain: cluster.WildcardDomain,
		SchedConfig:    schedulerConfig,
		System:         sysConfig,
		OpsConfig:      opsConfig,
		ManageConfig:   manageConfig,
		Urls:           urls,
		CreatedAt:      timestamppb.New(cluster.CreatedAt),
		UpdatedAt:      timestamppb.New(cluster.UpdatedAt),
		Cm:             cm,
	}
}
