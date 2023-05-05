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
	"time"

	"github.com/sirupsen/logrus"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
)

func (p *provider) AfterIssueUpdate(u *IssueUpdated) error {
	if u == nil {
		return fmt.Errorf("issue children update config is empty")
	}
	cache, err := NewIssueCache(p.db)
	if err != nil {
		return err
	}
	c := &issueValidationConfig{}
	iteration, err := cache.TryGetIteration(u.IterationID)
	if err != nil {
		return err
	}
	c.iteration = iteration
	if err := p.UpdateIssuePlanTimeByIteration(u, c); err != nil {
		return err
	}
	parents, err := p.db.GetIssueParents(u.Id, []string{apistructs.IssueRelationInclusion})
	if err != nil {
		return err
	}
	if len(parents) > 0 {
		parent := parents[0]
		if updateParentCondition(parent.Belong, u) {
			stateButton, err := p.GetNextAvailableState(&dao.Issue{
				ProjectID: parent.ProjectID,
				State:     parent.State,
				Type:      parent.Type,
			})
			if err != nil {
				return err
			}
			if stateButton != nil {
				if err := p.db.UpdateIssue(parent.ID, map[string]interface{}{"state": stateButton.StateID}); err != nil {
					return err
				}
				if err := p.Stream.CreateIssueStreamBySystem(parent.ID, map[string][]interface{}{
					"state": {parent.State, stateButton.StateID, common.ChildrenInProgress},
				}); err != nil {
					return err
				}
			}
		}

		if err := p.AfterIssueInclusionRelationChange(parent.ID); err != nil {
			return err
		}
	}

	if u.updateChildrenIteration {
		go func() {
			if err := p.UpdateIssueChildrenIteration(u, c); err != nil {
				logrus.Errorf("after issue children update failed %v", err)
			}
		}()
	}

	return nil
}

func updateParentCondition(state string, u *IssueUpdated) bool {
	if u.stateOld == "" || u.stateNew == "" || u.stateOld == u.stateNew {
		return false
	}
	return state == pb.IssueStateBelongEnum_OPEN.String() &&
		(u.stateOld != pb.IssueStateBelongEnum_OPEN.String() || u.stateNew != pb.IssueStateBelongEnum_OPEN.String())
}

func (p *provider) UpdateIssuePlanTimeByIteration(u *IssueUpdated, c *issueValidationConfig) error {
	fields := make(map[string]interface{})
	streamFields := make(map[string][]interface{})
	now := time.Date(time.Now().Year(), time.Now().Month(), time.Now().Day(), 0, 0, 0, 0, time.Now().Location())
	adjuster := issueCreateAdjuster{&now}
	v := issueValidator{}
	if u.withIteration && u.IterationID != 0 {
		if u.IterationID == u.iterationOld {
			return nil
		}
		fields["iteration_id"] = u.IterationID
		streamFields["iteration_id"] = []interface{}{u.iterationOld, u.IterationID, common.ParentIterationChanged}
	}

	if err := v.validateTimeWithInIteration(c, u.PlanFinishedAt); err != nil {
		finishedAt := adjuster.planFinished(func() bool {
			return u.IterationID > 0
		}, c.iteration)
		if finishedAt != nil {
			fields["expiry_status"] = dao.GetExpiryStatus(finishedAt, now)
			fields["plan_finished_at"] = finishedAt
			streamFields["plan_finished_at"] = []interface{}{u.PlanFinishedAt, finishedAt, common.IterationChanged}
			u.PlanFinishedAt = finishedAt
		}
	}
	startedAt := adjuster.planStarted(func() bool {
		return u.PlanStartedAt != nil && u.PlanFinishedAt != nil && u.PlanStartedAt.After(*u.PlanFinishedAt)
	}, u.PlanFinishedAt)
	if startedAt != nil {
		fields["plan_started_at"] = startedAt
		streamFields["plan_started_at"] = []interface{}{u.PlanStartedAt, u.PlanFinishedAt, common.PlanFinishedAtChanged}
	}
	if len(fields) > 0 {
		if err := p.db.UpdateIssue(u.Id, fields); err != nil {
			return err
		}
	}
	return p.Stream.CreateIssueStreamBySystem(u.Id, streamFields)
}

func (p *provider) GetNextAvailableState(issue *dao.Issue) (*pb.IssueStateButton, error) {
	button, err := p.GenerateButton(*issue, &commonpb.IdentityInfo{InternalClient: common.SystemOperator}, nil, nil, nil, nil)
	if err != nil {
		return nil, err
	}
	for i := range button {
		if button[i].Permission {
			return button[i], nil
		}
	}
	return nil, nil
}

func (p *provider) UpdateIssueChildrenIteration(u *IssueUpdated, c *issueValidationConfig) error {
	issues, _, err := p.GetIssueChildren(u.Id, pb.PagingIssueRequest{
		ProjectID: u.projectID,
	})
	if err != nil {
		return err
	}
	iterationID := u.IterationID
	for _, i := range issues {
		u := &IssueUpdated{
			Id:             i.ID,
			PlanStartedAt:  i.PlanStartedAt,
			PlanFinishedAt: i.PlanFinishedAt,
			withIteration:  true,
			IterationID:    iterationID,
			iterationOld:   i.IterationID,
		}
		if err := p.UpdateIssuePlanTimeByIteration(u, c); err != nil {
			return err
		}
	}
	return nil
}

func (p *provider) GetIssueChildren(id uint64, req pb.PagingIssueRequest) ([]dao.IssueItem, uint64, error) {
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 20
	}
	if id == 0 {
		requirements, tasks, total, err := p.db.FindIssueRoot(req)
		if err != nil {
			return nil, 0, err
		}
		if err := p.SetIssueChildrenCount(requirements); err != nil {
			return nil, 0, err
		}
		return append(requirements, tasks...), total, nil
	}
	return p.db.FindIssueChildren(id, req)
}

func (p *provider) SetIssueChildrenCount(issues []dao.IssueItem) error {
	issueIDs := make([]uint64, 0, len(issues))
	issueMap := make(map[uint64]*dao.IssueItem)
	for i := range issues {
		issueIDs = append(issueIDs, issues[i].ID)
		issueMap[issues[i].ID] = &issues[i]
	}
	countList, err := p.db.IssueChildrenCount(issueIDs, []string{apistructs.IssueRelationInclusion})
	if err != nil {
		return err
	}
	for _, i := range countList {
		if v, ok := issueMap[i.IssueID]; ok {
			v.ChildrenLength = i.Count
		}
	}
	return nil
}
