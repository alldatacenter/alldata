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
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/i18n"
)

// testSceneRun testSceneRun component protocol
func testSceneRun(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalStateData *apistructs.GlobalStateData) (err error) {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	locale := bdl.Bdl.GetLocale(bdl.Locale)
	projectId, err := strconv.ParseInt(bdl.InParams["projectId"].(string), 10, 64)
	if err != nil {
		return err
	}

	c.Operations = map[string]interface{}{
		"change": map[string]changeStruct{
			"params.test_space": {
				Key:    "changeTestSpace",
				Reload: true,
			},
			"params.test_scene_set": {
				Key:    "changeTestSet",
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

	// spaces, err := bdl.Bdl.ListTestSpace(projectId, 500, 1, "")
	spaces, err := bdl.Bdl.ListTestSpace(apistructs.AutoTestSpaceListRequest{
		ProjectID: projectId,
		PageNo:    1,
		PageSize:  500,
	})
	if err != nil {
		return err
	}
	testSpaces := make([]map[string]interface{}, 0, spaces.Total)
	for _, v := range spaces.List {
		testSpaces = append(testSpaces, map[string]interface{}{"name": v.Name, "value": v.ID})
	}
	testSceneSets := make([]map[string]interface{}, 0)

	testScenes := make([]map[string]interface{}, 0)

	formData := make(map[string]interface{})
	newMap, ok := c.State["formData"].(map[string]interface{})
	if !ok {
		newMap = nil
	}

	if strings.EqualFold(string(event.Operation), "changeTestSpace") {

		formData = changeTestSpace(newMap)

		params, ok := formData["params"].(map[string]interface{})
		if !ok {
			params = nil
		}
		space, _ := params["test_space"].(float64)
		spaceId := uint64(space)
		sceneSetReq := apistructs.SceneSetRequest{
			SpaceID: spaceId,
		}
		sceneSetReq.UserID = bdl.Identity.UserID
		sceneSets, err := bdl.Bdl.GetSceneSets(sceneSetReq)
		if err != nil {
			return err
		}
		for _, v := range sceneSets {
			testSceneSets = append(testSceneSets, map[string]interface{}{"name": v.Name, "value": v.ID})
		}
	} else if strings.EqualFold(string(event.Operation), "changeTestSet") {

		formData = changeTestSet(newMap)

		params, ok := formData["params"].(map[string]interface{})
		if !ok {
			params = nil
		}

		space, _ := params["test_space"].(float64)
		spaceId := uint64(space)
		sceneSetReq := apistructs.SceneSetRequest{
			SpaceID: spaceId,
		}
		sceneSetReq.UserID = bdl.Identity.UserID
		sceneSets, err := bdl.Bdl.GetSceneSets(sceneSetReq)
		if err != nil {
			return err
		}
		for _, v := range sceneSets {
			testSceneSets = append(testSceneSets, map[string]interface{}{"name": v.Name, "value": v.ID})
		}

		set, _ := params["test_scene_set"].(float64)
		setId := uint64(set)
		sceneReq := apistructs.AutotestSceneRequest{
			SetID: setId,
		}
		sceneReq.UserID = bdl.Identity.UserID
		_, scenes, err := bdl.Bdl.ListAutoTestScene(sceneReq)
		if err != nil {
			return err
		}
		for _, v := range scenes {
			testScenes = append(testScenes, map[string]interface{}{"name": v.Name, "value": v.ID})
		}
	} else {
		if actionData, ok := bdl.InParams["actionData"].(map[string]interface{}); ok {
			if params, ok := actionData["params"]; ok {
				param := params.(map[string]interface{})
				formData = actionData

				space, _ := param["test_space"].(float64)
				spaceId := uint64(space)
				sceneSetReq := apistructs.SceneSetRequest{
					SpaceID: spaceId,
				}
				sceneSetReq.UserID = bdl.Identity.UserID
				sceneSets, err := bdl.Bdl.GetSceneSets(sceneSetReq)
				if err != nil {
					return err
				}
				for _, v := range sceneSets {
					testSceneSets = append(testSceneSets, map[string]interface{}{"name": v.Name, "value": v.ID})
				}

				set, _ := param["test_scene_set"].(float64)
				setId := uint64(set)
				sceneReq := apistructs.AutotestSceneRequest{
					SetID: setId,
				}
				sceneReq.UserID = bdl.Identity.UserID
				_, scenes, err := bdl.Bdl.ListAutoTestScene(sceneReq)
				if err != nil {
					return err
				}
				for _, v := range scenes {
					testScenes = append(testScenes, map[string]interface{}{"name": v.Name, "value": v.ID})
				}
			}
		}
	}
	// 获取全局配置
	globalConfigRequest := apistructs.AutoTestGlobalConfigListRequest{
		ScopeID: bdl.InParams["projectId"].(string),
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

	newField := fillFields(locale, field, testSpaces, testSceneSets, testScenes, cms)
	newProps := map[string]interface{}{
		"fields": newField,
	}
	c.State["formData"] = formData
	c.Props = newProps
	return nil
}

func changeTestSpace(newMap map[string]interface{}) map[string]interface{} {
	formData := newMap
	if params, ok := formData["params"].(map[string]interface{}); ok {
		if _, ok := params["test_scene_set"]; ok {
			delete(params, "test_scene_set")
		}
		if _, ok := params["test_scene"]; ok {
			delete(params, "test_scene")
		}
		formData["params"] = params
		return formData
	}
	return formData
}

func changeTestSet(newMap map[string]interface{}) map[string]interface{} {
	formData := newMap
	if params, ok := formData["params"].(map[string]interface{}); ok {
		if _, ok := params["test_scene"]; ok {
			delete(params, "test_scene")
		}
		formData["params"] = params
		return formData
	}
	return formData
}

func fillFields(locale *i18n.LocaleResource, field []apistructs.FormPropItem, testSpaces []map[string]interface{}, testSceneSets []map[string]interface{}, testScenes []map[string]interface{}, cms []map[string]interface{}) []apistructs.FormPropItem {

	// Add task parameters
	taskParams := apistructs.FormPropItem{
		Component: "formGroup",
		ComponentProps: map[string]interface{}{
			"title": locale.Get("taskParams"),
		},
		Group: "params",
		Key:   "params",
	}
	spaceField := apistructs.FormPropItem{
		Label:     locale.Get("testSpace"),
		Component: "select",
		Required:  true,
		Key:       "params.test_space",
		ComponentProps: map[string]interface{}{
			"options":          testSpaces,
			"showSearch":       true,
			"optionFilterProp": "children",
		},
		Group: "params",
	}
	sceneSetField := apistructs.FormPropItem{
		Label:     locale.Get("tp.export.scene.set.sheet.name"),
		Component: "select",
		Required:  true,
		Key:       "params.test_scene_set",
		ComponentProps: map[string]interface{}{
			"options":          testSceneSets,
			"showSearch":       true,
			"optionFilterProp": "children",
		},
		Group: "params",
	}
	if testSceneSets != nil {

	}

	sceneField := apistructs.FormPropItem{
		Label:     locale.Get("scene"),
		Component: "select",
		Required:  true,
		Key:       "params.test_scene",
		ComponentProps: map[string]interface{}{
			"options":          testScenes,
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
			"options": cms,
		},
		Group: "params",
	}
	var newField []apistructs.FormPropItem
	for _, val := range field {
		newField = append(newField, val)
		if strings.EqualFold(val.Label, locale.Get("execCond")) {
			newField = append(newField, taskParams)
			newField = append(newField, spaceField)
			newField = append(newField, sceneSetField)
			newField = append(newField, sceneField)
			newField = append(newField, globalConfigField)
		}
	}
	return newField
}
