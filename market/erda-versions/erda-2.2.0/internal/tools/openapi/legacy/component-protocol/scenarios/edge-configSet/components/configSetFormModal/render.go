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
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-configSet/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

func (c ComponentFormModal) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if err := c.SetComponent(component); err != nil {
		return err
	}

	orgID, err := strconv.ParseInt(c.ctxBundle.Identity.OrgID, 10, 64)
	if err != nil {
		return fmt.Errorf("component %s parse org id error: %v", component.Name, err)
	}

	identity := c.ctxBundle.Identity

	if event.Operation.String() == apistructs.EdgeOperationSubmit {
		err = c.OperateSubmit(orgID, identity)
		if err != nil {
			return fmt.Errorf("operation submit error: %v", err)
		}
	}

	edgeClusters, err := c.ctxBundle.Bdl.ListEdgeCluster(uint64(orgID), apistructs.EdgeListValueTypeID, identity)
	if err != nil {
		return fmt.Errorf("get avaliable edge clusters error: %v", err)
	}

	c.component.Props = getProps(edgeClusters, i18nLocale)
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

func getProps(cluster []map[string]interface{}, lr *i18r.LocaleResource) apistructs.EdgeFormModalProps {
	return apistructs.EdgeFormModalProps{
		Title: lr.Get(i18n.I18nKeyCreateConfigSet),
		Fields: []apistructs.EdgeFormModalField{
			{
				Key:       "name",
				Label:     lr.Get(i18n.I18nKeyName),
				Component: "input",
				Required:  true,
				Rules: []apistructs.EdgeFormModalFieldRule{
					{
						Pattern: apistructs.EdgeDefaultRegexp,
						Message: apistructs.EdgeDefaultRegexpError,
					},
				},
				ComponentProps: map[string]interface{}{
					"maxLength": apistructs.EdgeDefaultNameMaxLength,
				},
			},
			{
				Key:       "cluster",
				Label:     lr.Get(i18n.I18nKeyCluster),
				Component: "select",
				Required:  true,
				ComponentProps: map[string]interface{}{
					"placeholder": lr.Get(i18n.I18nKeySelectCluster),
					"options":     cluster,
				},
			},
		},
	}
}
