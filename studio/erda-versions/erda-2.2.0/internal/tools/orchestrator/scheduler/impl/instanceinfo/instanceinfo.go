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

package instanceinfo

import (
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	insinfo "github.com/erda-project/erda/internal/tools/orchestrator/scheduler/instanceinfo"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

type InstanceInfo interface {
	QueryPod(QueryPodConditions) (apistructs.PodInfoDataList, error)
	QueryInstance(QueryInstanceConditions) (apistructs.InstanceInfoDataList, error)
	QueryService(QueryServiceConditions) (apistructs.ServiceInfoDataList, error)
}

type QueryPodConditions struct {
	Cluster         string
	OrgName         string
	OrgID           string
	ProjectName     string
	ProjectID       string
	ApplicationName string
	ApplicationID   string
	RuntimeName     string
	RuntimeID       string
	ServiceName     string
	Workspace       string
	ServiceType     string
	AddonID         string
	Phases          []string

	Limit int
}

type QueryInstanceConditions struct {
	Cluster             string
	OrgName             string
	OrgID               string
	ProjectName         string
	ProjectID           string
	ApplicationName     string
	EdgeApplicationName string
	EdgeSite            string
	ApplicationID       string
	RuntimeName         string
	RuntimeID           string
	ServiceName         string
	Workspace           string
	ContainerID         string
	ServiceType         string
	AddonID             string
	InstanceIP          string
	HostIP              string
	Phases              []string

	Limit int
}

type QueryServiceConditions struct {
	OrgName         string
	OrgID           string
	ProjectName     string
	ProjectID       string
	ApplicationName string
	ApplicationID   string
	RuntimeName     string
	RuntimeID       string
	ServiceName     string
	Workspace       string
	ServiceType     string
}

func (q *QueryPodConditions) IsEmpty() bool {
	return isempty(q.Cluster) &&
		isempty(q.OrgName) &&
		isempty(q.OrgID) &&
		isempty(q.ProjectName) &&
		isempty(q.ProjectID) &&
		isempty(q.ApplicationName) &&
		isempty(q.ApplicationID) &&
		isempty(q.RuntimeName) &&
		isempty(q.RuntimeID) &&
		isempty(q.ServiceName) &&
		isempty(q.Workspace) &&
		isempty(q.ServiceType) &&
		isempty(q.AddonID) &&
		len(q.Phases) == 0
}

func (q *QueryInstanceConditions) IsEmpty() bool {
	return isempty(q.Cluster) &&
		isempty(q.OrgName) &&
		isempty(q.OrgID) &&
		isempty(q.ProjectName) &&
		isempty(q.ProjectID) &&
		isempty(q.ApplicationName) &&
		isempty(q.ApplicationID) &&
		isempty(q.RuntimeName) &&
		isempty(q.RuntimeID) &&
		isempty(q.ServiceName) &&
		isempty(q.Workspace) &&
		isempty(q.ContainerID) &&
		isempty(q.InstanceIP) &&
		isempty(q.HostIP) &&
		isempty(q.ServiceType) &&
		isempty(q.AddonID) &&
		len(q.Phases) == 0
}

func (q *QueryServiceConditions) IsEmpty() bool {
	return isempty(q.OrgName) &&
		isempty(q.OrgID) &&
		isempty(q.ProjectName) &&
		isempty(q.ProjectID) &&
		isempty(q.ApplicationName) &&
		isempty(q.ApplicationID) &&
		isempty(q.RuntimeName) &&
		isempty(q.RuntimeID) &&
		isempty(q.ServiceName) &&
		isempty(q.Workspace) &&
		isempty(q.ServiceType)
}

type InstanceInfoImpl struct {
	db *insinfo.Client
}

func NewInstanceInfoImpl() *InstanceInfoImpl {
	return &InstanceInfoImpl{
		db: insinfo.New(dbengine.MustOpen()),
	}
}

func (i *InstanceInfoImpl) QueryPod(cond QueryPodConditions) (apistructs.PodInfoDataList, error) {
	if cond.IsEmpty() {
		return nil, fmt.Errorf("QueryPodCondition is empty")
	}
	r := i.db.PodReader()
	if !isempty(cond.Cluster) {
		r.ByCluster(cond.Cluster)
	}
	if !isempty(cond.OrgName) {
		r.ByOrgName(cond.OrgName)
	}
	if !isempty(cond.OrgID) {
		r.ByOrgID(cond.OrgID)
	}
	if !isempty(cond.ProjectName) {
		r.ByProjectName(cond.ProjectName)
	}
	if !isempty(cond.ProjectID) {
		r.ByProjectID(cond.ProjectID)
	}
	if !isempty(cond.ApplicationName) {
		r.ByApplicationName(cond.ApplicationName)
	}
	if !isempty(cond.ApplicationName) {
		r.ByApplicationName(cond.ApplicationName)
	}
	if !isempty(cond.ApplicationID) {
		r.ByApplicationID(cond.ApplicationID)
	}
	if !isempty(cond.RuntimeName) {
		r.ByRuntimeName(cond.RuntimeName)
	}
	if !isempty(cond.RuntimeID) {
		r.ByRuntimeID(cond.RuntimeID)
	}
	if !isempty(cond.ServiceName) {
		r.ByService(cond.ServiceName)
	}
	if !isempty(cond.Workspace) {
		r.ByWorkspace(cond.Workspace)
	}
	if !isempty(cond.ServiceType) {
		r.ByServiceType(cond.ServiceType)
	}
	if !isempty(cond.AddonID) {
		r.ByAddonID(cond.AddonID)
	}
	if len(cond.Phases) > 0 {
		r.ByPhases(cond.Phases...)
	}
	if cond.Limit == 0 {
		r.Limit(100)
	} else {
		r.Limit(cond.Limit)
	}
	pods, err := r.Do()
	if err != nil {
		return apistructs.PodInfoDataList{}, err
	}
	data := apistructs.PodInfoDataList{}
	for _, pod := range pods {
		data = append(data, apistructs.PodInfoData{
			Cluster:         pod.Cluster,
			Namespace:       pod.Namespace,
			Name:            pod.Name,
			OrgName:         pod.OrgName,
			OrgID:           pod.OrgID,
			ProjectName:     pod.ProjectName,
			ProjectID:       pod.ProjectID,
			ApplicationName: pod.ApplicationName,
			ApplicationID:   pod.ApplicationID,
			RuntimeName:     pod.RuntimeName,
			RuntimeID:       pod.RuntimeID,
			ServiceName:     pod.ServiceName,
			Workspace:       pod.Workspace,
			ServiceType:     pod.ServiceType,
			AddonID:         pod.AddonID,
			Uid:             pod.Uid,
			K8sNamespace:    pod.K8sNamespace,
			PodName:         pod.PodName,
			Phase:           string(pod.Phase),
			Message:         pod.Message,
			PodIP:           pod.PodIP,
			HostIP:          pod.HostIP,
			StartedAt:       pod.StartedAt,
			MemRequest:      pod.MemRequest,
			MemLimit:        pod.MemLimit,
			CpuRequest:      pod.CpuRequest,
			CpuLimit:        pod.CpuLimit,
		})
	}
	return data, nil
}

func (i *InstanceInfoImpl) QueryInstance(cond QueryInstanceConditions) (apistructs.InstanceInfoDataList, error) {
	if cond.IsEmpty() {
		return nil, fmt.Errorf("QueryInstanceCondition is empty")
	}
	r := i.db.InstanceReader()
	if !isempty(cond.Cluster) {
		r.ByCluster(cond.Cluster)
	}
	if !isempty(cond.OrgName) {
		r.ByOrgName(cond.OrgName)
	}
	if !isempty(cond.OrgID) {
		r.ByOrgID(cond.OrgID)
	}
	if !isempty(cond.ProjectName) {
		r.ByProjectName(cond.ProjectName)
	}
	if !isempty(cond.ProjectID) {
		r.ByProjectID(cond.ProjectID)
	}
	if !isempty(cond.ApplicationName) {
		r.ByApplicationName(cond.ApplicationName)
	}
	if !isempty(cond.ApplicationName) {
		r.ByApplicationName(cond.ApplicationName)
	}
	if !isempty(cond.ApplicationName) {
		r.ByApplicationName(cond.ApplicationName)
	}
	if !isempty(cond.EdgeApplicationName) {
		r.ByEdgeApplicationName(cond.EdgeApplicationName)
	}
	if !isempty(cond.EdgeSite) {
		r.ByEdgeSite(cond.EdgeSite)
	}
	if !isempty(cond.ApplicationID) {
		r.ByApplicationID(cond.ApplicationID)
	}
	if !isempty(cond.RuntimeName) {
		r.ByRuntimeName(cond.RuntimeName)
	}
	if !isempty(cond.RuntimeID) {
		r.ByRuntimeID(cond.RuntimeID)
	}
	if !isempty(cond.ServiceName) {
		r.ByService(cond.ServiceName)
	}
	if !isempty(cond.Workspace) {
		r.ByWorkspace(cond.Workspace)
	}
	if !isempty(cond.ContainerID) {
		r.ByContainerID(cond.ContainerID)
	}
	if !isempty(cond.ServiceType) {
		r.ByServiceType(cond.ServiceType)
	}
	if !isempty(cond.AddonID) {
		r.ByAddonID(cond.AddonID)
	}
	if !isempty(cond.InstanceIP) {
		r.ByInstanceIP(strutil.Split(cond.InstanceIP, ",", true)...)
	}
	if !isempty(cond.HostIP) {
		r.ByHostIP(strutil.Split(cond.HostIP, ",", true)...)
	}
	if len(cond.Phases) > 0 {
		r.ByPhases(cond.Phases...)
	}
	if cond.Limit == 0 {
		r.Limit(100)
	} else {
		r.Limit(cond.Limit)
	}
	ins, err := r.Do()
	if err != nil {
		return apistructs.InstanceInfoDataList{}, err
	}
	data := apistructs.InstanceInfoDataList{}
	for _, instance := range ins {
		taskid := instance.TaskID
		if taskid == apistructs.K8S {
			taskid = ""
		}
		data = append(data, apistructs.InstanceInfoData{
			Cluster:             instance.Cluster,
			Namespace:           instance.Namespace,
			Name:                instance.Name,
			OrgName:             instance.OrgName,
			OrgID:               instance.OrgID,
			ProjectName:         instance.ProjectName,
			ProjectID:           instance.ProjectID,
			ApplicationName:     instance.ApplicationName,
			EdgeApplicationName: instance.EdgeApplicationName,
			EdgeSite:            instance.EdgeSite,
			ApplicationID:       instance.ApplicationID,
			RuntimeName:         instance.RuntimeName,
			RuntimeID:           instance.RuntimeID,
			ServiceName:         instance.ServiceName,
			Workspace:           instance.Workspace,
			ServiceType:         instance.ServiceType,
			AddonID:             instance.AddonID,
			Meta:                instance.Meta,
			Phase:               string(instance.Phase),
			Message:             instance.Message,
			ContainerID:         instance.ContainerID,
			ContainerIP:         instance.ContainerIP,
			HostIP:              instance.HostIP,
			ExitCode:            instance.ExitCode,
			CpuOrigin:           instance.CpuOrigin,
			MemOrigin:           instance.MemOrigin,
			CpuRequest:          instance.CpuRequest,
			MemRequest:          instance.MemRequest,
			CpuLimit:            instance.CpuLimit,
			MemLimit:            instance.MemLimit,
			Image:               instance.Image,
			TaskID:              taskid,

			StartedAt:  instance.StartedAt,
			FinishedAt: instance.FinishedAt,
		})
	}
	return data, nil
}

func (i *InstanceInfoImpl) QueryService(cond QueryServiceConditions) (apistructs.ServiceInfoDataList, error) {
	if cond.IsEmpty() {
		return nil, fmt.Errorf("QueryServiceCondition is empty")
	}
	r := i.db.ServiceReader()
	if !isempty(cond.OrgName) {
		r.ByOrgName(cond.OrgName)
	}
	if !isempty(cond.OrgID) {
		r.ByOrgID(cond.OrgID)
	}
	if !isempty(cond.ProjectName) {
		r.ByProjectName(cond.ProjectName)
	}
	if !isempty(cond.ProjectID) {
		r.ByProjectID(cond.ProjectID)
	}
	if !isempty(cond.ApplicationName) {
		r.ByApplicationName(cond.ApplicationName)
	}
	if !isempty(cond.ApplicationID) {
		r.ByApplicationID(cond.ApplicationID)
	}
	if !isempty(cond.RuntimeName) {
		r.ByRuntimeName(cond.RuntimeName)
	}
	if !isempty(cond.RuntimeID) {
		r.ByRuntimeID(cond.RuntimeID)
	}
	if !isempty(cond.ServiceName) {
		r.ByService(cond.ServiceName)
	}
	if !isempty(cond.Workspace) {
		r.ByWorkspace(cond.Workspace)
	}
	if !isempty(cond.ServiceType) {
		r.ByServiceType(cond.ServiceType)
	}
	svcs, err := r.Do()
	if err != nil {
		return apistructs.ServiceInfoDataList{}, err
	}
	data := apistructs.ServiceInfoDataList{}
	for _, svc := range svcs {
		data = append(data, apistructs.ServiceInfoData{
			Cluster:         svc.Cluster,
			Namespace:       svc.Namespace,
			Name:            svc.Name,
			OrgName:         svc.OrgName,
			OrgID:           svc.OrgID,
			ProjectName:     svc.ProjectName,
			ProjectID:       svc.ProjectID,
			ApplicationName: svc.ApplicationName,
			ApplicationID:   svc.ApplicationID,
			RuntimeName:     svc.RuntimeName,
			RuntimeID:       svc.RuntimeID,
			ServiceName:     svc.ServiceName,
			Workspace:       svc.Workspace,
			ServiceType:     svc.ServiceType,
			Meta:            svc.Meta,
			Phase:           string(svc.Phase),
			Message:         svc.Message,
			StartedAt:       svc.StartedAt,
			FinishedAt:      svc.FinishedAt,
		})
	}
	return data, nil
}

func isempty(s string) bool {
	return s == ""
}

type ComponentInfo interface {
	Get() (apistructs.ComponentInfoDataList, error)
}

type ComponentInfoImpl struct {
	db *insinfo.Client
}

func NewComponentInfoImpl() *ComponentInfoImpl {
	return &ComponentInfoImpl{
		db: insinfo.New(dbengine.MustOpen()),
	}
}

func (c *ComponentInfoImpl) Get() (apistructs.ComponentInfoDataList, error) {
	r := c.db.InstanceReader()
	instances, err := r.ByMetaLike("dice_component=").Do()
	if err != nil {
		return apistructs.ComponentInfoDataList{}, err
	}
	result := apistructs.ComponentInfoDataList{}
	for _, ins := range instances {
		insInfo := apistructs.ComponentInfoData{}
		name, ok := ins.Metadata("dice_component")
		if !ok {
			continue
		}

		insInfo.Cluster = ins.Cluster
		insInfo.ComponentName = name
		insInfo.Phase = string(ins.Phase)
		insInfo.Message = ins.Message
		insInfo.ContainerID = ins.ContainerID
		insInfo.ContainerIP = ins.ContainerIP
		insInfo.HostIP = ins.HostIP
		insInfo.ExitCode = ins.ExitCode
		insInfo.CpuOrigin = ins.CpuOrigin
		insInfo.MemOrigin = ins.MemOrigin
		insInfo.CpuRequest = ins.CpuRequest
		insInfo.MemRequest = ins.MemRequest
		insInfo.CpuLimit = ins.CpuLimit
		insInfo.MemLimit = ins.MemLimit
		insInfo.Image = ins.Image
		insInfo.StartedAt = ins.StartedAt
		insInfo.FinishedAt = ins.FinishedAt
		result = append(result, insInfo)
	}
	return result, nil
}

func (i *InstanceInfoImpl) GetPodInfo(req apistructs.PodInfoRequest) (apistructs.PodInfoDataList, error) {
	cond := QueryPodConditions{
		Cluster:         req.Cluster,
		OrgName:         req.OrgName,
		OrgID:           req.OrgID,
		ProjectName:     req.ProjectName,
		ProjectID:       req.ProjectID,
		ApplicationName: req.ApplicationName,
		ApplicationID:   req.ApplicationID,
		RuntimeName:     req.RuntimeName,
		RuntimeID:       req.RuntimeID,
		ServiceName:     req.ServiceName,
		Workspace:       req.Workspace,
		ServiceType:     req.ServiceType,
		AddonID:         req.AddonID,
		Phases:          req.Phases,
		Limit:           req.Limit,
	}

	pods, err := i.QueryPod(cond)
	if err != nil {
		errstr := fmt.Sprintf("failed to query pod info: %v", err)
		logrus.Error(errstr)
		return apistructs.PodInfoDataList{}, err
	}
	return pods, nil
}

func (i *InstanceInfoImpl) GetInstanceInfo(req apistructs.InstanceInfoRequest) (apistructs.InstanceInfoDataList, error) {
	instanceList := apistructs.InstanceInfoDataList{}

	cond := QueryInstanceConditions{

		Cluster:         req.Cluster,
		OrgName:         req.OrgName,
		OrgID:           req.OrgID,
		ProjectName:     req.ProjectName,
		ProjectID:       req.ProjectID,
		ApplicationName: req.ApplicationName,
		//EdgeApplicationName:  "",
		//EdgeSite:             "",
		ApplicationID: req.ApplicationID,
		RuntimeName:   req.RuntimeName,
		RuntimeID:     req.RuntimeID,
		ServiceName:   req.ServiceName,
		Workspace:     req.Workspace,
		ContainerID:   req.ContainerID,
		ServiceType:   req.ServiceType,
		AddonID:       req.AddonID,
		InstanceIP:    req.InstanceIP,
		HostIP:        req.HostIP,
		Phases:        req.Phases,
		Limit:         req.Limit,
	}

	instanceList, err := i.QueryInstance(cond)
	if err != nil {
		errstr := fmt.Sprintf("failed to query instance info: %v", err)
		logrus.Error(errstr)
		return apistructs.InstanceInfoDataList{}, err
	}

	return instanceList, nil
}
