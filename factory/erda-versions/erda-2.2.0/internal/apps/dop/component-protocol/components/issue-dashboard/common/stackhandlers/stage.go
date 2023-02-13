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

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/model"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type StageStackHandler struct {
	reverse        bool
	issueStageList []*pb.IssueStage
}

func NewStageStackHandler(reverse bool, issueStageList []*pb.IssueStage) *StageStackHandler {
	return &StageStackHandler{
		reverse:        reverse,
		issueStageList: issueStageList,
	}
}

var stageColorList = []string{"red", "yellow", "green"}

func (h *StageStackHandler) GetStacks(ctx context.Context) []Stack {
	l := len(stageColorList)
	var stacks []Stack
	for idx, i := range h.issueStageList {
		stacks = append(stacks, Stack{
			Name:  i.Name,
			Value: i.Value,
			Color: stageColorList[idx%l],
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

func (h *StageStackHandler) GetIndexer() func(issue interface{}) string {
	return func(issue interface{}) string {
		switch issue.(type) {
		case *dao.IssueItem:
			return issue.(*dao.IssueItem).Stage
		case *model.LabelIssueItem:
			return issue.(*model.LabelIssueItem).Bug.Stage
		default:
			return ""
		}
	}
}

func (h *StageStackHandler) GetFilterOptions(ctx context.Context) []filter.PropConditionOption {
	return getFilterOptions(h.GetStacks(ctx))
}
