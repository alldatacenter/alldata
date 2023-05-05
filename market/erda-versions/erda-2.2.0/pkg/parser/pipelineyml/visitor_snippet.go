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

package pipelineyml

import (
	"fmt"
	"reflect"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/flosch/pongo2.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/expression"
)

const (
	Snippet            = "snippet"
	SnippetLogo        = "http://terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/22/410935c6-e399-463a-b87b-0b774240d12e.png"
	SnippetDisplayName = "嵌套流水线"
	SnippetDesc        = "嵌套流水线可以声明嵌套的其他 pipeline.yml"

	SnippetActionNameLinkAddr = "_"
)

// HandleSnippetConfigLabel polish snippet config label
func HandleSnippetConfigLabel(snippetConfig *SnippetConfig, globalSnippetConfigLabels map[string]string) SnippetConfig {

	if snippetConfig.Labels == nil {
		snippetConfig.Labels = map[string]string{}
	}

	if globalSnippetConfigLabels != nil {
		for k, v := range globalSnippetConfigLabels {
			snippetConfig.Labels[k] = v
		}
	}

	// dice
	scopeID := snippetConfig.Labels[apistructs.LabelDiceSnippetScopeID]
	if scopeID == "" {
		scopeID = "0"
	}

	version := snippetConfig.Labels[apistructs.LabelChooseSnippetVersion]
	if version == "" {
		version = "latest"
	}

	return *snippetConfig
}

// GetParamDefaultValue get default param value by type
func GetParamDefaultValue(paramType string) interface{} {
	switch paramType {
	case apistructs.PipelineParamStringType, apistructs.PipelineParamIntType:
		return ""
	case apistructs.PipelineParamBoolType:
		return "false"
	}
	return ""
}

func DoRenderTemplateNotReplaceParamsValue(params map[string]interface{}, templateAction *apistructs.PipelineTemplateSpec, alias string, templateVersion apistructs.TemplateVersion) (string, []apistructs.SnippetFormatOutputs, error) {
	return DoRenderTemplateHandler(params, templateAction, alias, templateVersion, func(snippet string, params map[string]interface{}) (string, error) {
		return snippet, nil
	})
}

func DoRenderTemplateWithFormat(params map[string]interface{}, templateAction *apistructs.PipelineTemplateSpec, alias string, templateVersion apistructs.TemplateVersion) (string, []apistructs.SnippetFormatOutputs, error) {
	return DoRenderTemplateHandler(params, templateAction, alias, templateVersion, doFormatAndReplaceValue)
}

func DoRenderTemplateHandler(params map[string]interface{}, templateAction *apistructs.PipelineTemplateSpec,
	alias string, templateVersion apistructs.TemplateVersion,
	handler func(snippet string, params map[string]interface{}) (string, error)) (string, []apistructs.SnippetFormatOutputs, error) {

	if err := setDefaultValue(templateAction, params); err != nil {
		return "", nil, err
	}

	if err := checkParams(templateAction, params); err != nil {
		return "", nil, err
	}

	snippet, err := handler(templateAction.Template, params)
	if err != nil {
		return "", nil, err
	}
	templateAction.Template = snippet

	if templateVersion == apistructs.TemplateVersionV2 {
		pipelineYml, err := New([]byte(templateAction.Template), WithFlatParams(true))
		if err != nil {
			return "", nil, errors.New(fmt.Sprintf("doRenderTemplate new template error: error %v", err))
		}

		pipelineYmlStr, aliasMap, err := replaceActionName(pipelineYml, alias)
		if err != nil {
			return "", nil, err
		}

		template := replaceOutput(pipelineYmlStr, aliasMap)

		outputs, err := getTemplateOutputs(templateAction, alias)
		if err != nil {
			return "", nil, err
		}

		return template, outputs, nil
	}

	return templateAction.Template, nil, nil
}

func doFormatAndReplaceValue(snippet string, params map[string]interface{}) (string, error) {
	//根据formats中的模板进行逐一渲染
	tpl, err := pongo2.FromString(snippet)
	if err != nil {
		logrus.Errorf("template %s, format error %v", snippet, err)
		return "", err
	}

	out, err := tpl.Execute(params)
	if err != nil {
		logrus.Errorf("template %s, params %v,  to render error %v", params, snippet, err)
		return "", err
	}
	out = ReplacePipelineParams(out, params)
	snippet = out
	return snippet, nil
}

func setDefaultValue(specYaml *apistructs.PipelineTemplateSpec, params map[string]interface{}) error {
	paramsList := specYaml.Params
	for _, v := range paramsList {
		runValue, ok := params[v.Name]
		if runValue == nil && v.Default == nil && v.Required && ok {
			return fmt.Errorf("params %s is required", v.Name)
		}

		if runValue == nil && v.Default != nil {
			params[v.Name] = v.Default
		}

		if runValue == nil && v.Default == nil {
			params[v.Name] = GetParamDefaultValue(v.Type)
		}

		if params[v.Name] == nil {
			return fmt.Errorf("params %s is required", v.Name)
		}
	}
	return nil
}

func getTemplateOutputs(action *apistructs.PipelineTemplateSpec, alias string) ([]apistructs.SnippetFormatOutputs, error) {

	outputs := action.Outputs
	var result []apistructs.SnippetFormatOutputs
	for _, v := range outputs {
		if v.Ref == "" {
			return nil, errors.New(fmt.Sprintf(" error to format snippet output, output %s ref is empty", v.Name))
		}

		// ${xxx:OUTPUT:xxx} -> ${xxx_xxx:OUTPUT:xxx}
		matchValues := expression.OldRe.FindStringSubmatch(v.Ref)
		if len(matchValues) > 1 {
			afterOutputMame := alias + SnippetActionNameLinkAddr + matchValues[1]
			formatOutput := apistructs.SnippetFormatOutputs{
				PreOutputName:   fmt.Sprintf("%s%s:%s:%s%s", expression.OldLeftPlaceholder, alias, RefOpOutput, v.Name, expression.OldRightPlaceholder),
				AfterOutputName: fmt.Sprintf("%s%s%s", expression.OldLeftPlaceholder, afterOutputMame, expression.OldRightPlaceholder),
			}
			result = append(result, formatOutput)
		}

		// ${{ outputs.xxx.key }} -> ${{ outputs.xxx_xxx.key }}
		matchValues = expression.Re.FindStringSubmatch(v.Ref)
		if len(matchValues) > 1 {
			splitNames := strings.Split(matchValues[1], ".")
			if splitNames == nil || len(splitNames) != 3 || strings.HasPrefix(splitNames[0], expression.LeftPlaceholder) {
				continue
			}

			if splitNames[0] != expression.Outputs {
				return nil, fmt.Errorf("can not parsing this output ref %s", v.Ref)
			}

			newFormatOutput := apistructs.SnippetFormatOutputs{
				PreOutputName: fmt.Sprintf("%s %s.%s.%s %s", expression.LeftPlaceholder, expression.Outputs, alias, v.Name, expression.RightPlaceholder),
				AfterOutputName: fmt.Sprintf("%s %s.%s.%s %s", expression.LeftPlaceholder, expression.Outputs,
					alias+SnippetActionNameLinkAddr+splitNames[1], splitNames[2], expression.RightPlaceholder),
			}
			result = append(result, newFormatOutput)
		}
	}
	return result, nil
}

func checkParams(specYaml *apistructs.PipelineTemplateSpec, params map[string]interface{}) error {
	paramsList := specYaml.Params
	for _, v := range paramsList {
		if v.Required == false {
			continue
		}

		if params[v.Name] == nil {
			return errors.New(fmt.Sprintf(" param %s need value ", v.Name))
		}

	}

	for name, v := range params {
		typ := reflect.TypeOf(v)
		if typ == nil {
			return errors.New(fmt.Sprintf(" param %s value type error ", name))
		}
		switch typ.Kind() {
		case reflect.String, reflect.Int, reflect.Float32, reflect.Float64, reflect.Bool:
			continue
		default:
			return errors.New(fmt.Sprintf(" param %s value type %v not support ", name, typ.Kind()))
		}
	}

	return nil
}

func replaceOutput(pipelineYamlStr string, aliasMap map[string]string) string {
	// template-output = template
	for aliasBefore, aliasAfter := range aliasMap {
		// ${xxx} ${xxx:output -> ${xxx_xxx} ${xxx_xxx:output
		pipelineYamlStr = strings.ReplaceAll(pipelineYamlStr, fmt.Sprintf("%s%s%s", expression.OldLeftPlaceholder, aliasBefore, expression.OldRightPlaceholder),
			fmt.Sprintf("%s%s%s", expression.OldLeftPlaceholder, aliasAfter, expression.OldRightPlaceholder))
		pipelineYamlStr = strings.ReplaceAll(pipelineYamlStr, fmt.Sprintf("%s%s:%s", expression.OldLeftPlaceholder, aliasBefore, RefOpOutput),
			fmt.Sprintf("%s%s:%s", expression.OldLeftPlaceholder, aliasAfter, RefOpOutput))

		// ${{ dirs.xxx }} -> ${{ dirs.xxx_xxx }}
		// ${{ outputs.xxx -> ${{ outputs.xxx_xxx
		pipelineYamlStr = strings.ReplaceAll(pipelineYamlStr, fmt.Sprintf("%s %s.%s %s", expression.LeftPlaceholder, expression.Dirs, aliasBefore, expression.RightPlaceholder),
			fmt.Sprintf("%s %s.%s %s", expression.LeftPlaceholder, expression.Dirs, aliasAfter, expression.RightPlaceholder))

		pipelineYamlStr = strings.ReplaceAll(pipelineYamlStr, fmt.Sprintf("%s %s.%s", expression.LeftPlaceholder, expression.Outputs, aliasBefore),
			fmt.Sprintf("%s %s.%s", expression.LeftPlaceholder, expression.Outputs, aliasAfter))

	}
	return pipelineYamlStr
}

func replaceActionName(pipelineYml *PipelineYml, alias string) (string, map[string]string, error) {
	aliasMap := make(map[string]string)
	for si := range pipelineYml.Spec().Stages {
		stage := pipelineYml.Spec().Stages[si]

		for typekey := range stage.Actions {
			typedAction := stage.Actions[typekey]

			for actionType := range typedAction {
				action := typedAction[actionType]

				if action.Alias == "" {
					action.Alias = ActionAlias(actionType.String())
				}

				newAlias := alias + SnippetActionNameLinkAddr + action.Alias.String()
				aliasMap[action.Alias.String()] = newAlias
				action.Alias = ActionAlias(newAlias)
			}
		}
	}

	pipelineYaml, err := GenerateYml(pipelineYml.Spec())
	if err != nil {
		return "", nil, err
	}

	return string(pipelineYaml), aliasMap, nil
}
