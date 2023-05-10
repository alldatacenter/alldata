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

package taskop

import (
	"context"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/pvolumes"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskrun"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

type start taskrun.TaskRun

func NewStart(tr *taskrun.TaskRun) *start {
	return (*start)(tr)
}

func (s *start) Op() taskrun.Op {
	return taskrun.Start
}

func (s *start) TaskRun() *taskrun.TaskRun {
	return (*taskrun.TaskRun)(s)
}

func (s *start) Processing() (interface{}, error) {
	// start
	data, err := s.Executor.Start(s.Ctx, s.Task)
	if err != nil {
		return nil, err
	}
	// inject volume id
	// 若该方法放在 WhenDone 中，start 成功后若分布式锁丢失，则下次 correct task 时会直接变成 queue 或者其他之后的真实状态，
	// 而不会执行 start.WhenDone，导致 volumeID 未注入
	s.TaskRun().LogStep(s.Op(), "begin injectVolumeID")
	defer s.TaskRun().LogStep(s.Op(), "end injectVolumeID")
	if err := injectVolumeID((*taskrun.TaskRun)(s), data); err != nil {
		logrus.Errorf("reconciler: pipelineID: %d, task %q failed to injectVolumeID when start done, err: %v",
			s.P.ID, s.Task.Name, err)
		return nil, err
	}
	return data, nil
}

func (s *start) WhenDone(data interface{}) error {
	s.Task.Status = apistructs.PipelineStatusQueue
	s.Task.Extra.TimeBeginQueue = time.Now()
	logrus.Infof("reconciler: pipelineID: %d, taskID: %d, taskName: %s, end start (%s -> %s)",
		s.P.ID, s.Task.ID, s.Task.Name, apistructs.PipelineStatusCreated, apistructs.PipelineStatusQueue)
	return nil
}

func (s *start) WhenLogicError(err error) error {
	s.Task.Status = apistructs.PipelineStatusStartError
	return nil
}

func (s *start) WhenTimeout() error {
	return nil
}

func (s *start) WhenCancel() error {
	if err := s.TaskRun().WhenCancel(); err != nil {
		return err
	}
	// check exist first
	_, started, err := s.Executor.Exist(s.Ctx, s.Task)
	if err != nil {
		return err
	}
	if !started {
		return nil
	}
	// if exists, then do cancel
	_, err = s.Executor.Cancel(s.Ctx, s.Task)
	return err
}

func (s *start) TimeoutConfig() (<-chan struct{}, context.CancelFunc, time.Duration) {
	return nil, nil, -1
}

func (s *start) TuneTriggers() taskrun.TaskOpTuneTriggers {
	return taskrun.TaskOpTuneTriggers{
		BeforeProcessing: aoptypes.TuneTriggerTaskBeforeStart,
		AfterProcessing:  aoptypes.TuneTriggerTaskAfterStart,
	}
}

func injectVolumeID(tr *taskrun.TaskRun, data interface{}) error {
	if data == nil {
		return nil
	}
	job, ok := data.(apistructs.Job)
	if !ok {
		return nil
	}

	// 所有声明的 volumes
	// 若提交时 volume.ID 为空，jobStart 返回时必须带上 ID 用于标识该 volume 创建成功
	diceVolumesMap := make(map[string]diceyml.Volume)
	for _, diceVolume := range job.Volumes {
		// 校验 ID
		if diceVolume.ID == nil || *diceVolume.ID == "" {
			return errors.Errorf("volume id is not provided after taskRun.Start done, storage: %s, path: %s",
				diceVolume.Storage, diceVolume.Path)
		}
		diceVolumesMap[diceVolume.Path] = diceVolume
	}

	// 遍历 task.Context.OutStorages，注入 volumeID
	for i, declaredVolume := range tr.Task.Context.OutStorages {
		if declaredVolume.Type == string(spec.StoreTypeDiceVolumeFake) || declaredVolume.Type == string(spec.StoreTypeDiceCacheNFS) {
			// fake volume 没有实际逻辑，只是为了被引用到
			continue
		}
		// 判断在返回的 diceVolumes 中是否存在
		diceVolume, ok := diceVolumesMap[declaredVolume.Value]
		if !ok {
			return errors.Errorf("declared volume not found in job.Volumes after taskRun.Start done, "+
				"storage: %s, name: %s, path: %s", declaredVolume.Type, declaredVolume.Name, declaredVolume.Value)
		}
		// 已经有 ID，则跳过
		if declaredVolume.Labels != nil && declaredVolume.Labels["ID"] != "" {
			continue
		}
		// 写入 volumeID
		// 若 label 为空，说明是老数据，重新生成一个
		if len(declaredVolume.Labels) == 0 {
			declaredVolume = pvolumes.GenerateTaskVolume(*tr.Task, declaredVolume.Name, diceVolume.ID)
		} else {
			declaredVolume.Labels[pvolumes.VoLabelKeyVolumeID] = *diceVolume.ID
		}
		// store to db
		tr.Task.Context.OutStorages[i] = declaredVolume
		if err := tr.DBClient.UpdatePipelineTaskContext(tr.Task.ID, tr.Task.Context); err != nil {
			return err
		}
	}

	return nil
}
