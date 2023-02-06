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

package custom

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type Model struct {
	StartTime  int64  `json:"startTime"`
	EndTime    int64  `json:"endTime"`
	TenantId   string `json:"tenantId"`
	ServiceId  string `json:"serviceId"`
	InstanceId string `json:"instanceId"`
	HostIp     string `json:"hostIp"`
}

type ServiceInParams struct {
	InParamsPtr *Model
}

func (b *ServiceInParams) CustomInParamsPtr() interface{} {
	b.InParamsPtr = &Model{}
	return b.InParamsPtr
}

func (b *ServiceInParams) EncodeFromCustomInParams(customInParamsPtr interface{}, stdInParamsPtr *cptype.ExtraMap) {
	cputil.MustObjJSONTransfer(customInParamsPtr, stdInParamsPtr)
}

func (b *ServiceInParams) DecodeToCustomInParams(stdInParamsPtr *cptype.ExtraMap, customInParamsPtr interface{}) {
	cputil.MustObjJSONTransfer(stdInParamsPtr, customInParamsPtr)
}
