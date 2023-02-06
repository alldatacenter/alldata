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
	"context"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type EdgeKVListDataItem struct {
	KeyName   string    `json:"keyName"`
	ValueName ItemValue `json:"valueName"`
}

type ItemValue struct {
	RenderType string           `json:"renderType"`
	Value      ItemValueContent `json:"value"`
}

type ItemValueContent struct {
	Text     string `json:"text"`
	CopyText string `json:"copyText"`
}

func (c *ComponentKVList) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}

	if err := c.SetComponent(component); err != nil {
		return err
	}

	identity := c.ctxBundle.Identity

	if event.Operation == apistructs.RenderingOperation {
		if err := c.OperationRendering(identity); err != nil {
			return err
		}
	}

	return nil
}

func getProps(visible bool) apistructs.EdgeKVListProps {
	return apistructs.EdgeKVListProps{
		Visible:    visible,
		Pagination: false,
		RowKey:     "keyName",
		Columns: []apistructs.EdgeKVListColumns{
			{
				EdgeColumns: apistructs.EdgeColumns{
					DataIndex: "keyName",
				},
				ColSpan: 0,
			},
			{
				EdgeColumns: apistructs.EdgeColumns{
					DataIndex: "valueName",
				},
				ColSpan: 0,
			},
		},
	}
}
