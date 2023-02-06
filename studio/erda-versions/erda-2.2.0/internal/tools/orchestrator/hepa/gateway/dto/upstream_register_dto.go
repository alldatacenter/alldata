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
	"strconv"
	"strings"

	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/hepa/legacy_upstream/pb"
)

type UpstreamRegisterDto struct {
	Az            string           `json:"az"`
	UpstreamName  string           `json:"-"`
	DiceAppId     string           `json:"diceAppId"`
	DiceService   string           `json:"diceService"`
	RuntimeName   string           `json:"runtimeName"`
	RuntimeId     string           `json:"runtimeId"`
	AppName       string           `json:"appName"`
	ServiceAlias  string           `json:"serviceName"`
	OrgId         string           `json:"orgId"`
	ProjectId     string           `json:"projectId"`
	Env           string           `json:"env"`
	ApiList       []UpstreamApiDto `json:"apiList"`
	OldRegisterId *int             `json:"registerId"`
	RegisterId    string           `json:"registerTag"`
	PathPrefix    *string          `json:"pathPrefix"`
}

func FromUpstream(u *pb.Upstream) *UpstreamRegisterDto {
	dto := &UpstreamRegisterDto{
		Az:           u.Az,
		DiceAppId:    u.DiceAppId,
		DiceService:  u.DiceService,
		RuntimeName:  u.RuntimeName,
		RuntimeId:    u.RuntimeId,
		AppName:      u.AppName,
		ServiceAlias: u.ServiceName,
		OrgId:        u.OrgId,
		ProjectId:    u.ProjectId,
		Env:          u.Env,
		RegisterId:   u.RegisterTag,
	}
	apiList := []UpstreamApiDto{}
	for _, api := range u.ApiList {
		apiList = append(apiList, FromUpstreamApi(api))
	}
	dto.ApiList = apiList
	if u.RegisterId != 0 {
		id := int(u.RegisterId)
		dto.OldRegisterId = &id
	}
	if u.PathPrefix != "" {
		dto.PathPrefix = &u.PathPrefix
	}
	return dto
}

func (dto *UpstreamRegisterDto) Init() bool {
	if len(dto.AppName) == 0 || len(dto.OrgId) == 0 || len(dto.ProjectId) == 0 || len(dto.ApiList) == 0 ||
		len(dto.Env) == 0 || (dto.OldRegisterId == nil && dto.RegisterId == "") {
		log.Errorf("invalid dto:%+v", dto)
		return false
	}
	dto.UpstreamName = dto.AppName
	if dto.DiceService == "" {
		dto.DiceService = dto.ServiceAlias
	}
	if dto.ServiceAlias != "" {
		dto.UpstreamName += "/" + dto.ServiceAlias
	}
	if dto.RuntimeId != "" {
		dto.UpstreamName += "/" + dto.RuntimeId
	}
	if dto.RuntimeName == "" {
		if dto.PathPrefix == nil {
			path := strings.ToLower("/" + dto.UpstreamName)
			dto.PathPrefix = &path
		} else {
			path := "/" + strings.TrimPrefix(*dto.PathPrefix, "/")
			path = strings.TrimSuffix(path, "/")
			dto.PathPrefix = &path
		}
	} else {
		dto.PathPrefix = nil
	}
	if dto.OldRegisterId != nil && dto.RegisterId == "" {
		dto.RegisterId = strconv.Itoa(*dto.OldRegisterId)
	}
	address := dto.ApiList[0].Address
	for i := len(dto.ApiList) - 1; i >= 0; i-- {
		if !dto.ApiList[i].IsCustom {
			dto.ApiList = append(dto.ApiList[:i], dto.ApiList[i+1:]...)
		}
	}
	if len(dto.ApiList) == 0 {
		dto.ApiList = []UpstreamApiDto{
			{
				Path:        "/",
				GatewayPath: "/",
				Method:      "",
				Address:     address,
				IsInner:     false,
				Name:        "/",
			},
		}
	}
	return true
}
