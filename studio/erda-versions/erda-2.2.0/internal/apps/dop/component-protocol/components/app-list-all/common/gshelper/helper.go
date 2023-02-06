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
	keyAppPagingRequest = "AppPagingRequest"
	keyOption           = "Option"
	OpKeyProjectID      = "projectId"
	OpKeyAppID          = "appId"
	OpValTargetRepo     = "repo"
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

func (h *GSHelper) SetAppPagingRequest(req apistructs.ApplicationListRequest) {
	if h.gs == nil {
		return
	}
	(*h.gs)[keyAppPagingRequest] = req
}

func (h *GSHelper) GetAppPagingRequest() (*apistructs.ApplicationListRequest, bool) {
	if h.gs == nil {
		return nil, false
	}
	v, ok := (*h.gs)[keyAppPagingRequest]
	if !ok {
		return nil, false
	}
	var req apistructs.ApplicationListRequest
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
