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

type KongUpstreamStatusRespDto struct {
	Data []KongTargetDto `json:"data"`
}

type KongTargetDto struct {
	Id     string `json:"id,omitempty"`
	Target string `json:"target"`
	// 默认是100
	Weight     int64  `json:"weight,omitempty"`
	UpstreamId string `json:"upstream_id,omitempty"`
	CreatedAt  int64  `json:"created_at,omitempty"`
	Health     string `json:"health,omitempty"`
}
