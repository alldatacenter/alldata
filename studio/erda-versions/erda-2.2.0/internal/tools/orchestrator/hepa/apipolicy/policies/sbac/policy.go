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

package sbac

import (
	"encoding/json"
	"net/textproto"
	"net/url"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

// Name "sbac" is ServerBasedAccessControl
const Name = "sbac"

func init() {
	if err := apipolicy.RegisterPolicyEngine(Name, new(Policy)); err != nil {
		panic(err)
	}
}

type Policy struct {
	apipolicy.BasePolicy
}

func (p Policy) CreateDefaultConfig(ctx map[string]interface{}) apipolicy.PolicyDto {
	return new(PluginConfig)
}

func (p Policy) UnmarshalConfig(config []byte) (apipolicy.PolicyDto, error, string) {
	var pc PluginConfig
	if err := json.Unmarshal(config, &pc); err != nil {
		return nil, errors.Wrapf(err, "failed to Unmarshal config: %s", string(config)), "Invalid config"
	}
	if err := pc.IsValidDto(); err != nil {
		return nil, errors.Wrap(err, "Invalid config"), "Invalid config"
	}
	return &pc, nil, ""
}

func (p Policy) ParseConfig(dto apipolicy.PolicyDto, ctx map[string]interface{}) (apipolicy.PolicyConfig, error) {
	l := logrus.WithField("pluginName", Name).WithField("func", "ParseConfig")
	var res apipolicy.PolicyConfig
	pc, ok := dto.(*PluginConfig)
	if !ok {
		return res, errors.Errorf("invalid config: %+v", dto)
	}
	adapter, ok := ctx[apipolicy.CTX_KONG_ADAPTER].(kong.KongAdapter)
	if !ok {
		return res, errors.Errorf("failed to get identify with %s: %+v", apipolicy.CTX_KONG_ADAPTER, ctx)
	}
	kongVersion, err := adapter.GetVersion()
	if err != nil {
		return res, errors.Wrap(err, "failed to retrieve Kong version")
	}
	if !strings.HasPrefix(kongVersion, "2.") {
		return res, errors.Errorf("the plugin %s is not supportted on the Kong version %s", Name, kongVersion)
	}
	zone, ok := ctx[apipolicy.CTX_ZONE].(*orm.GatewayZone)
	if !ok {
		return res, errors.Errorf("failed to get identify with %s: %+v", apipolicy.CTX_ZONE, ctx)
	}

	policyDB, _ := service.NewGatewayPolicyServiceImpl()
	exist, err := policyDB.GetByAny(&orm.GatewayPolicy{ZoneId: zone.Id, PluginName: Name})
	if err != nil {
		return res, err
	}

	packageAPIDB, _ := service.NewGatewayPackageApiServiceImpl()
	apis, err := packageAPIDB.SelectByAny(&orm.GatewayPackageApi{ZoneId: zone.Id})
	if err != nil {
		l.WithError(err).Warnf("failed to packageAPIDB.SelectByAny(&orm.GatewayPackageApi{ZoneId: %s})", zone.Id)
		return res, err
	}
	if len(apis) == 0 {
		l.WithError(err).Warnf("not found packageAPIDB.SelectByAny(&orm.GatewayPackageApi{ZoneId: %s})", zone.Id)
	}

	var routes = make(map[string]struct{})
	apiDB, _ := service.NewGatewayApiServiceImpl()
	routeDB, _ := service.NewGatewayRouteServiceImpl()
	for _, api := range apis {
		switch route, err := routeDB.GetByApiId(api.Id); {
		case err != nil:
			l.WithError(err).
				WithField("tb_gateway_package_api.id", api.Id).
				Warnf("failed to routeDB.GetByApiId(%s)", api.Id)
		case route == nil:
			l.WithError(err).
				WithField("tb_gateway_package_api.id", api.Id).
				Warnf("failed to routeDB.GetByApiId(%s): not found", api.Id)
		default:
			routes[route.RouteId] = struct{}{}
		}

		if api.DiceApiId == "" {
			continue
		}
		var cond orm.GatewayApi
		cond.Id = api.DiceApiId
		gatewayApis, err := apiDB.SelectByAny(&cond)
		if err != nil {
			l.WithError(err).
				WithField("tb_gateway_package_api.id", api.Id).
				WithField("tb_gateway_package_api.dice_api_id", api.DiceApiId).
				Warnf("failed to apiDB.SelectByAny(&orm.GatewayApi{Id: %s})", cond.Id)
			continue
		}
		for _, gatewayApi := range gatewayApis {
			switch route, err := routeDB.GetByApiId(gatewayApi.Id); {
			case err != nil:
				l.WithError(err).WithField("tb_gateway_package_api.id", api.Id).
					WithField("tb_gateway_package_api.dice_api_id", api.DiceApiId).
					Warnf("failed to routeDB.GetByApiId(%s)", gatewayApi.Id)
			case route == nil:
				l.WithError(err).
					WithField("tb_gateway_package_api.id", api.Id).
					Warnf("failed to routeDB.GetByApiId(%s): not found", gatewayApi.Id)
			default:
				routes[route.RouteId] = struct{}{}
			}
		}
	}

	req := pc.ToPluginReqDto()
	if !pc.Switch {
		if exist != nil {
			_ = adapter.RemovePlugin(exist.PluginId)
			_ = policyDB.DeleteById(exist.Id)
		}
		for routeID := range routes {
			routeReq := *req
			routeReq.RouteId = routeID
			plugin, err := adapter.GetPlugin(&routeReq)
			if err != nil {
				l.WithError(err).Warnf("failed to adapter.GetPlugin(%+v)", routeReq)
				continue
			}
			if plugin == nil {
				l.Warnf("failed to adapter.GetPlugin(%+v): not found", routeReq)
				continue
			}
			if err = adapter.RemovePlugin(plugin.Id); err != nil {
				l.WithError(err).Warnf("failed to adapter.RemovePlugin(%s)", plugin.Id)
			}
		}
		res.KongPolicyChange = true
		return res, nil
	}

	for routeID := range routes {
		routeReq := *req
		routeReq.RouteId = routeID
		if _, err := adapter.CreateOrUpdatePlugin(&routeReq); err != nil {
			l.WithError(err).Errorf("faield to adapter.CreateOrUpdatePlugin(%+v)", routeReq)
			return res, err
		}
	}

	if exist != nil {
		configByte, err := json.Marshal(req.Config)
		if err != nil {
			return res, err
		}
		exist.Config = configByte
		err = policyDB.Update(exist)
		if err != nil {
			return res, err
		}
	} else {
		configByte, err := json.Marshal(req.Config)
		if err != nil {
			return res, err
		}
		policyDao := &orm.GatewayPolicy{
			ZoneId:     zone.Id,
			PluginName: Name,
			Category:   "safety",
			PluginId:   "",
			Config:     configByte,
			Enabled:    1,
		}
		err = policyDB.Insert(policyDao)
		if err != nil {
			return res, err
		}
	}
	res.KongPolicyChange = true
	return res, nil
}

type PluginConfig struct {
	apipolicy.BaseDto

	AccessControlAPI string   `json:"accessControlAPI"`
	Methods          []string `json:"methods"`
	Patterns         []string `json:"patterns"`
	WithHeaders      []string `json:"withHeaders"`
	WithCookie       bool     `json:"withCookie"`
	WithBody         bool     `json:"withBody"`
}

func (pc PluginConfig) IsValidDto() error {
	if !pc.BaseDto.Switch {
		return nil
	}
	_, err := url.ParseRequestURI(pc.AccessControlAPI)
	return err
}

func (pc PluginConfig) ToPluginReqDto() *kongDto.KongPluginReqDto {
	var req = &kongDto.KongPluginReqDto{
		Name:    Name,
		Enabled: &pc.Switch,
		Config: map[string]interface{}{
			"access_control_api": pc.AccessControlAPI,
			"with_body":          pc.WithBody,
		},
	}
	// adjust "patterns"
	var patterns []string
	for _, pat := range pc.Patterns {
		if len(pat) > 0 {
			patterns = append(patterns, pat)
		}
	}
	if len(patterns) > 0 {
		req.Config["patterns"] = patterns
	}

	// adjust "methods"
	var methods = make(map[string]bool)
	for _, method := range pc.Methods {
		methods[strings.ToUpper(method)] = true
	}
	if len(methods) > 0 {
		req.Config["methods"] = methods
	}
	// adjust "with_headers"
	var headersKeys = make(map[string]struct{})
	if pc.WithCookie {
		headersKeys[textproto.CanonicalMIMEHeaderKey("cookie")] = struct{}{}
	}
	for _, header := range pc.WithHeaders {
		headersKeys[textproto.CanonicalMIMEHeaderKey(header)] = struct{}{}
	}
	var headers []string
	for key := range headersKeys {
		headers = append(headers, key)
	}
	if len(headers) > 0 {
		req.Config["with_headers"] = headers
	}
	return req
}
