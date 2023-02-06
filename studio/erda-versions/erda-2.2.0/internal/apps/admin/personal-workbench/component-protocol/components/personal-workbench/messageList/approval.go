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

package messageList

import (
	"fmt"
	"sort"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/list"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/pkg/arrays"
)

func (l *MessageList) doFilterApproval() (data *list.Data) {
	blockApprovalList, blockUsers, blockTotal := l.listBlockApprovals(1, 1000)
	deployApprovalList, deployUsers, deployTotal := l.listDeployApprovals(1, 1000)
	data = &list.Data{
		PageNo:   l.filterReq.PageNo,
		PageSize: l.filterReq.PageSize,
		Total:    uint64(blockTotal + deployTotal),
		Operations: map[cptype.OperationKey]cptype.Operation{
			list.OpChangePage{}.OpKey(): cputil.NewOpBuilder().Build(),
		},
		List:    append(blockApprovalList, deployApprovalList...),
		UserIDs: append(blockUsers, deployUsers...),
	}

	// TODO: union tables or refactor approval function
	sort.Slice(data.List, func(i, j int) bool {
		t1 := getApprovalUpdateAt(&data.List[i])
		if t1 == nil {
			return false
		}
		t2 := getApprovalUpdateAt(&data.List[j])
		if t2 == nil {
			return true
		}
		return t1.After(*t2)
	})
	start, end := arrays.Paging(l.filterReq.PageNo, l.filterReq.PageSize, data.Total)
	if start == -1 || end == -1 {
		return
	}
	data.List = data.List[start:end]
	return
}

func getApprovalUpdateAt(item *list.Item) *time.Time {
	t, ok := item.Extra.Extra["updatedAt"].(time.Time)
	if !ok {
		return nil
	}
	return &t
}

func (l *MessageList) listBlockApprovals(pageNo, pageSize uint64) (data []list.Item, userIDs []string, total int) {
	orgID, err := strconv.ParseUint(l.identity.OrgID, 10, 64)
	if err != nil {
		panic(err)
	}
	resp, err := l.bdl.ListApprove(orgID, l.identity.UserID, map[string][]string{
		"pageNo":   {strconv.FormatUint(pageNo, 10)},
		"pageSize": {strconv.FormatUint(pageSize, 10)},
		"status":   {"pending"},
	})
	if err != nil {
		panic(err)
	}

	total = resp.Data.Total
	userIDs = resp.UserIDs
	for _, i := range resp.Data.List {
		item := list.Item{
			ID:          strconv.FormatUint(i.ID, 10),
			Title:       i.Title,
			MainState:   &list.StateInfo{Status: common.UnreadMsgStatus},
			Description: i.Desc,
			Selectable:  true,
			ColumnsInfo: map[string]interface{}{
				"users": []string{i.Submitter},
				"text": []map[string]string{{
					"tip":  i.UpdatedAt.Format("2006-01-02"),
					"text": l.getReadableTimeText(i.UpdatedAt),
				}},
			},
			Operations: map[cptype.OperationKey]cptype.Operation{
				// list.OpItemClick{}.OpKey(): cputil.NewOpBuilder().Build(),
				// list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
				// 	WithSkipRender(true).
				// 	WithServerDataPtr(sd).Build(),
			},
			MoreOperations: []list.MoreOpItem{
				{
					ID:   "approved",
					Text: l.sdk.I18n("approve"),
					Operations: map[cptype.OperationKey]cptype.Operation{
						"click": {
							ClientData: &cptype.OpClientData{},
						},
					},
				},
				{
					ID:   "denied",
					Text: l.sdk.I18n("reject"),
					Operations: map[cptype.OperationKey]cptype.Operation{
						"click": {
							ClientData: &cptype.OpClientData{},
						},
					},
				},
			},
			Extra: cptype.Extra{
				Extra: map[string]interface{}{
					"updatedAt": i.UpdatedAt,
				},
			},
		}
		data = append(data, item)
	}
	return
}

func (l *MessageList) listDeployApprovals(pageNo, pageSize uint64) (data []list.Item, userIDs []string, total int) {
	resp, err := l.bdl.ListManualApproval(l.identity.OrgID, l.identity.UserID, map[string][]string{
		"pageNo":         {strconv.FormatUint(pageNo, 10)},
		"pageSize":       {strconv.FormatUint(pageSize, 10)},
		"approvalStatus": {"pending"},
	})
	if err != nil {
		panic(err)
	}
	total = resp.Data.Total
	userIDs = resp.UserIDs
	for _, i := range resp.Data.List {
		item := list.Item{
			ID:          strconv.FormatInt(i.Id, 10),
			Title:       fmt.Sprintf("%s/%s/%s", i.ProjectName, i.ApplicationName, i.BranchName),
			MainState:   &list.StateInfo{Status: common.UnreadMsgStatus},
			Description: fmt.Sprintf("%s/%v", i.ApprovalContent, i.BuildId),
			Selectable:  true,
			ColumnsInfo: map[string]interface{}{
				"users": []string{i.Operator},
				"text": []map[string]string{{
					"tip":  i.UpdatedAt.Format("2006-01-02"),
					"text": l.getReadableTimeText(i.UpdatedAt),
				}},
			},
			Operations: map[cptype.OperationKey]cptype.Operation{
				// list.OpItemClick{}.OpKey(): cputil.NewOpBuilder().Build(),
				// list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
				// 	WithSkipRender(true).
				// 	WithServerDataPtr(sd).Build(),
			},
			MoreOperations: []list.MoreOpItem{
				{
					ID:   "approveDeploy",
					Text: l.sdk.I18n("approve"),
					Operations: map[cptype.OperationKey]cptype.Operation{
						"click": {
							ClientData: &cptype.OpClientData{},
						},
					},
				},
				{
					ID:   "rejectDeploy",
					Text: l.sdk.I18n("reject"),
					Operations: map[cptype.OperationKey]cptype.Operation{
						"click": {
							ClientData: &cptype.OpClientData{},
						},
					},
				},
			},
			Extra: cptype.Extra{
				Extra: map[string]interface{}{
					"updatedAt": i.UpdatedAt,
				},
			},
		}
		data = append(data, item)
	}
	return
}
