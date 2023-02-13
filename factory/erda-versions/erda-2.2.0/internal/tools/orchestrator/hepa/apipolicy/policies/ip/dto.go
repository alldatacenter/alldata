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
	"fmt"
	"regexp"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
)

const (
	LIMIT_INNER_STATUS = 581
)

type IpRateUnit string

const (
	SECOND IpRateUnit = "qps"
	MINUTE IpRateUnit = "qpm"
)

type IpSourceType string

const (
	REMOTE_IP       IpSourceType = "remoteIp"
	X_REAL_IP       IpSourceType = "xRealIp"
	X_FORWARDED_FOR IpSourceType = "xForwardFor"
)

type IpAclType string

const (
	ACL_BLACK IpAclType = "black"
	ACL_WHITE IpAclType = "white"
)

type IpRate struct {
	Rate int64      `json:"rate"`
	Unit IpRateUnit `json:"unit"`
}

type PolicyDto struct {
	apipolicy.BaseDto
	IpSource         IpSourceType `json:"ipSource"`
	IpAclType        IpAclType    `json:"ipAclType"`
	IpAclList        []string     `json:"ipAclList,omitempty"`
	IpMaxConnections int64        `json:"ipMaxConnections,omitempty"`
	IpRate           *IpRate      `json:"ipRate,omitempty"`
}

var matchRegex = regexp.MustCompile(`^\d+\.\d+\.\d+\.\d+(\/\d+)?$`)

func (dto PolicyDto) IsValidDto() (bool, string) {
	if !dto.Switch {
		return true, ""
	}
	for _, ip := range dto.IpAclList {
		if ok := matchRegex.MatchString(ip); !ok {
			return false, fmt.Sprintf("IP地址非法: %s", ip)
		}
	}
	if dto.IpSource != REMOTE_IP && dto.IpSource != X_REAL_IP && dto.IpSource != X_FORWARDED_FOR {
		return false, fmt.Sprintf("IP来源字段非法: %s", dto.IpSource)
	}
	if dto.IpAclType != ACL_BLACK && dto.IpAclType != ACL_WHITE {
		return false, fmt.Sprintf("黑白名单字段非法：%s", dto.IpAclType)
	}
	if dto.IpRate != nil {
		if dto.IpRate.Rate <= 0 {
			return false, "请求速率限制值需要大于0"
		}
		if dto.IpRate.Unit != SECOND && dto.IpRate.Unit != MINUTE {
			return false, "请求速率单位非法"
		}
	}
	if dto.IpAclType == ACL_WHITE && len(dto.IpAclList) == 0 {
		return false, "白名单模式需要填写至少一个IP地址"
	}
	if dto.IpMaxConnections < 0 {
		return false, "连接数限制需要大于0"
	}
	return true, ""
}
