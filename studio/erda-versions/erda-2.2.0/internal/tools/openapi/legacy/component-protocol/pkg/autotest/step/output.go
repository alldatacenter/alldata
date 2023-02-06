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

package step

import (
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/expression"
)

// APISpec step.value 的json解析
type APISpec struct {
	APIInfo apistructs.APIInfoV2         `json:"apiSpec"`
	Loop    *apistructs.PipelineTaskLoop `json:"loop"`
}

func GetStepAllOutput(steps []apistructs.AutoTestSceneStep, bdl *bundle.Bundle, gs *cptype.GlobalStateData) (map[string]map[string]string, error) {
	var outputs = map[string]map[string]string{}
	apiOutput, err := buildStepOutPut(steps)
	if err != nil {
		return nil, err
	}

	configSheetOutput, err := buildConfigSheetStepOutPut(steps, bdl, gs)
	if err != nil {
		return nil, err
	}

	for stepID, stepOutput := range apiOutput {
		if outputs[stepID] == nil {
			outputs[stepID] = make(map[string]string, 0)
		}

		for key, express := range stepOutput {
			outputs[stepID][key] = express
		}
	}

	for stepID, stepOutput := range configSheetOutput {
		if outputs[stepID] == nil {
			outputs[stepID] = make(map[string]string, 0)
		}

		for key, express := range stepOutput {
			outputs[stepID][key] = express
		}
	}

	return outputs, nil
}

func buildStepOutPut(steps []apistructs.AutoTestSceneStep) (map[string]map[string]string, error) {
	outputs := make(map[string]map[string]string, 0)
	for _, step := range steps {
		var value APISpec
		if step.Type == apistructs.StepTypeAPI {
			if step.Value == "" {
				step.Value = "{}"
			}
			err := json.Unmarshal([]byte(step.Value), &value)
			if err != nil {
				return nil, err
			}
			if len(value.APIInfo.OutParams) == 0 {
				continue
			}

			stepIDStr := strconv.Itoa(int(step.ID))

			if outputs[stepIDStr] == nil {
				outputs[stepIDStr] = make(map[string]string, 0)
			}
			for _, v := range value.APIInfo.OutParams {
				outputs[stepIDStr][v.Key] = expression.GenOutputRef(stepIDStr, v.Key)
			}
		}
	}
	return outputs, nil
}

// GetStepOutPut get output parameter by autotest steps
func GetStepOutPut(steps []apistructs.AutoTestSceneStep) (map[string]map[string]string, error) {
	outputs := make(map[string]map[string]string, 0)
	for _, step := range steps {
		var value APISpec
		if step.Type == apistructs.StepTypeAPI {
			if step.Value == "" {
				step.Value = "{}"
			}
			err := json.Unmarshal([]byte(step.Value), &value)
			if err != nil {
				return nil, err
			}
			if len(value.APIInfo.OutParams) == 0 {
				continue
			}

			stepIDStr := strconv.Itoa(int(step.ID))
			stepKey := MakeStepSelectKey(stepIDStr, step.Name)

			if outputs[stepKey] == nil {
				outputs[stepKey] = make(map[string]string, 0)
			}
			for _, v := range value.APIInfo.OutParams {
				outputs[stepKey][v.Key] = expression.GenOutputRef(stepIDStr, v.Key)
			}
		}
	}
	return outputs, nil
}

func MakeStepSelectKey(stepID string, stepName string) string {
	return "#" + stepID + "-" + stepName
}

func MakeStepOutputSelectKey(stepID string, stepName string, key string) string {
	return "#" + stepID + "-" + stepName + ":" + key
}

func buildConfigSheetStepOutPut(steps []apistructs.AutoTestSceneStep, bdl *bundle.Bundle, gs *cptype.GlobalStateData) (map[string]map[string]string, error) {

	outputs := make(map[string]map[string]string, 0)

	var snippetConfigs []apistructs.SnippetDetailQuery
	for _, step := range steps {
		if step.Type != apistructs.StepTypeConfigSheet {
			continue
		}
		if step.Value == "" {
			step.Value = "{}"
		}

		var value apistructs.AutoTestRunConfigSheet
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return nil, err
		}

		if value.ConfigSheetID == "" {
			continue
		}

		stepIDStr := strconv.FormatUint(step.ID, 10)

		snippetConfigs = append(snippetConfigs, apistructs.SnippetDetailQuery{
			Alias: stepIDStr,
			SnippetConfig: apistructs.SnippetConfig{
				Name:   value.ConfigSheetID,
				Source: apistructs.PipelineSourceAutoTest.String(),
				Labels: map[string]string{
					apistructs.LabelSnippetScope: apistructs.FileTreeScopeAutoTestConfigSheet,
				},
			},
		})
	}

	if len(snippetConfigs) <= 0 {
		return outputs, nil
	}

	var req apistructs.SnippetQueryDetailsRequest
	req.SnippetConfigs = snippetConfigs
	detail, err := bdl.GetPipelineActionParamsAndOutputs(req)
	if err != nil {
		(*gs)[protocol.GlobalInnerKeyError.String()] = fmt.Sprintf("failed to query step outputs, please check config sheets")
	}

	for alias, detail := range detail {

		if outputs[alias] == nil {
			outputs[alias] = make(map[string]string, 0)
		}

		for _, output := range detail.Outputs {
			key, ok := expression.DecodeOutputKey(output)
			if !ok {
				continue
			}
			outputs[alias][key] = output
		}
	}

	return outputs, nil
}

func GetConfigSheetStepOutPut(steps []apistructs.AutoTestSceneStep, bdl *bundle.Bundle, gs *cptype.GlobalStateData) (map[string]map[string]string, error) {

	outputs := make(map[string]map[string]string, 0)

	var snippetConfigs []apistructs.SnippetDetailQuery
	var stepIDWithName = map[string]string{}
	for _, step := range steps {
		if step.Type != apistructs.StepTypeConfigSheet {
			continue
		}

		if step.Value == "" {
			step.Value = "{}"
		}

		var value apistructs.AutoTestRunConfigSheet
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return nil, err
		}

		if value.ConfigSheetID == "" {
			continue
		}

		stepIDStr := strconv.FormatUint(step.ID, 10)

		snippetConfigs = append(snippetConfigs, apistructs.SnippetDetailQuery{
			Alias: stepIDStr,
			SnippetConfig: apistructs.SnippetConfig{
				Name:   value.ConfigSheetID,
				Source: apistructs.PipelineSourceAutoTest.String(),
				Labels: map[string]string{
					apistructs.LabelSnippetScope: apistructs.FileTreeScopeAutoTestConfigSheet,
				},
			},
		})

		stepIDWithName[stepIDStr] = step.Name
	}

	if len(snippetConfigs) <= 0 {
		return outputs, nil
	}

	var req apistructs.SnippetQueryDetailsRequest
	req.SnippetConfigs = snippetConfigs
	detail, err := bdl.GetPipelineActionParamsAndOutputs(req)
	if err != nil {
		(*gs)[protocol.GlobalInnerKeyError.String()] = fmt.Sprintf("failed to query step outputs, err: %v", err)
	}

	for alias, detail := range detail {
		stepName := stepIDWithName[alias]
		selectKey := MakeStepSelectKey(alias, stepName)

		if outputs[selectKey] == nil {
			outputs[selectKey] = make(map[string]string, 0)
		}

		for _, output := range detail.Outputs {
			key, ok := expression.DecodeOutputKey(output)
			if !ok {
				continue
			}
			outputs[selectKey][key] = output
		}
	}

	return outputs, nil
}
