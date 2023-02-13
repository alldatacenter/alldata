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

package apitest

import (
	"context"
	"fmt"
	"sync"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/plugins/apitest/logic"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

var Kind = types.Kind(spec.PipelineTaskExecutorKindAPITest)

type define struct {
	name        types.Name
	options     map[string]string
	dbClient    *dbclient.Client
	runningAPIs sync.Map
}

func (d *define) Kind() types.Kind { return Kind }
func (d *define) Name() types.Name { return d.name }

func (d *define) Exist(ctx context.Context, task *spec.PipelineTask) (created bool, started bool, err error) {
	status := task.Status
	switch true {
	case status == apistructs.PipelineStatusAnalyzed, status == apistructs.PipelineStatusBorn:
		return false, false, nil
	case status == apistructs.PipelineStatusCreated:
		return true, false, nil
	case status == apistructs.PipelineStatusQueue, status == apistructs.PipelineStatusRunning:
		// if apitest task is not procesing, should make status-started false
		if _, alreadyProcessing := d.runningAPIs.Load(d.makeRunningApiKey(task)); alreadyProcessing {
			return true, true, nil
		}
		return true, false, nil
	case status.IsEndStatus():
		return true, true, nil
	default:
		return false, false, fmt.Errorf("invalid status when query task exist")
	}
}

func (d *define) Create(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (d *define) Start(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {

	go func(ctx context.Context, task *spec.PipelineTask) {
		doneChanDataVersion := task.GenerateExecutorDoneChanDataVersion()
		if _, alreadyProcessing := d.runningAPIs.LoadOrStore(d.makeRunningApiKey(task), task); alreadyProcessing {
			logrus.Warnf("apitest: task: %d already processing", task.ID)
			return
		}
		executorDoneCh, ok := ctx.Value(spec.MakeTaskExecutorCtxKey(task)).(chan spec.ExecutorDoneChanData)
		if !ok {
			logrus.Warnf("apitest: failed to get executor channel, pipelineID: %d, taskID: %d", task.PipelineID, task.ID)
		}

		var status = apistructs.PipelineStatusFailed
		defer func() {
			if r := recover(); r != nil {
				logrus.Errorf("api-test logic do panic recover:%s", r)
			}
			// if executor chan is nil, task framework can loop query meta get status
			if executorDoneCh != nil {
				executorDoneCh <- spec.ExecutorDoneChanData{
					Data:    apistructs.PipelineStatusDesc{Status: status},
					Version: doneChanDataVersion,
				}
			}
			d.runningAPIs.Delete(d.makeRunningApiKey(task))
		}()

		logic.Do(ctx, task)

		latestTask, err := d.dbClient.GetPipelineTask(task.ID)
		if err != nil {
			logrus.Errorf("failed to query latest task, err: %v \n", err)
			return
		}

		meta := latestTask.GetMetadata()
		for _, metaField := range meta {
			if metaField.Name == logic.MetaKeyResult {
				if metaField.Value == logic.ResultSuccess {
					status = apistructs.PipelineStatusSuccess
				}
			}
		}
	}(ctx, task)
	return nil, nil
}

func (d *define) Update(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (d *define) Status(ctx context.Context, task *spec.PipelineTask) (apistructs.PipelineStatusDesc, error) {
	latestTask, err := d.dbClient.GetPipelineTask(task.ID)
	if err != nil {
		return apistructs.PipelineStatusDesc{}, fmt.Errorf("failed to query latest task, err: %v", err)
	}
	//*task = latestTask

	if task.Status.IsEndStatus() {
		return apistructs.PipelineStatusDesc{Status: task.Status}, nil
	}

	created, started, err := d.Exist(ctx, task)
	if err != nil {
		return apistructs.PipelineStatusDesc{}, err
	}

	if !created {
		return apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusAnalyzed}, nil
	}

	meta := latestTask.GetMetadata()
	if !started && len(meta) == 0 {
		return apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusBorn}, nil
	}

	// status according to api success or not
	var status = apistructs.PipelineStatusFailed
	for _, metaField := range meta {
		if metaField.Name == logic.MetaKeyResult {
			if metaField.Value == logic.ResultSuccess {
				status = apistructs.PipelineStatusSuccess
			}
			if metaField.Value == logic.ResultFailed {
				status = apistructs.PipelineStatusFailed
			}
			return apistructs.PipelineStatusDesc{Status: status}, nil
		}
	}

	// return created status to do start step
	return apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusRunning}, nil
}

func (d *define) Inspect(ctx context.Context, task *spec.PipelineTask) (apistructs.TaskInspect, error) {
	return apistructs.TaskInspect{}, nil
}

func (d *define) Cancel(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (d *define) Remove(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (d *define) BatchDelete(ctx context.Context, actions []*spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (d *define) makeRunningApiKey(task *spec.PipelineTask) string {
	return fmt.Sprintf("%d-%d", task.PipelineID, task.ID)
}

func init() {
	types.MustRegister(Kind, func(name types.Name, options map[string]string) (types.ActionExecutor, error) {
		dbClient, err := dbclient.New()
		if err != nil {
			return nil, fmt.Errorf("failed to init dbclient, err: %v", err)
		}
		return &define{
			name:        name,
			options:     options,
			dbClient:    dbClient,
			runningAPIs: sync.Map{},
		}, nil
	})
}
