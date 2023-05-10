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

package gantt

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	issuecommon "github.com/erda-project/erda/internal/apps/dop/providers/issue/core/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator("issue-gantt", "gantt",
		func() servicehub.Provider { return &ComponentGantt{} })
}

func (f *ComponentGantt) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	f.sdk = cputil.SDK(ctx)
	f.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	f.issueSvc = ctx.Value(types.IssueService).(query.Interface)
	f.users = make([]string, 0)
	f.Data.Refresh = false
	inParamsBytes, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", cputil.SDK(ctx).InParams, err)
	}
	var inParams InParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}
	cputil.MustObjJSONTransfer(&c.State, &f.State)
	f.projectID, err = strconv.ParseUint(inParams.ProjectID, 10, 64)
	if err != nil {
		return err
	}
	parentIDs := inParams.ParentIDs
	var op OperationData
	dataBody, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(dataBody, &op); err != nil {
		return err
	}

	expand := make(map[uint64][]Item)
	update := make([]Item, 0)
	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		f.Operations = map[apistructs.OperationKey]Operation{
			apistructs.Update: {
				Key:      apistructs.Update.String(),
				Reload:   true,
				FillMeta: "nodes",
				Async:    true,
			},
			apistructs.ExpandNode: {
				Key:      apistructs.ExpandNode.String(),
				Reload:   true,
				FillMeta: "keys",
			},
		}

		if len(parentIDs) == 0 {
			issues, err := f.issueChildrenRetriever(0)
			if err != nil {
				return err
			}
			expand[0] = f.convertIssueItems(issues)
			f.Data.Refresh = true
		} else {
			for _, i := range parentIDs {
				issues, err := f.issueChildrenRetriever(i)
				if err != nil {
					return err
				}
				expand[i] = f.convertIssueItems(issues)
				if i != 0 {
					issue, err := f.issueSvc.GetIssueItem(i)
					if err != nil {
						return err
					}
					update = append(update, *convertIssueItem(issue))
				}
			}
		}

	case cptype.OperationKey(apistructs.ExpandNode):
		for _, key := range op.Meta.Keys {
			issues, err := f.issueChildrenRetriever(key)
			if err != nil {
				return err
			}
			expand[key] = f.convertIssueItems(issues)
		}
	case cptype.OperationKey(apistructs.Update):
		id := op.Meta.Nodes.Key
		if err := f.issueSvc.UpdateIssue(&pb.UpdateIssueRequest{
			Id:             id,
			PlanStartedAt:  timeFromMilli(op.Meta.Nodes.Start),
			PlanFinishedAt: timeFromMilli(op.Meta.Nodes.End),
			IdentityInfo: &commonpb.IdentityInfo{
				UserID: f.sdk.Identity.UserID,
			},
		}); err != nil {
			return err
		}

		parents, err := f.issueSvc.GetIssueParents(id, []string{apistructs.IssueRelationInclusion})
		if err != nil {
			return err
		}
		issue, err := f.issueSvc.GetIssueItem(id)
		if err != nil {
			return err
		}
		update = append(f.convertIssueItems(parents), *convertIssueItem(issue))
	}

	f.Data.ExpandList = expand
	f.Data.UpdateList = update
	(*gs)[protocol.GlobalInnerKeyUserIDs.String()] = strutil.DedupSlice(f.users)
	c.Data = nil
	cputil.MustObjJSONTransfer(&f, &c)
	return nil
}

func (f *ComponentGantt) issueChildrenRetriever(id uint64) ([]dao.IssueItem, error) {
	stateBelongs := []string{pb.IssueStateBelongEnum_OPEN.String(), pb.IssueStateBelongEnum_WORKING.String()}
	req := pb.PagingIssueRequest{
		ProjectID:    f.projectID,
		Type:         []string{pb.IssueTypeEnum_REQUIREMENT.String(), pb.IssueTypeEnum_TASK.String(), pb.IssueTypeEnum_BUG.String()},
		IterationIDs: f.State.Values.IterationIDs,
		Label:        f.State.Values.LabelIDs,
		Assignee:     f.State.Values.AssigneeIDs,
		StateBelongs: issuecommon.UnfinishedStateBelongs,
	}
	if id > 0 {
		req = pb.PagingIssueRequest{
			ProjectID:    f.projectID,
			Assignee:     f.State.Values.AssigneeIDs,
			Type:         []string{pb.IssueTypeEnum_TASK.String()},
			StateBelongs: stateBelongs,
		}
	}
	req.PageNo = 1
	req.PageSize = 500
	issues, _, err := f.issueSvc.GetIssueChildren(id, req)
	if err != nil {
		return nil, err
	}
	return issues, nil
}

func (f *ComponentGantt) convertIssueItems(issues []dao.IssueItem) []Item {
	res := make([]Item, 0, len(issues))
	for _, issue := range issues {
		res = append(res, *convertIssueItem(&issue))
		f.users = append(f.users, issue.Assignee)
	}
	return res
}

func convertIssueItem(issue *dao.IssueItem) *Item {
	return &Item{
		Title:  issue.Title,
		Key:    issue.ID,
		IsLeaf: issue.ChildrenLength == 0,
		Start:  issue.PlanStartedAt,
		End:    issue.PlanFinishedAt,
		Extra: Extra{
			Type: issue.Type,
			User: issue.Assignee,
			Status: Status{
				Text:   issue.Name,
				Status: common.GetUIIssueState(apistructs.IssueStateBelong(issue.Belong)),
			},
			IterationID: issue.IterationID,
		},
		ChildrenLength: issue.ChildrenLength,
	}
}

func timeFromMilli(millis int64) *string {
	// use seconds, ignore ms
	t := time.Unix(millis/1000, 0)
	s := t.Format(time.RFC3339)
	return &s
}

func milliFromTime(t *time.Time) int64 {
	return t.UnixNano() / int64(time.Millisecond)
}
