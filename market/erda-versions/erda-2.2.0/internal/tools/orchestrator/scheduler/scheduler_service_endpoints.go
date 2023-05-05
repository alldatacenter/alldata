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

// TODO: refactor me, 把这个文件中的所有逻辑都去掉，只有 http 处理和 检查 参数合法性
package scheduler

import (
	"context"
	"fmt"
	"path/filepath"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/task"
)

func (s *Scheduler) handleRuntime(ctx context.Context, runtime *apistructs.ServiceGroup, taskAction task.Action) (task.TaskResponse, error) {
	var result task.TaskResponse

	if err := clusterutil.SetRuntimeExecutorByCluster(runtime); err != nil {
		return result, err
	}

	// put the task in scheduler's buffered channel
	// handle the task in schduler's loop
	task, err := s.sched.Send(ctx, task.TaskRequest{
		ExecutorKind: getServiceExecutorKindByName(runtime.Executor),
		ExecutorName: runtime.Executor,
		Action:       taskAction,
		ID:           runtime.ID,
		Spec:         *runtime,
	})
	if err != nil {
		return result, err
	}

	// get response from task's channel
	if result = task.Wait(ctx); result.Err() != nil {
		return result, result.Err()
	}

	return result, nil
}

// to suppress the error, to be the same with origin semantic
func getServiceExecutorKindByName(name string) string {
	e, err := executor.GetManager().Get(executortypes.Name(name))
	if err != nil {
		return conf.DefaultRuntimeExecutor()
	}
	return string(e.Kind())
}

func (s *Scheduler) EpGetRuntimeStatus(ctx context.Context, vars map[string]string) (*apistructs.MultiLevelStatus, error) {
	name := vars["name"]
	namespace := vars["namespace"]
	runtime := apistructs.ServiceGroup{}

	if err := s.store.Get(ctx, makeRuntimeKey(namespace, name), &runtime); err != nil {
		logrus.Errorf("Cannot get runtime(%s/%s) from etcd, err: %v", namespace, name, err)
		return nil, errors.Errorf("Cannot get runtime(%s/%s) from etcd, err: %v", namespace, name, err)
	}

	result, err := s.handleRuntime(ctx, &runtime, task.TaskInspect)
	if err != nil {
		return nil, err
	}

	multiStatus := &apistructs.MultiLevelStatus{
		Namespace: namespace,
		Name:      name,
	}

	// return empty runtime
	if result.Extra == nil {
		logrus.Errorf("got runtime(%v/%v) empty, executor: %s", runtime.Type, runtime.ID, runtime.Executor)
		return nil, errors.Errorf("got runtime(%v/%v) but found it empty", runtime.Type, runtime.ID)
	}

	newRuntime := result.Extra.(*apistructs.ServiceGroup)
	multiStatus.Status = convertServiceStatus(newRuntime.Status)
	multiStatus.More = make(map[string]string)
	for _, service := range newRuntime.Services {
		multiStatus.More[service.Name] = convertServiceStatus(service.Status)
	}

	return multiStatus, nil
}

func convertServiceStatus(serviceStatus apistructs.StatusCode) string {
	switch serviceStatus {
	case apistructs.StatusReady:
		return string(apistructs.StatusHealthy)

	case apistructs.StatusProgressing:
		return string(apistructs.StatusUnHealthy)

	default:
		return string(apistructs.StatusUnknown)
	}
}

func makeRuntimeKey(namespace, name string) string {
	return filepath.Join("/dice/service/", namespace, name)
}

func (s *Scheduler) CancelServiceGroup(namespace, name string) (interface{}, error) {
	runtime := apistructs.ServiceGroup{}
	ctx := context.Background()

	if err := s.store.Get(ctx, makeRuntimeKey(namespace, name), &runtime); err != nil {
		return apistructs.ServiceGroupGetErrorResponse{
			Error: fmt.Sprintf("failed to cancel servicegroup: Cannot get runtime(%s/%s) from etcd, err: %v", namespace, name, err),
		}, nil
	}

	result, err := s.handleRuntime(ctx, &runtime, task.TaskCancel)
	if err != nil {
		return nil, fmt.Errorf("failed to cancel servicegroup: namespace:%s, name:%s, error: %v", namespace, name, err)
	}

	return result.Extra, nil
}
