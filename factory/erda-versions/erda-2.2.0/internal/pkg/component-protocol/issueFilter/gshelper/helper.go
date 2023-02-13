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
	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
)

const (
	KeyIssuePagingRequestKanban = "IssuePagingRequestKanban"
	KeyIssuePagingRequest       = "IssuePagingRequest"
	KeyIterationOptions         = "IterationOptions"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs}
}

func (h *GSHelper) SetIssuePagingRequest(key string, req pb.PagingIssueRequest) {
	if h.gs == nil {
		return
	}
	(*h.gs)[key] = req
}

func (h *GSHelper) SetIterationOptions(key string, options []model.SelectOption) {
	if h.gs == nil {
		return
	}
	(*h.gs)[key] = options
}
