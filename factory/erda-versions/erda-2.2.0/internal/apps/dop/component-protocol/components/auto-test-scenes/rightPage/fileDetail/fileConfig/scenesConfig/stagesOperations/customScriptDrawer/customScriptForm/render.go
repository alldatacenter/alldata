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

package customScriptForm

import (
	"context"
	"encoding/json"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

const (
	languagePython = "python"
	languageCustom = "custom"
	pythonImageKey = "pythonImage"
	customImageKey = "customImage"
)

const (
	defaultPythonImage       = "registry.erda.cloud/erda-actions/cdp-python:20210913-9f7576f9ef09e8bed32e2da4298f077328e532ea"
	defaultCustomScriptImage = "registry.erda.cloud/erda-actions/custom-script-action:20210519-01d2811"
)

type ComponentAction struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle
}

type Data struct {
	Language    string   `json:"language"`
	Name        string   `json:"name"`
	PythonImage string   `json:"pythonImage"`
	CustomImage string   `json:"customImage"`
	Commands    []string `json:"commands"`
	Command     string   `json:"command"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "customScriptForm",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	ca.sdk = cputil.SDK(ctx)
	v, err := json.Marshal(c.State["stepId"])
	if err != nil {
		return err
	}
	var stepID int
	err = json.Unmarshal(v, &stepID)
	if err != nil {
		return nil
	}
	if stepID <= 0 {
		return nil
	}
	switch event.Operation {
	case "submit":
		formDataJson, err := json.Marshal(c.State["formData"])
		if err != nil {
			return err
		}
		data := Data{}
		if err := json.Unmarshal(formDataJson, &data); err != nil {
			return err
		}
		if data.Language != languagePython && data.Language != languageCustom {
			return errors.Errorf("Invalid language: %s", data.Language)
		}
		var value apistructs.AutoTestRunCustom
		value.LanguageType = data.Language
		value.Image = data.CustomImage
		value.Commands = data.Commands
		if value.LanguageType == languagePython {
			value.Commands = []string{data.Command}
			value.Image = data.PythonImage
		}
		valueByte, err := json.Marshal(value)
		if err != nil {
			return err
		}
		var req apistructs.AutotestSceneRequest
		req.Name = data.Name
		req.ID = uint64(stepID)
		req.Value = string(valueByte)
		req.UserID = ca.sdk.Identity.UserID
		_, err = ca.bdl.UpdateAutoTestSceneStep(req)
		if err != nil {
			return err
		}
		c.State["drawVisible"] = false
	case "cancel":
		c.State["drawVisible"] = false
	case cptype.InitializeOperation, cptype.RenderingOperation:
		req := apistructs.AutotestGetSceneStepReq{
			ID:     uint64(stepID),
			UserID: ca.sdk.Identity.UserID,
		}
		step, err := ca.bdl.GetAutoTestSceneStep(req)
		if err != nil {
			return err
		}
		var (
			pythonImage string
			customImage string
			commands    []string
			command     string
			language    string
		)
		if step.Value == "" {
			pythonImage = defaultPythonImage
			customImage = defaultCustomScriptImage
			commands = []string{}
			command = ""
			language = languagePython
		} else {
			var value apistructs.AutoTestRunCustom
			if err := json.Unmarshal([]byte(step.Value), &value); err != nil {
				return err
			}
			language = value.LanguageType
			switch language {
			case languagePython:
				pythonImage = value.Image
				command = strings.Join(value.Commands, "\n")
			case languageCustom:
				customImage = value.Image
				commands = value.Commands
			default:
				pythonImage = defaultPythonImage
				customImage = defaultCustomScriptImage
				commands = []string{}
			}
		}
		c.State["formData"] = Data{
			Language:    language,
			Name:        step.Name,
			PythonImage: pythonImage,
			CustomImage: customImage,
			Commands:    commands,
			Command:     command,
		}

		c.State["drawVisible"] = true
		c.Props = map[string]interface{}{
			"fields": []map[string]interface{}{
				{
					"label":          ca.sdk.I18n("name"),
					"component":      "input",
					"required":       true,
					"key":            "name",
					"componentProps": map[string]interface{}{"placeholder": ca.sdk.I18n("enterName")},
				},
				{
					"label":     ca.sdk.I18n("scriptLang"),
					"component": "radio",
					"required":  true,
					"key":       "language",
					"componentProps": map[string]interface{}{
						"radioType": "button",
						"options": []map[string]interface{}{
							{
								"name":  languagePython,
								"value": languagePython,
							},
							{
								"name":  languageCustom,
								"value": languageCustom,
							},
						},
					},
					"defaultValue": languagePython,
				},
				{
					"label":     ca.sdk.I18n("image"),
					"component": "input",
					"required":  true,
					"key":       pythonImageKey,
					"removeWhen": []interface{}{
						[]map[string]interface{}{
							{
								"field":    "language",
								"operator": "!=",
								"value":    languagePython,
							},
						},
					},
					"defaultValue": defaultPythonImage,
				},
				{
					"label":     ca.sdk.I18n("image"),
					"component": "input",
					"required":  true,
					"key":       customImageKey,
					"removeWhen": []interface{}{
						[]map[string]interface{}{
							{
								"field":    "language",
								"operator": "!=",
								"value":    languageCustom,
							},
						},
					},
					"defaultValue": defaultCustomScriptImage,
				},
				{
					"label":     ca.sdk.I18n("command"),
					"component": "textarea",
					"key":       "command",
					"componentProps": map[string]interface{}{
						"placeholder": ca.sdk.I18n("enterCommand"),
						"autoSize": map[string]interface{}{
							"minRows": 3,
							"maxRows": 6,
						},
					},
					"removeWhen": []interface{}{
						[]map[string]interface{}{
							{
								"field":    "language",
								"operator": "!=",
								"value":    languagePython,
							},
						},
					},
				},
				{
					"label":     ca.sdk.I18n("command"),
					"component": "inputArray",
					"key":       "commands",
					"removeWhen": []interface{}{
						[]map[string]interface{}{
							{
								"field":    "language",
								"operator": "!=",
								"value":    languageCustom,
							},
						},
					},
				},
			},
		}
		c.Operations = map[string]interface{}{
			"submit": map[string]interface{}{
				"key":    "submit",
				"reload": true,
			},
			"cancel": map[string]interface{}{
				"reload": true,
				"key":    "cancel",
			},
		}
	}
	return nil
}
