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
	kong "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type PluginParams struct {
	PolicyId   string
	GroupId    string
	ServiceId  string
	RouteId    string
	ConsumerId string
	ApiId      string
}

type GatewayDbAssembler interface {
	Resp2GatewayService(*kong.KongServiceRespDto, *db.GatewayService) error
	Resp2GatewayServiceByApi(*kong.KongServiceRespDto, gw.ApiDto, string) (*db.GatewayService, error)
	Resp2GatewayRoute(*kong.KongRouteRespDto, *db.GatewayRoute) error
	Resp2GatewayRouteByAPi(*kong.KongRouteRespDto, string, string) (*db.GatewayRoute, error)
	Resp2GatewayPluginInstance(*kong.KongPluginRespDto, PluginParams) (*db.GatewayPluginInstance, error)
	BuildGatewayApi(gw.ApiDto, string, []db.GatewayPolicy, string, ...string) (*db.GatewayApi, error)
	BuildConsumerInfo(*db.GatewayConsumer) (*gw.ConsumerInfoDto, error)
	BuildConsumerApiInfo(*db.GatewayConsumerApi, *db.GatewayApi) (*gw.ConsumerApiInfoDto, error)
	BuildConsumerApiPolicyInfo(*db.GatewayPolicy) (*gw.ConsumerApiPolicyInfoDto, error)
}

type GatewayKongAssembler interface {
	BuildKongServiceReq(string, *gw.ApiDto) (*kong.KongServiceReqDto, error)
	BuildKongRouteReq(string, *gw.ApiDto, string, bool) (*kong.KongRouteReqDto, error)
	BuildKongPluginReqDto(string, *db.GatewayPolicy, string, string, string) (*kong.KongPluginReqDto, error)
}

type GatewayGroupAssembler interface {
	GroupInfo2Dto([]gw.GatewayGroupInfo) ([]gw.GwApiGroupDto, error)
}
