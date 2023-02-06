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

package waf

import (
	"fmt"
	"regexp"
	"strings"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
)

const (
	WAF_SWITCH_ON       = "on"
	WAF_SWITCH_OFF      = "off"
	WAF_SWITCH_WATCH    = "watch"
	MODSECURITY_SNIPPET = "nginx.ingress.kubernetes.io/modsecurity-snippet"
	MODSECURITY_ENABLE  = "nginx.ingress.kubernetes.io/enable-modsecurity"
)

type PolicyDto struct {
	apipolicy.BaseDto
	WafEnable   string `json:"wafEnable"`
	RemoveRules string `json:"removeRules"`
}

var matchRegex = regexp.MustCompile(`^"?[0-9]+\-?[0-9]*"?$`)

func (dto PolicyDto) IsValidDto() (bool, string) {
	if !dto.Switch {
		return true, ""
	}
	if dto.WafEnable != WAF_SWITCH_ON &&
		dto.WafEnable != WAF_SWITCH_OFF &&
		dto.WafEnable != WAF_SWITCH_WATCH {
		return false, "流量拦截开关参数错误"
	}
	dto.RemoveRules = strings.TrimSpace(dto.RemoveRules)
	if dto.RemoveRules != "" {
		rules := strings.Split(dto.RemoveRules, " ")
		for _, rule := range rules {
			if ok := matchRegex.MatchString(rule); !ok {
				return false, fmt.Sprintf("移除规则ID非法: %s", rule)
			}
		}
	}
	return true, ""
}
