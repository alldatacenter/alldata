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

	"github.com/ghodss/yaml"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/utils"
)

var EXTENSIONSPUSH = command.Command{
	Name:       "push",
	ParentName: "EXT",
	ShortHelp:  "push extension",
	Example:    "$ erda-cli ext push -f --public",
	Flags: []command.Flag{
		command.BoolFlag{Short: "f", Name: "force", Doc: "if true, override exist version", DefaultValue: false},
		command.BoolFlag{Short: "a", Name: "all", Doc: "if true, override exist extension and version, must with -f", DefaultValue: false},
		command.StringFlag{Short: "d", Name: "dir", Doc: "the extension dir", DefaultValue: ""},
		command.StringFlag{Short: "r", Name: "registry", Doc: "the new registry", DefaultValue: ""},
	},
	Run: RunExtensionsPush,
}

func RunExtensionsPush(ctx *command.Context, force bool, all bool, dir, registry string) error {
	var request apistructs.ExtensionVersionCreateRequest
	workDir, err := os.Getwd()
	if err != nil {
		return err
	}
	if dir != "" {
		workDir = dir
	}

	specBytes, err := ioutil.ReadFile(path.Join(workDir, "spec.yml"))
	if err != nil {
		return err
	}
	specData := apistructs.Spec{}
	err = yaml.Unmarshal(specBytes, &specData)
	if err != nil {
		return errors.Wrap(err, "failed to parse spec")
	}

	request.Public = specData.Public
	request.ForceUpdate = force
	request.Name = specData.Name
	request.Version = specData.Version
	request.SpecYml = string(specBytes)
	request.All = all
	request.IsDefault = specData.IsDefault

	diceBytes, err := ioutil.ReadFile(path.Join(workDir, "dice.yml"))

	if registry != "" && err == nil {
		diceBytes, _, err = replaceDiceRegistry(diceBytes, specData.Type, registry)
		if err != nil {
			return fmt.Errorf("%s ext_dir:%s", err, workDir)
		}
	}

	if err == nil {
		request.DiceYml = string(diceBytes)
	}

	readmeBytes, err := ioutil.ReadFile(path.Join(workDir, "README.md"))
	if err == nil {
		request.Readme = string(readmeBytes)
	}
	swaggerBytes, err := ioutil.ReadFile(path.Join(workDir, "swagger.yml"))
	if err == nil {
		request.SwaggerYml = string(swaggerBytes)
	}

	err = pushExtension(ctx, request)
	if err != nil {
		return err
	}

	ctx.Succ(fmt.Sprintf("extension %s push success\n", specData.Name))

	return nil
}

func pushExtension(ctx *command.Context, request apistructs.ExtensionVersionCreateRequest) error {
	var resp apistructs.ExtensionVersionCreateResponse
	var b bytes.Buffer
	urlPath := "/api/extensions/" + request.Name
	response, err := ctx.Post().Path(urlPath).JSONBody(request).Do().Body(&b)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("extension push", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("extension push",
			fmt.Sprintf("failed to request, status-code: %d %s",
				response.StatusCode(), b.String()), false))
	}

	if err = json.Unmarshal(b.Bytes(), &resp); err != nil {
		return fmt.Errorf(utils.FormatErrMsg("extension push",
			fmt.Sprintf("failed to unmarshal build extension response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("extension push",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}
	return nil
}
