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

package gshelper

import (
	"encoding/json"

	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/pkg/component-protocol/issueFilter"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func (h *GSHelper) SetIteration(l apistructs.Iteration) {
	if h.gs == nil {
		return
	}
	(*h.gs)["Iteration"] = l
}

func (h *GSHelper) GetIteration() apistructs.Iteration {
	if h.gs == nil {
		return apistructs.Iteration{}
	}
	var res apistructs.Iteration
	b, err := json.Marshal((*h.gs)["Iteration"])
	if err != nil {
		return apistructs.Iteration{}
	}
	err = json.Unmarshal(b, &res)
	if err != nil {
		return apistructs.Iteration{}
	}
	return res
}

func (h *GSHelper) SetMembers(l []apistructs.Member) {
	if h.gs == nil {
		return
	}
	(*h.gs)["Members"] = l
}

func (h *GSHelper) GetMembers() []apistructs.Member {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.Member, 0)
	_ = assign((*h.gs)["Members"], &res)
	return res
}

func (h *GSHelper) SetIssueList(l []dao.IssueItem) {
	if h.gs == nil {
		return
	}
	(*h.gs)["IssueList"] = l
}

func (h *GSHelper) GetIssueList() []dao.IssueItem {
	if h.gs == nil {
		return nil
	}
	res := make([]dao.IssueItem, 0)
	b, err := json.Marshal((*h.gs)["IssueList"])
	if err != nil {
		return nil
	}
	err = json.Unmarshal(b, &res)
	if err != nil {
		return nil
	}
	return res
}

func (h *GSHelper) SetIssueConditions(l issueFilter.FrontendConditions) {
	if h.gs == nil {
		return
	}
	(*h.gs)["IssueCondtions"] = l
}

func (h *GSHelper) GetIssueCondtions() issueFilter.FrontendConditions {
	if h.gs == nil {
		return issueFilter.FrontendConditions{}
	}
	var res issueFilter.FrontendConditions
	_ = assign((*h.gs)["IssueCondtions"], &res)
	return res
}

func (h *GSHelper) SetIssueStageList(l []apistructs.IssueStage) {
	if h.gs == nil {
		return
	}
	(*h.gs)["IssueStageList"] = l
}

func (h *GSHelper) GetIssueStageList() []apistructs.IssueStage {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.IssueStage, 0)
	_ = assign((*h.gs)["IssueStageList"], &res)
	return res
}

func (h *GSHelper) SetBurnoutChartType(t []string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["BurnoutChartType"] = t
}

func (h *GSHelper) GetBurnoutChartType() []string {
	if h.gs == nil {
		return nil
	}
	t := make([]string, 0)
	_ = assign((*h.gs)["BurnoutChartType"], &t)
	return t
}

func (h *GSHelper) SetBurnoutChartDimension(t string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["BurnoutChartDimension"] = t
}

func (h *GSHelper) GetBurnoutChartDimension() string {
	if h.gs == nil {
		return ""
	}
	var t string
	_ = assign((*h.gs)["BurnoutChartDimension"], &t)
	return t
}

func assign(src, dst interface{}) error {
	if src == nil || dst == nil {
		return nil
	}
	return mapstructure.Decode(src, dst)
}

func (h *GSHelper) SetStackChartType(t string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["StackChartType"] = t
}

func (h *GSHelper) GetStackChartType() string {
	if h.gs == nil {
		return ""
	}
	var t string
	_ = assign((*h.gs)["StackChartType"], &t)
	return t
}
