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

package statusviewgroup

import (
	"context"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-app-site/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

type EdgeViewGroupState struct {
	Value string `json:"value"`
}

func (c ComponentViewGroup) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	if err := c.SetComponent(component); err != nil {
		return err
	}

	if c.component.State == nil {
		c.component.State = map[string]interface{}{}
	}

	identity := c.ctxBundle.Identity

	if event.Operation == apistructs.EdgeOperationChangeRadio || event.Operation == apistructs.InitializeOperation ||
		event.Operation == apistructs.RenderingOperation {
		err := c.OperationChangeViewGroup()
		if err != nil {
			return err
		}

		err = c.Operation(identity)
		if err != nil {
			return err
		}

		c.component.Operations = getOperations()
	}

	return nil
}

func getProps(total, success, processing, error int, lr *i18r.LocaleResource) apistructs.EdgeRadioProps {
	return apistructs.EdgeRadioProps{
		RadioType:   "button",
		ButtonStyle: "outline",
		Size:        "small",
		Options: []apistructs.EdgeButtonOption{
			{Text: fmt.Sprintf("%s(%d)", lr.Get(i18n.I18nKeyAll), total), Key: "total"},
			{Text: fmt.Sprintf("%s(%d)", lr.Get(i18n.I18nKeySuccess), success), Key: "success"},
			{Text: fmt.Sprintf("%s(%d)", lr.Get(i18n.I18nKeyDeploying), processing), Key: "processing"},
			{Text: fmt.Sprintf("%s(%d)", lr.Get(i18n.I18nKeyFailed), error), Key: "error"},
		},
	}
}

func getOperations() apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		"onChange": apistructs.EdgeOperation{
			Key:    apistructs.EdgeOperationChangeRadio,
			Reload: true,
		},
	}
}
