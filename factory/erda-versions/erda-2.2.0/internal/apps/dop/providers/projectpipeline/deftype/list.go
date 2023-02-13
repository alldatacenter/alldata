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

package deftype

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
)

type ProjectPipelineList struct {
	ProjectID   uint64   `json:"projectID"`
	AppName     []string `json:"appName"`
	Ref         []string `json:"ref"`
	Creator     []string `json:"creator"`
	Executor    []string `json:"executor"`
	Category    []string `json:"category"`
	PageNo      uint64   `json:"pageNo"`
	PageSize    uint64   `json:"pageSize"`
	Name        string   `json:"name"`
	TimeCreated []string `json:"timeCreated"`
	TimeStarted []string `json:"timeStarted"`
	Status      []string `json:"status"`
	DescCols    []string `json:"descCols"`
	AscCols     []string `json:"ascCols"`
	CategoryKey string   `json:"categoryKey"`
	IsOthers    bool     `json:"isOthers"`

	IdentityInfo apistructs.IdentityInfo
}

func (p *ProjectPipelineList) Validate() error {
	if p.ProjectID == 0 {
		return fmt.Errorf("the projectID is 0")
	}
	if p.PageNo == 0 {
		p.PageNo = 1
	}
	if p.PageSize == 0 {
		p.PageSize = 20
	}
	return nil
}

type ProjectPipelineListResult struct {
}

type ProjectPipelineUsedRefList struct {
	ProjectID uint64 `json:"projectID"`
	AppID     uint64 `json:"appID"`

	IdentityInfo apistructs.IdentityInfo
}

type ProjectPipelineUsedRefListResult struct {
	Refs []string `json:"refs"`
}

func (p *ProjectPipelineUsedRefList) Validate() error {
	if p.ProjectID == 0 {
		return fmt.Errorf("the projectID is 0")
	}
	return nil
}
