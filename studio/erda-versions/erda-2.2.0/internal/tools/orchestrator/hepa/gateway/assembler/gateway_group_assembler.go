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

package assembler

import (
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
)

type GatewayGroupAssemblerImpl struct {
}

func (GatewayGroupAssemblerImpl) GroupInfo2Dto(infos []gw.GatewayGroupInfo) ([]gw.GwApiGroupDto, error) {
	dtos := []gw.GwApiGroupDto{}
	for _, info := range infos {
		group := info.GatewayGroup
		policies := info.Policies
		if group.IsEmpty() {
			continue
		}
		dto := &gw.GwApiGroupDto{}
		dto.GroupId = group.Id
		dto.GroupName = group.GroupName
		dto.DisplayName = group.DispalyName
		dto.CreateAt = group.CreateTime.Format("2006-01-02T15:04:05")
		if len(policies) == 0 {
			dtos = append(dtos, *dto)
			continue
		}
		for _, policy := range policies {
			policyDto := &gw.GwApiPolicyDto{}
			policyDto.PolicyId = policy.Id
			policyDto.Category = policy.Category
			policyDto.DisplayName = policy.DisplayName
			dto.Policies = append(dto.Policies, *policyDto)
		}
		dtos = append(dtos, *dto)
	}
	return dtos, nil
}
