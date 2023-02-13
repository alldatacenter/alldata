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
	"encoding/json"
	"strconv"
	"strings"

	"github.com/pkg/errors"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	kong "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayDbAssemblerImpl struct {
}

func (GatewayDbAssemblerImpl) Resp2GatewayService(resp *kong.KongServiceRespDto, service *db.GatewayService) error {
	if resp == nil || service == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	service.ServiceId = resp.Id
	service.ServiceName = resp.Name
	service.Protocol = resp.Protocol
	service.Host = resp.Host
	service.Port = strconv.Itoa(resp.Port)
	service.Path = resp.Path
	return nil
}

func (impl GatewayDbAssemblerImpl) Resp2GatewayServiceByApi(resp *kong.KongServiceRespDto, apiDto gw.ApiDto, apiId string) (*db.GatewayService, error) {
	if resp == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	res := &db.GatewayService{}
	res.Url = apiDto.RedirectAddr
	res.ApiId = apiId
	err := impl.Resp2GatewayService(resp, res)
	if err != nil {
		return nil, errors.Wrap(err, "Resp2GatewayService failed")
	}
	return res, nil
}

func (GatewayDbAssemblerImpl) Resp2GatewayRoute(resp *kong.KongRouteRespDto, route *db.GatewayRoute) error {
	if resp == nil || route == nil {
		return errors.New(ERR_INVALID_ARG)
	}
	route.RouteId = resp.Id
	protocols, err := json.Marshal(resp.Protocols)
	if err != nil {
		return errors.Wrap(err, ERR_JSON_FAIL)
	}
	route.Protocols = string(protocols)
	hosts, err := json.Marshal(resp.Hosts)
	if err != nil {
		return errors.Wrap(err, ERR_JSON_FAIL)
	}
	route.Hosts = string(hosts)
	paths, err := json.Marshal(resp.Paths)
	if err != nil {
		return errors.Wrap(err, ERR_JSON_FAIL)
	}
	route.Paths = string(paths)
	methods, err := json.Marshal(resp.Methods)
	if err != nil {
		return errors.Wrap(err, ERR_JSON_FAIL)
	}
	route.Methods = string(methods)
	return nil
}
func (impl GatewayDbAssemblerImpl) Resp2GatewayRouteByAPi(resp *kong.KongRouteRespDto, serviceId string, apiId string) (*db.GatewayRoute, error) {
	if resp == nil || len(serviceId) == 0 || len(apiId) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	res := &db.GatewayRoute{}
	res.ServiceId = serviceId
	res.ApiId = apiId
	err := impl.Resp2GatewayRoute(resp, res)
	if err != nil {
		return nil, errors.Wrap(err, "Resp2GatewayRoute failed")
	}
	return res, nil

}
func (GatewayDbAssemblerImpl) Resp2GatewayPluginInstance(resp *kong.KongPluginRespDto, params PluginParams) (*db.GatewayPluginInstance, error) {
	if resp == nil {
		return nil, errors.Errorf("invalid resp[%+v] params[%+v]", resp, params)
	}
	res := &db.GatewayPluginInstance{}
	res.PluginId = resp.Id
	res.PluginName = resp.Name
	res.PolicyId = params.PolicyId
	res.GroupId = params.GroupId
	res.ApiId = params.ApiId
	if len(params.ServiceId) != 0 {
		res.ServiceId = params.ServiceId
	}
	if len(params.RouteId) != 0 {
		res.RouteId = params.RouteId
	}
	if len(params.ConsumerId) != 0 {
		res.ConsumerId = params.ConsumerId
	}
	return res, nil
}
func (GatewayDbAssemblerImpl) BuildGatewayApi(reqDto gw.ApiDto, consumerId string, policies []db.GatewayPolicy, zoneId string, upstreamApiId ...string) (*db.GatewayApi, error) {
	res := &db.GatewayApi{}
	res.ApiPath = reqDto.Path
	res.Method = reqDto.Method
	res.RedirectAddr = reqDto.RedirectAddr
	res.Description = reqDto.Description
	res.RegisterType = reqDto.RegisterType
	res.DiceApp = reqDto.DiceApp
	res.DiceService = reqDto.DiceService
	res.RuntimeServiceId = reqDto.RuntimeServiceId
	if reqDto.Swagger != nil {
		swaggerJson, ok := reqDto.Swagger.([]byte)
		if !ok {
			var err error
			swaggerJson, err = json.Marshal(reqDto.Swagger)
			if err != nil {
				return nil, errors.Wrapf(err, "json marshal falied, swagger:%+v", reqDto.Swagger)
			}
		}
		res.Swagger = swaggerJson
	}
	if reqDto.RedirectType != "" {
		res.RedirectType = reqDto.RedirectType
	}
	if reqDto.OuterNetEnable {
		res.NetType = gw.NT_OUT
	} else {
		res.NetType = gw.NT_IN
	}
	res.NeedAuth = reqDto.NeedAuth
	if len(consumerId) != 0 {
		res.ConsumerId = consumerId
	}
	if len(upstreamApiId) > 0 {
		res.UpstreamApiId = upstreamApiId[0]
	}
	if reqDto.UpstreamApiId != "" {
		res.UpstreamApiId = reqDto.UpstreamApiId
	}
	if policies != nil {
		newPolicies := []gw.PolicyDto{}
		for _, policy := range policies {
			newPolicies = append(newPolicies, gw.PolicyDto{
				PolicyId:    policy.Id,
				Category:    policy.Category,
				DisplayName: policy.DisplayName,
			})
		}
		serialize, err := json.Marshal(newPolicies)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		res.Policies = string(serialize)
	}
	return res, nil
}
func (GatewayDbAssemblerImpl) BuildConsumerInfo(consumer *db.GatewayConsumer) (*gw.ConsumerInfoDto, error) {
	if consumer == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dto := &gw.ConsumerInfoDto{}
	dto.ConsumerId = consumer.Id
	dto.ConsumerName = consumer.ConsumerName
	return dto, nil
}
func (GatewayDbAssemblerImpl) BuildConsumerApiInfo(consumerApi *db.GatewayConsumerApi, gwApi *db.GatewayApi) (*gw.ConsumerApiInfoDto, error) {
	if consumerApi == nil || gwApi == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dto := &gw.ConsumerApiInfoDto{}
	dto.ConsumerId = consumerApi.ConsumerId
	dto.Id = consumerApi.Id
	index := strings.Index(gwApi.ApiPath[1:], "/")
	if index < 0 {
		index = 0
	} else {
		index++
	}
	dto.ApiPath = gwApi.ApiPath[index:]
	dto.ApiId = gwApi.Id
	dto.Description = gwApi.Description
	dto.Method = gwApi.Method
	dto.RedirectAddr = gwApi.RedirectAddr
	return dto, nil
}
func (GatewayDbAssemblerImpl) BuildConsumerApiPolicyInfo(policy *db.GatewayPolicy) (*gw.ConsumerApiPolicyInfoDto, error) {
	if policy == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	dto := &gw.ConsumerApiPolicyInfoDto{}
	dto.Category = policy.Category
	dto.Config = string(policy.Config)
	dto.Description = policy.Description
	dto.DisplayName = policy.DisplayName
	dto.PolicyId = policy.Id
	dto.PolicyName = policy.PolicyName
	return dto, nil
}
