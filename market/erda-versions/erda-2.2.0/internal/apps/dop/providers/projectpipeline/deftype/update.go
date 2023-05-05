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

type ProjectPipelineUpdate struct {
	IdentityInfo apistructs.IdentityInfo

	ID         string              `json:"id"`
	Name       string              `json:"name"`
	ProjectID  uint64              `json:"projectID"`
	AppID      uint64              `json:"appID"`
	SourceType ProjectPipelineType `json:"sourceType"`
	Ref        string              `json:"ref"`
	Path       string              `json:"path"`
	FileName   string              `json:"fileName"`
}

func (p *ProjectPipelineUpdate) Validate() error {
	if p.ID == "" {
		return fmt.Errorf("the ID is empty")
	}
	if p.Name == "" {
		return fmt.Errorf("the name is empty")
	}
	if p.ProjectID == 0 {
		return fmt.Errorf("the projectID is zero")
	}
	return nil
}
