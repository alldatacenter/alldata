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

// CreateReviewUser 创建成员
func (client *DBClient) CreateReviewUser(review *model.ReviewUser) error {
	return client.Table("dice_manual_review_user").Create(review).Error
}

func (client *DBClient) GetReviewByTaskId(param *apistructs.GetReviewByTaskIdIdRequest) (apistructs.GetReviewByTaskIdIdResponse, error) {
	var total int
	var reviews []model.ManualReview

	err := client.Table("dice_manual_review").Where("task_id = ?", param.TaskId).Find(&reviews).Count(&total).Error
	var review apistructs.GetReviewByTaskIdIdResponse
	if len(reviews) == 0 {
		return review, nil
	}
	review.ApprovalStatus = reviews[0].ApprovalStatus
	review.Total = total
	review.Id = reviews[0].ID
	if err != nil {
		return review, err
	}
	return review, nil
}

func (client *DBClient) CreateReview(review *model.ManualReview) error {
	return client.Table("dice_manual_review").Create(review).Error
}

// GetReviewByID get review by id
func (client *DBClient) GetReviewByID(id int64) (review model.ManualReview, err error) {
	err = client.Table("dice_manual_review").Where("id = ?", id).First(&review).Error
	return
}

func (client *DBClient) UpdateApproval(param *apistructs.UpdateApproval) error {
	approvalStatus := "Accept"
	if param.Reject {
		approvalStatus = "Reject"
	}

	return client.Table("dice_manual_review").Where("org_id = ?", param.OrgId).Where("id = ?", param.Id).Updates(map[string]interface{}{"approval_status": approvalStatus, "approval_reason": param.Reason}).Error
}

func (client *DBClient) GetReviewsBySponsorId(param *apistructs.GetReviewsBySponsorIdRequest) (int, []model.ManualReview, error) {
	var reviews []model.ManualReview

	db := client.Table("dice_manual_review").Where("sponsor_id = ?", param.SponsorId).Where("org_id = ?", param.OrgId).Where("approval_status = ? ", param.ApprovalStatus)

	if param.Id != 0 {
		db = db.Where("id = ?", param.Id)
	}
	if param.ProjectId != 0 {
		db = db.Where("project_id = ?", param.ProjectId)
	}
	var total int
	err := db.Offset((param.PageNo - 1) * param.PageSize).Limit(param.PageSize).
		Find(&reviews).Offset(0).Limit(-1).Count(&total).Error
	if err != nil {
		return 0, nil, err
	}
	return total, reviews, nil
}

func (client *DBClient) GetReviewsByUserId(param *apistructs.GetReviewsByUserIdRequest, tasks []int64) (int, []model.ManualReview, error) {
	db := client.Table("dice_manual_review").Where("task_id in (?)", tasks)
	if param.Id != 0 {
		db = db.Where("id = ?", param.Id)
	}
	var state []string
	state = append(state, "Accept")
	state = append(state, "Reject")
	if param.ApprovalStatus == "pending" {
		db = db.Where("approval_status NOT IN (?)", state)
	} else {
		db = db.Where("approval_status in (?)", state)
	}

	if param.ProjectId != 0 {
		db = db.Where("project_id = ?", param.ProjectId)
	}

	if param.Operator != 0 {
		db = db.Where("sponsor_id = ?", param.Operator)
	}

	var total int
	var reviews []model.ManualReview

	err := db.Offset((param.PageNo - 1) * param.PageSize).Limit(param.PageSize).
		Find(&reviews).Offset(0).Limit(-1).Count(&total).Error
	if err != nil {
		return 0, nil, err
	}

	return total, reviews, nil
}
