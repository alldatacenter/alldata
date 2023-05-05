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

package servicegroup

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/task"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/k8s/storage"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	runtimeNameFormat                = `^[a-zA-Z0-9\-]+$`
	runtimeFormater   *regexp.Regexp = regexp.MustCompile(runtimeNameFormat)

	LastRestartTimeKey      = "lastRestartTime"
	LastConfigUpdateTimeKey = "lastConfigUpdateTime"
)

type Namespace string

type ServiceGroup interface {
	Create(sg apistructs.ServiceGroupCreateV2Request) (apistructs.ServiceGroup, error)
	Update(sg apistructs.ServiceGroupUpdateV2Request) (apistructs.ServiceGroup, error)
	Restart(namespace string, name string) error
	Cancel(namespace string, name string) error
	Delete(namespace string, name, force string) error
	Info(ctx context.Context, namespace string, name string) (apistructs.ServiceGroup, error)
	Precheck(sg apistructs.ServiceGroupPrecheckRequest) (apistructs.ServiceGroupPrecheckData, error)
	ConfigUpdate(sg apistructs.ServiceGroup) error
	KillPod(ctx context.Context, namespace string, name string, podname string) error
	Scale(sg *apistructs.ServiceGroup) (apistructs.ServiceGroup, error)
	InspectServiceGroupWithTimeout(namespace, name string) (*apistructs.ServiceGroup, error)
}

type ServiceGroupImpl struct {
	Js          jsonstore.JsonStore
	Sched       *task.Sched
	Clusterinfo clusterinfo.ClusterInfo
}

func NewServiceGroupImplInit() ServiceGroup {
	sched, err := task.NewSched()
	if err != nil {
		panic(err)
	}

	js, err := jsonstore.New()
	if err != nil {
		panic(err)
	}
	clusterinfoImpl := clusterinfo.NewClusterInfoImpl(js)
	return &ServiceGroupImpl{
		Js:          js,
		Sched:       sched,
		Clusterinfo: clusterinfoImpl,
	}
}

func NewServiceGroupImpl(js jsonstore.JsonStore, sched *task.Sched, clusterinfo clusterinfo.ClusterInfo) ServiceGroup {
	return &ServiceGroupImpl{js, sched, clusterinfo}
}

func (s ServiceGroupImpl) handleKillPod(ctx context.Context, sg *apistructs.ServiceGroup, containerID string) (task.TaskResponse, error) {
	var (
		result task.TaskResponse
		err    error
	)
	if err = setServiceGroupExecutorByCluster(sg, s.Clusterinfo); err != nil {
		return result, err
	}
	t, err := s.Sched.Send(ctx, task.TaskRequest{
		ExecutorKind: getServiceExecutorKindByName(sg.Executor),
		ExecutorName: sg.Executor,
		Action:       task.TaskKillPod,
		ID:           sg.ID,
		Spec:         containerID,
	})
	if err != nil {
		return result, err
	}
	if result = t.Wait(ctx); result.Err() != nil {
		return result, result.Err()
	}
	return result, nil
}

func (s ServiceGroupImpl) handleServiceGroup(ctx context.Context, sg *apistructs.ServiceGroup, taskAction task.Action) (task.TaskResponse, error) {
	var (
		result task.TaskResponse
		err    error
	)
	if err = setServiceGroupExecutorByCluster(sg, s.Clusterinfo); err != nil {
		return result, err
	}

	t, err := s.Sched.Send(ctx, task.TaskRequest{
		ExecutorKind: getServiceExecutorKindByName(sg.Executor),
		ExecutorName: sg.Executor,
		Action:       taskAction,
		ID:           sg.ID,
		Spec:         *sg,
	})
	if err != nil {
		return result, err
	}
	if result = t.Wait(ctx); result.Err() != nil {
		return result, result.Err()
	}
	return result, nil

}

///////////////////////////////////////////////////////////////////////////////
//                   util funcs
//  TODO: Delete related code such as marathon in the following code
///////////////////////////////////////////////////////////////////////////////

func getServiceExecutorKindByName(name string) string {
	e, err := executor.GetManager().Get(executortypes.Name(name))
	if err != nil {
		return conf.DefaultRuntimeExecutor()
	}
	return string(e.Kind())
}
func mkServiceGroupKey(namespace, name string) string {
	return filepath.Join("/dice/service", namespace, name)
}
func validateServiceGroupName(name string) bool {
	return len(name) > 0 && runtimeFormater.MatchString(name)
}
func validateServiceGroupNamespace(namespace string) bool {
	return len(namespace) > 0 && runtimeFormater.MatchString(namespace)
}

// When the executor of the servicegroup is empty, set the executor according to the cluster value
func setServiceGroupExecutorByCluster(sg *apistructs.ServiceGroup, clusterinfo clusterinfo.ClusterInfo) error {
	if len(sg.Executor) > 0 {
		return nil
	}
	if len(sg.ClusterName) == 0 {
		return errors.Errorf("servicegroup(%s/%s) neither executor nor cluster is set", sg.Type, sg.ID)
	}

	// Determine whether it is edas deployment addon scenario
	isEdasStatefull := isEdasStatefull(sg, clusterinfo)

	if isEdasStatefull {
		sg.Executor = clusterutil.GenerateExecutorByCluster(sg.ClusterName, clusterutil.EdasKindInK8s)
	} else {
		sg.Executor = clusterutil.GenerateExecutorByCluster(sg.ClusterName, clusterutil.ServiceKindMarathon)
	}
	logrus.Infof("generate executor(%s) for servicegroup(%s) in cluster(%s)", sg.Executor, sg.ID, sg.ClusterName)
	return nil
}

func isEdasStatefull(sg *apistructs.ServiceGroup, clusterinfo clusterinfo.ClusterInfo) bool {
	info, err := clusterinfo.Info(sg.ClusterName)
	if err != nil {
		logrus.Errorf("failed to get cluster info, clusterName: %s, (%v)", sg.ClusterName, err)
		return false
	}

	//Set the executor of edasink8s when the cluster type is edas and it is an addon type service
	if info.Get(apistructs.DICE_CLUSTER_TYPE) == "edas" {
		_, ok := sg.Labels["USE_OPERATOR"]
		if ok {
			return true
		}
		if sg.Labels["SERVICE_TYPE"] == "ADDONS" {
			return true
		}
	}
	return false
}

func convertServiceGroup(req apistructs.ServiceGroupCreateV2Request, clusterinfo clusterinfo.ClusterInfo) (apistructs.ServiceGroup, error) {
	sg := apistructs.ServiceGroup{}
	sg.ClusterName = req.ClusterName
	sg.Force = true
	if !validateServiceGroupName(req.ID) || !validateServiceGroupNamespace(req.Type) {
		return apistructs.ServiceGroup{}, errors.New("invalid Name or Namespace")
	}

	// build match tags and exclude tags
	sg.Labels = appendServiceTags(sg.Labels, sg.Executor)

	sg.ID = req.ID
	sg.Type = req.Type
	sg.ProjectNamespace = req.ProjectNamespace
	sg.Labels = req.GroupLabels
	sg.ServiceDiscoveryMode = req.ServiceDiscoveryMode

	yml := req.DiceYml
	// expand global envs to service's envs
	diceyml.ExpandGlobalEnv(&yml)
	reqVolumesInfo := req.Volumes

	// override sg.Labels
	if sg.Labels == nil {
		sg.Labels = map[string]string{}
	}
	for k, v := range yml.Meta {
		sg.Labels[k] = v
	}
	// override sg.ServiceDiscoveryMode
	if mode, ok := yml.Meta["SERVICE_DISCOVERY_MODE"]; ok {
		sg.ServiceDiscoveryMode = strutil.ToUpper(mode)
	}

	sgServices := []apistructs.Service{}
	for name, service := range yml.Services {
		// check eci enabled
		enableECI := false
		for k, v := range service.Labels {
			if k == apistructs.AlibabaECILabel && v == "true" {
				enableECI = true
				break
			}
		}

		for k, v := range service.Deployments.Labels {
			if k == apistructs.AlibabaECILabel && v == "true" {
				enableECI = true
				break
			}
		}

		volumes := []apistructs.Volume{}
		// binds only for hostPath volume
		binds := []apistructs.ServiceBind{}
		ymlbinds, err := diceyml.ParseBinds(service.Binds)
		if err != nil {
			return apistructs.ServiceGroup{}, err
		}
		for _, bind := range ymlbinds {
			ro := true
			if bind.Type == "rw" {
				ro = false
			}

			// 卷映射的容器目录合法性检查
			if bind.ContainerPath == "" || bind.ContainerPath == "/" {
				return apistructs.ServiceGroup{}, errors.New(fmt.Sprintf("invalid bind container path [%s]", bind.ContainerPath))
			}

			binds = append(binds, apistructs.ServiceBind{
				Bind: apistructs.Bind{
					ContainerPath: bind.ContainerPath,
					HostPath:      bind.HostPath,
					ReadOnly:      ro,
				},
			})
		}

		// hostPath not supported by ECI Pod
		if len(binds) > 0 && enableECI {
			logrus.Errorf("Service has Binds(hostpath) can not running as ECI Pod\n")
			return apistructs.ServiceGroup{}, errors.New("Service has Binds(hostpath) can not running as ECI Pod")
		}

		volumeInfo, ok := reqVolumesInfo[name]
		if ok {
			var tp apistructs.VolumeType
			var err error
			if volumeInfo.Type == "" {
				tp = apistructs.LocalVolume
			} else {
				tp, err = apistructs.VolumeTypeFromString(volumeInfo.Type)
				if err != nil {
					return apistructs.ServiceGroup{}, fmt.Errorf("bad volume type: %v", volumeInfo.Type)

				}
			}
			// 卷映射的容器目录合法性检查
			if volumeInfo.ContainerPath == "" || volumeInfo.ContainerPath == "/" {
				return apistructs.ServiceGroup{}, errors.New(fmt.Sprintf("invalid ServiceGroupCreateV2Request RequestVolumeInfo volume container path [%s]", volumeInfo.ContainerPath))
			}

			volumes = append(volumes, apistructs.Volume{
				ID:            volumeInfo.ID,
				VolumeType:    tp,
				Size:          10,
				ContainerPath: volumeInfo.ContainerPath,
			})
		}

		// 从  diceYaml 的 Service 里读取 volumes 配置用于更新 ServiceGroup 里的 Service 的 volumes
		vs, err := setServiceVolumes(sg.ClusterName, service.Volumes, clusterinfo, enableECI)
		if err != nil {
			return apistructs.ServiceGroup{}, fmt.Errorf("set service %s volumes failed: %v", name, err)

		}
		volumes = append(volumes, vs...)

		sgService := apistructs.Service{
			Name:          name,
			Image:         service.Image,
			ImageUsername: service.ImageUsername,
			ImagePassword: service.ImagePassword,
			Cmd:           service.Cmd,
			Ports:         service.Ports,
			Scale:         service.Deployments.Replicas,
			Resources: apistructs.Resources{
				Cpu:                      service.Resources.CPU,
				MaxCPU:                   service.Resources.MaxCPU,
				Mem:                      float64(service.Resources.Mem),
				MaxMem:                   float64(service.Resources.MaxMem),
				Disk:                     float64(service.Resources.Disk),
				EmptyDirCapacity:         service.Resources.EmptyDirCapacity,
				EphemeralStorageCapacity: service.Resources.EphemeralStorageCapacity,
			},
			Depends:          service.DependsOn,
			Env:              service.Envs,
			Labels:           service.Labels,
			Selectors:        service.Deployments.Selectors,
			WorkLoad:         service.Deployments.Workload,
			DeploymentLabels: service.Deployments.Labels,
			Binds:            binds,
			Volumes:          volumes,
			Hosts:            service.Hosts,
			NewHealthCheck:   convertHealthcheck(service.HealthCheck),
			SideCars:         service.SideCars,
			InitContainer:    service.Init,
			MeshEnable:       service.MeshEnable,
			TrafficSecurity:  service.TrafficSecurity,
			K8SSnippet:       service.K8SSnippet,
		}
		sgServices = append(sgServices, sgService)
	}
	for name, job := range yml.Jobs {
		// check eci enabled
		enableECI := false
		for k, v := range job.Labels {
			if k == apistructs.AlibabaECILabel && v == "true" {
				enableECI = true
				break
			}
		}

		volumes := []apistructs.Volume{}
		// binds only for hostPath volume
		binds := []apistructs.ServiceBind{}
		ymlbinds, err := diceyml.ParseBinds(job.Binds)
		if err != nil {
			return apistructs.ServiceGroup{}, err
		}
		for _, bind := range ymlbinds {
			ro := true
			if bind.Type == "rw" {
				ro = false
			}

			// 卷映射的容器目录合法性检查
			if bind.ContainerPath == "" || bind.ContainerPath == "/" {
				return apistructs.ServiceGroup{}, errors.New(fmt.Sprintf("invalid bind container path [%s]", bind.ContainerPath))
			}

			binds = append(binds, apistructs.ServiceBind{
				Bind: apistructs.Bind{
					ContainerPath: bind.ContainerPath,
					HostPath:      bind.HostPath,
					ReadOnly:      ro,
				},
			})
		}

		// hostPath not supported by ECI Pod
		if len(binds) > 0 && enableECI {
			logrus.Errorf("Service has Binds(hostpath) can not running as ECI Pod\n")
			return apistructs.ServiceGroup{}, errors.New("Service has Binds(hostpath) can not running as ECI Pod")
		}

		volumeInfo, ok := reqVolumesInfo[name]
		if ok {
			var tp apistructs.VolumeType
			var err error
			if volumeInfo.Type == "" {
				tp = apistructs.LocalVolume
			} else {
				tp, err = apistructs.VolumeTypeFromString(volumeInfo.Type)
				if err != nil {
					return apistructs.ServiceGroup{}, fmt.Errorf("bad volume type: %v", volumeInfo.Type)

				}
			}
			// 卷映射的容器目录合法性检查
			if volumeInfo.ContainerPath == "" || volumeInfo.ContainerPath == "/" {
				return apistructs.ServiceGroup{}, errors.New(fmt.Sprintf("invalid ServiceGroupCreateV2Request RequestVolumeInfo volume container path [%s]", volumeInfo.ContainerPath))
			}

			volumes = append(volumes, apistructs.Volume{
				ID:            volumeInfo.ID,
				VolumeType:    tp,
				Size:          10,
				ContainerPath: volumeInfo.ContainerPath,
			})
		}

		// 从  diceYaml 的 Service 里读取 volumes 配置用于更新 ServiceGroup 里的 Service 的 volumes
		vs, err := setServiceVolumes(sg.ClusterName, job.Volumes, clusterinfo, enableECI)
		if err != nil {
			return apistructs.ServiceGroup{}, fmt.Errorf("set service %s volumes failed: %v", name, err)

		}
		volumes = append(volumes, vs...)

		sgService := apistructs.Service{
			Name:  name,
			Image: job.Image,
			Cmd:   job.Cmd,
			Resources: apistructs.Resources{
				Cpu:                      job.Resources.CPU,
				MaxCPU:                   job.Resources.MaxCPU,
				Mem:                      float64(job.Resources.Mem),
				MaxMem:                   float64(job.Resources.MaxMem),
				Disk:                     float64(job.Resources.Disk),
				EmptyDirCapacity:         job.Resources.EmptyDirCapacity,
				EphemeralStorageCapacity: job.Resources.EphemeralStorageCapacity,
			},
			Env:           job.Envs,
			WorkLoad:      "JOB",
			Labels:        job.Labels,
			Binds:         binds,
			Volumes:       volumes,
			Hosts:         job.Hosts,
			InitContainer: job.Init,
		}
		sgServices = append(sgServices, sgService)
	}
	sg.Services = sgServices
	if err := setServiceGroupExecutorByCluster(&sg, clusterinfo); err != nil {
		return apistructs.ServiceGroup{}, err
	}
	return sg, nil
}
func convertHealthcheck(hc diceyml.HealthCheck) *apistructs.NewHealthCheck {
	nhc := apistructs.NewHealthCheck{}
	if hc.HTTP != nil && hc.HTTP.Port != 0 && hc.HTTP.Path != "" {
		nhc.HttpHealthCheck = &apistructs.HttpHealthCheck{
			Port:     hc.HTTP.Port,
			Path:     hc.HTTP.Path,
			Duration: hc.HTTP.Duration,
		}
	}
	if hc.Exec != nil && hc.Exec.Cmd != "" {
		nhc.ExecHealthCheck = &apistructs.ExecHealthCheck{
			Cmd:      hc.Exec.Cmd,
			Duration: hc.Exec.Duration,
		}
	}
	return &nhc
}

func appendServiceTags(labels map[string]string, executor string) map[string]string {
	matchTags := make([]string, 0)
	if labels["SERVICE_TYPE"] == "STATELESS" {
		matchTags = append(matchTags, apistructs.TagServiceStateless)
	} else if executor == "MARATHONFORTERMINUSY" && labels["SERVICE_TYPE"] == "ADDONS" {
		matchTags = append(matchTags, apistructs.TagServiceStateful)
	}
	if labels == nil {
		labels = make(map[string]string)
	}
	labels[apistructs.LabelMatchTags] = strings.Join(matchTags, ",")
	labels[apistructs.LabelExcludeTags] = apistructs.TagLocked + "," + apistructs.TagPlatform
	return labels
}

func setServiceVolumes(clusterName string, vs diceyml.Volumes, clusterinfo clusterinfo.ClusterInfo, enableECI bool) ([]apistructs.Volume, error) {
	volumes := []apistructs.Volume{}

	// TODO: 集群里加入 CLUSTER_SC_VENDOR 信息
	info, err := clusterinfo.Info(clusterName)
	if err != nil {
		logrus.Errorf("failed to get cluster info, clusterName: %s, (%v)", clusterName, err)
		return []apistructs.Volume{}, errors.Errorf("failed to get cluster info, clusterName: %s, (%v)", clusterName, err)
	}

	for i, v := range vs {
		// TODO:   oldVolumeType  may not needed
		oldVolumeType := apistructs.NasVolume
		snap := int32(0)
		if v.Snapshot != nil && v.Snapshot.MaxHistory > snap {
			snap = v.Snapshot.MaxHistory
		}

		cloudProvisioner := info.Get(apistructs.CLUSTER_SC_VENDOR)
		if cloudProvisioner == "" {
			cloudProvisioner = os.Getenv("CLOUD_PROVISIONER")
		}

		scName := apistructs.DiceNFSVolumeSC
		if v.Storage != "" {
			switch v.Storage {
			case "local":
				scName = apistructs.DiceLocalVolumeSC
			case "nfs":
				scName = apistructs.DiceNFSVolumeSC
			default:
				logrus.Errorf("can not detect storageclass for volume: storage %s invalid", v.Storage)
				return []apistructs.Volume{}, errors.Errorf("can not detect storageclass for volume: storage %s invalid", v.Storage)
			}
		} else {
			scName, err = storage.VolumeTypeToSCName(v.Type, cloudProvisioner)
			if err != nil {
				logrus.Errorf("can not detect storageclass for volume: %v", err)
				return []apistructs.Volume{}, errors.Errorf("can not detect storageclass for volume: %v", err)
			}
		}

		if enableECI {
			if scName != apistructs.AlibabaSSDSC && scName != apistructs.AlibabaNASSC {
				logrus.Errorf("can not create Service with ECI enabled for volumes use storageclass %s\n", scName)
				return []apistructs.Volume{}, errors.Errorf("can not create Service with ECI enabled for volumes use storageclass %s", scName)
			}
		}

		// 小于 等于 20 （包括负数，一般写错才有负数），修正为默认值
		if v.Capacity < diceyml.AddonVolumeSizeMin {
			v.Capacity = diceyml.AddonVolumeSizeMin
		}
		// 大于 32768，修正为 32768
		if v.Capacity >= diceyml.AddonVolumeSizeMax {
			v.Capacity = diceyml.AddonVolumeSizeMax
		}

		if v.Path != "" && v.TargetPath != "" && v.Path != v.TargetPath {
			return []apistructs.Volume{}, errors.New("if path and targetPath set in volume, they must same.")
		}
		if v.TargetPath == "" && v.Path != "" {
			v.TargetPath = v.Path
		}
		// 卷映射的容器目录合法性检查
		if v.TargetPath == "" || v.TargetPath == "/" {
			return []apistructs.Volume{}, errors.New(fmt.Sprintf("invalid targetPath [%s]", v.TargetPath))
		}

		if v.Type == "" {
			switch v.Storage {
			case "local":
				v.Type = apistructs.VolumeTypeDiceLOCAL
			default:
				v.Type = apistructs.VolumeTypeDiceNAS
			}
		}

		scVolume := apistructs.SCVolume{
			Type:             v.Type,
			StorageClassName: scName,
			Capacity:         v.Capacity,
			//SourcePath:       v.SourcePath,
			TargetPath: v.TargetPath,
			ReadOnly:   v.ReadOnly,
			Snapshot:   &apistructs.VolumeSnapshot{MaxHistory: snap},
		}
		volumes = append(volumes, apistructs.Volume{
			ID: fmt.Sprintf(strconv.Itoa(i)),
			//VolumePath: v.SourcePath,
			// TODO: oldVolumeType  may not needed
			VolumeType:    oldVolumeType,
			Size:          int(v.Capacity),
			ContainerPath: v.TargetPath,
			SCVolume:      scVolume,
		})
	}
	return volumes, nil
}
