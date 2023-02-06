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

package precheck

import (
	"context"
	"encoding/json"
	"fmt"
	"regexp"
	"strings"
	"sync"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/pvolumes"
	"github.com/erda-project/erda/internal/tools/pipeline/precheck/checkers/actionchecker/api_register"
	"github.com/erda-project/erda/internal/tools/pipeline/precheck/checkers/actionchecker/buildpack"
	"github.com/erda-project/erda/internal/tools/pipeline/precheck/checkers/actionchecker/release"
	"github.com/erda-project/erda/internal/tools/pipeline/precheck/prechecktype"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

var hideFileRegexp, _ = regexp.Compile(`^\/(?:[^\/]+\/)*\.[^\/]*`)
var fileRegexp, _ = regexp.Compile(`^\/(\w+\/?)+$`)

// files: key: fileName, value: fileContent
// return: abort, message
func PreCheck(ctx context.Context, pipelineYmlByte []byte, itemForCheck prechecktype.ItemsForCheck) (globalAbort bool, showMessage apistructs.ShowMessage) {
	initialize()

	defer func() {
		if r := recover(); r != nil {
			showMessage.AbortRun = true
		}
		if !showMessage.AbortRun {
			showMessage.AbortRun = globalAbort
		}
		showMessage.Msg = "Pre Check Detected WARNINGS, please confirm risks before run!"
		if globalAbort {
			showMessage.Msg = "Pre Check Failed, please fix problems and create pipeline again."
		}
	}()

	// nil check
	if itemForCheck.Files == nil {
		itemForCheck.Files = make(map[string]string)
	}
	if itemForCheck.ActionSpecs == nil {
		itemForCheck.ActionSpecs = make(map[string]apistructs.ActionSpec)
	}

	// parse pipelineyml
	y, err := pipelineyml.New(pipelineYmlByte,
		pipelineyml.WithFlatParams(true), // params: map[string]string
		pipelineyml.WithEnvs(itemForCheck.Envs),
	)
	if err != nil {
		showMessage.Stacks = append(showMessage.Stacks, err.Error())
		return true, showMessage
	}

	// use ActionPreChecker
	availableRefs := pipelineyml.Refs{}
	availableOutputs := pipelineyml.Outputs{}
	for _, stage := range y.Spec().Stages {
		stageRefs := pipelineyml.Refs{}
		stageOutputs := pipelineyml.Outputs{}
		for _, actions := range stage.Actions {
			for _, action := range actions {
				if action.Disable {
					continue
				}
				// check refs / outputs / secrets
				precheckY, err := pipelineyml.New(pipelineYmlByte,
					pipelineyml.WithEnvs(itemForCheck.Envs),
					pipelineyml.WithAliasesToCheckRefOp(itemForCheck.GlobalSnippetConfigLabels, action.Alias),
					pipelineyml.WithRefs(availableRefs),
					pipelineyml.WithRefOpOutputs(availableOutputs),
					pipelineyml.WithAllowMissingCustomScriptOutputs(true),
					pipelineyml.WithSecrets(itemForCheck.Secrets),
				)
				if err != nil {
					globalAbort = true
					showMessage.Stacks = append(showMessage.Stacks, err.Error())
				}
				if precheckY != nil {
					for _, warn := range precheckY.Warns() {
						showMessage.Stacks = append(showMessage.Stacks, strutil.Concat("[WARN] ", warn))
					}
				}

				// check spec exist
				actionSpec := itemForCheck.ActionSpecs[action.GetActionTypeVersion()]

				// check required params
				checkResults := checkRequiredParams(*action, actionSpec)
				if len(checkResults) > 0 {
					showMessage.Stacks = append(showMessage.Stacks, checkResults...)
				}

				if errs := CheckCaches(action, itemForCheck.Labels); errs != nil && len(errs) > 0 {
					showMessage.Stacks = append(showMessage.Stacks, errs...)
					return true, showMessage
				}

				// type checker
				checker, ok := actionPreCheckerMap[action.Type]
				if ok {
					abort, warnings := checker.Check(ctx, *action, itemForCheck)
					if abort {
						globalAbort = true
					}
					var polishWarnings []string
					for _, warning := range warnings {
						polishWarnings = append(polishWarnings, fmt.Sprintf("taskName: %s, message: %s", action.Alias, warning))
					}
					showMessage.Stacks = append(showMessage.Stacks, polishWarnings...)
				}

				// append to stage available
				stageRefs[action.Alias.String()] = fmt.Sprintf("ref(%s)", action.Alias)
				for _, ns := range action.Namespaces {
					stageRefs[ns] = fmt.Sprintf("ref(%s)", ns)
				}
				stageOutputs[action.Alias] = make(map[string]string)
				for _, output := range actionSpec.Outputs {
					key := output.Name
					stageOutputs[action.Alias][key] = fmt.Sprintf("value(%s)", key)
				}
				setActionDynamicOutput(action, stageOutputs)
			}
		}
		// add stage refs/outputs to available for next stage
		for alias, ref := range stageRefs {
			availableRefs[alias] = ref
		}
		for alias, kv := range stageOutputs {
			availableOutputs[alias] = kv
		}
	}

	return globalAbort, showMessage
}

var diceymlPreCheckers []prechecktype.DiceYmlPreChecker
var actionPreCheckerMap map[pipelineyml.ActionType]prechecktype.ActionPreChecker

var initOnce = &sync.Once{}

func initialize() {
	initOnce.Do(func() {
		// action prechecker map
		actionPreCheckers := []prechecktype.ActionPreChecker{
			buildpack.New(),
			release.New(),
			api_register.New(),
		}
		actionPreCheckerMap = make(map[pipelineyml.ActionType]prechecktype.ActionPreChecker)
		for _, checker := range actionPreCheckers {
			actionPreCheckerMap[checker.ActionType()] = checker
		}
	})
}

func CheckCaches(actualAction *pipelineyml.Action, labels map[string]string) []string {
	var checkResults []string

	caches := actualAction.Caches

	if caches == nil {
		return nil
	}

	if labels == nil {
		checkResults = append(checkResults, fmt.Sprintf("checkCaches: secrets is empty"))
		return checkResults
	}

	projectID := labels[apistructs.LabelProjectID]
	appID := labels[apistructs.LabelAppID]

	for _, v := range caches {

		isHideFile := hideFileRegexp.MatchString(v.Path)
		isFile := fileRegexp.MatchString(v.Path)
		isActionAddr := strings.HasPrefix(v.Path, "${")

		if !isFile && !isHideFile && !isActionAddr {
			checkResults = append(checkResults, fmt.Sprintf("taskName: %s, cache path: %s error: %s ",
				actualAction.Alias, v.Path, " just support / begin or action addr ${actionName}, not support ~/ and relative path"))
		}

		if projectID == "" && appID == "" && v.Key == "" {
			checkResults = append(checkResults, fmt.Sprintf("taskName: %s, cache path: %s error: %s ",
				actualAction.Alias, v.Path, "if projectID is empty and appID is empty, key can not be empty"))
			continue
		}

		if v.Key != "" {

			v.Key = strings.ReplaceAll(v.Key, " ", "")

			if !strings.HasPrefix(v.Key, pvolumes.TaskCachePathBasePath) {
				checkResults = append(checkResults, fmt.Sprintf("taskName: %s, cache key: %s error: %s ",
					actualAction.Alias, v.Key, "Key should be start with "+pvolumes.TaskCachePathBasePath))
			}

			if !strings.HasSuffix(v.Key, pvolumes.TaskCachePathEndPath) {
				checkResults = append(checkResults, fmt.Sprintf("taskName: %s, cache key: %s error: %s ",
					actualAction.Alias, v.Key, "Key should be end with "+pvolumes.TaskCachePathEndPath))
			}
		}

	}

	return checkResults
}

func checkRequiredParams(actualAction pipelineyml.Action, actionSpec apistructs.ActionSpec) []string {
	checkResults := make([]string, 0)

	requiredSpecParams := make(map[string]apistructs.ActionSpecParam)
	for i := range actionSpec.Params {
		paramSpec := actionSpec.Params[i]
		if paramSpec.Required {
			requiredSpecParams[paramSpec.Name] = paramSpec
		}
	}
	// travel actualAction.Params
	for actualParam := range actualAction.Params {
		delete(requiredSpecParams, actualParam)
	}
	for _, param := range requiredSpecParams {
		checkResults = append(checkResults, fmt.Sprintf("taskName: %s, message: missing required param: %s",
			actualAction.Alias, param.Name))
	}

	return checkResults
}

func setActionDynamicOutput(action *pipelineyml.Action, stageOutputs pipelineyml.Outputs) {

	switch action.Type {
	case "api-test":
		params := action.Params
		if params == nil {
			return
		}

		outParams := params["out_params"]
		if outParams == nil {
			return
		}

		var outs []apistructs.APIOutParam
		err := json.Unmarshal([]byte(outParams.(string)), &outs)
		if err != nil {
			logrus.Errorf("unmarshal api-test out_params error: %v", err)
			return
		}
		if outs == nil {
			return
		}

		for _, v := range outs {
			stageOutputs[action.Alias][v.Key] = fmt.Sprintf("value(%s)", v.Key)
		}
	}
}
