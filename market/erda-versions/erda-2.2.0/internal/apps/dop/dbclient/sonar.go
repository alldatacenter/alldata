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
)

// QASonar 存储sonar分析的结果，对应数据库表qa_sonar
type QASonar struct {
	ID        int64     `xorm:"pk autoincr" json:"id"`
	CreatedAt time.Time `xorm:"created" json:"createdAt"`
	UpdatedAt time.Time `xorm:"updated" json:"updatedAt"`

	ApplicationID    int64  `xorm:"app_id" json:"applicationId"`
	ProjectID        int64  `xorm:"project_id" json:"projectId" validate:"required"`
	BuildID          int64  `xorm:"build_id" json:"buildId"`
	LogID            string `xorm:"log_id" json:"logId"`
	ApplicationName  string `xorm:"app_name" json:"applicationName"`
	OperatorID       string `xorm:"operator_id" json:"operatorId" validate:"required"`
	CommitID         string `xorm:"commit_id" json:"commitId"`
	Branch           string `xorm:"branch" json:"branch" validate:"required"`
	GitRepo          string `xorm:"git_repo" json:"gitRepo" validate:"required"`
	Key              string `xorm:"not null VARCHAR(255)" json:"key,omitempty"`
	Bugs             string `xorm:"longtext" json:"bugs,omitempty"`
	CodeSmells       string `xorm:"longtext" json:"code_smells,omitempty"`
	Vulnerabilities  string `xorm:"longtext" json:"vulnerabilities,omitempty"`
	Coverage         string `xorm:"longtext" json:"coverage,omitempty"`
	Duplications     string `xorm:"longtext" json:"duplications,omitempty"`
	IssuesStatistics string `xorm:"text" json:"issues_statistics,omitempty"`
}

// TableName QASonar对应的数据库表qa_sonar
func (QASonar) TableName() string {
	return "qa_sonar"
}
