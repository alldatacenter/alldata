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
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"strings"

	"sigs.k8s.io/yaml"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/utils"
)

var EXTENSIONSPULL = command.Command{
	Name:       "pull",
	ParentName: "EXT",
	ShortHelp:  "pull extension",
	Example:    "$ erda-cli ext pull git-checkout@1.0 -o git-checkout",
	Args: []command.Arg{
		command.StringArg{}.Name("extension"),
	},
	Flags: []command.Flag{
		command.StringFlag{Short: "o", Name: "output", Doc: "the directory to export to", DefaultValue: ""},
	},
	Run: RunExtensionsPull,
}

func RunExtensionsPull(ctx *command.Context, extension string, dir string) error {
	var resp apistructs.ExtensionVersionGetResponse
	var b bytes.Buffer
	urlPath := "/api/extensions/" + strings.Replace(extension, "@", "/", -1)
	response, err := ctx.Get().Path(urlPath).Param("yamlFormat", "true").
		Do().Body(&b)

	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("extension get", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("extension get",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err = json.Unmarshal(b.Bytes(), &resp); err != nil {
		return fmt.Errorf(utils.FormatErrMsg("extension get",
			fmt.Sprintf("failed to unmarshal build extension response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("extension get",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	workDir, err := os.Getwd()
	if err != nil {
		return err
	}
	if dir != "" {
		workDir = dir
	}
	if _, err := os.Stat(workDir); os.IsNotExist(err) {
		err := os.MkdirAll(workDir, 0755)
		if err != nil {
			return err
		}
	}

	saveExtension(workDir, resp.Data)
	ctx.Succ("extension pull success\n")
	return nil
}

func saveExtension(workDir string, extVersion apistructs.ExtensionVersion) {
	ioutil.WriteFile(path.Join(workDir, "dice.yml"), []byte(extVersion.Dice.(string)), 0755)
	ioutil.WriteFile(path.Join(workDir, "spec.yml"), []byte(extVersion.Spec.(string)), 0755)
	ioutil.WriteFile(path.Join(workDir, "swagger.yml"), []byte(extVersion.Swagger.(string)), 0755)
	ioutil.WriteFile(path.Join(workDir, "README.md"), []byte(extVersion.Readme), 0755)
}

func replaceDiceRegistry(diceBytes []byte, typ string, registry string) ([]byte, map[string]string, error) {
	var diceData map[string]interface{}
	pushImages := map[string]string{}
	err := yaml.Unmarshal(diceBytes, &diceData)
	if err != nil {
		return nil, nil, err
	}
	if typ == "action" {
		jobs, ok := diceData["jobs"].(map[string]interface{})
		if !ok {
			return nil, nil, fmt.Errorf("failed to parse dice action jobs")
		}
		for name, v := range jobs {
			cfg := v.(map[string]interface{})
			_, imageExist := cfg["image"]
			if !imageExist {
				continue
			}
			originalImage := cfg["image"].(string)
			newImage := newDockerImage(originalImage, registry)
			cfg["image"] = newImage
			pushImages[name] = newImage
		}
	} else if typ == "addon" {
		jobs, ok := diceData["services"].(map[string]interface{})
		if !ok {
			return diceBytes, pushImages, nil
		}
		for name, v := range jobs {
			cfg := v.(map[string]interface{})
			_, imageExist := cfg["image"]
			if !imageExist {
				continue
			}
			originalImage := cfg["image"].(string)
			newImage := newDockerImage(originalImage, registry)
			cfg["image"] = newImage
			pushImages[name] = newImage
		}
	}
	newDiceBytes, err := yaml.Marshal(diceData)
	if err != nil {
		return nil, nil, err
	}
	return newDiceBytes, pushImages, nil
}

func newDockerImage(oldImage string, newRegistry string) string {
	if newRegistry == "" {
		return oldImage
	}
	index := strings.Index(oldImage, "/")
	if index > 0 {
		return newRegistry + oldImage[index:]
	} else {
		return oldImage
	}
}
