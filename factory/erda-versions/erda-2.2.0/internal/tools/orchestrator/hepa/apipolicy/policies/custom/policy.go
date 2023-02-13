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
	"encoding/json"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
)

type Policy struct {
	apipolicy.BasePolicy
}

func (policy Policy) CreateDefaultConfig(ctx map[string]interface{}) apipolicy.PolicyDto {
	dto := &PolicyDto{}
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
		empty := ""
		res.IngressAnnotation = &apipolicy.IngressAnnotation{
			LocationSnippet: &empty,
		}
		return res, nil
	}
	if policyDto.Config != "" {
		res.IngressAnnotation = &apipolicy.IngressAnnotation{
			LocationSnippet: &policyDto.Config,
		}
	}
	return res, nil
}

func init() {
	err := apipolicy.RegisterPolicyEngine("custom", &Policy{})
	if err != nil {
		panic(err)
	}
}
