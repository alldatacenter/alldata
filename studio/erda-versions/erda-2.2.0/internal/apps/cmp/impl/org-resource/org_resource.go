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

package org_resource

import (
	"strconv"

	"github.com/go-redis/redis"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
)

type OrgResource struct {
	db       *dbclient.DBClient
	bdl      *bundle.Bundle
	redisCli *redis.Client
}

type Option func(*OrgResource)

func New(options ...Option) *OrgResource {
	o := &OrgResource{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(o *OrgResource) {
		o.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(o *OrgResource) {
		o.bdl = bdl
	}
}

// WithRedisClient 配置 redis client
func WithRedisClient(cli *redis.Client) Option {
	return func(o *OrgResource) {
		o.redisCli = cli
	}
}

// ListOrgRunningTasks impl: list running task or service by orgID
func (o *OrgResource) ListOrgRunningTasks(param *apistructs.OrgRunningTasksListRequest,
	orgID int64) (int64, []apistructs.OrgRunningTasks, error) {
	var (
		total       int64
		resultTasks []apistructs.OrgRunningTasks
	)

	if param.Type == "deployment" {
		totalCount, deployments, err := o.db.ListDeploymentsByOrgID(param, uint64(orgID))
		if err != nil {
			return 0, nil, err
		}

		for _, dep := range *deployments {
			taskData := apistructs.OrgRunningTasks{
				OrgID:           dep.OrgID,
				ProjectID:       dep.ProjectID,
				ApplicationID:   dep.ApplicationID,
				PipelineID:      dep.PipelineID,
				TaskID:          dep.TaskID,
				QueueTimeSec:    dep.QueueTimeSec,
				CostTimeSec:     dep.CostTimeSec,
				Status:          dep.Status,
				Env:             dep.Env,
				ClusterName:     dep.ClusterName,
				CreatedAt:       dep.CreatedAt,
				ProjectName:     dep.ProjectName,
				ApplicationName: dep.ApplicationName,
				TaskName:        dep.TaskName,
				RuntimeID:       dep.RuntimeID,
				ReleaseID:       dep.ReleaseID,
				UserID:          dep.UserID,
			}

			resultTasks = append(resultTasks, taskData)
		}

		total = totalCount
	} else if param.Type == "job" {
		totalCount, jobs, err := o.db.ListJobsByOrgID(param, uint64(orgID))
		if err != nil {
			return 0, nil, err
		}

		for _, job := range *jobs {
			taskData := apistructs.OrgRunningTasks{
				OrgID:           job.OrgID,
				ProjectID:       job.ProjectID,
				ApplicationID:   job.ApplicationID,
				PipelineID:      job.PipelineID,
				TaskID:          job.TaskID,
				QueueTimeSec:    job.QueueTimeSec,
				CostTimeSec:     job.CostTimeSec,
				Status:          job.Status,
				Env:             job.Env,
				ClusterName:     job.ClusterName,
				CreatedAt:       job.CreatedAt,
				ProjectName:     job.ProjectName,
				ApplicationName: job.ApplicationName,
				TaskName:        job.TaskName,
				TaskType:        job.TaskType,
				UserID:          job.UserID,
			}

			resultTasks = append(resultTasks, taskData)
		}

		total = totalCount
	}

	return total, resultTasks, nil
}

// DealReceiveTaskEvent 处理接收到 pipeline 的 task 事件
func (o *OrgResource) DealReceiveTaskEvent(req *apistructs.PipelineTaskEvent) (int64, error) {
	// 参数校验
	if err := o.checkReceiveTaskEventParam(req); err != nil {
		// 若订阅的事件参数校验失败，则无需处理
		return 0, nil
	}

	// task如果是dice，则属于deployment
	// 如果是其它，则属于job
	if req.Content.ActionType == "dice" {
		return o.dealReceiveDeploymentEvent(req)
	} else {
		return o.dealReceiveJobEvent(req)
	}
}

func (o *OrgResource) checkReceiveTaskEventParam(req *apistructs.PipelineTaskEvent) error {
	if req.OrgID == "" {
		return errors.Errorf("OrgID is empty")
	}

	if req.Content.Status == "" {
		return errors.Errorf("status is empty")
	}

	if req.Content.ActionType == "" {
		return errors.Errorf("actionType is empty")
	}

	if req.Content.PipelineTaskID == 0 {
		return errors.Errorf("pipelineTaskID is empty")
	}

	return nil
}

func (o *OrgResource) dealReceiveDeploymentEvent(req *apistructs.PipelineTaskEvent) (int64, error) {
	// 判断记录是否已存在，存在只是更新状态
	deployments := o.db.GetDeployment(req.OrgID, req.Content.PipelineTaskID)
	if deployments != nil && len(deployments) > 0 {
		deployment := &deployments[0]
		// delete other running task record
		for i, task := range deployments {
			if i == 0 {
				continue
			}

			o.db.DeleteDeployment(req.OrgID, task.TaskID)
		}

		if deployment.Status == req.Content.Status && deployment.RuntimeID == req.Content.RuntimeID &&
			deployment.ReleaseID == req.Content.ReleaseID {
			return deployment.ID, nil
		} else {
			deployment.Status = req.Content.Status
			deployment.RuntimeID = req.Content.RuntimeID
			deployment.ReleaseID = req.Content.ReleaseID
		}

		return deployment.ID, o.db.UpdateDeploymentStatus(deployment)
	}

	// 正在运行的任务信息入库
	orgID, err := strconv.ParseInt(req.OrgID, 10, 64)
	if err != nil {
		return 0, err
	}

	projectID, err := strconv.ParseInt(req.ProjectID, 10, 64)
	if err != nil {
		return 0, err
	}

	appID, err := strconv.ParseInt(req.ApplicationID, 10, 64)
	if err != nil {
		return 0, err
	}

	task := &dbclient.Deployments{
		OrgID:           uint64(orgID),
		ProjectID:       uint64(projectID),
		ApplicationID:   uint64(appID),
		PipelineID:      req.Content.PipelineID,
		TaskID:          req.Content.PipelineTaskID,
		QueueTimeSec:    req.Content.QueueTimeSec,
		CostTimeSec:     req.Content.CostTimeSec,
		Status:          req.Content.Status,
		Env:             req.Env,
		ClusterName:     req.Content.ClusterName,
		UserID:          req.Content.UserID,
		CreatedAt:       req.Content.CreatedAt,
		ProjectName:     req.Content.ProjectName,
		ApplicationName: req.Content.ApplicationName,
		TaskName:        req.Content.TaskName,
		RuntimeID:       req.Content.RuntimeID,
		ReleaseID:       req.Content.ReleaseID,
	}

	if err := o.db.CreateDeployment(task); err != nil {
		return 0, err
	}

	return task.ID, nil
}

func (o *OrgResource) dealReceiveJobEvent(req *apistructs.PipelineTaskEvent) (int64, error) {
	// 判断记录是否已存在，存在只是更新状态
	jobs := o.db.GetJob(req.OrgID, req.Content.PipelineTaskID)
	if jobs != nil && len(jobs) > 0 {
		job := &jobs[0]
		// delete other running task record
		for i, task := range jobs {
			if i == 0 {
				continue
			}

			o.db.DeleteJob(req.OrgID, task.TaskID)
		}

		if job.Status == req.Content.Status {
			return job.ID, nil
		} else {
			job.Status = req.Content.Status
		}

		return job.ID, o.db.UpdateJobStatus(job)
	}

	// 正在运行的任务信息入库
	orgID, err := strconv.ParseInt(req.OrgID, 10, 64)
	if err != nil {
		return 0, err
	}

	projectID, _ := strconv.ParseInt(req.ProjectID, 10, 64)

	appID, _ := strconv.ParseInt(req.ApplicationID, 10, 64)

	task := &dbclient.Jobs{
		OrgID:           uint64(orgID),
		ProjectID:       uint64(projectID),
		ApplicationID:   uint64(appID),
		PipelineID:      req.Content.PipelineID,
		TaskID:          req.Content.PipelineTaskID,
		QueueTimeSec:    req.Content.QueueTimeSec,
		CostTimeSec:     req.Content.CostTimeSec,
		Status:          req.Content.Status,
		Env:             req.Env,
		ClusterName:     req.Content.ClusterName,
		UserID:          req.Content.UserID,
		CreatedAt:       req.Content.CreatedAt,
		ProjectName:     req.Content.ProjectName,
		ApplicationName: req.Content.ApplicationName,
		TaskName:        req.Content.TaskName,
	}

	if err := o.db.CreateJob(task); err != nil {
		return 0, err
	}

	return task.ID, nil
}

// DealReceiveTaskRuntimeEvent 处理接收到 pipeline 的 runtimeID 事件
func (o *OrgResource) DealReceiveTaskRuntimeEvent(req *apistructs.PipelineTaskEvent) (int64, error) {
	// check param
	if err := o.checkReceiveTaskRuntimeEventParam(req); err != nil {
		// 若订阅的事件参数校验失败，则无需处理
		return 0, nil
	}

	// 更新状态 runtimeID, releaseID
	jobs := o.db.GetDeployment(req.OrgID, req.Content.PipelineTaskID)
	if jobs != nil && len(jobs) > 0 {
		job := &jobs[0]
		// delete other running job record
		for i, task := range jobs {
			if i == 0 {
				continue
			}

			o.db.DeleteDeployment(req.OrgID, task.TaskID)
		}

		if job.RuntimeID == req.Content.RuntimeID &&
			job.ReleaseID == req.Content.ReleaseID {
			return job.ID, nil
		} else {
			job.RuntimeID = req.Content.RuntimeID
			job.ReleaseID = req.Content.ReleaseID
		}

		return job.ID, o.db.UpdateDeploymentStatus(job)
	}

	return 0, nil
}

func (o *OrgResource) checkReceiveTaskRuntimeEventParam(req *apistructs.PipelineTaskEvent) error {
	if req.OrgID == "" {
		return errors.Errorf("OrgID is empty")
	}

	if req.Content.RuntimeID == "" {
		return errors.Errorf("RuntimeID is empty")
	}

	if req.Content.PipelineTaskID == 0 {
		return errors.Errorf("pipelineTaskID is empty")
	}

	return nil
}
