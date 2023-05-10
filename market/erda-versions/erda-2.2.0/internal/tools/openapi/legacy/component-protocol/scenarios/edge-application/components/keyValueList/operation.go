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

package keyvaluelist

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentKVList struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

type EdgeKVListState struct {
	AppID   uint64 `json:"appID"`
	Visible bool   `json:"visible,omitempty"`
}

func (c *ComponentKVList) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentKVList) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

func (c *ComponentKVList) OperationRendering(identity apistructs.Identity) error {
	var (
		kvState = EdgeKVListState{}
	)

	jsonData, err := json.Marshal(c.component.State)
	if err != nil {
		return fmt.Errorf("marshal component state error: %v", err)
	}

	err = json.Unmarshal(jsonData, &kvState)
	if err != nil {
		return fmt.Errorf("unmarshal state json data error: %v", err)
	}

	if kvState.AppID != 0 {
		edgeApp, err := c.ctxBundle.Bdl.GetEdgeApp(kvState.AppID, identity)
		if err != nil {
			return fmt.Errorf("get edge app error: %v", err)
		}

		resList := make([]EdgeKVListDataItem, 0)

		for key, value := range edgeApp.ExtraData {
			resList = append(resList, EdgeKVListDataItem{
				KeyName: key,
				ValueName: ItemValue{
					RenderType: "copyText",
					Value: ItemValueContent{
						Text:     value,
						CopyText: value,
					},
				},
			})
		}

		c.component.Data = map[string]interface{}{
			"list": resList,
		}
	}

	c.component.Props = getProps(kvState.Visible)

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentKVList{}
}
