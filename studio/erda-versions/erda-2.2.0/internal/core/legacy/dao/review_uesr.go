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

package dao

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

func (client *DBClient) GetOperatorByTaskID(taskID []int) ([]model.ReviewUser, error) {
	var reviewUsers []model.ReviewUser
	err := client.Table("dice_manual_review_user").Where("task_id in (?)", taskID).Find(&reviewUsers).Error
	if err != nil {
		return nil, err
	}
	return reviewUsers, nil
}

func (client *DBClient) GetTaskIDByOperator(param *apistructs.GetReviewsByUserIdRequest) ([]int64, error) {
	user := client.Table("dice_manual_review_user").Where("operator = ?", param.UserId).Where("org_id = ?", param.OrgId)
	var ids []model.ReviewUser
	err := user.Scan(&ids).Error
	if err != nil {
		return nil, err
	}
	var tasks []int64
	for _, v := range ids {
		tasks = append(tasks, v.TaskId)
	}
	return tasks, nil
}

func (client *DBClient) GetAuthorityByUserId(param *apistructs.GetAuthorityByUserIdRequest) (apistructs.GetAuthorityByUserIdResponse, error) {
	var total int
	var authority apistructs.GetAuthorityByUserIdResponse
	authority.Authority = "NO"
	err := client.Table("dice_manual_review_user").Where("operator = ?", param.Operator).Where("org_id = ?", param.OrgId).Where("task_id = ?", param.TaskId).Count(&total).Error
	if err != nil {
		return authority, err
	}
	if total > 0 {
		authority.Authority = "YES"
	}
	return authority, nil
}
