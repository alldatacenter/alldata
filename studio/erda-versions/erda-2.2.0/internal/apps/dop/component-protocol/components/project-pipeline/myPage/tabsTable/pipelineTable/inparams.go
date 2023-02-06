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

package pipelineTable

import (
	"strconv"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type InParams struct {
	OrgID uint64 `json:"orgId,omitempty"`

	FrontendProjectID           string `json:"projectId,omitempty"`
	FrontendUrlQuery            string `json:"issueFilter__urlQuery,omitempty"`
	FrontendAppID               string `json:"appId,omitempty"`
	FrontendPipelineCategoryKey string `json:"pipelineCategoryKey,omitempty"`

	ProjectID uint64 `json:"-"`
	AppID     uint64 `json:"-"`
}

func (p *PipelineTable) CustomInParamsPtr() interface{} {
	if p.InParams == nil {
		p.InParams = &InParams{}
	}
	return p.InParams
}

func (p *PipelineTable) EncodeFromCustomInParams(customInParamsPtr interface{}, stdInParamsPtr *cptype.ExtraMap) {
	cputil.MustObjJSONTransfer(&customInParamsPtr, stdInParamsPtr)
}

func (p *PipelineTable) DecodeToCustomInParams(stdInParamsPtr *cptype.ExtraMap, customInParamsPtr interface{}) {
	cputil.MustObjJSONTransfer(stdInParamsPtr, &customInParamsPtr)
	var err error
	if p.InParams.FrontendProjectID != "" {
		p.InParams.ProjectID, err = strconv.ParseUint(p.InParams.FrontendProjectID, 10, 64)
		if err != nil {
			panic(err)
		}
	}
}
