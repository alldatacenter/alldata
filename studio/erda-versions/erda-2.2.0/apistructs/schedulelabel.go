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

package apistructs

type ScheduleLabelListRequest struct{}
type ScheduleLabelListResponse struct {
	Header
	Data ScheduleLabelListData `json:"data"`
}
type ScheduleLabelListData struct {
	// map-key: label name
	// map-value: is this label a prefix?
	Labels map[string]bool `json:"labels"`
}

type ScheduleLabelSetRequest struct {
	// 对于 dcos 的 tag, 由于只有 key, 则 tag 中的 value 都为空
	Tags        map[string]string `json:"tag"`
	Hosts       []string          `json:"hosts"`
	ClusterName string            `json:"clustername"`
	ClusterType string            `json:"clustertype"`
	SoldierURL  string            `json:"soldierURL"`
}

type ScheduleLabelSetResponse struct {
	Header
}
