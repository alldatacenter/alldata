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
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
)

const (
	keyProjectPagingRequest = "ProjectPagingRequest"
	keyOption               = "Option"
	keyIsEmpty              = "IsEmpty"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func assign(src, dst interface{}) error {
	if src == nil || dst == nil {
		return nil
	}

	return cputil.ObjJSONTransfer(src, dst)
}

func (h *GSHelper) SetProjectPagingRequest(req apistructs.ProjectListRequest) {
	if h.gs == nil {
		return
	}
	(*h.gs)[keyProjectPagingRequest] = req
}

func (h *GSHelper) GetProjectPagingRequest() (*apistructs.ProjectListRequest, bool) {
	if h.gs == nil {
		return nil, false
	}
	v, ok := (*h.gs)[keyProjectPagingRequest]
	if !ok {
		return nil, false
	}
	var req apistructs.ProjectListRequest
	cputil.MustObjJSONTransfer(v, &req)
	return &req, true
}

func (h *GSHelper) SetOption(key string) {
	if h.gs == nil {
		return
	}
	(*h.gs)[keyOption] = key
}

func (h *GSHelper) GetOption() string {
	if h.gs == nil {
		return ""
	}
	if v, ok := (*h.gs)[keyOption].(string); ok {
		return v
	}
	return ""
}

func (h *GSHelper) SetIsEmpty(key bool) {
	if h.gs == nil {
		return
	}
	(*h.gs)[keyIsEmpty] = key
}

func (h *GSHelper) GetIsEmpty() bool {
	if h.gs == nil {
		return false
	}
	if v, ok := (*h.gs)[keyIsEmpty].(bool); ok {
		return v
	}
	return false
}
