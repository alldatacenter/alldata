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

package serverguard

import (
	"encoding/json"
	"fmt"
	"math"
	"regexp"
	"strconv"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
)

const (
	LIMIT_INNER_STATUS = 581
)

var allowsCors = `
    more_set_headers 'Access-Control-Allow-Origin: $from_request_origin_or_referer';
    more_set_headers 'Access-Control-Allow-Methods: GET, PUT, POST, DELETE, PATCH, OPTIONS';
    more_set_headers 'Access-Control-Allow-Headers: $http_access_control_request_headers';
    more_set_headers 'Access-Control-Allow-Credentials: true';
    more_set_headers 'Access-Control-Max-Age: 86400';
    more_set_headers 'Content-Type: text/plain charset=UTF-8';
    more_set_headers 'Content-Length: 0';
`

type PolicyDto struct {
	apipolicy.BaseDto
	MaxTps         int64  `json:"maxTps,omitempty"`
	ExtraLatency   int64  `json:"extraLatency,omitempty"`
	RefuseCode     int64  `json:"refuseCode,omitempty"`
	RefuseResponse string `json:"refuseResponse,omitempty"`
}

func (dto PolicyDto) IsValidDto() (bool, string) {
	if !dto.Switch {
		return true, ""
	}
	if dto.RefuseCode < 100 || dto.RefuseCode >= 600 {
		return false, fmt.Sprintf("拒绝状态码非法: %d", dto.RefuseCode)
	}
	if dto.RefuseCode >= 300 && dto.RefuseCode < 400 {
		if ok, _ := regexp.MatchString(`^(http://|https://).+`, dto.RefuseResponse); !ok {
			return false, "拒绝状态码为3xx时，拒绝应答需要配置一个http地址"
		}
	}
	if dto.MaxTps == 0 {
		return false, "最大吞吐必须配置，且不能为0"
	}
	if dto.ExtraLatency != 0 && dto.ExtraLatency < 1000/dto.MaxTps*2 {
		return false, fmt.Sprintf("根据最大吞吐: %d 请求/秒，额外延时至少需要配置:%d 毫秒", dto.MaxTps, int64(math.Ceil(1000/float64(dto.MaxTps)*2)))
	}
	return true, ""
}

func (dto *PolicyDto) AdjustDto() {
	if dto.MaxTps <= 0 {
		dto.Switch = false
		return
	}
	if dto.RefuseCode < 100 || dto.RefuseCode >= 600 {
		dto.RefuseCode = 429
	}
	if dto.RefuseCode >= 300 && dto.RefuseCode < 400 {
		if ok, _ := regexp.MatchString(`^(http://|https://).+`, dto.RefuseResponse); !ok {
			dto.RefuseCode = 429
		}
	}
	if dto.ExtraLatency != 0 && dto.ExtraLatency < 1000/dto.MaxTps*2 {
		dto.ExtraLatency = int64(math.Ceil(1000 / float64(dto.MaxTps) * 2))
	}
}

func (dto PolicyDto) RefuseResonseCanBeJson() bool {
	return json.Unmarshal([]byte(dto.RefuseResponse), new(interface{})) == nil
}

func (dto PolicyDto) RefuseResponseQuote() string {
	return strconv.Quote(dto.RefuseResponse)
}
