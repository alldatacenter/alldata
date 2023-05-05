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

package configsetformmodal

import (
	"encoding/json"
	"fmt"
	"regexp"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/strutil"
)

type ComponentFormModal struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

type ConfigSetCreateForm struct {
	ClusterID     int64  `json:"cluster"`
	ConfigSetName string `json:"name"`
}

func (c *ComponentFormModal) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentFormModal) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

func (c *ComponentFormModal) OperateSubmit(orgID int64, identity apistructs.Identity) error {
	var (
		formEntity = ConfigSetCreateForm{}
	)

	jsonData, err := json.Marshal(c.component.State["formData"])
	if err != nil {
		return fmt.Errorf("marshal form data error: %v", err)
	}

	err = json.Unmarshal(jsonData, &formEntity)
	if err != nil {
		return fmt.Errorf("unmarshal form data error: %v", err)
	}

	err = validateSubmitData(formEntity)
	if err != nil {
		return fmt.Errorf("submit data error: %v", err)
	}

	req := &apistructs.EdgeConfigSetCreateRequest{
		OrgID:     orgID,
		Name:      formEntity.ConfigSetName,
		ClusterID: formEntity.ClusterID,
	}

	err = c.ctxBundle.Bdl.CreateEdgeConfigset(req, identity)
	if err != nil {
		return fmt.Errorf("create edge config set error: %v", err)
	}

	return nil
}

func validateSubmitData(formEntity ConfigSetCreateForm) error {
	if err := strutil.Validate(formEntity.ConfigSetName, strutil.MaxRuneCountValidator(apistructs.EdgeDefaultNameMaxLength)); err != nil {
		return err
	}

	isRight, err := regexp.MatchString(apistructs.EdgeDefaultMatchPattern, formEntity.ConfigSetName)
	if err != nil {
		return err
	}
	if !isRight {
		return fmt.Errorf(apistructs.EdgeDefaultRegexpError)
	}

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentFormModal{}
}
