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

package action

import (
	"context"
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/i18n"

	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

func mysqlCliRender(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalStateData *apistructs.GlobalStateData) (err error) {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	locale := bdl.Bdl.GetLocale(bdl.Locale)
	var formData map[string]interface{}
	switch string(event.Operation) {
	case "changeDateSource":
		stateMap, ok := c.State["formData"].(map[string]interface{})
		if !ok {
			goto RESULT
		}

		params, ok := stateMap["params"].(map[string]interface{})
		if !ok {
			goto RESULT
		}

		datasource, ok := params["datasource"].(string)
		if !ok {
			goto RESULT
		}

		addon, err := bdl.Bdl.GetAddon(datasource, bdl.Identity.OrgID, bdl.Identity.UserID)
		if err != nil {
			return err
		}

		if addon.Config["MYSQL_DATABASE"] != nil {
			params["database"] = addon.Config["MYSQL_DATABASE"]
		}
		stateMap["params"] = params
		formData = stateMap
	default:
		var param = map[string]interface{}{}
		if actionData, ok := bdl.InParams["actionData"].(map[string]interface{}); ok {
			if params, ok := actionData["params"]; ok {
				param = params.(map[string]interface{})
			}
		}

		formData = map[string]interface{}{
			"params": param,
		}
	}

RESULT:

	c.Operations = map[string]interface{}{
		"change": map[string]changeStruct{
			"params.datasource": {
				Key:    "changeDateSource",
				Reload: true,
			},
		},
	}

	var field []apistructs.FormPropItem
	props, ok := c.Props.(map[string]interface{})
	if !ok {
		return err
	}
	for key, val := range props {
		if key == "fields" {
			field = val.([]apistructs.FormPropItem)
			break
		}
	}

	query := make(map[string][]string)
	query["displayName"] = []string{"MySQL", "Custom", "AliCloud-Rds"}
	query["workspace"] = []string{"TEST"}
	query["type"] = []string{"database_addon"}
	query["value"] = []string{bdl.InParams["projectId"].(string)}
	query["projectId"] = []string{bdl.InParams["projectId"].(string)}
	addons, err := bdl.Bdl.ListConfigSheetAddon(query, bdl.Identity.OrgID, bdl.Identity.UserID)
	if err != nil {
		return err
	}

	var dataSourceList []map[string]interface{}
	for _, addon := range addons.Data {

		var name = addon.Name
		if addon.Tag != "" {
			name += fmt.Sprintf("(%s)", addon.Tag)
		}

		dataSourceList = append(dataSourceList, map[string]interface{}{
			"value": addon.ID,
			"name":  name,
		})
	}

	newField := fillMysqlCliFields(locale, field, dataSourceList)
	newProps := map[string]interface{}{
		"fields": newField,
	}
	c.Props = newProps
	c.State["formData"] = formData
	return nil
}

func fillMysqlCliFields(locale *i18n.LocaleResource, field []apistructs.FormPropItem, dataSourceList []map[string]interface{}) []apistructs.FormPropItem {
	taskParams := apistructs.FormPropItem{
		Component: "formGroup",
		ComponentProps: map[string]interface{}{
			"title": locale.Get("taskParams"),
		},
		Group: "params",
		Key:   "params",
	}

	dataSourceSelect := apistructs.FormPropItem{
		Label:     "datasource",
		Component: "select",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"options": dataSourceList,
		},
		Group:    "params",
		Key:      "params.datasource",
		LabelTip: locale.Get("dataSource"),
	}

	databaseField := apistructs.FormPropItem{
		Label:     "database",
		Component: "input",
		Required:  true,
		Key:       "params.database",
		ComponentProps: map[string]interface{}{
			"placeholder": locale.Get("dataInput"),
		},
		Group:    "params",
		LabelTip: locale.Get("dbName"),
	}

	sqlField := apistructs.FormPropItem{
		Label:     "sql",
		Component: "textarea",
		Required:  true,
		Key:       "params.sql",
		ComponentProps: map[string]interface{}{
			"autoSize": map[string]interface{}{
				"minRows": 2,
				"maxRows": 12,
			},
			"placeholder": locale.Get("dataInput"),
		},
		Group:    "params",
		LabelTip: locale.Get("sql"),
	}

	var newField []apistructs.FormPropItem
	for _, val := range field {
		newField = append(newField, val)
		if strings.EqualFold(val.Key, "if") {
			newField = append(newField, taskParams)
			newField = append(newField, dataSourceSelect)
			newField = append(newField, databaseField)
			newField = append(newField, sqlField)
		}
	}
	return newField
}
