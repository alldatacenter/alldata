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

package customFilter

import (
	"strconv"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type InParams struct {
	ProjectID    string `json:"projectId,omitempty"`
	ProjectIDInt uint64
	AppID        string `json:"appId,omitempty"`
	AppIDInt     uint64
	OrgIDInt     uint64
}

func (p *CustomFilter) CustomInParamsPtr() interface{} {
	if p.InParams == nil {
		p.InParams = &InParams{}
	}
	return p.InParams
}

func (p *CustomFilter) EncodeFromCustomInParams(customInParamsPtr interface{}, stdInParamsPtr *cptype.ExtraMap) {
	cputil.MustObjJSONTransfer(&customInParamsPtr, stdInParamsPtr)
}

func (p *CustomFilter) DecodeToCustomInParams(stdInParamsPtr *cptype.ExtraMap, customInParamsPtr interface{}) {
	cputil.MustObjJSONTransfer(stdInParamsPtr, &customInParamsPtr)
	if p.InParams.ProjectID != "" {
		value, err := strconv.ParseUint(p.InParams.ProjectID, 10, 64)
		if err != nil {
			panic(err)
		}
		p.InParams.ProjectIDInt = value
	}
	if p.InParams.AppID != "" {
		value, err := strconv.ParseUint(p.InParams.AppID, 10, 64)
		if err != nil {
			panic(err)
		}
		p.InParams.AppIDInt = value
	}
}
