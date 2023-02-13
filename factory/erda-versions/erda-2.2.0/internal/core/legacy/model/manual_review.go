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

package model

type ManualReview struct {
	BaseModel
	BuildId         int    `gorm:"build_id"`
	ProjectId       int    `gorm:"project_id"`
	ApplicationId   int    `gorm:"application_id"`
	ApplicationName string `gorm:"application_name"`
	SponsorId       string `gorm:"sponsor_id"`
	CommitID        string `gorm:"commit_id"`
	OrgId           int64  `gorm:"org_id"`
	TaskId          int    `gorm:"task_id"`
	ProjectName     string `gorm:"project_name"`
	BranchName      string `gorm:"branch_name"`
	ApprovalStatus  string `gorm:"approval_status"`
	CommitMessage   string `gorm:"commit_message"`
	ApprovalReason  string `gorm:"approval_reason"`
}

type ReviewUser struct {
	BaseModel
	Operator string `gorm:"operator"`
	OrgId    int64  `gorm:"org_id"`
	TaskId   int64  `gorm:"task_id"`
}
