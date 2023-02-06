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
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

var stateJoinSQL = "LEFT OUTER JOIN dice_issue_state_relations AS re on dice_issue_state.id=re.start_state_id"

type IssueStateJoinSQL struct {
	ID         int64  `gorm:"primary_key"`
	ProjectID  uint64 `gorm:"column:project_id"`
	IssueType  string `gorm:"column:issue_type"`
	Name       string `gorm:"column:name"`
	Belong     string `gorm:"column:belong"`
	Index      int64  `gorm:"column:index"`
	EndStateID int64  `gorm:"column:end_state_id"`
}
type IssueState struct {
	dbengine.BaseModel

	ProjectID uint64 `gorm:"column:project_id"`
	IssueType string `gorm:"column:issue_type"`
	Name      string `gorm:"column:name"`
	Belong    string `gorm:"column:belong"`
	Index     int64  `gorm:"column:index"`
	Role      string `gorm:"column:role"`
}

func (IssueState) TableName() string {
	return "dice_issue_state"
}

type IssueStateRelation struct {
	dbengine.BaseModel

	ProjectID    int64  `gorm:"column:project_id"`
	IssueType    string `gorm:"column:issue_type"`
	StartStateID int64  `gorm:"column:start_state_id"`
	EndStateID   int64  `gorm:"column:end_state_id"`
}

func (IssueStateRelation) TableName() string {
	return "dice_issue_state_relations"
}

func (client *DBClient) CreateIssuesState(state *IssueState) error {
	return client.Create(state).Error
}

func (client *DBClient) DeleteIssuesState(id int64) error {
	return client.Table("dice_issue_state").Where("id = ?", id).Delete(IssueState{}).Error
}

func (client *DBClient) DeleteIssuesStateRelationByStartID(id int64) error {
	return client.Table("dice_issue_state_relations").Where("start_state_id = ?", id).Delete(IssueStateRelation{}).Error
}

func (client *DBClient) DeleteIssuesStateByProjectID(projectID int64) error {
	err := client.Table("dice_issue_state").Where("project_id = ?", projectID).Delete(IssueState{}).Error
	if err != nil {
		return err
	}
	err = client.Table("dice_issue_state_relations").Where("project_id = ?", projectID).Delete(IssueStateRelation{}).Error
	return err
}

func (client *DBClient) UpdateIssuesStateIndex(id int64, index int64) error {
	return client.Table("dice_issue_state").Where("id = ?", id).Update("index", index).Error
}

func (client *DBClient) UpdateIssueStateRelations(projectID int64, issueType string, StateRelations []IssueStateRelation) error {
	if err := client.Table("dice_issue_state_relations").Where("project_id = ?", projectID).
		Where("issue_type = ?", issueType).Delete(IssueStateRelation{}).Error; err != nil {
		return err
	}
	return client.BulkInsert(StateRelations)
}

func (client *DBClient) UpdateIssueState(state *IssueState) error {
	return client.Save(state).Error
}

func (client *DBClient) UpdateIssuesStateBelong(id int64, belong apistructs.IssueStateBelong) error {
	return client.Table("dice_issue_state_relations").Where("id = ?", id).Update("state_belong", belong).Error
}

func (client *DBClient) GetIssuesStateRelations(projectID uint64, issueType string) ([]IssueStateJoinSQL, error) {
	var issueStateJoinSql []IssueStateJoinSQL
	db := client.Table("dice_issue_state").Select("*").Joins(stateJoinSQL).Where("dice_issue_state.project_id = ?", projectID)
	if issueType != "" {
		db = db.Where("dice_issue_state.issue_type = ?", issueType)
	}
	if err := db.Order("dice_issue_state.issue_type,dice_issue_state.index").
		Select("dice_issue_state.id,dice_issue_state.project_id,dice_issue_state.issue_type,dice_issue_state.name,dice_issue_state.belong,dice_issue_state.index,re.end_state_id").
		Scan(&issueStateJoinSql).Error; err != nil {
		return nil, err
	}
	return issueStateJoinSql, nil
}

func (client *DBClient) GetIssuesStatesByProjectID(projectID uint64, issueType string) ([]IssueState, error) {
	var states []IssueState
	db := client.Table("dice_issue_state").Where("project_id = ?", projectID)
	if issueType != "" {
		db = db.Where("issue_type = ?", issueType)
	}
	if err := db.Order("index").Find(&states).Error; err != nil {
		return nil, err
	}
	return states, nil
}

func (client *DBClient) GetIssuesStates(req *pb.GetIssueStatesRequest) ([]IssueState, error) {
	var states []IssueState
	db := client.Table("dice_issue_state").Select("id").Where("project_id = ?", req.ProjectID)
	if req.IssueType != "" {
		db = db.Where("issue_type = ?", req.IssueType)
	}
	if len(req.StateBelongs) > 0 {
		db = db.Where("belong IN (?)", req.StateBelongs)
	}
	if err := db.Order("index").Find(&states).Error; err != nil {
		return nil, err
	}
	return states, nil
}

func (client *DBClient) GetIssuesStatesByTypes(req *apistructs.IssueStatesRequest) ([]IssueState, error) {
	var states []IssueState
	db := client.Table("dice_issue_state").Select("id").Where("project_id = ?", req.ProjectID)
	if len(req.IssueType) > 0 {
		db = db.Where("issue_type IN (?)", req.IssueType)
	}
	if len(req.StateBelongs) > 0 {
		db = db.Where("belong IN (?)", req.StateBelongs)
	}
	if err := db.Order("'issue_type', 'index'").Find(&states).Error; err != nil {
		return nil, err
	}
	return states, nil
}

// get all state by projectID list
func (client *DBClient) GetIssuesStatesByProjectIDList(projectIDList []uint64) ([]IssueState, error) {
	var states []IssueState
	db := client.Table("dice_issue_state").Where("project_id in (?)", projectIDList)
	if err := db.Order("index").Find(&states).Error; err != nil {
		return nil, err
	}
	return states, nil
}

func (client *DBClient) GetIssueStateByID(ID int64) (*IssueState, error) {
	var state IssueState
	db := client.Table("dice_issue_state").Where("id = ?", ID)
	if err := db.Find(&state).Error; err != nil {
		return nil, err
	}
	return &state, nil
}

func (client *DBClient) GetIssueStateByIDs(ID []int64) ([]IssueState, error) {
	var state []IssueState
	db := client.Table("dice_issue_state").Where("id in (?)", ID)
	if err := db.Find(&state).Error; err != nil {
		return nil, err
	}
	return state, nil
}

func (client *DBClient) GetIssueStatePermission(role string, st int64, ed int64) (*IssueStateRelation, error) {
	// 是否有权限转移至ed状态
	var state IssueState
	db := client.Table("dice_issue_state").Where("id = ?", ed).Where("role LIKE ?", "%"+role+"%")
	if err := db.Find(&state).Error; err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	// 有权限，继续搜索是否能从st状态到达ed状态
	var permission IssueStateRelation
	db = client.Where("start_state_id = ?", st).
		Where("end_state_id = ?", ed)
	if err := db.Limit(1).Find(&permission).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &permission, nil
}

// GetClosedBugState 获取一个项目下已关闭的bug状态id
func (client *DBClient) GetClosedBugState(projectID int64) ([]IssueState, error) {
	var state []IssueState
	db := client.Table("dice_issue_state").Where("project_id = ?", projectID).Where("issue_type = ?", "BUG").
		Where("belong = ?", "CLOSED")
	if err := db.Find(&state).Error; err != nil {
		return nil, err
	}
	return state, nil
}
