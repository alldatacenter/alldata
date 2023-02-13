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

package cmd

import (
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"path/filepath"

	"github.com/ghodss/yaml"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
)

var EXTENSIONSRETAG = command.Command{
	Name:       "retag",
	ParentName: "EXT",
	ShortHelp:  "generate retag script",
	Example:    "$ erda-cli ext retag -d extensions -r registry.default.svc.cluster.local:5000 -o retag.sh",
	Flags: []command.Flag{
		command.StringFlag{Short: "d", Name: "dir", Doc: "the extension dir", DefaultValue: "."},
		command.StringFlag{Short: "r", Name: "registry", Doc: "the new registry", DefaultValue: "registry.default.svc.cluster.local:5000"},
		command.StringFlag{Short: "o", Name: "output", Doc: "the output script file", DefaultValue: "retag.sh"},
	},
	Run: RunExtensionsReTag,
}

type ExtensionInfo struct {
	Path    string
	Spec    apistructs.Spec
	Dice    map[string]interface{}
	DiceErr error
	SpecErr error
}

func RunExtensionsReTag(ctx *command.Context, dir string, registry string, output string) error {
	outTxt := ""
	if checkPath(dir) != nil {
		ctx.Fail(fmt.Sprintf("path not exist: %s \n", dir))
		return nil
	}
	extMetas := GetExtMetas(dir)
	for _, extMeta := range extMetas {
		if extMeta.DiceErr == nil && extMeta.SpecErr == nil {
			if extMeta.Spec.Type == "action" {
				jobs, ok := extMeta.Dice["jobs"].(map[string]interface{})
				if !ok {
					continue
				}
				for _, job := range jobs {
					cfg := job.(map[string]interface{})
					originalImage := cfg["image"].(string)
					newImage := newDockerImage(originalImage, registry)
					outTxt += generateReTagCmd(originalImage, newImage)
				}
			} else if extMeta.Spec.Type == "addon" {
				services, ok := extMeta.Dice["services"].(map[string]interface{})
				if !ok {
					continue
				}
				for _, v := range services {
					cfg := v.(map[string]interface{})
					_, exist := cfg["image"]
					if !exist {
						continue
					}
					originalImage := cfg["image"].(string)
					newImage := newDockerImage(originalImage, registry)
					outTxt += generateReTagCmd(originalImage, newImage)
				}
			}
		}
	}
	err := ioutil.WriteFile(output, []byte(outTxt), os.ModePerm)
	if err != nil {
		return err
	}
	ctx.Succ(fmt.Sprintf("script %s generate success\n", output))
	return nil
}

func checkPath(path string) error {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return err
	}
	return nil
}

func generateReTagCmd(oldImage, newImage string) string {
	return fmt.Sprintf("docker pull %s\n", oldImage) +
		fmt.Sprintf("docker tag %s %s\n", oldImage, newImage) +
		fmt.Sprintf("docker push %s\n", newImage)
}

func GetExtMetas(dir string) []ExtensionInfo {
	result := []ExtensionInfo{}
	filepath.Walk(dir, func(relPath string, info os.FileInfo, err error) error {
		if info.IsDir() {
			specFile := path.Join(relPath, "spec.yml")
			diceFile := path.Join(relPath, "dice.yml")
			ext := ExtensionInfo{
				Path: relPath,
			}
			if checkPath(specFile) == nil {
				specBytes, err := ioutil.ReadFile(specFile)
				if err != nil {
					ext.SpecErr = err
				}
				spec := apistructs.Spec{}
				err = yaml.Unmarshal(specBytes, &spec)
				if err != nil {
					ext.SpecErr = errors.Wrap(err, "failed to parse spec "+specFile)
				}
				ext.Spec = spec

				if checkPath(diceFile) == nil {
					diceBytes, err := ioutil.ReadFile(diceFile)
					if err != nil {
						ext.DiceErr = errors.Wrap(err, "failed to read dice "+diceFile)
					}
					var diceData map[string]interface{}
					err = yaml.Unmarshal(diceBytes, &diceData)
					if err != nil {
						ext.DiceErr = errors.Wrap(err, "failed to parse dice "+diceFile)
					}
					ext.Dice = diceData
				}
				result = append(result, ext)
			}
		}
		return nil
	})
	return result
}
