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

package pipelinesvc

import (
	"encoding/json"
	"fmt"

	"github.com/pkg/errors"
	"github.com/xormplus/xorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/events"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/db"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/metadata"
)

func (s *PipelineSvc) DealPipelineCallbackOfAction(data []byte) (err error) {
	defer func() {
		if err != nil {
			err = errors.Wrapf(err, "failed to deal with pipeline action callback")
		}
	}()

	// 回调数据格式校验
	var cb apistructs.ActionCallback
	if err = json.Unmarshal(data, &cb); err != nil {
		return err
	}
	if cb.PipelineTaskID <= 0 {
		return errors.Errorf("invalid pipelineTaskID [%d]", cb.PipelineTaskID)
	}

	task, err := s.dbClient.GetPipelineTask(cb.PipelineTaskID)
	if err != nil {
		return err
	}
	p, err := s.dbClient.GetPipeline(task.PipelineID)
	if err != nil {
		return err
	}

	if task.PipelineID != p.ID {
		return apierrors.ErrCallback.InvalidParameter(
			fmt.Sprintf("task not belong to pipeline, taskID: %d, pipelineID: %d", task.ID, p.ID))
	}

	// update task.metadata
	if err = s.appendPipelineTaskMetadata(&p, &task, cb); err != nil {
		return err
	}

	// update task.inspect
	if err = s.appendPipelineTaskInspect(&p, &task, cb); err != nil {
		return err
	}

	// 处理特殊回调逻辑
	// 1. runtimeID
	if err = s.doCallbackOfRuntimeID(&p, &task, cb); err != nil {
		return err
	}
	// 2. flink/spark jar resource
	if err = s.doCallbackOfJarResource(&p, &task, cb); err != nil {
		return err
	}

	return nil
}

func (s *PipelineSvc) appendPipelineTaskInspect(p *spec.Pipeline, task *spec.PipelineTask, cb apistructs.ActionCallback) error {
	if cb.MachineStat == nil {
		return nil
	}
	// machine stat
	if cb.MachineStat != nil {
		task.Inspect.MachineStat = cb.MachineStat
	}

	if err := s.dbClient.UpdatePipelineTaskInspect(task.ID, task.Inspect); err != nil {
		return err
	}
	return nil
}

func (s *PipelineSvc) appendPipelineTaskMetadata(p *spec.Pipeline, task *spec.PipelineTask, cb apistructs.ActionCallback) error {
	if len(cb.Metadata) == 0 && len(cb.Errors) == 0 {
		return nil
	}
	if task.Result == nil {
		task.Result = &taskresult.Result{Metadata: metadata.Metadata{}, Errors: taskerror.OrderedErrors{}}
	}

	task.Result.Metadata = append(task.Result.Metadata, cb.Metadata...)
	task.Result.Errors = task.Result.Errors.AppendError(cb.Errors...)
	if err := s.dbClient.UpdatePipelineTaskMetadata(task.ID, task.Result); err != nil {
		return err
	}

	// emit event when meta updated
	events.EmitTaskEvent(task, p)

	return nil
}

// doCallbackOfRuntimeID 发送 websocket 消息，及时更新页面 link
func (s *PipelineSvc) doCallbackOfRuntimeID(p *spec.Pipeline, task *spec.PipelineTask, cb apistructs.ActionCallback) error {
	for _, meta := range cb.Metadata {
		if meta.Type == apistructs.ActionCallbackTypeLink &&
			meta.Name == apistructs.ActionCallbackRuntimeID {
			events.EmitTaskRuntimeEvent(task, p)
			break
		}
	}
	return nil
}

// doCallbackOfJarResource 获取 flink/spark 任务需要的 jar resource
func (s *PipelineSvc) doCallbackOfJarResource(p *spec.Pipeline, task *spec.PipelineTask, cb apistructs.ActionCallback) error {
	for _, meta := range cb.Metadata {
		if meta.Name != "bigdataJarResource" {
			continue
		}
		// 寻找需要这个 task 生成的 jar resource 的 flink/spark task
		flinkSparkTasks, err := s.findFlinkSparkTasks(p, task.Name)
		if err != nil {
			return err
		}
		for _, fst := range flinkSparkTasks {
			fst.Extra.FlinkSparkConf.JarResource = meta.Value
			if err = s.dbClient.UpdatePipelineTask(fst.ID, &fst); err != nil {
				return err
			}
		}
	}
	return nil
}

// findFlinkSparkTasks 寻找 depend 为指定值的 task
func (s *PipelineSvc) findFlinkSparkTasks(p *spec.Pipeline, depend string) ([]spec.PipelineTask, error) {
	tasks, err := s.dbClient.ListPipelineTasksByPipelineID(p.ID)
	if err != nil {
		return nil, err
	}
	var result []spec.PipelineTask
	for i := range tasks {
		task := tasks[i]
		if isFlinkSparkAction(task.Type) && task.Extra.FlinkSparkConf.Depend == depend && len(task.Extra.FlinkSparkConf.JarResource) == 0 {
			result = append(result, task)
		}
	}
	return result, nil
}

func isFlinkSparkAction(action string) bool {
	return action == "flink" || action == "spark"
}

func (s *PipelineSvc) DealPipelineCallbackOfTask(data []byte) error {
	var pt spec.PipelineTask
	if err := json.Unmarshal(data, &pt); err != nil {
		return err
	}

	return s.CreateOrUpdatePipelineTask(&pt)
}

func (s *PipelineSvc) DealPipelineCallbackOfPipeline(data []byte) error {
	var pst spec.PipelineWithStageAndTask
	if err := json.Unmarshal(data, &pst); err != nil {
		return err
	}

	err := s.CreateOrUpdatePipeline(&pst.Pipeline)
	if err != nil {
		return err
	}

	err = s.dbClient.DeletePipelineTasksByPipelineID(pst.ID)
	if err != nil {
		return err
	}
	err = s.dbClient.BatchCreatePipelineTasks(pst.PipelineTasks)
	if err != nil {
		return err
	}

	err = s.dbClient.DeletePipelineStagesByPipelineID(pst.ID)
	if err != nil {
		return err
	}
	return s.dbClient.BatchCreatePipelineStages(pst.PipelineStages)
}

func (s *PipelineSvc) DealPipelineCallbackOfCron(data []byte) error {
	var pc db.PipelineCron
	if err := json.Unmarshal(data, &pc); err != nil {
		return err
	}

	return s.CreateOrUpdatePipelineCron(&pc)
}

func (s *PipelineSvc) CreateOrUpdatePipeline(pipeline *spec.Pipeline) error {
	var baseDao spec.PipelineBase
	exist, err := s.dbClient.ID(pipeline.ID).Get(&baseDao)
	if err != nil {
		return err
	}
	if !exist {
		_, err = s.dbClient.Transaction(func(session *xorm.Session) (interface{}, error) {
			return nil, s.dbClient.CreatePipeline(pipeline, dbclient.WithTxSession(session))
		})
		return err
	}
	err = s.dbClient.UpdatePipelineBase(pipeline.ID, &pipeline.PipelineBase)
	if err != nil {
		return err
	}

	err = s.dbClient.UpdatePipelineExtraByPipelineID(pipeline.ID, &pipeline.PipelineExtra)
	if err != nil {
		return err
	}

	err = s.dbClient.DeletePipelineLabelsByPipelineID(pipeline.ID)
	if err != nil {
		return err
	}
	return s.dbClient.CreatePipelineLabels(pipeline)
}

func (s *PipelineSvc) CreateOrUpdatePipelineTask(pt *spec.PipelineTask) error {
	var dao spec.PipelineTask
	exist, err := s.dbClient.ID(pt.ID).Get(&dao)
	if err != nil {
		return err
	}
	if exist {
		return s.dbClient.UpdatePipelineTask(pt.ID, pt)
	}
	return s.dbClient.CreatePipelineTask(pt)
}

func (s *PipelineSvc) CreateOrUpdatePipelineCron(pc *db.PipelineCron) error {
	var dao db.PipelineCron
	exist, err := s.dbClient.ID(pc.ID).Get(&dao)
	if err != nil {
		return err
	}
	dbClient := &db.Client{Interface: s.mysql}
	if exist {
		return dbClient.UpdatePipelineCron(pc.ID, pc)
	}
	return dbClient.CreatePipelineCron(pc)
}
