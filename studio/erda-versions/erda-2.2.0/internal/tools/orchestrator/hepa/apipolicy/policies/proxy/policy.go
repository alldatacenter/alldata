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

package proxy

import (
	"encoding/json"
	"fmt"

	"github.com/pkg/errors"

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
	dto := &PolicyDto{
		ReqBuffer:         true,
		RespBuffer:        true,
		ClientReqLimit:    100,
		ClientReqTimeout:  60,
		ClientRespTimeout: 60,
		ProxyReqTimeout:   60,
		ProxyRespTimeout:  60,
		HostPassthrough:   false,
		SSLRedirect:       true,
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

func (policy Policy) ParseConfig(dto apipolicy.PolicyDto, ctx map[string]interface{}) (apipolicy.PolicyConfig, error) {
	res := apipolicy.PolicyConfig{}
	policyDto, ok := dto.(*PolicyDto)
	if !ok {
		return res, errors.Errorf("invalid config:%+v", dto)
	}
	if !policyDto.Switch {
		emptyStr := ""
		// use empty str trigger regions update
		res.IngressAnnotation = &apipolicy.IngressAnnotation{
			LocationSnippet: &emptyStr,
			Annotation: map[string]*string{
				ANNOTATION_REQ_BUFFER:         nil,
				ANNOTATION_RESP_BUFFER:        nil,
				ANNOTATION_REQ_LIMIT:          nil,
				ANNOTATION_PROXY_REQ_TIMEOUT:  nil,
				ANNOTATION_PROXY_RESP_TIMEOUT: nil,
				ANNOTATION_SSL_REDIRECT:       nil,
			},
		}
		return res, nil
	}
	annotation := map[string]*string{}
	if policyDto.ReqBuffer {
		value := "on"
		annotation[ANNOTATION_REQ_BUFFER] = &value
	} else {
		value := "off"
		annotation[ANNOTATION_REQ_BUFFER] = &value
	}
	if policyDto.RespBuffer {
		value := "on"
		annotation[ANNOTATION_RESP_BUFFER] = &value
	} else {
		value := "off"
		annotation[ANNOTATION_RESP_BUFFER] = &value
	}
	if policyDto.SSLRedirect {
		value := "true"
		annotation[ANNOTATION_SSL_REDIRECT] = &value
	} else {
		value := "false"
		annotation[ANNOTATION_SSL_REDIRECT] = &value
	}
	limit := fmt.Sprintf("%dm", policyDto.ClientReqLimit)
	annotation[ANNOTATION_REQ_LIMIT] = &limit
	reqTimeout := fmt.Sprintf("%d", policyDto.ProxyReqTimeout)
	annotation[ANNOTATION_PROXY_REQ_TIMEOUT] = &reqTimeout
	respTimeout := fmt.Sprintf("%d", policyDto.ProxyRespTimeout)
	annotation[ANNOTATION_PROXY_RESP_TIMEOUT] = &respTimeout
	clientHeaderTimeout := fmt.Sprintf("client_header_timeout %ds;", policyDto.ClientReqTimeout)
	annotation[ANNOTATION_SERVER_SNIPPET] = &clientHeaderTimeout
	snippet := fmt.Sprintf(`
client_body_timeout %ds;
send_timeout %ds;
`, policyDto.ClientReqTimeout, policyDto.ClientRespTimeout)
	res.IngressAnnotation = &apipolicy.IngressAnnotation{
		LocationSnippet: &snippet,
		Annotation:      annotation,
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
		PluginName: "host-passthrough",
	})
	if err != nil {
		return res, err
	}
	if exist != nil && !policyDto.HostPassthrough {
		err = adapter.RemovePlugin(exist.PluginId)
		if err != nil {
			return res, err
		}
		policyDb, _ := db.NewGatewayPolicyServiceImpl()
		_ = policyDb.DeleteById(exist.Id)
		res.KongPolicyChange = true
	}
	if exist == nil && policyDto.HostPassthrough {
		disable := false
		kongReq := &kongDto.KongPluginReqDto{
			Name:    "host-passthrough",
			Config:  map[string]interface{}{},
			Enabled: &disable,
		}
		resp, err := adapter.AddPlugin(kongReq)
		if err != nil {
			return res, err
		}
		configByte, err := json.Marshal(resp.Config)
		if err != nil {
			return res, err
		}
		policyDao := &orm.GatewayPolicy{
			ZoneId:     zone.Id,
			PluginName: "host-passthrough",
			Category:   "proxy",
			PluginId:   resp.Id,
			Config:     configByte,
			Enabled:    1,
		}
		policyDb, _ := db.NewGatewayPolicyServiceImpl()
		err = policyDb.Insert(policyDao)
		if err != nil {
			return res, err
		}
		res.KongPolicyChange = true
	}
	return res, nil
}

func init() {
	err := apipolicy.RegisterPolicyEngine("proxy", &Policy{})
	if err != nil {
		panic(err)
	}
}
