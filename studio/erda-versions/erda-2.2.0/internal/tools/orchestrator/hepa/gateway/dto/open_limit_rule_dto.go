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

package dto

import (
	"github.com/erda-project/erda-proto-go/core/hepa/openapi_rule/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/exdto"
)

type OpenLimitRuleDto struct {
	ConsumerId string                 `json:"consumerId"`
	PackageId  string                 `json:"packageId"`
	Method     string                 `json:"method"`
	ApiPath    string                 `json:"apiPath"`
	Limit      exdto.LimitType        `json:"limit"`
	KongConfig map[string]interface{} `json:"-"`
	ApiId      string                 `json:"-"`
}

func (dto OpenLimitRuleDto) ToLimitRequest() *pb.LimitRequest {
	res := &pb.LimitRequest{
		ConsumerId: dto.ConsumerId,
		PackageId:  dto.PackageId,
		Method:     dto.Method,
		ApiPath:    dto.ApiPath,
	}
	res.Limit = &pb.LimitType{}
	if dto.Limit.Day != nil {
		res.Limit.Qpd = (int32)(*dto.Limit.Day)
	}
	if dto.Limit.Hour != nil {
		res.Limit.Qph = (int32)(*dto.Limit.Hour)
	}
	if dto.Limit.Minute != nil {
		res.Limit.Qpm = (int32)(*dto.Limit.Minute)
	}
	if dto.Limit.Second != nil {
		res.Limit.Qps = (int32)(*dto.Limit.Second)
	}
	return res
}

func FromLimitRequest(limit *pb.LimitRequest) *OpenLimitRuleDto {
	res := &OpenLimitRuleDto{
		ConsumerId: limit.ConsumerId,
		PackageId:  limit.PackageId,
		Method:     limit.Method,
		ApiPath:    limit.ApiPath,
	}
	if limit.Limit == nil {
		return res
	}
	lt := exdto.LimitType{}
	if limit.Limit.Qpd > 0 {
		qpd := int(limit.Limit.Qpd)
		lt.Day = &qpd
	}
	if limit.Limit.Qph > 0 {
		qph := int(limit.Limit.Qph)
		lt.Hour = &qph
	}
	if limit.Limit.Qpm > 0 {
		qpm := int(limit.Limit.Qpm)
		lt.Minute = &qpm
	}
	if limit.Limit.Qps > 0 {
		qps := int(limit.Limit.Qps)
		lt.Second = &qps
	}
	res.Limit = lt
	return res
}
