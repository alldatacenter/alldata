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

package models

import (
	"strconv"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type Model struct {
	StartTime int64  `json:"startTime"`
	EndTime   int64  `json:"endTime"`
	TenantId  string `json:"tenantId"`
}

type BrowserOverviewInParams struct {
	InParamsPtr *Model
}

func (b *BrowserOverviewInParams) CustomInParamsPtr() interface{} {
	b.InParamsPtr = &Model{}
	return b.InParamsPtr
}

func (b *BrowserOverviewInParams) EncodeFromCustomInParams(customInParamsPtr interface{}, stdInParamsPtr *cptype.ExtraMap) {
	cputil.MustObjJSONTransfer(customInParamsPtr, stdInParamsPtr)
}

func (b *BrowserOverviewInParams) DecodeToCustomInParams(stdInParamsPtr *cptype.ExtraMap, customInParamsPtr interface{}) {
	// for debug
	if startTime, ok := (*stdInParamsPtr)["startTime"].(string); ok {
		t, _ := strconv.ParseInt(startTime, 10, 64)
		(*stdInParamsPtr)["startTime"] = t
	}
	if endTime, ok := (*stdInParamsPtr)["endTime"].(string); ok {
		t, _ := strconv.ParseInt(endTime, 10, 64)
		(*stdInParamsPtr)["endTime"] = t
	}
	cputil.MustObjJSONTransfer(stdInParamsPtr, customInParamsPtr)
}
