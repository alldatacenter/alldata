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
	"encoding/json"
	"fmt"
	"sync"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const DataValueKey = "action_data_value_key"

func (a *ComponentAction) GenActionState(c *apistructs.Component) (version string, err error) {
	sByte, err := json.Marshal(c.State)
	if err != nil {
		err = fmt.Errorf("failed to marshal action version, state:%+v, err:%v", c.State, err)
		return
	}
	state := ComponentActionState{}
	err = json.Unmarshal(sByte, &state)
	if err != nil {
		return
	}
	a.SetActionState(state)

	data := c.State["formData"]
	if data == nil {
		return
	}

	fromData, ok := data.(map[string]interface{})
	if !ok {
		return
	}
	versionInterface, ok := fromData["version"]
	if !ok {
		return
	}

	version = versionInterface.(string)

	return
}

func GenHeaderProps(local *i18n.LocaleResource, actionExt *apistructs.ExtensionVersion, versions []VersionOption) (props []apistructs.FormPropItem, err error) {
	if actionExt == nil {
		err = fmt.Errorf("empty action extension")
		return
	}
	// input
	// alias: 任务名称
	aliasInput := apistructs.FormPropItem{
		Label:     local.Get("taskName"),
		Component: "input",
		Required:  true,
		Key:       "alias",
		ComponentProps: map[string]interface{}{
			"placeholder": local.Get("taskNameInput"),
		},
		DefaultValue: actionExt.Name,
	}

	// select
	// version: 版本
	// 动态注入：所有版本选项，版本默认值
	verSelect := apistructs.FormPropItem{
		Label:     local.Get("version"),
		Component: "select",
		Required:  true,
		Key:       "version",
		ComponentProps: map[string]interface{}{
			"options": versions,
		},
		DefaultValue: actionExt.Version,
	}

	// input
	// if: 执行条件
	ifInput := apistructs.FormPropItem{
		Label:     local.Get("execCond"),
		Component: "input",
		Key:       "if",
		ComponentProps: map[string]interface{}{
			"placeholder": local.Get("execCondInput"),
		},
	}
	props = append(props, aliasInput, verSelect, ifInput)
	return
}

func GenTimeoutProps(local *i18n.LocaleResource) (props []apistructs.FormPropItem, err error) {
	timeout := apistructs.FormPropItem{
		Label:     local.Get("wb.content.action.input.label.timeout"),
		Component: "inputNumber",
		Key:       "timeout",
		ComponentProps: map[string]interface{}{
			"placeholder": local.Get("wb.content.action.input.label.timeoutPlaceholder"),
		},
		DefaultValue: 3600,
	}

	props = append(props, timeout)
	return
}

func GenResourceProps(local *i18n.LocaleResource, actionExt *apistructs.ExtensionVersion) (props []apistructs.FormPropItem, err error) {
	if actionExt == nil {
		err = fmt.Errorf("empty action extension")
		return
	}

	var job diceyml.Job
	diceYmlStr, err := yaml.Marshal(actionExt.Dice)
	if err != nil {
		err = fmt.Errorf("failed to marshal dice yaml, error:%v", err)
		return
	}

	diceYml, err := diceyml.New(diceYmlStr, false)
	if err != nil {
		err = fmt.Errorf("failed to parse action dice spec, error:%v", err)
		return
	}
	for k := range diceYml.Obj().Jobs {
		job = *diceYml.Obj().Jobs[k]
		break
	}

	// resourceFormGroup
	// fromGroup下会根据resource 构建一个资源列表框
	GroupResource := "resources"
	resourceFormGroup := apistructs.FormPropItem{
		Key:       GroupResource,
		Component: "formGroup",
		Group:     GroupResource,
		ComponentProps: map[string]interface{}{
			"title":         local.Get("runResource"),
			"expandable":    true,
			"defaultExpand": false,
		},
	}
	// 动态注入：cpu默认值
	resourceCpu := apistructs.FormPropItem{
		Label:          local.Get("cpu"),
		Component:      "inputNumber",
		Key:            GroupResource + "." + "cpu",
		Group:          GroupResource,
		ComponentProps: map[string]interface{}{},
		DefaultValue:   job.Resources.CPU,
	}
	// 动态注入：mem默认值
	resourceMem := apistructs.FormPropItem{
		Label:          "mem(MB)",
		Component:      "inputNumber",
		Key:            GroupResource + "." + "mem",
		Group:          GroupResource,
		ComponentProps: map[string]interface{}{},
		DefaultValue:   job.Resources.Mem,
	}

	props = append(props, resourceFormGroup, resourceCpu, resourceMem)
	return
}

func GenParamAndLoopProps(local *i18n.LocaleResource, actionExt *apistructs.ExtensionVersion) (params []apistructs.FormPropItem, loop []apistructs.FormPropItem, err error) {
	if actionExt == nil {
		err = fmt.Errorf("empty action extension")
		return
	}
	specBytes, err := yaml.Marshal(actionExt.Spec)
	if err != nil {
		err = fmt.Errorf("failed to marshal action spec, error:%v", err)
		return
	}
	actionSpec := apistructs.ActionSpec{}
	err = yaml.Unmarshal(specBytes, &actionSpec)
	if err != nil {
		err = fmt.Errorf("failed to unmarshal action spec, error:%v", err)
		return
	}
	params = actionSpec.FormProps
	loop = GenLoopProps(local, actionSpec.Loop)
	return
}

func GenLoopProps(local *i18n.LocaleResource, loop *apistructs.PipelineTaskLoop) (loopFp []apistructs.FormPropItem) {
	// loopFormGroup
	// fromGroup下会根据loop 构建一个表单
	GroupLoop := "loop"
	loopFormGroup := apistructs.FormPropItem{
		Key:       GroupLoop,
		Component: "formGroup",
		Group:     GroupLoop,
		ComponentProps: map[string]interface{}{
			"title":         local.Get("loopStrategy"),
			"expandable":    true,
			"defaultExpand": false,
		},
	}
	breakCon := apistructs.FormPropItem{
		Label:     local.Get("loopEndCondtion"),
		Component: "input",
		Key:       GroupLoop + "." + "break",
		Group:     GroupLoop,
	}
	maxTimes := apistructs.FormPropItem{
		Label:     local.Get("maxLoop"),
		Component: "inputNumber",
		ComponentProps: map[string]interface{}{
			"precision": 0,
		},
		Key:   GroupLoop + "." + "strategy.max_times",
		Group: GroupLoop,
	}
	declineRatio := apistructs.FormPropItem{
		Label:     local.Get("declineRatio"),
		Component: "inputNumber",
		Key:       GroupLoop + "." + "strategy.decline_ratio",
		Group:     GroupLoop,
		LabelTip:  local.Get("intervalRatio"),
	}
	declineLimit := apistructs.FormPropItem{
		Label:     local.Get("declineMax"),
		Component: "inputNumber",
		ComponentProps: map[string]interface{}{
			"precision": 0,
		},
		Key:      GroupLoop + "." + "strategy.decline_limit_sec",
		Group:    GroupLoop,
		LabelTip: local.Get("loopMaxInterval"),
	}
	interval := apistructs.FormPropItem{
		Label:     local.Get("startInterval"),
		Component: "inputNumber",
		Key:       GroupLoop + "." + "strategy.interval_sec",
		Group:     GroupLoop,
	}
	if loop != nil {
		breakCon.DefaultValue = loop.Break
		if loop.Strategy != nil {
			maxTimes.DefaultValue = loop.Strategy.MaxTimes
			declineRatio.DefaultValue = loop.Strategy.DeclineRatio
			declineLimit.DefaultValue = loop.Strategy.DeclineLimitSec
			interval.DefaultValue = loop.Strategy.IntervalSec
		}
	}

	loopFp = append(loopFp, loopFormGroup, breakCon, maxTimes, declineRatio, declineLimit, interval)
	return
}

func GenActionProps(ctx context.Context, c *apistructs.Component, name, version string) (err error) {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	actionExt, versions, err := QueryExtensionVersion(bdl.Bdl, name, version)
	if err != nil {
		logrus.Errorf("query extension version failed, name:%s, version:%s, err:%v", name, version, err)
		return
	}
	// 默认请求时，version为空，需要以默认版本覆盖
	c.State["version"] = actionExt.Version
	local := bdl.Bdl.GetLocale(bdl.Locale)

	header, err := GenHeaderProps(local, actionExt, versions)
	if err != nil {
		logrus.Errorf("generate action header props failed, name:%s, version:%s, err:%v", name, version, err)
		return
	}

	params, loop, err := GenParamAndLoopProps(local, actionExt)
	if err != nil {
		logrus.Errorf("generate action params and loop props failed, name:%s, version:%s, err:%v", name, version, err)
		return
	}

	resource, err := GenResourceProps(local, actionExt)
	if err != nil {
		logrus.Errorf("generate action resource props failed, name:%s, version:%s, err:%v", name, version, err)
		return
	}

	timeouts, err := GenTimeoutProps(local)
	if err != nil {
		logrus.Errorf("generate action timeout props failed, name: %s, version: %s, err:%v", name, version, err)
		return
	}

	var props []apistructs.FormPropItem
	props = append(props, header...)
	props = append(props, params...)
	props = append(props, resource...)
	props = append(props, loop...)
	props = append(props, timeouts...)
	c.Props = map[string]interface{}{
		"fields": props,
	}
	return
}

var actionTypeRender map[string]func(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalStateData *apistructs.GlobalStateData) (err error)
var one sync.Once

func registerActionTypeRender() {
	one.Do(func() {
		actionTypeRender = make(map[string]func(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalStateData *apistructs.GlobalStateData) (err error))
		actionTypeRender["manual-review"] = func(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalStateData *apistructs.GlobalStateData) (err error) {
			bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
			action := ctx.Value(DataValueKey).(*apistructs.PipelineYmlAction)
			if action == nil {
				return nil
			}

			defer func() {
				c.Props = setMemberSelectorComponentScopeIDFieldWithAppID(c.Props, bdl.InParams["appId"])
			}()

			params := action.Params
			if params == nil || params["processor"] == nil {
				return nil
			}
			processorJson, err := json.Marshal(params["processor"])
			if err != nil {
				return err
			}

			var processor []string
			err = json.Unmarshal(processorJson, &processor)
			if err != nil {
				return err
			}

			(*globalStateData)[protocol.GlobalInnerKeyUserIDs.String()] = processor
			return nil
		}

		actionTypeRender["testplan-run"] = testPlanRun

		actionTypeRender["testscene-run"] = testSceneRun

		//actionTypeRender["mysql-cli"] = mysqlCliRender
	})
}

func setMemberSelectorComponentScopeIDFieldWithAppID(props interface{}, appID interface{}) (resultProps interface{}) {
	if props == nil {
		return
	}

	value, ok := props.(map[string]interface{})
	if !ok {
		return
	}

	fields, ok := value["fields"].([]apistructs.FormPropItem)
	if !ok {
		return
	}

	for _, field := range fields {
		if field.Component != "memberSelector" {
			continue
		}
		props, ok := field.ComponentProps.(map[string]interface{})
		if !ok {
			continue
		}
		props["scopeId"] = appID
	}
	return props
}

func (a *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalStateData *apistructs.GlobalStateData) (err error) {
	registerActionTypeRender()
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	err = a.SetBundle(bdl)
	if err != nil {
		return err
	}

	if c.State == nil {
		c.State = map[string]interface{}{}
	}

	var changeVersion string

	switch event.Operation {
	case apistructs.ChangeOperation:
		changeVersion, err = a.GenActionState(c)
		if err != nil {
			logrus.Errorf("generate action state failed,  err:%v", err)
			return
		}
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
	default:
		logrus.Warnf("operation [%s] not support, use default operation instead", event.Operation)
	}

	actionName := scenario.ScenarioKey
	chooseActionData := getChooseActionData(bdl)

	var chooseActionVersion string
	if chooseActionData != nil && chooseActionData.Version != "" && chooseActionData.Type == actionName {
		chooseActionVersion = chooseActionData.Version
	}

	version := GetActionVersion(c)
	if chooseActionData != nil && chooseActionData.Type == actionName {
		version = chooseActionVersion
	}
	if version == "[前端选择列表选择]" {
		version = ""
	}

	if changeVersion != "" {
		version = changeVersion
	}

	err = GenActionProps(ctx, c, actionName, version)
	if err != nil {
		logrus.Errorf("generate action props failed, name:%s, version:%s, err:%v", actionName, version, err)
		return err
	}
	version = GetActionVersion(c)

	doFunc := actionTypeRender[actionName]
	if doFunc == nil {
		return nil
	}

	if chooseActionData == nil {
		chooseActionData = &apistructs.PipelineYmlAction{
			Type:    actionName,
			Version: version,
		}
	}

	newCtx := context.WithValue(ctx, DataValueKey, chooseActionData)
	return doFunc(newCtx, c, scenario, event, globalStateData)
}

func getChooseActionData(bdl protocol.ContextBundle) *apistructs.PipelineYmlAction {
	if bdl.InParams == nil {
		return nil
	}
	if bdl.InParams["actionData"] == nil {
		return nil
	}
	actionDataJson, err := json.Marshal(bdl.InParams["actionData"])
	if err != nil {
		fmt.Printf("failed to marshal actionData:%+v, err:%v", bdl.InParams["actionData"], err)
		return nil
	}
	if len(actionDataJson) <= 0 {
		return nil
	}

	var action = &apistructs.PipelineYmlAction{}
	err = json.Unmarshal(actionDataJson, action)
	if err != nil {
		fmt.Printf("failed to Unmarshal actionData:%+v, err:%v", actionDataJson, err)
		return nil
	}

	return action
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}
