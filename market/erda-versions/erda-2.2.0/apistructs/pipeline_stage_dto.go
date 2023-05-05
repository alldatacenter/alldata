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

import (
	"time"
)

type PipelineStageDTO struct {
	ID         uint64 `json:"id"`
	PipelineID uint64 `json:"pipelineID"`

	Name   string         `json:"name"`
	Status PipelineStatus `json:"status"`

	CostTimeSec int64     `json:"costTimeSec"`
	TimeBegin   time.Time `json:"timeBegin"`
	TimeEnd     time.Time `json:"timeEnd"`
	TimeCreated time.Time `json:"timeCreated"`
	TimeUpdated time.Time `json:"timeUpdated"`
}
