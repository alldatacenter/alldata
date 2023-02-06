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
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
)

// ListJobsByOrgID 获取指定企业的job列表
func (c *DBClient) ListJobsByOrgID(param *apistructs.OrgRunningTasksListRequest,
	orgID uint64) (int64, *[]Jobs, error) {
	var (
		total   int64
		jobs    []Jobs
		endedAt time.Time
		err     error
	)

	db := c.DB.Where("org_id = ?", orgID)
	if param.Cluster != "" {
		db = db.Where("cluster_name = ?", param.Cluster)
	}
	if param.ProjectName != "" {
		db = db.Where("project_name = ?", param.ProjectName)
	}
	if param.AppName != "" {
		db = db.Where("application_name = ?", param.AppName)
	}
	if param.PipelineID != 0 {
		db = db.Where("pipeline_id = ?", param.PipelineID)
	}
	if param.Status != "" {
		db = db.Where("status = ?", param.Status)
	}
	if param.UserID != "" {
		db = db.Where("user_id = ?", param.UserID)
	}
	if param.Env != "" {
		db = db.Where("env = ?", param.Env)
	}

	if param.EndTime == 0 {
		endedAt = time.Now()
	} else {
		endedAt = time.Unix(0, param.EndTime*1000000)
	}
	db = db.Where("created_at < ?", endedAt)
	if param.StartTime != 0 {
		startedAt := time.Unix(0, param.StartTime*1000000)
		db = db.Where("created_at > ?", startedAt)
	}

	if err = db.Order("updated_at DESC").
		Offset((param.PageNo - 1) * param.PageSize).
		Limit(param.PageSize).Find(&jobs).Error; err != nil {
		return 0, nil, err
	}

	// 符合条件的 job 总量
	if err = db.Model(&Jobs{}).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, &jobs, nil
}

// DeleteJob 删除 Job 信息
func (c *DBClient) DeleteJob(orgID string, taskID uint64) error {
	return c.Where("org_id = ?", orgID).Where("task_id = ?", taskID).
		Delete(&Jobs{}).Error
}

// GetJob 获取job信息
func (c *DBClient) GetJob(orgID string, taskID uint64) []Jobs {
	var Jobs []Jobs
	if err := c.Where("org_id = ?", orgID).
		Where("task_id = ?", taskID).
		Find(&Jobs).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil
		}
		return nil
	}
	return Jobs
}

// UpdateJobStatus 更新Job状态
func (c *DBClient) UpdateJobStatus(Job *Jobs) error {
	return c.Save(Job).Error
}

// CreateJob 创建正在运行的Job
func (c *DBClient) CreateJob(Job *Jobs) error {
	return c.Create(Job).Error
}

// ListExpiredJobs 列出过期的job
func (c *DBClient) ListExpiredJobs(startTime string) []Jobs {
	var jobs []Jobs
	if err := c.Where("created_at < ?", startTime).
		Find(&jobs).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil
		}
		return nil
	}
	return jobs
}

// ListRunningJobs list running job
func (c *DBClient) ListRunningJobs() []Jobs {
	var jobs []Jobs
	if err := c.Where("status not in (?)",
		[]string{"Success", "AnalyzeFailed", "Timeout", "StopByUser", "Failed", "NoNeedBySystem"}).
		Find(&jobs).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil
		}
		return nil
	}
	return jobs
}
