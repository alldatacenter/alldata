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

	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/model"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type SeverityStackHandler struct {
	reverse bool
}

func NewSeverityStackHandler(reverse bool) *SeverityStackHandler {
	return &SeverityStackHandler{reverse: reverse}
}

var severityColorMap = map[apistructs.IssueSeverity]string{
	apistructs.IssueSeverityFatal:   "maroon",
	apistructs.IssueSeveritySerious: "red",
	apistructs.IssueSeverityNormal:  "yellow",
	apistructs.IssueSeveritySlight:  "darkseagreen",
	apistructs.IssueSeverityLow:     "green",
}

func (h *SeverityStackHandler) GetStacks(ctx context.Context) []Stack {
	var stacks []Stack
	for i := len(apistructs.IssueSeveritys) - 1; i >= 0; i-- {
		severity := apistructs.IssueSeveritys[i]
		stacks = append(stacks, Stack{
			Name:  cputil.I18n(ctx, string(severity)),
			Value: string(severity),
			Color: severityColorMap[severity],
		})
	}
	if h.reverse {
		reverseStacks(stacks)
	}
	return stacks
}

func (h *SeverityStackHandler) GetIndexer() func(issue interface{}) string {
	return func(issue interface{}) string {
		switch issue.(type) {
		case *dao.IssueItem:
			return string(issue.(*dao.IssueItem).Severity)
		case *model.LabelIssueItem:
			return string(issue.(*model.LabelIssueItem).Bug.Severity)
		default:
			return ""
		}
	}
}

func (h *SeverityStackHandler) GetFilterOptions(ctx context.Context) []filter.PropConditionOption {
	return getFilterOptions(h.GetStacks(ctx), true)
}
