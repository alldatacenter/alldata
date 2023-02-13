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

package configitemlistfilter

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentListFilter struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

func (c *ComponentListFilter) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentListFilter) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

func (c *ComponentListFilter) OperationFilter() error {
	var (
		condition apistructs.EdgeSearchCondition
	)

	cdJson, err := json.Marshal(c.component.State)
	if err != nil {
		return err
	}

	if err = json.Unmarshal(cdJson, &condition); err != nil {
		return err
	}

	if condition.Values.Condition != "" {
		c.component.State["searchCondition"] = condition.Values.Condition
	} else {
		c.component.State["searchCondition"] = ""
	}

	c.component.State["isFirstFilter"] = true

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentListFilter{}
}
