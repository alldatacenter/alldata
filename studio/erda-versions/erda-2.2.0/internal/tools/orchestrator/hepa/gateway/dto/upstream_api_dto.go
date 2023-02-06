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
	"strings"

	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/hepa/legacy_upstream/pb"
)

type UpstreamApiDto struct {
	Path        string      `json:"path"`
	GatewayPath string      `json:"gatewayPath"`
	Method      string      `json:"method"`
	Address     string      `json:"address"`
	IsInner     bool        `json:"isInner"`
	IsCustom    bool        `json:"isCustom"`
	Doc         interface{} `json:"doc"`
	Name        string      `json:"name"`
}

func FromUpstreamApi(api *pb.UpstreamApi) UpstreamApiDto {
	return UpstreamApiDto{
		Path:        api.Path,
		GatewayPath: api.GatewayPath,
		Method:      api.Method,
		Address:     api.Address,
		IsInner:     api.IsInner,
		IsCustom:    api.IsCustom,
		Name:        api.Name,
		Doc:         api.Doc.AsInterface(),
	}
}

func (dto *UpstreamApiDto) Init() bool {
	if len(dto.Path) == 0 || dto.Path[0] != '/' {
		log.Errorf("invalid path:%+v", dto)
		return false
	}
	if len(dto.Address) == 0 || !strings.HasPrefix(dto.Address, "http") {
		log.Errorf("invalid address:%+v", dto)
		return false
	}
	if len(dto.Name) == 0 {
		dto.Name = dto.Method + dto.Path
	}
	if dto.GatewayPath == "" {
		dto.GatewayPath = dto.Path
	}
	return true
}
