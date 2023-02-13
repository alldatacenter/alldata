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
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/task"
	"github.com/erda-project/erda/pkg/jsonstore"
)

var DeleteNotFound error = errors.New("not found")

// TODO: Compared with service_endpoints.go, should the returned content be changed?
func (s ServiceGroupImpl) Cancel(namespace string, name string) error {
	sg := apistructs.ServiceGroup{}
	if err := s.Js.Get(context.Background(), mkServiceGroupKey(namespace, name), &sg); err != nil {
		return err
	}

	if _, err := s.handleServiceGroup(context.Background(), &sg, task.TaskCancel); err != nil {
		return err
	}
	return nil
}

func (s ServiceGroupImpl) ConfigUpdate(sg apistructs.ServiceGroup) error {

	sg.LastModifiedTime = time.Now().Unix()

	logrus.Debugf("config update sg: %+v", sg)
	if err := s.Js.Put(context.Background(), mkServiceGroupKey(sg.Type, sg.ID), &sg); err != nil {
		return err
	}

	sg.Labels = appendServiceTags(sg.Labels, sg.Executor)

	if _, err := s.handleServiceGroup(context.Background(), &sg, task.TaskUpdate); err != nil {
		return err
	}
	return nil
}

func (s ServiceGroupImpl) Create(req apistructs.ServiceGroupCreateV2Request) (apistructs.ServiceGroup, error) {
	sg, err := convertServiceGroupCreateV2Request(req, s.Clusterinfo)
	if err != nil {
		return apistructs.ServiceGroup{}, errors.Errorf("failed to convert sg createV2Request, err: %v", err)
	}

	if err := s.Js.Put(context.Background(), mkServiceGroupKey(sg.Type, sg.ID), sg); err != nil {
		logrus.Errorf("failed to put sg to jsonStore, err: %v", err)
		return apistructs.ServiceGroup{}, err
	}

	sg.Labels = appendServiceTags(sg.Labels, sg.Executor)
	if _, err := s.handleServiceGroup(context.Background(), &sg, task.TaskCreate); err != nil {
		logrus.Errorf("failed to handle sg, err: %v", err)
		return apistructs.ServiceGroup{}, err
	}

	return sg, err
}

func convertServiceGroupCreateV2Request(req apistructs.ServiceGroupCreateV2Request, clusterinfo clusterinfo.ClusterInfo) (apistructs.ServiceGroup, error) {
	sg, err := convertServiceGroup(req, clusterinfo)
	if err != nil {
		return apistructs.ServiceGroup{}, err
	}
	sg.CreatedTime = time.Now().Unix()
	sg.LastModifiedTime = sg.CreatedTime
	sg.Status = apistructs.StatusCreated
	return sg, nil
}

func (s ServiceGroupImpl) Delete(namespace string, name, force string) error {
	sg := apistructs.ServiceGroup{}
	// force offline, first time not set status offline, delete etcd data; after set status, get and delete again, not return error
	if err := s.Js.Get(context.Background(), mkServiceGroupKey(namespace, name), &sg); err != nil {
		if force != "true" {
			if err == jsonstore.NotFoundErr {
				logrus.Errorf("not found runtime %s on namespace %s", name, namespace)
				return DeleteNotFound
			}
			logrus.Errorf("get from etcd err: %v when delete runtime %s on namespace %s", err, name, namespace)
			return err
		}
	}
	ns := sg.ProjectNamespace
	if ns == "" {
		ns = sg.Type
	}
	logrus.Infof("start to delete service group %s on namespace %s", sg.ID, ns)
	if _, err := s.handleServiceGroup(context.Background(), &sg, task.TaskDestroy); err != nil {
		if force != "true" {
			return err
		}
	}
	if err := s.Js.Remove(context.Background(), mkServiceGroupKey(namespace, name), nil); err != nil {
		if force != "true" {
			return err
		}
	}
	logrus.Infof("delete service group %s on namespace %s successfully", sg.ID, ns)
	return nil
}

func (s ServiceGroupImpl) Info(ctx context.Context, namespace string, name string) (apistructs.ServiceGroup, error) {
	sg := apistructs.ServiceGroup{}
	if err := s.Js.Get(context.Background(), mkServiceGroupKey(namespace, name), &sg); err != nil {
		return sg, err
	}

	result, err := s.handleServiceGroup(ctx, &sg, task.TaskInspect)
	if err != nil {
		return sg, err
	}
	if result.Extra == nil {
		err = errors.Errorf("Cannot get servicegroup(%v/%v) info from TaskInspect", sg.Type, sg.ID)
		logrus.Error(err.Error())
		return sg, err
	}

	newsg := result.Extra.(*apistructs.ServiceGroup)
	return *newsg, nil
}

func (s ServiceGroupImpl) KillPod(ctx context.Context, namespace string, name string, containerID string) error {
	sg := apistructs.ServiceGroup{}
	if err := s.Js.Get(context.Background(), mkServiceGroupKey(namespace, name), &sg); err != nil {
		return err
	}

	_, err := s.handleKillPod(ctx, &sg, containerID)
	if err != nil {
		return err
	}
	return nil
}

func (s ServiceGroupImpl) Precheck(req apistructs.ServiceGroupPrecheckRequest) (apistructs.ServiceGroupPrecheckData, error) {
	sg, err := convertServiceGroupCreateV2Request(apistructs.ServiceGroupCreateV2Request(req), s.Clusterinfo)
	if err != nil {
		return apistructs.ServiceGroupPrecheckData{}, err
	}
	t, err := s.handleServiceGroup(context.Background(), &sg, task.TaskPrecheck)
	if err != nil {
		return apistructs.ServiceGroupPrecheckData{}, err
	}

	return t.Extra.(apistructs.ServiceGroupPrecheckData), nil
}

func (s ServiceGroupImpl) Restart(namespace string, name string) error {
	sg := apistructs.ServiceGroup{}
	if err := s.Js.Get(context.Background(), mkServiceGroupKey(namespace, name), &sg); err != nil {
		return err
	}
	sg.Extra[LastRestartTimeKey] = time.Now().String()
	sg.LastModifiedTime = time.Now().Unix()

	sg.Labels = appendServiceTags(sg.Labels, sg.Executor)

	if _, err := s.handleServiceGroup(context.Background(), &sg, task.TaskUpdate); err != nil {
		return err
	}

	if err := s.Js.Put(context.Background(), mkServiceGroupKey(sg.Type, sg.ID), &sg); err != nil {
		return err
	}
	return nil
}

func (s *ServiceGroupImpl) Scale(sg *apistructs.ServiceGroup) (apistructs.ServiceGroup, error) {
	oldSg := apistructs.ServiceGroup{}
	if err := s.Js.Get(context.Background(), mkServiceGroupKey(sg.Type, sg.ID), &oldSg); err != nil {
		return apistructs.ServiceGroup{}, fmt.Errorf("Cannot get servicegroup(%s/%s) from etcd, err: %v", sg.Type, sg.ID, err)
	}

	// get sg info from etcd storage, and set the project namespace to the scale sg
	// when the project namespace is not empty
	if oldSg.ProjectNamespace != "" {
		sg.ProjectNamespace = oldSg.ProjectNamespace
	}

	//newService := sg.Services[0]
	oldServiceReplicas := make(map[string]int)
	for index, svc := range oldSg.Services {
		for newIndex, newSvc := range sg.Services {
			if svc.Name == newSvc.Name {
				if svc.Scale != newSvc.Scale {
					if newSvc.Scale == 0 {
						oldServiceReplicas[svc.Name] = svc.Scale
					}
					svc.Scale = newSvc.Scale
				}
				if svc.Resources.Cpu != newSvc.Resources.Cpu || svc.Resources.Mem != newSvc.Resources.Mem {
					svc.Resources = newSvc.Resources
				}
				oldSg.Services[index] = svc
				sg.Services[newIndex] = oldSg.Services[index]
				break
			}
		}
	}
	_, err := s.handleServiceGroup(context.Background(), sg, task.TaskScale)
	if err != nil {
		logrus.Errorf("scale service %s err: %v", sg.ID, err)
		return *sg, err
	}

	// 如果目前操作是停止(副本数为0)，为了后续恢复，需要保留停止操作前的副本数
	for index, oldSvc := range oldSg.Services {
		if replicas, ok := oldServiceReplicas[oldSvc.Name]; ok {
			oldSg.Services[index].Scale = replicas
		}
	}

	if err := s.Js.Put(context.Background(), mkServiceGroupKey(sg.Type, sg.ID), &oldSg); err != nil {
		return apistructs.ServiceGroup{}, err
	}
	return *sg, nil
}

func (s ServiceGroupImpl) Update(req apistructs.ServiceGroupUpdateV2Request) (apistructs.ServiceGroup, error) {
	sg, err := convertServiceGroupUpdateV2Request(req, s.Clusterinfo)
	if err != nil {
		return apistructs.ServiceGroup{}, err
	}

	oldSg := apistructs.ServiceGroup{}
	if err := s.Js.Get(context.Background(), mkServiceGroupKey(sg.Type, sg.ID), &oldSg); err != nil {
		return apistructs.ServiceGroup{}, fmt.Errorf("Cannot get servicegroup(%s/%s) from etcd, err: %v", sg.Type, sg.ID, err)
	}
	diffAndPatchRuntime(&sg, &oldSg)

	oldSg.Labels = appendServiceTags(oldSg.Labels, oldSg.Executor)
	if _, err := s.handleServiceGroup(context.Background(), &oldSg, task.TaskUpdate); err != nil {
		return apistructs.ServiceGroup{}, err
	}

	if err := s.Js.Put(context.Background(), mkServiceGroupKey(sg.Type, sg.ID), &oldSg); err != nil {
		return apistructs.ServiceGroup{}, err
	}
	return oldSg, nil
}

func convertServiceGroupUpdateV2Request(req apistructs.ServiceGroupUpdateV2Request, clusterinfo clusterinfo.ClusterInfo) (apistructs.ServiceGroup, error) {
	return convertServiceGroup(apistructs.ServiceGroupCreateV2Request(req), clusterinfo)
}

func diffAndPatchRuntime(newsg *apistructs.ServiceGroup, oldsg *apistructs.ServiceGroup) {
	// generate LastModifiedTime according to current time
	oldsg.LastModifiedTime = time.Now().Unix()

	oldsg.Labels = newsg.Labels
	oldsg.ServiceDiscoveryKind = newsg.ServiceDiscoveryKind

	// TODO: refactor it, separate data and status into different etcd key
	// Full update
	oldsg.Services = newsg.Services
}

// TODO: an ugly hack, need refactor, it may cause goroutine explosion
func (s ServiceGroupImpl) InspectServiceGroupWithTimeout(namespace, name string) (*apistructs.ServiceGroup, error) {
	var (
		sg  apistructs.ServiceGroup
		err error
	)
	done := make(chan struct{}, 1)
	go func() {
		sg, err = s.Info(context.Background(), namespace, name)
		done <- struct{}{}
	}()
	select {
	case <-done:
		return &sg, err
	case <-time.After(time.Duration(conf.InspectServiceGroupTimeout()) * time.Second):
		return nil, fmt.Errorf("timeout for invoke getServiceGroup for namesapce %s name %s", namespace, name)
	}
}
