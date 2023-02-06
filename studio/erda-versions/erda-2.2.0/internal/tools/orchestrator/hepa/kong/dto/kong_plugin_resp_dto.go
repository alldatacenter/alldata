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

type KongPluginRespDto struct {
	Id         string                 `json:"id"`
	ServiceId  string                 `json:"service_id"`
	RouteId    string                 `json:"route_id"`
	ConsumerId string                 `json:"consumer_id"`
	Route      *KongObj               `json:"route"`
	Service    *KongObj               `json:"service"`
	Consumer   *KongObj               `json:"consumer"`
	Name       string                 `json:"name"`
	Config     map[string]interface{} `json:"config"`
	Enabled    bool                   `json:"enabled"`
	CreatedAt  int64                  `json:"created_at"`
	PolicyId   string                 `json:"-"`
}

type KongPluginsDto struct {
	Total int64               `json:"total"`
	Data  []KongPluginRespDto `json:"data"`
}

func (dto *KongPluginRespDto) Compatiable() {
	if dto == nil {
		return
	}
	if dto.Route != nil {
		dto.RouteId = dto.Route.Id
	}
	if dto.Service != nil {
		dto.ServiceId = dto.Service.Id
	}
	if dto.Consumer != nil {
		dto.ConsumerId = dto.Consumer.Id
	}
}
