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

package stackhandlers

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/model"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type StateStackHandler struct {
	reverse        bool
	issueStateList []dao.IssueState
}

func NewStateStackHandler(reverse bool, issueStateList []dao.IssueState) *StateStackHandler {
	return &StateStackHandler{
		reverse:        reverse,
		issueStateList: issueStateList,
	}
}

var stateColorMap = map[string][]string{
	// 待处理
	pb.IssueStateBelongEnum_OPEN.String(): {"yellow"},
	// 进行中
	pb.IssueStateBelongEnum_WORKING.String(): {"blue", "steelblue", "darkslategray", "darkslateblue"},
	// 已解决
	pb.IssueStateBelongEnum_RESOLVED.String(): {"green"},
	// 已完成
	pb.IssueStateBelongEnum_DONE.String(): {"green"},
	// 重新打开
	pb.IssueStateBelongEnum_REOPEN.String(): {"red"},
	// 无需修复
	pb.IssueStateBelongEnum_WONTFIX.String(): {"orange", "grey"},
	// 已关闭
	pb.IssueStateBelongEnum_CLOSED.String(): {"darkseagreen"},
}

func (h *StateStackHandler) GetStacks(ctx context.Context) []Stack {
	var stacks []Stack
	belongCounter := make(map[string]int)
	for _, i := range h.issueStateList {
		color := stateColorMap[i.Belong][belongCounter[i.Belong]%len(stateColorMap[i.Belong])]
		belongCounter[i.Belong]++
		stacks = append(stacks, Stack{
			Name:  i.Name,
			Value: fmt.Sprintf("%d", i.ID),
			Color: color,
		})
	}
	if h.reverse {
		reverseStacks(stacks)
	}
	if len(stacks) == 0 {
		stacks = append(stacks, emptyStack)
	}
	return stacks
}

func (h *StateStackHandler) GetIndexer() func(issue interface{}) string {
	return func(issue interface{}) string {
		switch issue.(type) {
		case *dao.IssueItem:
			return fmt.Sprintf("%d", issue.(*dao.IssueItem).State)
		case *model.LabelIssueItem:
			return fmt.Sprintf("%d", issue.(*model.LabelIssueItem).Bug.State)
		default:
			return ""
		}
	}
}

func (h *StateStackHandler) GetFilterOptions(ctx context.Context) []filter.PropConditionOption {
	return getFilterOptions(h.GetStacks(ctx))
}
