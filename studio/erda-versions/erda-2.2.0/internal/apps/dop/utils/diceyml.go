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

package utils

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

const (
	ActionType = "release"
)

func Check(data interface{}) error {
	// data type: string
	diceymlContent, ok := data.(string)
	if !ok {
		return fmt.Errorf("the dada type is not string")
	}

	// validate=false, 是否需要 validate 由 release-action precheker 实现
	d, err := diceyml.New([]byte(diceymlContent), false)
	if err != nil {
		return fmt.Errorf("failed to parse dice.yml without validate, err: %v", err)
	}
	// we can add d.Compose here
	_ = d
	return nil
}

// FetchRealDiceYml That may exist dice_development_yml,dice_test_yml,dice_staging_yml,dice_production_yml
func FetchRealDiceYml(bdl *bundle.Bundle, pipelineYml, gitRepo, branch string, workspace apistructs.DiceWorkspace, userID string) (string, error) {
	if pipelineYml == "" {
		return "", nil
	}

	y, err := pipelineyml.New([]byte(pipelineYml))
	if err != nil {
		return "", err
	}

	// fetch dice.yml
	diceYml, err := fetchDiceYml(bdl, gitRepo, branch, "dice.yml", userID)
	if err != nil {
		logrus.Errorf("failed to fetch dice.yml, err: %v", err)
	}

	var (
		worn error
		yml  = diceYml
	)
	y.Spec().LoopStagesActions(func(stage int, action *pipelineyml.Action) {
		if action.Type != ActionType {
			return
		}
		var realDiceYmlParam interface{}
		switch workspace {
		case "DEV":
			realDiceYmlParam = action.Params["dice_development_yml"]
		case "PROD":
			realDiceYmlParam = action.Params["dice_production_yml"]
		case "TEST":
			realDiceYmlParam = action.Params["dice_test_yml"]
		case "STAGING":
			realDiceYmlParam = action.Params["dice_staging_yml"]
		default:
			realDiceYmlParam = action.Params["dice_yml"]
		}

		if realDiceYmlParam == nil {
			return
		}

		var realDiceYmlStr, ok = realDiceYmlParam.(string)
		if !ok {
			return
		}

		realDiceYmlSplit := strings.Split(realDiceYmlStr, "/")
		var length = len(realDiceYmlSplit)
		if length < 1 {
			return
		}

		// fetch real dice yml
		realDiceYml, err := fetchDiceYml(bdl, gitRepo, branch, realDiceYmlSplit[length-1], userID)
		if err != nil {
			logrus.Errorf("failed to fetch workspace %v dice_yml, error: %v", workspace, err)
			return
		}

		var check = true
		if action.Params["check_diceyml"] != nil {
			check, err = strconv.ParseBool(action.Params["check_diceyml"].(string))
			if err != nil {
				check = true
			}
		}

		// compose diceYml and realDiceYml
		yml, err = composeEnvYml(diceYml, check, realDiceYml, workspace.String())
		if err != nil {
			logrus.Errorf("failed to composeEnvYml dice.yml error: %v", err)
			worn = err
			return
		}
	})

	return yml, worn
}

// fetchDiceYml fetch dice yml
func fetchDiceYml(bdl *bundle.Bundle, gittarURL, ref, diceYmlName, userID string) (string, error) {
	return bdl.GetGittarFile(gittarURL, ref, diceYmlName, "", "", userID)
}

func composeEnvYml(diceYaml string, check bool, otherYaml string, workspace string) (string, error) {
	d, err := diceyml.New([]byte(diceYaml), check)
	if err != nil {
		return "", errors.Wrap(err, "new parser failed")
	}

	switch workspace {
	case string(apistructs.DevWorkspace):
		err = composeYaml(d, "development", otherYaml)
	case string(apistructs.TestWorkspace):
		err = composeYaml(d, "test", otherYaml)
	case string(apistructs.StagingWorkspace):
		err = composeYaml(d, "staging", otherYaml)
	case string(apistructs.ProdWorkspace):
		err = composeYaml(d, "production", otherYaml)
	}
	if err != nil {
		return "", errors.Wrap(err, "failed to compose diceyml")
	}

	return d.YAML()
}

func composeYaml(targetYml *diceyml.DiceYaml, env, envYmlFile string) error {
	envYml, err := diceyml.New([]byte(envYmlFile), false)
	if err != nil {
		return err
	}

	err = targetYml.Compose(env, envYml)
	if err != nil {
		return err
	}

	return nil
}
