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

package appsitebreadcrumb

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-app-site-ip/i18n"
)

type EdgeAppSiteIPInParam struct {
	ID       int64  `json:"id"`
	AppName  string `json:"appName"`
	SiteName string `json:"siteName"`
}

func (c ComponentBreadCrumb) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if err := c.SetComponent(component); err != nil {
		return err
	}

	inParam := EdgeAppSiteIPInParam{}

	jsonData, err := json.Marshal(c.ctxBundle.InParams)
	if err != nil {
		return fmt.Errorf("marshal id from inparams error: %v", err)
	}

	if err = json.Unmarshal(jsonData, &inParam); err != nil {
		return fmt.Errorf("unmarshal inparam to object error: %v", err)
	}

	c.component.Data = map[string]interface{}{
		"list": []map[string]interface{}{
			{
				"key":  "appName",
				"item": i18nLocale.Get(i18n.I18nKeySiteList),
			},
			{
				"key":  "siteName",
				"item": inParam.SiteName,
			},
		},
	}

	c.component.Operations = getOperations(inParam.SiteName, inParam.AppName)
	return nil
}

func getOperations(siteName, appName string) apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		apistructs.EdgeOperationClick: apistructs.EdgeOperation{
			Key:      "selectItem",
			FillMeta: "key",
			Meta: map[string]interface{}{
				"key": "",
			},
			Reload: false,
			Command: apistructs.EdgeJumpCommand{
				Key:     "goto",
				Target:  "ecpAppSiteManage",
				JumpOut: false,
				State: apistructs.EdgeJumpCommandState{
					Params: map[string]interface{}{
						"siteName": siteName,
					},
					Query: map[string]interface{}{
						"appName": appName,
					},
				},
			},
		},
	}
}
