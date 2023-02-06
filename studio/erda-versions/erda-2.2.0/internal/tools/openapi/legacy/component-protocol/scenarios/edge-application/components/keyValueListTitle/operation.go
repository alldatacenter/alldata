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

package keyvaluelisttitle

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentKVListTitle struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

func (c *ComponentKVListTitle) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

type EdgeKVListStateTitle struct {
	Visible bool `json:"visible,omitempty"`
}

func (c *ComponentKVListTitle) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

func (c *ComponentKVListTitle) OperationRendering() error {
	var (
		titleState = EdgeKVListStateTitle{}
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err := json.Marshal(c.component.State)
	if err != nil {
		return fmt.Errorf("marshal component state error: %v", err)
	}

	err = json.Unmarshal(jsonData, &titleState)
	if err != nil {
		return fmt.Errorf("unmarshal state json data error: %v", err)
	}

	c.component.Props = getProps(titleState.Visible, i18nLocale)

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentKVListTitle{}
}
