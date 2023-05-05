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

package pipelineymlv1

import (
	"fmt"
	"strings"

	"github.com/pkg/errors"
)

func (y *PipelineYml) InsertDiceHub() error {
	const (
		RES_TYPE_RELEASE = "release"
	)
	//Check if exit dicehub task, if exit, no need insert.
	var (
		isDiceHub    = false
		diceHubIndex = -1
		diceIndex    = -1
		abilityIndex = -1
	)
	for i, res := range y.obj.Resources {
		if strings.HasPrefix(res.Type, string(RES_TYPE_DICEHUB)) {
			isDiceHub = true
			diceHubIndex = i
		}
		if strings.HasPrefix(res.Type, string(RES_TYPE_DICE)) {
			diceIndex = i
		}
		if strings.HasPrefix(res.Type, string(RES_TYPE_ABILITY)) {
			abilityIndex = i
		}
	}

	//Insert dicehub_release file path to deploy.
	if isDiceHub {
		if diceIndex != -1 {
			if y.obj.Resources[diceIndex].Source == nil {
				y.obj.Resources[diceIndex].Source = make(map[string]interface{})
			}
			y.obj.Resources[diceIndex].Source["release_id_path"] = y.obj.Resources[diceHubIndex].Name
		}

		if abilityIndex != -1 {
			if y.obj.Resources[abilityIndex].Source == nil {
				y.obj.Resources[abilityIndex].Source = make(map[string]interface{})
			}
			y.obj.Resources[abilityIndex].Source["release_id_path"] = y.obj.Resources[diceHubIndex].Name
		}

		return nil
	} else {
		if diceIndex != -1 {
			if y.obj.Resources[diceIndex].Source == nil {
				y.obj.Resources[diceIndex].Source = make(map[string]interface{})
			}
			y.obj.Resources[diceIndex].Source["release_id_path"] = RES_TYPE_RELEASE
		}

		if abilityIndex != -1 {
			if y.obj.Resources[abilityIndex].Source == nil {
				y.obj.Resources[abilityIndex].Source = make(map[string]interface{})
			}
			y.obj.Resources[abilityIndex].Source["release_id_path"] = RES_TYPE_RELEASE
		}
	}

	//Not exit dicehub task, Inster it.
	var (
		images     []string
		repo       string
		configs    []TaskConfig
		params     = map[string]interface{}{}
		taskConfig = map[string]interface{}{}
	)

	//Compose pack images result.
	for _, res := range y.obj.Resources {
		if strings.HasPrefix(res.Type, string(RES_TYPE_BP_IMAGE)) || strings.HasPrefix(res.Type, string(RES_TYPE_BUILDPACK)) {
			images = append(images, fmt.Sprint(res.Name, "/pack-result"))
		}
		if strings.HasPrefix(res.Type, string(RES_TYPE_GIT)) {
			repo = res.Name
		}
	}

	params["dice_yml"] = fmt.Sprint(repo, "/dice.yml")
	params["dice_test_yml"] = fmt.Sprint(repo, "/dice_test.yml")
	params["dice_development_yml"] = fmt.Sprint(repo, "/dice_development.yml")
	params["dice_staging_yml"] = fmt.Sprint(repo, "/dice_staging.yml")
	params["dice_production_yml"] = fmt.Sprint(repo, "/dice_production.yml")
	params["replacement_images"] = images

	taskConfig["params"] = params
	taskConfig["put"] = RES_TYPE_RELEASE

	configs = append(configs, taskConfig)

	stage := &Stage{
		Name:        string(RES_TYPE_DICEHUB),
		TaskConfigs: configs,
	}

	//Find insert position.
	var pos = -1
	for i, stage := range y.obj.Stages {
		for _, task := range stage.Tasks {
			resType := task.GetResourceType()
			if strings.HasPrefix(resType, string(RES_TYPE_BUILDPACK)) || strings.HasPrefix(resType, string(RES_TYPE_BP_IMAGE)) {
				pos = i
			}
		}
	}

	if pos == -1 {
		return nil
	}

	err := InsertStage(&y.obj.Stages, pos+1, stage)
	if err != nil {
		return errors.Wrapf(err, "insertDiceHub error")
	}

	//Compose resource.
	res := Resource{
		Name: RES_TYPE_RELEASE,
		Type: string(RES_TYPE_DICEHUB),
	}

	y.obj.Resources = append(y.obj.Resources, res)

	yml, err := y.YAML()
	if err != nil {
		return err
	}
	y.byteData = []byte(yml)

	return nil
}

func InsertStage(s *[]*Stage, index int, data *Stage) error {
	if index == 0 {
		return errors.New("Index is zero.")
	} else {
		slice := *s
		ln := len(slice)
		if index == ln {
			*s = append(slice, data)
		} else {
			if index < 0 || index > ln {
				return errors.New("insert stage: index out of range")
			}
			cp := cap(slice)
			total := ln + 1
			if total > cp {
				slice = make([]*Stage, total)
				copy(slice, *s)
			} else {
				slice = slice[0:total:cp]
			}

			copy(slice[index+1:], slice[index:])
			slice[index] = data
			*s = slice
		}
	}

	return nil
}
