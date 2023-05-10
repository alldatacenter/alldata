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

package spec

import (
	"time"

	"github.com/erda-project/erda/apistructs"
)

// PipelineLabel 标签
type PipelineLabel struct {
	ID uint64 `json:"id" xorm:"pk autoincr"`

	Type     apistructs.PipelineLabelType `json:"type,omitempty"`
	TargetID uint64                       `json:"targetID"`

	PipelineSource  apistructs.PipelineSource `json:"pipelineSource"`
	PipelineYmlName string                    `json:"pipelineYmlName"`

	Key   string `json:"key"`
	Value string `json:"value"`

	TimeCreated time.Time `json:"timeCreated" xorm:"created"`
	TimeUpdated time.Time `json:"timeUpdated" xorm:"updated"`
}

func (p PipelineLabel) TableName() string {
	return "pipeline_labels"
}
