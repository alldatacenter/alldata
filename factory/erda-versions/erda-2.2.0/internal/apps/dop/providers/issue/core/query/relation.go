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
	"time"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

// GetIssueRelationsByIssueIDs 获取issue的关联关系
func (p *provider) GetIssueRelationsByIssueIDs(issueID uint64, relationType []string) ([]uint64, []uint64, error) {
	relatingIssueIDs, err := p.db.GetRelatingIssues(issueID, relationType)
	if err != nil {
		return nil, nil, err
	}

	relatedIssueIDs, err := p.db.GetRelatedIssues(issueID, relationType)
	if err != nil {
		return nil, nil, err
	}

	return relatingIssueIDs, relatedIssueIDs, nil
}

var IssueTypes = []string{pb.IssueTypeEnum_REQUIREMENT.String(), pb.IssueTypeEnum_TASK.String(), pb.IssueTypeEnum_BUG.String(), pb.IssueTypeEnum_TICKET.String(), pb.IssueTypeEnum_EPIC.String()}

// GetIssuesByIssueIDs 通过issueIDs获取事件列表
func (p *provider) GetIssuesByIssueIDs(issueIDs []uint64) ([]*pb.Issue, error) {
	issueIDs = strutil.DedupUint64Slice(issueIDs)
	issueModels, err := p.db.GetIssueByIssueIDs(issueIDs)
	if err != nil {
		return nil, err
	}
	issueMap := make(map[uint64]*dao.Issue)
	for i := range issueModels {
		issueMap[issueModels[i].ID] = &issueModels[i]
	}
	results := make([]dao.Issue, 0, len(issueIDs))
	for _, i := range issueIDs {
		results = append(results, *issueMap[i])
	}
	issues, err := p.BatchConvert(results, IssueTypes)
	if err != nil {
		return nil, apierrors.ErrPagingIssues.InternalError(err)
	}

	return issues, nil
}

func (p *provider) AfterIssueAppRelationCreate(issueIDs []int64) error {
	types := []pb.IssueTypeEnum_Type{pb.IssueTypeEnum_TASK, pb.IssueTypeEnum_BUG}
	for _, i := range types {
		issues, err := p.db.ListIssueItems(pb.IssueListRequest{
			IDs:          issueIDs,
			Type:         []string{i.String()},
			StateBelongs: []string{pb.IssueStateBelongEnum_OPEN.String()},
		})
		if err != nil {
			return err
		}
		if len(issues) == 0 {
			continue
		}
		item := issues[0]
		stateButton, err := p.GetNextAvailableState(&dao.Issue{
			ProjectID: item.ProjectID,
			State:     item.State,
			Type:      i.String(),
		})
		if err != nil {
			return err
		}
		if stateButton == nil {
			continue
		}
		ids := make([]uint64, 0, len(issues))
		for _, i := range issues {
			ids = append(ids, i.ID)
		}
		if err = p.db.BatchUpdateIssues(&pb.BatchUpdateIssueRequest{
			Type:  i,
			Ids:   ids,
			State: stateButton.StateID,
		}); err != nil {
			return err
		}

		streams := make([]dao.IssueStream, 0, len(issues))
		for _, i := range ids {
			streams = append(streams, dao.IssueStream{
				IssueID:    int64(i),
				Operator:   common.SystemOperator,
				StreamType: common.ISTTransferState,
				StreamParams: common.ISTParam{
					CurrentState: item.Name,
					NewState:     stateButton.StateName,
					ReasonDetail: common.MrCreated,
				},
			})
		}
		if err := p.db.BatchCreateIssueStream(streams); err != nil {
			return err
		}
	}

	return nil
}

func (p *provider) AfterIssueInclusionRelationChange(id uint64) error {
	fields := make(map[string]interface{})
	streamFields := make(map[string][]interface{})
	start, end, err := p.db.FindIssueChildrenTimeRange(id)
	if err != nil {
		return err
	}
	issue, err := p.db.GetIssue(int64(id))
	if err != nil {
		return err
	}
	if start != nil && issue.PlanStartedAt != nil && !start.Equal(*issue.PlanStartedAt) {
		fields["plan_started_at"] = start
		streamFields["plan_started_at"] = []interface{}{issue.PlanStartedAt, start, common.ChildrenPlanUpdated}
	}
	if end != nil && issue.PlanFinishedAt != nil && !end.Equal(*issue.PlanFinishedAt) {
		now := time.Date(time.Now().Year(), time.Now().Month(), time.Now().Day(), 0, 0, 0, 0, time.Now().Location())
		fields["expiry_status"] = dao.GetExpiryStatus(end, now)
		fields["plan_finished_at"] = end
		streamFields["plan_finished_at"] = []interface{}{issue.PlanFinishedAt, end, common.ChildrenPlanUpdated}
	}
	if len(fields) > 0 {
		if err := p.db.UpdateIssue(id, fields); err != nil {
			return apierrors.ErrUpdateIssue.InternalError(err)
		}
	}
	if err := p.Stream.CreateIssueStreamBySystem(id, streamFields); err != nil {
		return err
	}
	return nil
}
