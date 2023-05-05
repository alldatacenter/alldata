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

package inParamsForm

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/expression"
)

func (i *ComponentInParamsForm) SetProps(props cptype.ComponentProps) {
	// default temp value
	paramsNameProp := PropColumn{
		Title: i.sdk.I18n("paramName"),
		Key:   PropsKeyParamsName,
		Width: 150,
		Render: PropRender{
			Type:        "input",
			Required:    true,
			UniqueValue: true,
			Rules: []PropRenderRule{
				{
					Pattern: "/^[a-zA-Z0-9_-]*$/",
					Msg:     i.sdk.I18n("paramNameMessage2"),
				},
			},
			Props: PropRenderProp{MaxLength: 50},
		},
	}
	descProp := PropColumn{
		Title: i.sdk.I18n("name"),
		Key:   PropsKeyDesc,
		Width: 150,
		Render: PropRender{
			Type:  "input",
			Props: PropRenderProp{MaxLength: 1000},
		},
	}
	defaultValueProp := PropColumn{
		Title:    i.sdk.I18n("refValue"),
		TitleTip: i.sdk.I18n("validInPlan"),
		Key:      PropsKeyDefaultValue,
		Flex:     2,
		Render: PropRender{
			Type:             "inputSelect",
			ValueConvertType: "last",
			Required:         true,
			Props: PropRenderProp{
				Placeholder: i.sdk.I18n("selectiveExp"),
				Options: []PropChangeOption{
					{
						Label:  i.sdk.I18n("preSceneOut"),
						Value:  BeforeSceneOutPutOptionValue.String(),
						IsLeaf: false,
					},
					{
						Label:  "mock",
						Value:  MockOptionValue.String(),
						IsLeaf: false,
					},
					{
						Label:  i.sdk.I18n("globalInput"),
						Value:  GlobalOptionValue.String(),
						IsLeaf: false,
					},
				},
			},
			Operations: make(map[string]interface{}),
		},
	}

	o := OperationInfo{}
	o.Key = apistructs.AutoTestSceneInputOnSelectOperationKey.String()
	o.Reload = true
	o.FillMeta = "selectOption"
	defaultValueProp.Render.Operations[apistructs.AutoTestSceneInputOnSelectOperationKey.String()] = o
	valueProp := PropColumn{
		Title:    i.sdk.I18n("debugValue"),
		TitleTip: i.sdk.I18n("validInScene"),
		Key:      PropsKeyValue,
		Width:    200,
	}
	i.Props["temp"] = []PropColumn{paramsNameProp, descProp, defaultValueProp, valueProp}

	// set request props temp value
	if props == nil || props["temp"] == nil {
		return
	}
	tempValue, err := json.Marshal(props["temp"])
	if err != nil {
		logrus.Errorf("Marshal temp %v error %v", props["temp"], err)
		return
	}
	var reqPropColumn []PropColumn
	err = json.Unmarshal(tempValue, &reqPropColumn)
	if err != nil {
		logrus.Errorf("Unmarshal tempValue error %v", err)
		return
	}
	if reqPropColumn == nil {
		return
	}
	i.Props["temp"] = reqPropColumn
}

func (i *ComponentInParamsForm) RenderListInParamsForm() error {
	rsp, err := i.bdl.ListAutoTestSceneInput(i.State.AutotestSceneRequest)
	if err != nil {
		return err
	}
	list := make([]ParamData, 0, 0)
	for _, v := range rsp {
		pd := ParamData{
			ParamsName:   v.Name,
			Desc:         v.Description,
			DefaultValue: v.Value,
			Value:        v.Temp,
			ID:           v.ID,
		}
		list = append(list, pd)
	}

	i.Data.List = list

	i.Operations = make(map[string]interface{})
	o := apistructs.Operation{}
	o.Key = apistructs.AutoTestSceneInputUpdateOperationKey.String()
	o.Reload = true
	i.Operations[apistructs.AutoTestSceneInputUpdateOperationKey.String()] = o
	return nil
}

func (i *ComponentInParamsForm) RenderUpdateInParamsForm() error {
	req := apistructs.AutotestSceneInputUpdateRequest{
		AutotestSceneRequest: i.State.AutotestSceneRequest,
		List:                 i.State.List,
	}
	req.UserID = i.sdk.Identity.UserID
	_, err := i.bdl.UpdateAutoTestSceneInputs(req)
	if err != nil {
		return err
	}
	return nil
}

func (i *ComponentInParamsForm) RenderOnSelect(ctx context.Context, opsData interface{}) error {
	req, err := GetOpsInfo(opsData)
	if err != nil {
		return err
	}
	selectOptions := req.SelectOption
	var idIndex int
	pcl := i.Props["temp"].([]PropColumn)
	for id, p := range pcl {
		if p.Key == PropsKeyDefaultValue {
			idIndex = id
			break
		}
	}
	// 每一个已经选择的下拉菜单
	for _, value := range selectOptions {
		// 搜索对应value值的option
		for index := range pcl[idIndex].Render.Props.Options {
			op := (&pcl[idIndex].Render.Props.Options[index]).FindValue(value.Value)
			if op == nil {
				continue
			}
			children := make([]PropChangeOption, 0, 0)
			if strings.HasPrefix(value.Value, BeforeSceneOutPutOptionValue.String()) {
				// 前置场景出参
				_, list, err := i.bdl.ListAutoTestScene(i.State.AutotestSceneRequest)
				if err != nil {
					return err
				}
				for _, v := range list {
					if v.ID == i.State.AutotestSceneRequest.SceneID {
						break
					}
					o := PropChangeOption{
						Label:  v.Name,
						Value:  SceneOptionValue.String() + strconv.FormatInt(int64(v.ID), 10),
						IsLeaf: false,
					}
					children = append(children, o)
				}
			} else if strings.HasPrefix(value.Value, SceneOptionValue.String()) {
				// 场景
				// 从场景的value中获取id的string
				idStr := strings.TrimPrefix(op.Value, SceneOptionValue.String())
				id, err := strconv.ParseInt(idStr, 10, 64)
				if err != nil {
					return err
				}
				req := i.State.AutotestSceneRequest
				req.SceneID = uint64(id)
				dbScene, err := i.bdl.GetAutoTestScene(req)
				if err != nil {
					return err
				}

				// if the scene references the scene set
				if dbScene.RefSetID > 0 {
					// list sceneSet to children
					var autotestSceneRequest apistructs.AutotestSceneRequest
					autotestSceneRequest.UserID = i.sdk.Identity.UserID
					autotestSceneRequest.SetID = dbScene.RefSetID
					autotestSceneRequest.SpaceID = i.InParams.SpaceId
					_, list, err := i.bdl.ListAutoTestScene(autotestSceneRequest)
					if err != nil {
						return err
					}
					for _, v := range list {
						if v.ID == i.State.AutotestSceneRequest.SceneID {
							break
						}
						o := PropChangeOption{
							Label:  v.Name,
							Value:  refSceneSetOptionValue.String() + strconv.FormatInt(int64(v.ID), 10) + "_" + strconv.FormatInt(int64(dbScene.ID), 10),
							IsLeaf: false,
						}
						children = append(children, o)
					}
				} else {
					// list scene output to children
					list, err := i.bdl.ListAutoTestSceneOutput(req)
					if err != nil {
						return err
					}
					for _, v := range list {
						str, err := i.GetSceneOutPutValue(dbScene.ID, v.Name)
						if err != nil {
							return err
						}
						o := PropChangeOption{
							Label:  v.Name,
							Value:  str,
							IsLeaf: true,
						}
						children = append(children, o)
					}
				}
			} else if strings.HasPrefix(value.Value, refSceneSetOptionValue.String()) {
				// get sceneID and ref sceneID
				sceneRefSetValue := strings.TrimPrefix(op.Value, refSceneSetOptionValue.String())
				sceneRefSet := strings.Split(sceneRefSetValue, "_")
				if len(sceneRefSet) != 2 {
					return fmt.Errorf("error find ref sceneSet output")
				}
				refSceneID, err := strconv.ParseInt(sceneRefSet[0], 10, 64)
				if err != nil {
					return err
				}
				sceneID, err := strconv.ParseUint(sceneRefSet[1], 10, 64)
				if err != nil {
					return err
				}

				// list scene output to children
				req := i.State.AutotestSceneRequest
				req.SceneID = uint64(refSceneID)
				dbScene, err := i.bdl.GetAutoTestScene(req)
				if err != nil {
					return err
				}
				list, err := i.bdl.ListAutoTestSceneOutput(req)
				if err != nil {
					return err
				}
				for _, v := range list {
					str, err := i.GetSceneOutPutValue(sceneID, strconv.FormatInt(int64(dbScene.ID), 10)+"_"+v.Name)
					if err != nil {
						return err
					}
					o := PropChangeOption{
						Label:  v.Name,
						Value:  str,
						IsLeaf: true,
					}
					children = append(children, o)
				}
			} else if strings.HasPrefix(value.Value, MockOptionValue.String()) {
				for _, v := range expression.MockString {
					o := PropChangeOption{
						Label:   v,
						Value:   expression.GenRandomRef(v),
						IsLeaf:  true,
						ToolTip: i.sdk.I18n("wb.content.autotest.scene." + v),
					}
					children = append(children, o)
				}
			} else if strings.HasPrefix(value.Value, GlobalOptionValue.String()) {
				cfgReq := apistructs.AutoTestGlobalConfigListRequest{Scope: "project-autotest-testcase", ScopeID: strconv.Itoa(int(cputil.GetInParamByKey(ctx, "projectId").(float64)))}
				cfgReq.UserID = i.sdk.Identity.UserID
				cfgs, err := i.bdl.ListAutoTestGlobalConfig(cfgReq)
				if err != nil {
					return err
				}
				cfgChildren0 := make([]PropChangeOption, 0, 0)
				for _, cfg := range cfgs {
					// Header 是自动带上去的
					// cfgChildren1, cfgChildren2, cfgChildren3 := make([]Input, 0, 0), make([]Input, 0, 0), make([]Input, 0, 0)
					cfgChildren1, cfgChildren3 := make([]PropChangeOption, 0, 0), make([]PropChangeOption, 0, 0)
					// for k := range cfg.APIConfig.Header {
					// 	cfgChildren2 = append(cfgChildren2, Input{Label: k, Value: "{{" + k + "}}", IsLeaf: true})
					// }
					for _, v := range cfg.APIConfig.Global {
						cfgChildren3 = append(cfgChildren3, PropChangeOption{Label: v.Name, Value: expression.GenAutotestConfigParams(v.Name), IsLeaf: true})
					}
					// cfgChildren1 = append(cfgChildren1, Input{Label: "Header", Value: "Header", IsLeaf: false, Children: cfgChildren2})
					cfgChildren1 = append(cfgChildren1, PropChangeOption{Label: "Global", Value: "Global", IsLeaf: false, Children: cfgChildren3})
					cfgChildren0 = append(cfgChildren0, PropChangeOption{Label: cfg.DisplayName, Value: cfg.DisplayName, IsLeaf: false, Children: cfgChildren1})
				}
				children = cfgChildren0
			}
			op.Children = children
			break
		}
	}
	i.Props["temp"] = pcl
	return nil
}

func (i *ComponentInParamsForm) GetSceneOutPutValue(sceneName uint64, name string) (string, error) {
	valStr := fmt.Sprintf("${{ outputs.%v.%s }}", sceneName, name)
	return valStr, nil
}

func (i *ComponentInParamsForm) Filter() []ParamData {
	var (
		list []ParamData //新添加且未保存的行不在数据库里，但是需要返回
	)
	for _, v := range i.State.List {
		data := ParamData{
			ParamsName:   v.Name,
			Desc:         v.Description,
			DefaultValue: v.Value,
			Value:        v.Temp,
		}
		list = append(list, data)
	}
	return list
}
