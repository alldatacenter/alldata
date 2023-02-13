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

package issuestate

import (
	_ "embed"
	"encoding/json"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

var (
	//go:embed state-zh_CN.json
	zhStateConfig   []byte
	zhStateInitData []apistructs.StateDefinitionCustomizeData
	//go:embed state-en_US.json
	enStateConfig   []byte
	enStateInitData []apistructs.StateDefinitionCustomizeData

	stateInitRole = "Ops,Dev,QA,Owner,Lead"
)

func init() {
	if err := json.Unmarshal(zhStateConfig, &zhStateInitData); err != nil {
		panic(err)
	}
	if err := json.Unmarshal(enStateConfig, &enStateInitData); err != nil {
		panic(err)
	}
}

// IssueState issue state service 对象
type IssueState struct {
	bdl *bundle.Bundle
	db  IssueStater
}

// Option 定义 IssueState 对象配置选项
type Option func(*IssueState)

// New 新建 issue state 对象
func New(options ...Option) *IssueState {
	is := &IssueState{}
	for _, op := range options {
		op(is)
	}
	return is
}

// WithDBClient 配置 db client
func WithDBClient(db IssueStater) Option {
	return func(is *IssueState) {
		is.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(is *IssueState) {
		is.bdl = bdl
	}
}

// CreateIssueState 创建事件状态请求
func (is *IssueState) CreateIssueState(req *apistructs.IssueStateCreateRequest) (*dao.IssueState, error) {
	states, err := is.db.GetIssuesStatesByProjectID(req.ProjectID, req.IssueType)
	var maxIndex int64 = -1
	for _, v := range states {
		if v.Index > maxIndex {
			maxIndex = v.Index
		}
	}
	if err != nil {
		return nil, err
	}
	createState := &dao.IssueState{
		ProjectID: req.ProjectID,
		IssueType: req.IssueType,
		Name:      req.StateName,
		Belong:    req.StateBelong,
		Index:     maxIndex + 1,
		Role:      "Ops,Dev,QA,Owner,Lead",
	}
	if err = is.db.CreateIssuesState(createState); err != nil {
		return nil, err
	}
	return createState, nil
}

// DeleteIssueState 删除事件状态请求
func (is *IssueState) DeleteIssueState(stateID int64) error {
	// 如果有事件是该状态则不可删除
	_, err := is.db.GetIssueByState(stateID)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return err
		}
	} else {
		return apierrors.ErrDeleteIssueState.InvalidState("有事件处于该状态,不可删除")
	}
	// 删除该状态的关联
	if err := is.db.DeleteIssuesStateRelationByStartID(stateID); err != nil {
		return err
	}
	// 删除状态
	if err := is.db.DeleteIssuesState(stateID); err != nil {
		return err
	}
	return nil
}

// GetIssueStates 获取状态列表请求
func (is *IssueState) GetIssueStates(req *apistructs.IssueStatesGetRequest) ([]apistructs.IssueTypeState, error) {
	var states []apistructs.IssueTypeState
	st, err := is.db.GetIssuesStatesByProjectID(req.ProjectID, "")
	if err != nil {
		return nil, err
	}
	states = append(states, apistructs.IssueTypeState{
		IssueType: apistructs.IssueTypeTask,
	})
	states = append(states, apistructs.IssueTypeState{
		IssueType: apistructs.IssueTypeRequirement,
	})
	states = append(states, apistructs.IssueTypeState{
		IssueType: apistructs.IssueTypeBug,
	})
	states = append(states, apistructs.IssueTypeState{
		IssueType: apistructs.IssueTypeEpic,
	})
	states = append(states, apistructs.IssueTypeState{
		IssueType: apistructs.IssueTypeTicket,
	})
	for _, v := range st {
		if v.IssueType == apistructs.IssueTypeTask {
			states[0].State = append(states[0].State, v.Name)
		} else if v.IssueType == apistructs.IssueTypeRequirement {
			states[1].State = append(states[1].State, v.Name)
		} else if v.IssueType == apistructs.IssueTypeBug {
			states[2].State = append(states[2].State, v.Name)
		} else if v.IssueType == apistructs.IssueTypeEpic {
			states[3].State = append(states[3].State, v.Name)
		} else if v.IssueType == apistructs.IssueTypeTicket {
			states[4].State = append(states[4].State, v.Name)
		}
	}
	return states, nil
}

func (is *IssueState) GetIssueStatesMap(req *apistructs.IssueStatesGetRequest) (map[apistructs.IssueType][]apistructs.IssueStatus, error) {
	stateMap := map[apistructs.IssueType][]apistructs.IssueStatus{
		apistructs.IssueTypeRequirement: make([]apistructs.IssueStatus, 0),
		apistructs.IssueTypeTask:        make([]apistructs.IssueStatus, 0),
		apistructs.IssueTypeBug:         make([]apistructs.IssueStatus, 0),
		apistructs.IssueTypeTicket:      make([]apistructs.IssueStatus, 0),
	}
	st, err := is.db.GetIssuesStatesByProjectID(req.ProjectID, "")
	if err != nil {
		return nil, err
	}
	for _, v := range st {
		if _, ok := stateMap[v.IssueType]; ok {
			stateMap[v.IssueType] = append(stateMap[v.IssueType], apistructs.IssueStatus{
				ProjectID:   v.ProjectID,
				IssueType:   v.IssueType,
				StateID:     int64(v.ID),
				StateName:   v.Name,
				StateBelong: v.Belong,
				Index:       v.Index,
			})
		}
	}
	return stateMap, nil
}

func (is *IssueState) GetIssueStateIDs(req *apistructs.IssueStatesGetRequest) ([]int64, error) {
	st, err := is.db.GetIssuesStates(req)
	if err != nil {
		return nil, err
	}
	res := make([]int64, 0)
	for _, v := range st {
		res = append(res, int64(v.ID))
	}
	return res, nil
}

func (is *IssueState) GetIssuesStatesByID(id int64) (*apistructs.IssueStatus, error) {
	state, err := is.db.GetIssueStateByID(id)
	if err != nil {
		return nil, err
	}
	status := &apistructs.IssueStatus{
		ProjectID:   state.ProjectID,
		IssueType:   state.IssueType,
		StateID:     int64(state.ID),
		StateName:   state.Name,
		StateBelong: state.Belong,
		Index:       state.Index,
	}
	return status, nil
}

func (is *IssueState) GetIssuesStatesNameByID(id []int64) ([]apistructs.IssueStatus, error) {
	state, err := is.db.GetIssueStateByIDs(id)
	if err != nil {
		return nil, err
	}
	var status []apistructs.IssueStatus
	for _, v := range state {
		status = append(status, apistructs.IssueStatus{
			ProjectID:   v.ProjectID,
			IssueType:   v.IssueType,
			StateID:     int64(v.ID),
			StateName:   v.Name,
			StateBelong: v.Belong,
			Index:       v.Index,
		})
	}

	return status, nil
}

func (is *IssueState) getStateInitData(locale string) []apistructs.StateDefinitionCustomizeData {
	switch locale {
	case "zh-CN":
		return zhStateInitData
	case "en-US":
		return enStateInitData
	default:
		return zhStateInitData
	}
}

func (is *IssueState) InitProjectState(projectID int64, locale string) error {
	initData := is.getStateInitData(locale)
	for _, stateData := range initData {
		var (
			states    []dao.IssueState
			relations []dao.IssueStateRelation
		)
		for _, initState := range stateData.States {
			state := dao.IssueState{
				ProjectID: uint64(projectID),
				IssueType: stateData.IssueType,
				Name:      initState.Name,
				Belong:    initState.IssueStateBelong,
				Index:     initState.Index,
				Role:      stateInitRole,
			}
			if err := is.db.CreateIssuesState(&state); err != nil {
				return err
			}
			states = append(states, state)
		}
		for _, customRelation := range stateData.Relations {
			relation := dao.IssueStateRelation{
				ProjectID:    projectID,
				IssueType:    stateData.IssueType,
				StartStateID: int64(states[customRelation.From].ID),
				EndStateID:   int64(states[customRelation.To].ID),
			}
			relations = append(relations, relation)
		}
		if err := is.db.UpdateIssueStateRelations(projectID, stateData.IssueType, relations); err != nil {
			return err
		}
	}
	return nil
}

type IssueStater interface {
	UpdateIssueStateRelations(projectID int64, issueType apistructs.IssueType, StateRelations []dao.IssueStateRelation) error
	CreateIssuesState(state *dao.IssueState) error
	GetIssuesStatesByProjectID(projectID uint64, issueType apistructs.IssueType) ([]dao.IssueState, error)
	GetIssueStateByIDs(ID []int64) ([]dao.IssueState, error)
	GetIssueStateByID(ID int64) (*dao.IssueState, error)
	GetIssuesStates(req *apistructs.IssueStatesGetRequest) ([]dao.IssueState, error)
	GetIssuesStatesByTypes(req *apistructs.IssueStatesRequest) ([]dao.IssueState, error)
	GetIssueByState(state int64) (*dao.Issue, error)
	DeleteIssuesStateRelationByStartID(id int64) error
	DeleteIssuesState(id int64) error
	GetIssuesStateRelations(projectID uint64, issueType apistructs.IssueType) ([]dao.IssueStateJoinSQL, error)
	UpdateIssueState(state *dao.IssueState) error
}
