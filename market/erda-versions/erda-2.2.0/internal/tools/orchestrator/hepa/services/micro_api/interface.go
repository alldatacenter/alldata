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

package micro_api

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

var Service GatewayApiService

type GatewayApiService interface {
	Clone(context.Context) GatewayApiService
	GetRuntimeApis(runtimeServiceId string, registerType ...string) ([]dto.ApiDto, error)
	CreateRuntimeApi(dto *dto.ApiDto, session ...*service.SessionHelper) (string, vars.StandardErrorCode, error)
	CreateApi(*dto.ApiReqDto) (string, error)
	GetApiInfos(*dto.GetApisDto) (*common.PageQuery, error)
	DeleteApi(string) error
	UpdateApi(string, *dto.ApiReqDto) (*dto.ApiInfoDto, error)
	CreateUpstreamBindApi(*orm.GatewayConsumer, string, string, string, *orm.GatewayUpstreamApi, string) (string, error)
	UpdateUpstreamBindApi(*orm.GatewayConsumer, string, string, *orm.GatewayUpstreamApi, string) error
	DeleteUpstreamBindApi(*orm.GatewayUpstreamApi) error
	TouchRuntimeApi(*orm.GatewayRuntimeService, *service.SessionHelper, bool) error
	ClearRuntimeApi(*orm.GatewayRuntimeService) error
}
