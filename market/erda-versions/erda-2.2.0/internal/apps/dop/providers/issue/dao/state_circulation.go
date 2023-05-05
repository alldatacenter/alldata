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
	"time"

	"github.com/google/uuid"
)

type IssueStateTransition struct {
	ID        string `gorm:"primary_key"`
	CreatedAt time.Time
	UpdatedAt time.Time
	ProjectID uint64
	IssueID   uint64
	StateFrom uint64
	StateTo   uint64
	Creator   string
}

func (IssueStateTransition) TableName() string {
	return "erda_issue_state_transition"
}

func (client *DBClient) CreateIssueStateTransition(statesTrans *IssueStateTransition) error {
	id, err := uuid.NewRandom()
	if err != nil {
		return err
	}
	statesTrans.ID = id.String()
	return client.Create(statesTrans).Error
}

func (client *DBClient) BatchCreateIssueTransition(statesTrans []IssueStateTransition) error {
	return client.BulkInsert(statesTrans)
}

func (client *DBClient) DeleteIssuesStateTransition(issueID uint64) error {
	return client.Model(&IssueStateTransition{}).Where("issue_id = ?", issueID).Delete(IssueStateTransition{}).Error
}

func (client *DBClient) ListStatesTransByProjectID(projectID uint64) ([]IssueStateTransition, error) {
	var statesTrans []IssueStateTransition
	db := client.Model(&IssueStateTransition{}).Where("project_id = ?", projectID)
	if err := db.Find(&statesTrans).Error; err != nil {
		return nil, err
	}
	return statesTrans, nil
}
