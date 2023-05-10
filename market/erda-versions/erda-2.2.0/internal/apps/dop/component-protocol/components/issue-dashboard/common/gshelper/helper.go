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
	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	issuedao "github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func (h *GSHelper) SetIterations(l []apistructs.Iteration) {
	if h.gs == nil {
		return
	}
	(*h.gs)["Iterations"] = l
}

func (h *GSHelper) GetIterations() []apistructs.Iteration {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.Iteration, 0)
	_ = assign((*h.gs)["Iterations"], &res)
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

func (h *GSHelper) SetIssueList(l []issuedao.IssueItem) {
	if h.gs == nil {
		return
	}
	(*h.gs)["IssueList"] = l
}

func (h *GSHelper) GetIssueList() []issuedao.IssueItem {
	if h.gs == nil {
		return nil
	}
	res := make([]issuedao.IssueItem, 0)
	_ = assign((*h.gs)["IssueList"], &res)
	return res
}

func (h *GSHelper) SetIssueStateList(l []issuedao.IssueState) {
	if h.gs == nil {
		return
	}
	(*h.gs)["IssueStateList"] = l
}

func (h *GSHelper) GetIssueStateList() []issuedao.IssueState {
	if h.gs == nil {
		return nil
	}
	res := make([]issuedao.IssueState, 0)
	_ = assign((*h.gs)["IssueStateList"], &res)
	return res
}

func (h *GSHelper) SetIssueStageList(l []*pb.IssueStage) {
	if h.gs == nil {
		return
	}
	(*h.gs)["IssueStageList"] = l
}

func (h *GSHelper) GetIssueStageList() []*pb.IssueStage {
	if h.gs == nil {
		return nil
	}
	res := make([]*pb.IssueStage, 0)
	_ = assign((*h.gs)["IssueStageList"], &res)
	return res
}

func assign(src, dst interface{}) error {
	if src == nil || dst == nil {
		return nil
	}
	return mapstructure.Decode(src, dst)
}
