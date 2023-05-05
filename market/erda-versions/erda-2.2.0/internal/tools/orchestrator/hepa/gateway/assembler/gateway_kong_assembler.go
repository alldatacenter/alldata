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
	"strings"

	"github.com/pkg/errors"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	kongConst "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kong "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GatewayKongAssemblerImpl struct {
}

func (GatewayKongAssemblerImpl) BuildKongServiceReq(serviceId string, dto *gw.ApiDto) (*kong.KongServiceReqDto, error) {
	if dto == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req := &kong.KongServiceReqDto{}
	req.Url = dto.RedirectAddr
	req.ConnectTimeout = 5000
	req.ReadTimeout = 600000
	req.WriteTimeout = 600000
	i := 0
	req.Retries = &i
	if len(serviceId) != 0 {
		req.ServiceId = serviceId
	}
	return req, nil
}
func (GatewayKongAssemblerImpl) BuildKongRouteReq(routeId string, dto *gw.ApiDto, serviceId string, isRegexPath bool) (*kong.KongRouteReqDto, error) {
	if dto == nil || len(serviceId) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req := kong.NewKongRouteReqDto()
	req.Service = &kong.Service{}
	req.Service.Id = serviceId
	if len(routeId) != 0 {
		req.RouteId = routeId
	}
	if len(dto.Method) != 0 {
		methods := []string{}
		methods = append(methods, dto.Method)
		req.Methods = methods
	}
	if len(dto.Path) != 0 {
		paths := []string{}
		paths = append(paths, dto.Path)
		req.Paths = paths
	}
	if !strings.EqualFold(dto.Env, ENV_TYPE_PROD) {
		for i := 0; i < len(dto.Hosts); i++ {
			if dto.Hosts[i] == kongConst.InnerHost {
				dto.Hosts[i] = strings.ToLower(dto.Env + "." + dto.Hosts[i])
			} else {
				dto.Hosts[i] = strings.ToLower(dto.Env + config.ServerConf.SubDomainSplit + dto.Hosts[i])
			}
		}
	}
	req.Hosts = dto.Hosts
	if isRegexPath {
		ignore := strings.Count(dto.Path, "^/") + strings.Count(dto.Path, `\/`)
		req.RegexPriority = strings.Count(dto.Path, "/") - ignore
	}
	return req, nil
}
func (GatewayKongAssemblerImpl) BuildKongPluginReqDto(pluginId string, policy *db.GatewayPolicy, serviceId string, routeId string, consumerId string) (*kong.KongPluginReqDto, error) {
	if policy == nil || len(policy.PluginName) == 0 {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req := &kong.KongPluginReqDto{}
	configMap := &map[string]interface{}{}
	err := json.Unmarshal([]byte(policy.Config), configMap)
	if err != nil {
		return nil, errors.Wrap(err, ERR_JSON_FAIL)
	}
	//backward compatibility
	// if policy.PolicyName == "acl" {
	// 	if whiteList, ok := (*configMap)["whitelist"]; ok {
	// 		if whiteListStr, ok := whiteList.(string); ok {
	// 			(*configMap)["whitelist"] = strings.Split(whiteListStr, ",")
	// 		}
	// 	}
	// }

	carrier, ok := (*configMap)["CARRIER"]
	if !ok {
		return nil, errors.Errorf("CARRIER not in configMap[%s]", policy.Config)

	}
	carrierStr, ok := carrier.(string)
	if !ok {
		return nil, errors.Errorf("carrier transfer to string failed, configMap[%s]",
			policy.Config)
	}
	if len(serviceId) != 0 && strings.Contains(carrierStr, "SERVICE") {
		req.ServiceId = serviceId
	}
	if len(routeId) != 0 && strings.Contains(carrierStr, "ROUTE") {
		req.RouteId = routeId
	}
	if len(consumerId) != 0 && strings.Contains(carrierStr, "CONSUMER") {
		req.ConsumerId = consumerId
	}
	delete(*configMap, "CARRIER")
	req.Name = policy.PluginName
	req.Config = *configMap
	if len(pluginId) != 0 {
		req.PluginId = pluginId
	}
	return req, nil
}
