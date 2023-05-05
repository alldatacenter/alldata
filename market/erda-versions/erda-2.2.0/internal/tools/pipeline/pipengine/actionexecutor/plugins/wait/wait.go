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

package wait

import (
	"context"
	"fmt"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/envconf"
)

var Kind = types.Kind(spec.PipelineTaskExecutorKindWait)

func init() {
	types.MustRegister(Kind, func(name types.Name, options map[string]string) (types.ActionExecutor, error) {
		return &Wait{
			name:    name,
			options: options,
		}, nil
	})
}

type Wait struct {
	name    types.Name
	options map[string]string
}

func (w *Wait) Kind() types.Kind {
	return Kind
}

func (w *Wait) Name() types.Name {
	return w.name
}

func (w *Wait) Exist(ctx context.Context, task *spec.PipelineTask) (bool, bool, error) {
	status := task.Status
	switch true {
	case status == apistructs.PipelineStatusAnalyzed, status == apistructs.PipelineStatusBorn:
		return false, false, nil
	case status == apistructs.PipelineStatusCreated:
		return true, false, nil
	case status == apistructs.PipelineStatusQueue, status == apistructs.PipelineStatusRunning:
		return true, true, nil
	case status.IsEndStatus():
		return true, true, nil
	default:
		return false, false, fmt.Errorf("invalid status when query task exist")
	}
}

func (w *Wait) Create(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (w *Wait) Start(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	created, started, err := w.Exist(ctx, task)
	if err != nil {
		return apistructs.PipelineStatusDesc{}, err
	}

	if !created {
		logrus.Warnf("wait: action not create yet, try to create, pipelineID: %d, taskID: %d", task.PipelineID, task.ID)
		_, err = w.Create(ctx, task)
		if err != nil {
			return nil, err
		}
		logrus.Warnf("scheduler: action created, continue to start, pipelineID: %d, taskID: %d", task.PipelineID, task.ID)
	}

	if started {
		logrus.Warnf("wait: action already started, pipelineID: %d, taskID: %d", task.PipelineID, task.ID)
		return nil, nil
	}

	executorDoneCh := ctx.Value(spec.MakeTaskExecutorCtxKey(task)).(chan spec.ExecutorDoneChanData)
	if executorDoneCh == nil {
		return nil, errors.Errorf("wait: failed to get exector channel, pipelineID: %d, taskID: %d", task.PipelineID, task.ID)
	}

	waitSec, err := w.getWaitSec(task)
	if err != nil {
		return nil, err
	}

	timer := time.NewTimer(time.Duration(waitSec) * time.Second)
	go func() {
		doneChanDataVersion := task.GenerateExecutorDoneChanDataVersion()
		select {
		case <-ctx.Done():
			logrus.Warnf("wait received stop timer signal, canceled, reason: %s", ctx.Err())
			return
		case <-timer.C:
			executorDoneCh <- spec.ExecutorDoneChanData{
				Data:    apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusSuccess},
				Version: doneChanDataVersion,
			}
			return
		}
	}()
	return nil, nil
}

func (w *Wait) Update(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (w *Wait) Status(ctx context.Context, task *spec.PipelineTask) (apistructs.PipelineStatusDesc, error) {
	created, _, err := w.Exist(ctx, task)
	if err != nil {
		return apistructs.PipelineStatusDesc{}, err
	}

	if !created {
		return apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusAnalyzed}, nil
	}
	if task.TimeBegin.IsZero() {
		return apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusRunning}, nil
	}

	waitSec, err := w.getWaitSec(task)
	if err != nil {
		return apistructs.PipelineStatusDesc{
			Status: apistructs.PipelineStatusFailed,
			Desc:   err.Error(),
		}, nil
	}

	endTime := task.TimeBegin.Add(time.Duration(waitSec) * time.Second)
	now := time.Now()
	if now.Equal(endTime) || now.After(endTime) {
		return apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusSuccess}, nil
	}

	return apistructs.PipelineStatusDesc{Status: apistructs.PipelineStatusRunning}, nil
}

func (w *Wait) Inspect(ctx context.Context, task *spec.PipelineTask) (apistructs.TaskInspect, error) {
	return apistructs.TaskInspect{}, nil
}

func (w *Wait) Cancel(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (w *Wait) Remove(ctx context.Context, task *spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func (w *Wait) BatchDelete(ctx context.Context, tasks []*spec.PipelineTask) (interface{}, error) {
	return nil, nil
}

func mergeEnvs(task *spec.PipelineTask) map[string]string {
	envs := make(map[string]string)
	for k, v := range task.Extra.PublicEnvs {
		envs[k] = v
	}
	for k, v := range task.Extra.PrivateEnvs {
		envs[k] = v
	}
	return envs
}

func (w *Wait) getWaitSec(task *spec.PipelineTask) (int, error) {
	envs := mergeEnvs(task)

	var cfg apistructs.AutoTestRunWait
	if err := envconf.Load(&cfg, envs); err != nil {
		return 0, errors.Errorf("failed to get wati time, err: %v", err)
	}
	// TODO delete waitTime
	if cfg.WaitTime > 0 {
		cfg.WaitTimeSec = cfg.WaitTime
	}
	if cfg.WaitTimeSec <= 0 {
		return 0, errors.Errorf("invalid wait time: %d", cfg.WaitTime)
	}
	return cfg.WaitTimeSec, nil
}
