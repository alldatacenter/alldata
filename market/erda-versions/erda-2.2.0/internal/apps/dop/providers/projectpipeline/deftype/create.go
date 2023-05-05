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

type ProjectPipelineType string

const (
	ErdaProjectPipelineType   ProjectPipelineType = "erda"
	GithubProjectPipelineType ProjectPipelineType = "github"
)

var EffectProjectPipelineType = []ProjectPipelineType{ErdaProjectPipelineType, GithubProjectPipelineType}

func (p ProjectPipelineType) isEffectProjectPipelineType() bool {
	for _, v := range EffectProjectPipelineType {
		if p == v {
			return true
		}
	}
	return false
}

func (p ProjectPipelineType) String() string {
	return string(p)

}

type ProjectPipelineCreate struct {
	IdentityInfo apistructs.IdentityInfo

	ProjectID  uint64              `json:"projectID"`
	Name       string              `json:"name"`
	AppID      uint64              `json:"appID"`
	SourceType ProjectPipelineType `json:"sourceType"`
	Ref        string              `json:"ref"`
	Path       string              `json:"path"`
	FileName   string              `json:"fileName"`
}

type ProjectPipelineBatchCreate struct {
	ProjectID    uint64              `json:"projectID"`
	Name         string              `json:"name"`
	AppID        uint64              `json:"appID"`
	SourceType   ProjectPipelineType `json:"sourceType"`
	Ref          string              `json:"ref"`
	PipelineYmls []string            `json:"pipelineYmls"`
}

func (p *ProjectPipelineBatchCreate) Validate() error {
	if p.SourceType == ErdaProjectPipelineType && p.AppID == 0 {
		return fmt.Errorf("the appID is 0")
	}
	if !p.SourceType.isEffectProjectPipelineType() {
		return fmt.Errorf("the type is err")
	}
	if p.Ref == "" {
		return fmt.Errorf("the ref is empty")
	}
	if len(p.PipelineYmls) == 0 {
		return fmt.Errorf("the pipelineYmls is empty")
	}
	return nil
}

func (p *ProjectPipelineCreate) Validate() error {
	if p.Name == "" {
		return fmt.Errorf("the name is empty")
	}
	if p.SourceType == ErdaProjectPipelineType && p.AppID == 0 {
		return fmt.Errorf("the appID is 0")
	}
	if !p.SourceType.isEffectProjectPipelineType() {
		return fmt.Errorf("the type is err")
	}
	if p.Ref == "" {
		return fmt.Errorf("the ref is empty")
	}
	if p.Path == "" {
		return fmt.Errorf("the path is empty")
	}
	if p.FileName == "" {
		return fmt.Errorf("the fileName is empty")
	}
	return nil
}

type ProjectPipelineCreateResult struct {
	ID string `json:"id"`
}
