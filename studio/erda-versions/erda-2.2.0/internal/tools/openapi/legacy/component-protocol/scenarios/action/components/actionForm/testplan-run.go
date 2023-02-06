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
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/i18n"
)

// testSceneRun testSceneRun component protocol
func testPlanRun(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalStateData *apistructs.GlobalStateData) (err error) {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	locale := bdl.Bdl.GetLocale(bdl.Locale)
	projectId, err := strconv.Atoi(bdl.InParams["projectId"].(string))
	if err != nil {
		return err
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

	// get testplan
	testPlanRequest := apistructs.TestPlanV2PagingRequest{
		ProjectID:  uint64(projectId),
		PageNo:     1,
		PageSize:   999,
		IsArchived: &[]bool{false}[0],
	}
	testPlanRequest.UserID = bdl.Identity.UserID
	plans, err := bdl.Bdl.PagingTestPlansV2(testPlanRequest)
	if err != nil {
		return err
	}
	testPlans := make([]map[string]interface{}, 0, plans.Total)
	for _, v := range plans.List {
		testPlans = append(testPlans, map[string]interface{}{"name": fmt.Sprintf("%s-%d", v.Name, v.ID), "value": v.ID})
	}
	// get globalConfigRequest
	globalConfigRequest := apistructs.AutoTestGlobalConfigListRequest{
		ScopeID: strconv.Itoa(projectId),
		Scope:   "project-autotest-testcase",
	}
	globalConfigRequest.UserID = bdl.Identity.UserID

	globalConfigs, err := bdl.Bdl.ListAutoTestGlobalConfig(globalConfigRequest)
	if err != nil {
		return err
	}
	cms := make([]map[string]interface{}, 0, len(globalConfigs))
	for _, v := range globalConfigs {
		cms = append(cms, map[string]interface{}{"name": v.DisplayName, "value": v.Ns})
	}

	var newField []apistructs.FormPropItem
	newField = fillTestPlanFields(locale, field, testPlans, cms)
	newProps := map[string]interface{}{
		"fields": newField,
	}
	c.Props = newProps
	return nil
}

func fillTestPlanFields(locale *i18n.LocaleResource, field []apistructs.FormPropItem, testPlans []map[string]interface{}, cms []map[string]interface{}) []apistructs.FormPropItem {

	// Add task parameters
	taskParams := apistructs.FormPropItem{
		Component: "formGroup",
		ComponentProps: map[string]interface{}{
			"title": locale.Get("taskParams"),
		},
		Group: "params",
		Key:   "params",
	}
	testPlanField := apistructs.FormPropItem{
		Label:     locale.Get("testPlan"),
		Component: "select",
		Required:  true,
		Key:       "params.test_plan",
		ComponentProps: map[string]interface{}{
			"options":          testPlans,
			"showSearch":       true,
			"optionFilterProp": "children",
		},
		Group: "params",
	}
	globalConfigField := apistructs.FormPropItem{
		Label:     locale.Get("paramsConfig"),
		Component: "select",
		Required:  true,
		Key:       "params.cms",
		ComponentProps: map[string]interface{}{
			"options":          cms,
			"showSearch":       true,
			"optionFilterProp": "children",
		},
		Group: "params",
	}
	isContinueExecutionField := apistructs.FormPropItem{
		Label:        locale.Get("rerun"),
		Component:    "input",
		Required:     false,
		Key:          "params.is_continue_execution",
		Group:        "params",
		DefaultValue: true,
	}
	var newField []apistructs.FormPropItem
	for _, val := range field {
		newField = append(newField, val)
		if strings.EqualFold(val.Label, locale.Get("execCond")) {
			newField = append(newField, taskParams)
			newField = append(newField, testPlanField)
			newField = append(newField, globalConfigField)
			newField = append(newField, isContinueExecutionField)
		}
	}
	return newField
}
