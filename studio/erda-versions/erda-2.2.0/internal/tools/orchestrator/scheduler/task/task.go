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

package task

import (
	"context"
	"fmt"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
)

type Action int

const (
	TaskCreate Action = iota
	TaskDestroy
	TaskStatus
	TaskRemove
	TaskUpdate
	TaskInspect
	TaskCancel
	TaskPrecheck
	TaskJobVolumeCreate
	TaskKillPod
	TaskScale
)

var (
	BadSpec = errors.New("invalid service spec")
)

type TaskRequest struct {
	Spec         interface{}
	ExecutorKind string
	ExecutorName string
	ID           string
	Action       Action
}

type TaskResponse struct {
	err   error
	desc  apistructs.StatusDesc
	Extra interface{}
}

func (tr *TaskResponse) Err() error {
	return tr.err
}

func (tr *TaskResponse) Status() apistructs.StatusDesc {
	return tr.desc
}

type Result interface {
	Wait(ctx context.Context) TaskResponse
}

type Task struct {
	TaskRequest
	ctx           context.Context
	executor      executortypes.Executor
	c             chan TaskResponse
	volumeDrivers map[apistructs.VolumeType]volume.Volume
}

func (t *Task) Wait(ctx context.Context) TaskResponse {
	select {
	case resp := <-t.c:
		return resp
	case <-ctx.Done():
		return TaskResponse{
			err: ctx.Err(),
		}
	}
}

func (t *Task) Run(ctx context.Context) TaskResponse {
	executor := t.executor
	if executor == nil {
		return TaskResponse{
			err: errors.New("not found executor"),
		}
	}

	logrus.Infof("[Task.Run] action: %v, executor Name: %v, executor Kind: %v", t.Action, t.executor.Name(), t.executor.Kind())
	switch t.Action {
	case TaskCreate:
		var (
			resp interface{}
			err  error
		)
		runtime, ok := t.Spec.(apistructs.ServiceGroup)
		if ok {
			/*
				if err := t.volumesAttach(&runtime); err != nil {
					return TaskResponse{
						err: err,
					}
				}
			*/
			t.Spec = runtime
		}
		if resp, err = executor.Create(ctx, t.Spec); err != nil {
			return TaskResponse{
				err: err,
			}
		}
		return TaskResponse{
			Extra: resp,
			err:   err,
		}
	case TaskUpdate:
		var (
			resp interface{}
			err  error
		)
		runtime, ok := t.Spec.(apistructs.ServiceGroup)
		if ok {
			/*
				if err := t.volumesAttach(&runtime); err != nil {
					return TaskResponse{
						err: err,
					}
				}
			*/
			t.Spec = runtime

		}
		if resp, err = executor.Update(ctx, t.Spec); err != nil {
			return TaskResponse{
				err: err,
			}
		}
		return TaskResponse{
			Extra: resp,
			err:   err,
		}
	case TaskDestroy:
		if err := executor.Destroy(ctx, t.Spec); err != nil {
			return TaskResponse{
				err: err,
			}
		}
	case TaskRemove:
		if err := executor.Remove(ctx, t.Spec); err != nil {
			return TaskResponse{
				err: err,
			}
		}
	case TaskStatus:
		var (
			desc apistructs.StatusDesc
			err  error
		)
		if desc, err = executor.Status(ctx, t.Spec); err != nil {
			return TaskResponse{
				err: err,
			}
		}

		return TaskResponse{
			err:  err,
			desc: desc,
		}
	case TaskInspect:
		var (
			resp interface{}
			err  error
		)
		if resp, err = executor.Inspect(ctx, t.Spec); err != nil {
			return TaskResponse{
				err: err,
			}
		}
		return TaskResponse{
			Extra: resp,
			err:   err,
		}
	case TaskCancel:
		var (
			resp interface{}
			err  error
		)
		if resp, err = executor.Cancel(ctx, t.Spec); err != nil {
			return TaskResponse{
				err: err,
			}
		}
		return TaskResponse{
			Extra: resp,
			err:   err,
		}
	case TaskPrecheck:
		r, err := executor.Precheck(ctx, t.Spec)
		return TaskResponse{
			err:   err,
			Extra: r,
		}
	case TaskJobVolumeCreate:
		r, err := executor.JobVolumeCreate(ctx, t.Spec)
		return TaskResponse{
			err:   err,
			Extra: r,
		}
	case TaskKillPod:
		err := executor.KillPod(t.Spec.(string)) // containerid
		return TaskResponse{
			err: err,
		}
	case TaskScale:
		r, err := executor.Scale(ctx, t.Spec)
		return TaskResponse{
			err:   err,
			Extra: r,
		}
	default:
		return TaskResponse{
			err: errors.Errorf("invlaid action: %d", t.Action),
		}
	}

	return TaskResponse{}
}

func (t *Task) String() string {
	return fmt.Sprintf("executor %s/%s (id: %s, action: %v)", t.ExecutorKind, t.ExecutorName, t.ID, t.Action)
}
func (t *Task) volumesAttach(runtime *apistructs.ServiceGroup) error {
	for _, s := range runtime.Services {
		for _, v := range s.Volumes {
			driver, ok := t.volumeDrivers[v.VolumeType]
			if !ok {
				return fmt.Errorf("not found volumedriver: %v", v.VolumeType)
			}
			if v.ID == "" {
				return fmt.Errorf("volume no ID or name assigned")
			}
			cb, err := driver.Attach(volume.VolumeIdentity(v.ID), volume.AttachDest{
				Namespace: runtime.Type,
				Service:   s.Name,
				Path:      v.ContainerPath,
			})
			if err != nil {
				return err
			}
			if _, err = cb(runtime); err != nil {
				return err
			}
		}
	}
	return nil
}

func (a *Action) String() string {
	switch *a {
	case TaskCreate:
		return "TaskCreate"
	case TaskDestroy:
		return "TaskDestroy"
	case TaskStatus:
		return "TaskStatus"
	case TaskRemove:
		return "TaskRemove"
	case TaskUpdate:
		return "TaskUpdate"
	case TaskInspect:
		return "TaskInspect"
	case TaskCancel:
		return "TaskCancel"
	case TaskPrecheck:
		return "TaskPrecheck"
	case TaskJobVolumeCreate:
		return "TaskJobVolumeCreate"
	case TaskKillPod:
		return "TaskKillPod"
	case TaskScale:
		return "TaskScale"
	}
	panic("unreachable")
}
