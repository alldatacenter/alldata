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

	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var CONFIGUPDATE = command.Command{
	Name:       "update",
	ParentName: "CONFIG",
	ShortHelp:  "update project workspace config",
	Example: `
  $ erda-cli config update ECI=disable --org xxx --project yyy --workspace DEV
`,
	Args: []command.Arg{
		command.StringArg{}.Name("feature"),
	},
	Flags: []command.Flag{
		command.StringFlag{Name: "org", Doc: "[required]which org the project belongs", DefaultValue: ""},
		command.StringFlag{Name: "project", Doc: "[required]which project's feature to delete", DefaultValue: ""},
		command.StringFlag{Name: "workspace", Doc: "[optional]which workspace's feature to delete", DefaultValue: ""},
	},
	Run: RunFeaturesUpdate,
}

func RunFeaturesUpdate(ctx *command.Context, feature, org, project, workspace string) error {
	if project == "" || workspace == "" || org == "" {
		return fmt.Errorf(
			utils.FormatErrMsg("config update", "failed to update config, one of the flags [org, project, workspace] not set", true))
	}

	if err := common.UpdateProjectWorkspaceConfigs(ctx, feature, org, project, workspace); err != nil {
		return err
	}

	ctx.Succ("config update success\n")
	return nil
}
