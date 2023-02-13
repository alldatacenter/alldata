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

package dbclient

import (
	"encoding/json"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/statusutil"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/retry"
)

func (client *Client) CreatePipelineTask(pt *spec.PipelineTask, ops ...SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	if pt.ID == 0 {
		pt.ID = uuid.SnowFlakeIDUint64()
	}
	_, err = session.InsertOne(pt)
	return err
}

// FindCauseFailedPipelineTasks 寻找导致失败的节点
func (client *Client) FindCauseFailedPipelineTasks(pipelineID uint64) (spec.RerunFailedDetail, error) {
	var failedStageIndex int
	failedTasks := make(map[string]uint64, 0)
	successTasks := make(map[string]uint64, 0)
	notExecuteTasks := make(map[string]uint64, 0)
	stages, err := client.ListPipelineStageByPipelineID(pipelineID)
	if err != nil {
		return spec.RerunFailedDetail{}, err
	}
	var foundFailedStage bool
	for si, stage := range stages {
		tasks, err := client.ListPipelineTasksByStageID(stage.ID)
		if err != nil {
			return spec.RerunFailedDetail{}, err
		}
		if !foundFailedStage {
			// 存在 stage 状态与 task 状态不一致的情况，比如：
			//   stage 下有一个 repo 任务，repo 执行成功，此时被 pipeline 被取消，stage 状态为 stopByUser，
			//   repo 为 success，stage 真实状态应该为 success；
			// 因此需要根据 task 计算 stage 的真实状态
			stageStatus := statusutil.CalculatePipelineStatusV2(tasks)
			if stageStatus.IsFailedStatus() {
				failedStageIndex = si
				foundFailedStage = true
			}
		}
		for _, task := range tasks {
			if !foundFailedStage { // 如果还没找到 failed stage，则说明当前 stage 下的 task 都是成功的
				successTasks[task.Name] = task.ID
			} else {
				if si > failedStageIndex { // 如果已经找到 failed stage，并且当前 stage 比 failedStage 大，则都为未执行的
					notExecuteTasks[task.Name] = task.ID
				}
				if si < failedStageIndex { // 当前 stage 小于 failedStage，则都为成功
					successTasks[task.Name] = task.ID
				}
				if si == failedStageIndex { // failedStage，需要注意 task 的执行情况
					if task.Status.IsFailedStatus() {
						failedTasks[task.Name] = task.ID
					} else {
						successTasks[task.Name] = task.ID
					}
				}
			}
		}
	}
	if len(failedTasks) == 0 {
		return spec.RerunFailedDetail{}, errors.New("no failed-tasks need to rerun-failed, please check")
	}
	return spec.RerunFailedDetail{
		RerunPipelineID: pipelineID,
		StageIndex:      failedStageIndex,
		SuccessTasks:    successTasks,
		FailedTasks:     failedTasks,
		NotExecuteTasks: notExecuteTasks,
	}, nil
}

func (client *Client) GetPipelineTask(id interface{}) (spec.PipelineTask, error) {
	var pa spec.PipelineTask
	exist, err := client.ID(id).Get(&pa)
	if err != nil {
		return spec.PipelineTask{}, errors.Wrapf(err, "failed to get pipeline task by id [%v]", id)
	}
	if !exist {
		return spec.PipelineTask{}, errors.Errorf("not found pipeline task by id [%v]", id)
	}
	return pa, nil
}

func (client *Client) FindPipelineTaskByName(pipelineID uint64, name string) (spec.PipelineTask, error) {
	var pa = spec.PipelineTask{
		PipelineID: pipelineID,
		Name:       name,
	}
	exist, err := client.Get(&pa)
	if err != nil {
		return spec.PipelineTask{}, errors.Wrapf(err, "failed to get pipeline task, pipelineID [%d], name [%s]", pipelineID, name)
	}
	if !exist {
		return spec.PipelineTask{}, errors.Errorf("not found pipeline task, pipelineID [%d], name [%s]", pipelineID, name)
	}
	return pa, nil
}

func (client *Client) ListPipelineTasksByStageID(stageID uint64) ([]*spec.PipelineTask, error) {
	var actions []*spec.PipelineTask
	if err := client.Find(&actions, spec.PipelineTask{StageID: stageID}); err != nil {
		return nil, errors.Wrapf(err, "failed to list pipeline tasks by stageID [%d]", stageID)
	}
	return actions, nil
}

func (client *Client) ListPipelineTasksByPipelineID(pipelineID uint64, ops ...SessionOption) ([]spec.PipelineTask, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var tasks []spec.PipelineTask
	if err := session.Find(&tasks, spec.PipelineTask{PipelineID: pipelineID}); err != nil {
		return nil, errors.Wrapf(err, "failed to list pipeline tasks by pipelineID [%d]", pipelineID)
	}
	return tasks, nil
}

func (client *Client) UpdatePipelineTaskMetadata(id uint64, result *taskresult.Result) error {
	_, err := client.ID(id).Cols("result").Update(&spec.PipelineTask{Result: result})
	if err != nil {
		b, _ := json.Marshal(&result)
		return errors.Errorf("failed to update pipeline task result, taskID: %d, result: %s, err: %v", id, string(b), err)
	}
	return nil
}

func (client *Client) UpdatePipelineTaskInspect(id uint64, inspect taskinspect.Inspect) error {
	_, err := client.ID(id).Cols("inspect").Update(&spec.PipelineTask{Inspect: inspect})
	if err != nil {
		b, _ := json.Marshal(&inspect)
		return errors.Errorf("failed to update pipeline task inspect, taskID: %d, inspect: %s, err: %v", id, string(b), err)
	}
	return nil
}

func (client *Client) UpdatePipelineTask(id uint64, task *spec.PipelineTask, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	const maxRetryNum = 3
	retryNum := 0

	for {
		affectedRows, err := client.ID(id).Update(task)
		if err != nil {
			return err
		}
		if affectedRows == 0 && !session.AllowZeroAffectedRows {
			if retryNum < maxRetryNum {
				time.Sleep(time.Second * 2)
				retryNum++
				continue
			}
			logrus.Errorf("failed to update pipeline task, pipelineID: %d, taskID: %d, err: %v",
				task.PipelineID, task.ID, ErrZeroAffectedRows)
			return nil
		}
		return nil
	}
}

func (client *Client) UpdatePipelineTaskStatus(id uint64, status apistructs.PipelineStatus, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).Cols("status").Update(&spec.PipelineTask{Status: status})
	return err
}

// UpdatePipelineTaskTime update the costTime,timeBegin and timeEnd of pipeline task
func (client *Client) UpdatePipelineTaskTime(p *spec.Pipeline, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	if p.TimeBegin == nil || p.TimeBegin.IsZero() {
		p.TimeBegin = p.TimeCreated
	}
	if p.TimeEnd == nil || p.TimeEnd.IsZero() {
		p.TimeEnd = p.TimeUpdated
	}
	timeBegin, timeEnd := *p.TimeBegin, *p.TimeEnd

	costTimeSec := p.CostTimeSec
	if costTimeSec == -1 {
		costTimeSec = int64(timeEnd.Sub(timeBegin).Seconds())
	}
	_, err := session.ID(p.ParentTaskID).Cols("cost_time_sec", "time_begin", "time_end").
		Update(&spec.PipelineTask{CostTimeSec: costTimeSec, TimeBegin: timeBegin, TimeEnd: timeEnd})
	return err
}

func (client *Client) UpdatePipelineTaskContext(id uint64, ctx spec.PipelineTaskContext, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).Cols("context").Update(&spec.PipelineTask{Context: ctx})
	return err
}

func (client *Client) UpdatePipelineTaskExtra(id uint64, extra spec.PipelineTaskExtra, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).Cols("extra").Update(&spec.PipelineTask{Extra: extra})
	return err
}

func (client *Client) RefreshPipelineTask(task *spec.PipelineTask) error {
	r, err := client.GetPipelineTask(task.ID)
	if err != nil {
		return err
	}
	*task = r
	return nil
}

func (client *Client) ListPipelineTasksByTypeStatuses(typ string, statuses ...apistructs.PipelineStatus) ([]spec.PipelineTask, error) {
	var actionList []spec.PipelineTask
	if err := client.Where("type = ?", typ).In("status", statuses).Find(&actionList); err != nil {
		return nil, err
	}
	return actionList, nil
}

func (client *Client) DeletePipelineTasksByPipelineID(pipelineID uint64, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	return retry.DoWithInterval(func() error {
		_, err := session.Delete(&spec.PipelineTask{PipelineID: pipelineID})
		return err
	}, 3, time.Second)
}

func (client *Client) CleanPipelineTaskResult(id uint64, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	if _, err := session.Table("pipeline_tasks").
		Where("id = ?", id).Cols("result").Update(&spec.PipelineTask{Result: nil}); err != nil {
		return err
	}
	return nil
}

func (client *Client) BatchCreatePipelineTasks(pts []spec.PipelineTask, ops ...SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	task := &spec.PipelineTask{}
	_, err = session.Table(task.TableName()).InsertMulti(&pts)
	return err
}
