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

package dto

import "strings"

type KongPluginReqDto struct {
	// 插件名称,必填
	Name string `json:"name"`
	// 1、服务id
	ServiceId string `json:"service_id,omitempty"`
	// 2、路由id
	RouteId string `json:"route_id,omitempty"`
	// 3、消费者id
	ConsumerId string   `json:"consumer_id,omitempty"`
	Route      *KongObj `json:"route,omitempty"`
	Service    *KongObj `json:"service,omitempty"`
	Consumer   *KongObj `json:"consumer,omitempty"`
	// 是否开启，默认true
	Enabled *bool `json:"enabled,omitempty"`
	// 其余配置
	Config    map[string]interface{} `json:"config,omitempty"`
	Id        string                 `json:"id,omitempty"`
	CreatedAt int64                  `json:"created_at,omitempty"`
	// 插件id，删除或更新时使用
	PluginId string `json:"-"`
}

// IsEmpty
func (dto KongPluginReqDto) IsEmpty() bool {
	return len(dto.PluginId) == 0 && (len(dto.Name) == 0 ||
		(len(dto.ServiceId) == 0 && len(dto.RouteId) == 0 &&
			len(dto.ConsumerId) == 0))
}

// ToV2
func (dto *KongPluginReqDto) ToV2() {
	if dto == nil {
		return
	}
	if dto.RouteId != "" {
		dto.Route = &KongObj{Id: dto.RouteId}
		dto.RouteId = ""
	}
	if dto.ServiceId != "" {
		dto.Service = &KongObj{Id: dto.ServiceId}
		dto.ServiceId = ""
	}
	if dto.ConsumerId != "" {
		dto.Consumer = &KongObj{Id: dto.ConsumerId}
		dto.ConsumerId = ""
	}
	if dto.Name == "acl" {
		if item, ok := dto.Config["whitelist"]; ok {
			if whitelist, ok := item.(string); ok {
				if whitelist != "," {
					vars := strings.Split(whitelist, ",")
					dto.Config["allow"] = vars
				} else {
					dto.Config["allow"] = []string{"%NO_ONE_CAN_CONSUME%"}
				}
				delete(dto.Config, "whitelist")
			}
		}
	}
}
