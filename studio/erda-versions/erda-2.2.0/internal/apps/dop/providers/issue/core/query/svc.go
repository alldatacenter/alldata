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

package query

import (
	"fmt"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func (p *provider) GetAllIssuesByProject(req pb.IssueListRequest) ([]dao.IssueItem, error) {
	return p.db.ListIssueItems(req)
}

func (p *provider) GetIssuesStatesByProjectID(projectID uint64, issueType string) ([]dao.IssueState, error) {
	return p.db.GetIssuesStatesByProjectID(projectID, issueType)
}

func (p *provider) GetIssueLabelsByProjectID(projectID uint64) ([]dao.IssueLabel, error) {
	return p.db.GetIssueLabelsByProjectID(projectID)
}

func (p *provider) GetIssueItem(id uint64) (*dao.IssueItem, error) {
	issue, err := p.db.GetIssueItem(id)
	if err != nil {
		return nil, err
	}
	if err := p.IssueChildrenCount(&issue); err != nil {
		return nil, err
	}
	return &issue, nil
}

func (p *provider) IssueChildrenCount(issue *dao.IssueItem) error {
	countList, err := p.db.IssueChildrenCount([]uint64{issue.ID}, []string{apistructs.IssueRelationInclusion})
	if err != nil {
		return err
	}
	if len(countList) > 0 {
		issue.ChildrenLength = countList[0].Count
	}
	return nil
}

func (p *provider) GetIssueParents(issueID uint64, relationType []string) ([]dao.IssueItem, error) {
	issues, err := p.db.GetIssueParents(issueID, relationType)
	if err != nil {
		return nil, err
	}
	if err := p.SetIssueChildrenCount(issues); err != nil {
		return nil, err
	}
	return issues, nil
}

func (p *provider) ListStatesTransByProjectID(projectID uint64) ([]dao.IssueStateTransition, error) {
	return p.db.ListStatesTransByProjectID(projectID)
}

func (p *provider) GetIssueStateIDsByTypes(req *apistructs.IssueStatesRequest) ([]int64, error) {
	st, err := p.db.GetIssuesStatesByTypes(req)
	if err != nil {
		return nil, err
	}
	res := make([]int64, 0)
	for _, v := range st {
		res = append(res, int64(v.ID))
	}
	return res, nil
}

func (p *provider) GetIssueStatesMap(req *pb.GetIssueStatesRequest) (map[string][]pb.IssueStatus, error) {
	stateMap := map[string][]pb.IssueStatus{
		pb.IssueTypeEnum_REQUIREMENT.String(): make([]pb.IssueStatus, 0),
		pb.IssueTypeEnum_TASK.String():        make([]pb.IssueStatus, 0),
		pb.IssueTypeEnum_BUG.String():         make([]pb.IssueStatus, 0),
		pb.IssueTypeEnum_TICKET.String():      make([]pb.IssueStatus, 0),
	}
	st, err := p.db.GetIssuesStatesByProjectID(req.ProjectID, "")
	if err != nil {
		return nil, err
	}
	for _, v := range st {
		if _, ok := stateMap[v.IssueType]; ok {
			stateMap[v.IssueType] = append(stateMap[v.IssueType], pb.IssueStatus{
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

func (p *provider) GetIssueStateIDs(req *pb.GetIssueStatesRequest) ([]int64, error) {
	st, err := p.db.GetIssuesStates(req)
	if err != nil {
		return nil, err
	}
	res := make([]int64, 0)
	for _, v := range st {
		res = append(res, int64(v.ID))
	}
	return res, nil
}

func (p *provider) GetIssueStatesBelong(req *pb.GetIssueStateRelationRequest) ([]apistructs.IssueStateState, error) {
	var states []apistructs.IssueStateState
	st, err := p.db.GetIssuesStatesByProjectID(req.ProjectID, req.IssueType)
	if err != nil {
		return nil, err
	}
	BelongMap := make(map[string][]apistructs.IssueStateName)
	for _, s := range st {
		BelongMap[s.Belong] = append(BelongMap[s.Belong], apistructs.IssueStateName{
			Name: s.Name,
			ID:   int64(s.ID),
		})
	}
	stateIndex := GetStateBelongIndex(req.IssueType)
	for _, state := range stateIndex {
		for key, value := range BelongMap {
			if key != state {
				continue
			}
			states = append(states, apistructs.IssueStateState{
				StateBelong: apistructs.IssueStateBelong(key),
				States:      value,
			})
		}
	}
	return states, nil
}

var index = []string{pb.IssueStateBelongEnum_OPEN.String(), pb.IssueStateBelongEnum_WORKING.String(), pb.IssueStateBelongEnum_DONE.String()}

func GetStateBelongIndex(t string) []string {
	switch t {
	case pb.IssueTypeEnum_REQUIREMENT.String():
		return index
	case pb.IssueTypeEnum_TASK.String():
		return index
	case pb.IssueTypeEnum_EPIC.String():
		return index
	case pb.IssueTypeEnum_BUG.String():
		return []string{pb.IssueStateBelongEnum_OPEN.String(), pb.IssueStateBelongEnum_WONTFIX.String(), pb.IssueStateBelongEnum_REOPEN.String(), pb.IssueStateBelongEnum_RESOLVED.String(), pb.IssueStateBelongEnum_CLOSED.String()}
	default:
		panic(fmt.Sprintf("invalid issue type: %s", t))
	}

}
