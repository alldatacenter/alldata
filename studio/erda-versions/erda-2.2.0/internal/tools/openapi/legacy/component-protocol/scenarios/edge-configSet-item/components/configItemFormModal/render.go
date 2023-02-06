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

package configitemformmodal

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-configSet-item/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

const (
	CfgItemKeyMatchPattern = "^[a-zA-Z-._][a-zA-Z0-9-._]*$"
)

var (
	CfgItemKeyRegexp = fmt.Sprintf("/%v/", CfgItemKeyMatchPattern)
)

func (c ComponentFormModal) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	var (
		inParam = apistructs.EdgeRenderingID{}
	)

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}

	if err := c.SetComponent(component); err != nil {
		return err
	}

	orgID, err := strconv.ParseInt(c.ctxBundle.Identity.OrgID, 10, 64)
	if err != nil {
		return fmt.Errorf("component %s parse org id error: %v", component.Name, err)
	}

	identity := c.ctxBundle.Identity

	jsonData, err := json.Marshal(c.ctxBundle.InParams)
	if err != nil {
		return fmt.Errorf("marshal id from inparams error: %v", err)
	}

	if err = json.Unmarshal(jsonData, &inParam); err != nil {
		return fmt.Errorf("unmarshal inparam to object error: %v", err)
	}

	if event.Operation.String() == apistructs.EdgeOperationSubmit {
		err = c.OperateSubmit(inParam.ID, identity)
		if err != nil {
			return err
		}
	} else if event.Operation == apistructs.RenderingOperation {
		err = c.OperateRendering(orgID, inParam.ID, identity)
		if err != nil {
			return fmt.Errorf("rendering operation error: %v", err)
		}
		c.component.Operations = getOperations()
		return nil
	}

	c.component.Operations = getOperations()
	c.component.State = map[string]interface{}{
		"visible": false,
	}

	return nil
}

func getOperations() apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		"submit": apistructs.EdgeOperation{
			Key:    "submit",
			Reload: true,
		},
	}
}

func getProps(sites []map[string]interface{}, modeEdit bool, lr *i18r.LocaleResource) apistructs.EdgeFormModalProps {
	edgeFormModal := apistructs.EdgeFormModalProps{
		Title: lr.Get(i18n.I18nKeyCreateConfigItem),
		Fields: []apistructs.EdgeFormModalField{
			{
				Key:       "key",
				Label:     lr.Get(i18n.I18nKeyKey),
				Component: "input",
				Disabled:  modeEdit,
				Required:  true,
				Rules: []apistructs.EdgeFormModalFieldRule{
					{
						Pattern: CfgItemKeyRegexp,
						Message: lr.Get(i18n.I18nKeyInputConfigItemTip),
					},
				},
				ComponentProps: map[string]interface{}{
					"maxLength": apistructs.EdgeDefaultNameMaxLength,
				},
			},
			{
				Key:       "value",
				Label:     lr.Get(i18n.I18nKeyValue),
				Component: "input",
				Required:  true,
				ComponentProps: map[string]interface{}{
					"maxLength": apistructs.EdgeDefaultLagerLength,
				},
			},
			{
				Key:          "scope",
				Label:        lr.Get(i18n.I18nKeyScope),
				Component:    "select",
				Disabled:     modeEdit,
				Required:     true,
				InitialValue: ScopeCommon,
				ComponentProps: map[string]interface{}{
					"placeholder": lr.Get(i18n.I18nKeySelectScope),
					"options": []map[string]interface{}{
						{"name": lr.Get(i18n.I18nKeyUniversal), "value": ScopeCommon},
						{"name": lr.Get(i18n.I18nKeySite), "value": ScopeSite},
					},
				},
			},
			{
				Key:       "sites",
				Label:     lr.Get(i18n.I18nKeySite),
				Component: "select",
				Disabled:  modeEdit,
				Required:  true,
				ComponentProps: map[string]interface{}{
					"mode":        "multiple",
					"placeholder": lr.Get(i18n.I18nKeySelectScope),
					"options":     sites,
					"selectAll":   true,
				},
				RemoveWhen: [][]map[string]interface{}{
					{
						{"field": "scope", "operator": "=", "value": ScopeCommon},
					},
				},
			},
		},
	}

	if modeEdit {
		edgeFormModal.Title = lr.Get(i18n.I18nKeyEditConfigItem)
	}

	return edgeFormModal
}
