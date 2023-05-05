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
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

type Policy struct {
	apipolicy.BasePolicy
}

func (policy Policy) CreateDefaultConfig(ctx map[string]interface{}) apipolicy.PolicyDto {
	value, ok := ctx[apipolicy.CTX_SERVICE_INFO]
	if !ok {
		log.Errorf("get identify failed:%+v", ctx)
		return nil
	}
	info, ok := value.(apipolicy.ServiceInfo)
	if !ok {
		log.Errorf("convert failed:%+v", value)
		return nil
	}
	tokenName := strings.ToLower(fmt.Sprintf("x-%s-%s-csrf-token", strings.Replace(info.ProjectName, "_", "-", -1), info.Env))
	dto := &PolicyDto{
		ExcludedMethod: []string{"GET", "HEAD", "OPTIONS", "TRACE"},
		TokenName:      tokenName,
		CookieSecure:   false,
		ValidTTL:       1800,
		RefreshTTL:     10,
		ErrStatus:      403,
		ErrMsg:         `{"message":"This form has expired. Please refresh and try again."}`,
	}
	dto.Switch = false
	return dto
}

func (policy Policy) UnmarshalConfig(config []byte) (apipolicy.PolicyDto, error, string) {
	policyDto := &PolicyDto{}
	err := json.Unmarshal(config, policyDto)
	if err != nil {
		return nil, errors.Wrapf(err, "json parse config failed, config:%s", config), "Invalid config"
	}
	ok, msg := policyDto.IsValidDto()
	if !ok {
		return nil, errors.Errorf("invalid policy dto, msg:%s", msg), msg
	}
	return policyDto, nil, ""
}

func (policy Policy) buildPluginReq(dto *PolicyDto) *kongDto.KongPluginReqDto {
	req := &kongDto.KongPluginReqDto{
		Name:    "csrf-token",
		Config:  map[string]interface{}{},
		Enabled: &dto.Switch,
	}
	req.Config["biz_cookie"] = []string{dto.UserCookie}
	if dto.TokenDomain != "" {
		req.Config["biz_domain"] = dto.TokenDomain
	}
	req.Config["excluded_method"] = dto.ExcludedMethod
	req.Config["token_key"] = dto.TokenName
	req.Config["token_cookie"] = dto.TokenName
	req.Config["secure_cookie"] = dto.CookieSecure
	req.Config["valid_ttl"] = dto.ValidTTL
	req.Config["refresh_ttl"] = dto.RefreshTTL
	req.Config["err_status"] = dto.ErrStatus
	req.Config["err_message"] = dto.ErrMsg
	sha := sha256.New()
	_, _ = sha.Write([]byte(dto.TokenName + ":secret"))
	tokenSecret := fmt.Sprintf("%x", sha.Sum(nil))
	req.Config["jwt_secret"] = tokenSecret[:16] + tokenSecret[48:]

	return req
}

func (policy Policy) ParseConfig(dto apipolicy.PolicyDto, ctx map[string]interface{}) (apipolicy.PolicyConfig, error) {
	l := log.WithField("func", "CSRF-Token.ParseConfig")
	res := apipolicy.PolicyConfig{}
	policyDto, ok := dto.(*PolicyDto)
	if !ok {
		return res, errors.Errorf("invalid config:%+v", dto)
	}
	value, ok := ctx[apipolicy.CTX_KONG_ADAPTER]
	if !ok {
		return res, errors.Errorf("get identify failed:%+v", ctx)
	}
	adapter, ok := value.(kong.KongAdapter)
	if !ok {
		return res, errors.Errorf("convert failed:%+v", value)
	}
	value, ok = ctx[apipolicy.CTX_ZONE]
	if !ok {
		return res, errors.Errorf("get identify failed:%+v", ctx)
	}
	zone, ok := value.(*orm.GatewayZone)
	if !ok {
		return res, errors.Errorf("convert failed:%+v", value)
	}

	policyDb, _ := db.NewGatewayPolicyServiceImpl()
	exist, err := policyDb.GetByAny(&orm.GatewayPolicy{
		ZoneId:     zone.Id,
		PluginName: "csrf-token",
	})
	if err != nil {
		return res, err
	}
	req := policy.buildPluginReq(policyDto)

	var routes = make(map[string]struct{})
	packageAPIDB, _ := db.NewGatewayPackageApiServiceImpl()
	apiDB, _ := db.NewGatewayApiServiceImpl()
	routeDB, _ := db.NewGatewayRouteServiceImpl()
	apis, err := packageAPIDB.SelectByAny(&orm.GatewayPackageApi{ZoneId: zone.Id})
	if err != nil || len(apis) == 0 {
		l.WithError(err).Warnf("failed to packageAPIDB.SelectByAny(&orm.GatewayPackageApi{ZoneId: %s})", zone.Id)
	}

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

	if !policyDto.Switch {
		if exist != nil {
			adapter.RemovePlugin(exist.PluginId)
			_ = policyDb.DeleteById(exist.Id)
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
		err = policyDb.Update(exist)
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
			PluginName: "csrf-token",
			Category:   "safety",
			PluginId:   "",
			Config:     configByte,
			Enabled:    1,
		}
		err = policyDb.Insert(policyDao)
		if err != nil {
			return res, err
		}
	}
	res.KongPolicyChange = true
	return res, nil
}

func init() {

	err := apipolicy.RegisterPolicyEngine("safety-csrf", &Policy{})
	if err != nil {
		panic(err)
	}
}
