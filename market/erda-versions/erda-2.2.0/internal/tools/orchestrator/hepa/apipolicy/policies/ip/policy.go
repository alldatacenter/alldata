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

package ip

import (
	"encoding/json"
	"fmt"
	"math"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
)

type Policy struct {
	apipolicy.BasePolicy
}

func (policy Policy) CreateDefaultConfig(ctx map[string]interface{}) apipolicy.PolicyDto {
	dto := &PolicyDto{
		IpSource:  REMOTE_IP,
		IpAclType: ACL_BLACK,
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
		}
		res.IngressController = &apipolicy.IngressController{
			HttpSnippet: &emptyStr,
		}
		res.AnnotationReset = true
		return res, nil
	}
	value, ok := ctx[apipolicy.CTX_IDENTIFY]
	if !ok {
		return res, errors.Errorf("get identify failed:%+v", ctx)
	}
	id, ok := value.(string)
	if !ok {
		return res, errors.Errorf("convert failed:%+v", value)
	}
	value, ok = ctx[apipolicy.CTX_K8S_CLIENT]
	if !ok {
		return res, errors.Errorf("get k8s client failed:%+v", ctx)
	}
	adapter, ok := value.(k8s.K8SAdapter)
	if !ok {
		return res, errors.Errorf("convert failed:%+v", value)
	}
	count, err := adapter.CountIngressController()
	if err != nil {
		count = 1
		logrus.Errorf("get ingress controller count failed, err:%+v", err)
	}
	ipSource := ""
	if policyDto.IpSource != REMOTE_IP {
		ipSource += "set_real_ip_from 0.0.0.0/0;\n"
		if policyDto.IpSource == X_REAL_IP {
			ipSource += "real_ip_header X-Real-IP;\n"
		} else if policyDto.IpSource == X_FORWARDED_FOR {
			ipSource += "real_ip_header X-Forwarded-For;\n"
		}
	}
	acls := ""
	if len(policyDto.IpAclList) > 0 {
		var prefix string
		var bottom string
		if policyDto.IpAclType == ACL_BLACK {
			bottom = "allow all;\n"
			prefix = "deny "
		} else {
			bottom = "deny all;\n"
			prefix = "allow "
		}
		for _, ip := range policyDto.IpAclList {
			acls += prefix + ip + ";\n"
		}
		acls += bottom
	}
	limitConnZone := ""
	limitConn := ""
	if policyDto.IpMaxConnections > 0 {
		limitConnZone = fmt.Sprintf("limit_conn_zone $binary_remote_addr zone=ip-conn-%s:10m;\n", id)
		maxConn := int64(math.Ceil(float64(policyDto.IpMaxConnections) / float64(count)))
		limitConn = fmt.Sprintf("limit_conn ip-conn-%s %d;\n", id, maxConn)
	} else {
		limitConnZone = fmt.Sprintf("limit_conn_zone $binary_remote_addr zone=ip-conn-%s:10m;\n", id)
	}
	limitReqZone := ""
	limitReq := ""
	if policyDto.IpRate != nil {
		unit := "r/s"
		if policyDto.IpRate.Unit == MINUTE {
			unit = "r/m"
		}
		maxReq := int64(math.Ceil(float64(policyDto.IpRate.Rate) / float64(count)))
		limitReqZone = fmt.Sprintf("limit_req_zone $binary_remote_addr zone=ip-req-%s:10m rate=%d%s;\n",
			id, maxReq, unit)
		limitReq = fmt.Sprintf("limit_req zone=ip-req-%s nodelay;\n", id)
	} else {
		limitReqZone = fmt.Sprintf("limit_req_zone $binary_remote_addr zone=ip-req-%s:10m rate=%d%s;\n",
			id, 10000, "r/s")
	}
	locationSnippet := ipSource + acls + limitConn + limitReq
	res.IngressAnnotation = &apipolicy.IngressAnnotation{
		LocationSnippet: &locationSnippet,
	}
	httpSnippet := limitConnZone + limitReqZone
	res.IngressController = &apipolicy.IngressController{
		HttpSnippet: &httpSnippet,
	}
	return res, nil
}

func init() {
	err := apipolicy.RegisterPolicyEngine("safety-ip", &Policy{})
	if err != nil {
		panic(err)
	}
}
