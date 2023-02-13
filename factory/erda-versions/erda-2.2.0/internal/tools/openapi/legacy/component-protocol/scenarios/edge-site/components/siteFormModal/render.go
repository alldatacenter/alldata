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

package siteformmodal

import (
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-site/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

const (
	SiteNameMatchPattern = "^[a-z][a-z0-9-]*[a-z0-9]$"
)

var (
	SiteNameMatchRegexp = fmt.Sprintf("/%v/", SiteNameMatchPattern)
)

type SiteFormSubmitBase struct {
	ID       int64  `json:"id"`
	SiteName string `json:"siteName"`
	Desc     string `json:"desc"`
}

type SiteFormCreate struct {
	SiteFormSubmitBase
	RelatedCluster int64 `json:"relatedCluster"`
}

func (c ComponentFormModal) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

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

	if c.component.State == nil {
		c.component.State = map[string]interface{}{}
	}

	identity := c.ctxBundle.Identity

	if event.Operation.String() == apistructs.EdgeOperationSubmit {
		err = c.OperateSubmit(orgID, identity)
		if err != nil {
			return err
		}
	} else if event.Operation == apistructs.RenderingOperation {
		err = c.OperateRendering(orgID, identity)
		if err != nil {
			return err
		}
		c.component.Operations = getOperations()
		return nil
	}

	c.component.State = map[string]interface{}{
		"reload":  true,
		"visible": false,
	}
	c.component.Operations = getOperations()

	return nil
}

func getOperations() apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		"submit": map[string]interface{}{
			"key":    "submit",
			"reload": true,
		},
	}
}

func getProps(relatedCluster []map[string]interface{}, modeEdit bool, lr *i18r.LocaleResource) apistructs.EdgeFormModalProps {

	siteNameField := apistructs.EdgeFormModalField{
		Key:       "siteName",
		Label:     lr.Get(i18n.I18nKeySiteName),
		Component: "input",
		Required:  true,
		Rules: []apistructs.EdgeFormModalFieldRule{
			{
				Pattern: SiteNameMatchRegexp,
				Message: lr.Get(i18n.I18nKeyInputSiteNameTip),
			},
		},
		ComponentProps: map[string]interface{}{
			"maxLength": SiteNameLength,
		},
	}

	clusterField := apistructs.EdgeFormModalField{
		Key:       "relatedCluster",
		Label:     lr.Get(i18n.I18nKeyAssociatedCluster),
		Component: "select",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeySelectCluster),
			"options":     relatedCluster,
		},
	}

	descField := apistructs.EdgeFormModalField{
		Key:       "desc",
		Label:     lr.Get(i18n.I18nKeySiteDescription),
		Component: "textarea",
		Required:  false,
		ComponentProps: map[string]interface{}{
			"maxLength": apistructs.EdgeDefaultLagerLength / 2,
		},
	}

	siteNameField.Disabled = modeEdit
	clusterField.Disabled = modeEdit

	props := apistructs.EdgeFormModalProps{
		Title: lr.Get(i18n.I18nKeyCreateSite),
		Fields: []apistructs.EdgeFormModalField{
			siteNameField,
			clusterField,
			descField,
		},
	}

	if modeEdit {
		props.Title = lr.Get(i18n.I18nKeyEditSite)
	}

	return props
}
