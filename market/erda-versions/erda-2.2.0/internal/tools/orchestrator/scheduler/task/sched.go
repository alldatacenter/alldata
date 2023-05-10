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
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume/driver"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

type Sched struct {
	mgr           *executor.Manager
	volumeDrivers map[apistructs.VolumeType]volume.Volume
}

func NewSched() (*Sched, error) {
	js, err := jsonstore.New()
	if err != nil {
		return nil, err
	}

	volumeDrivers := map[apistructs.VolumeType]volume.Volume{
		apistructs.LocalVolume: driver.NewLocalVolumeDriver(js),
		apistructs.NasVolume:   driver.NewNasVolumeDriver(js),
	}

	s := &Sched{
		mgr:           executor.GetManager(),
		volumeDrivers: volumeDrivers,
	}
	go s.PrintPoolUsage()

	return s, nil
}

func (s *Sched) PrintPoolUsage() {
	logrus.Infof("enter PrintPoolUsage")
	for {
		select {
		case <-time.After(1 * time.Minute):
			s.mgr.PrintPoolUsage()
		}
	}
}

func (s *Sched) runTask(ctx context.Context, task *Task) error {
	// find a right executor
	executor, err := s.FindExecutor(task.ExecutorName, task.ExecutorKind)
	if err != nil {
		s.Return(task.ctx, task, TaskResponse{
			err: err,
		})
		return err
	}

	if task.ExecutorName == "" {
		task.ExecutorName = string(executor.Name())
	}
	task.executor = executor

	if err = s.setObjLabelScheduleInfo(task); err != nil {
		s.Return(task.ctx, task, TaskResponse{
			err: err,
		})
		return err
	}

	logrus.Infof("start to run the task: %s", task)

	var resp TaskResponse
	defer func() {
		s.Return(ctx, task, resp)
		logrus.Infof("finish to run the task: %s", task)
	}()
	if resp = task.Run(ctx); resp.Err() != nil {
		return resp.Err()
	}
	return nil
}

func (s *Sched) Send(ctx context.Context, req TaskRequest) (Result, error) {
	task := &Task{
		TaskRequest:   req,
		c:             make(chan TaskResponse, 1),
		ctx:           ctx,
		volumeDrivers: s.volumeDrivers,
	}

	err := s.mgr.Pool(executortypes.Name(task.ExecutorName)).GoWithTimeout(func() {
		if err := s.runTask(ctx, task); err != nil {
			logrus.Errorf("failed to execute task: %s (%v)", task, err)
		}
	}, time.Second*5)
	if err != nil {
		return nil, err
	}

	return task, nil
}

func (s *Sched) Return(ctx context.Context, t *Task, resp TaskResponse) {
	t.c <- resp
	close(t.c)
}

func (s *Sched) ListExecutors() []executortypes.Executor {
	return s.mgr.ListExecutors()
}

func (s *Sched) FindExecutor(name, kind string) (executortypes.Executor, error) {
	if name != "" {
		executor, err := s.mgr.Get(executortypes.Name(name))
		if err != nil {
			return nil, err
		}
		return executor, nil
	}

	executors := s.mgr.GetByKind(executortypes.Kind(kind))
	// FIXME not random
	if len(executors) != 0 {
		return executors[0], nil
	}
	return nil, errors.Errorf("not found executor: %s", kind)
}

func (s *Sched) setObjLabelScheduleInfo(task *Task) error {
	// Only do tag filtering for POST or PUT requests
	if task.Action != TaskCreate && task.Action != TaskUpdate && task.Action != TaskPrecheck {
		return nil
	}

	configs, err := s.mgr.GetExecutorConfigs(executortypes.Name(task.ExecutorName))
	if err != nil {
		return err
	}
	scheduleInfo2, scheduleInfo, refinedConfig_, err := schedulepolicy.LabelFilterChain(configs, task.ExecutorName, task.ExecutorKind, task.Spec)
	if err != nil {
		logrus.Errorf("setting label constrains for task(%v) error: %v", task.ID, err)
	}

	logrus.Infof("got task(%s)", task.ID)

	switch task.ExecutorKind {
	case labelconfig.EXECUTOR_MARATHON, labelconfig.EXECUTOR_K8S, labelconfig.EXECUTOR_EDASV2:
		r, ok := task.Spec.(apistructs.ServiceGroup)
		if !ok {
			logrus.Errorf("task(%s) spec not fit ServiceGroup", task.ID)
			// anything to do?
			return nil
		}

		if refinedConfig_ != nil {
			if extra, ok := refinedConfig_.(map[string]string); ok {
				r.Extra = extra
				logrus.Infof("got refined configs(%v) for runtime(%s)", extra, r.ID)
			}
		}

		r.ScheduleInfo = scheduleInfo
		r.ScheduleInfo2 = scheduleInfo2
		task.Spec = r

	case labelconfig.EXECUTOR_METRONOME, labelconfig.EXECUTOR_K8SJOB:
		j, ok := task.Spec.(apistructs.Job)
		if !ok {
			logrus.Errorf("task(%s) spec not fit Job", task.ID)
			// anything to do?
			return nil
		}

		j.ScheduleInfo = scheduleInfo
		j.ScheduleInfo2 = scheduleInfo2
		task.Spec = j
	case labelconfig.EXECUTOR_SPARK, labelconfig.EXECUTOR_K8SSPARK:
		j, ok := task.Spec.(apistructs.Job)
		if !ok {
			logrus.Errorf("task(%s) spec not fit Job", task.ID)
			return nil
		}
		j.ScheduleInfo = scheduleInfo
		j.ScheduleInfo2 = scheduleInfo2
		task.Spec = j
	}

	return nil
}
