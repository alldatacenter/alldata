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

// /api/scheduler/resource
// method: get
// 查询调度资源
type SchedulerResourceFecthRequest struct {
	Cluster  string            `json:"cluster"`
	Resource SchedulerResource `json:"resource"`
	Attr     Attribute         `json:"attribute"`
	Extra    map[string]string `json:"extra,omitempty"`
}

type SchedulerResource struct {
	CPU  float64 `json:"cpus"`
	Mem  float64 `json:"mem"`
	Disk float64 `json:"disk"`
}

// Attribute dice_tags like & unlike
type Attribute struct {
	Likes   []string `json:"like"`
	UnLikes []string `json:"unlike"`
}
