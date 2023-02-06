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
	"net/url"
	"os"
	"strconv"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var PUSH = command.Command{
	Name:      "push",
	ShortHelp: "push project to a Erda platform",
	Example:   "$ erda-cli push <project-url>",
	Args: []command.Arg{
		command.StringArg{}.Name("url"),
	},
	Flags: []command.Flag{
		command.StringListFlag{Short: "", Name: "application", Doc: "applications to push", DefaultValue: nil},
		command.StringFlag{Short: "", Name: "configfile", Doc: "config file contains applications", DefaultValue: ""},
		command.BoolFlag{Short: "", Name: "all", Doc: "if true, push all applications", DefaultValue: false},
		command.BoolFlag{Short: "", Name: "force", Doc: "if true, git push with --force flag", DefaultValue: false},
	},
	Run: Push,
}

func Push(ctx *command.Context, urlstr string, applications []string, configfile string, pushall, force bool) error {
	u, err := url.Parse(urlstr)
	if err != nil {
		return err
	}
	t, paths, err := utils.ClassifyURL(u.Path)
	if err != nil {
		return err
	}

	if t != utils.ProjectURL {
		return errors.Errorf("Invalid project url %s", urlstr)
	}

	if len(applications) > 0 && configfile != "" {
		return errors.New("Should not both set --application and --configfile")
	}

	if len(applications) == 0 && configfile == "" && !pushall {
		return errors.New("No application set to push.")
	}

	var org, project string
	var orgID, projectID uint64

	org = paths[1]
	projectID, err = strconv.ParseUint(paths[4], 10, 64)

	org, orgID, err = common.GetOrgID(ctx, org)
	if err != nil {
		return err
	}

	p, err := common.GetProjectDetail(ctx, orgID, projectID)
	if err != nil {
		return err
	}
	project = p.Name

	existProjectList, err := common.GetApplications(ctx, orgID, projectID)
	existProjectNames := map[string]apistructs.ApplicationDTO{}
	for _, p := range existProjectList {
		existProjectNames[p.Name] = p
	}

	var applications2push []command.ApplicationInfo

	_, c, err := command.GetProjectConfig()
	if err != nil {
		return errors.Errorf("Failed to get project config, %v", err)
	}

	if len(applications) > 0 {
		cMap := map[string]command.ApplicationInfo{}
		for _, a := range c.Applications {
			cMap[a.Application] = a
		}
		for _, app := range applications {
			if a, ok := cMap[app]; ok {
				applications2push = append(applications2push, a)
			} else {
				return errors.Errorf("Failed to get application in local project.")
			}
		}
	} else if configfile != "" {
		config, err := command.GetProjectConfigFrom(configfile)
		if err != nil {
			return errors.Errorf("Failed to get application from config file %s", configfile)
		}

		applications2push = append(applications2push, config.Applications...)

	} else if pushall {
		applications2push = append(applications2push, c.Applications...)
	}

	if len(applications2push) == 0 {
		return errors.New("No application set to push.")
	}

	for _, a := range applications2push {
		if _, err := os.Stat(a.Application); err != nil {
			return errors.Errorf("Application %s is not found in current directory. You may change to root directory of the project.", a.Application)
		}

		var gitRepo string
		if p, ok := existProjectNames[a.Application]; ok {
			gitRepo = p.GitRepoNew
		} else {
			remoteApp, err := common.CreateApplication(ctx, projectID, a.Application, a.Mode, a.Desc, a.Sonarhost, a.Sonartoken, a.Sonarproject)
			if err != nil {
				return err
			}
			gitRepo = remoteApp.GitRepoNew
		}

		ss := strings.Split(ctx.CurrentOpenApiHost, "://")
		if len(ss) < 1 {
			return errors.Errorf("Invalid openapi host %s", ctx.CurrentOpenApiHost)
		}
		repo := fmt.Sprintf("%s://%s", ss[0], gitRepo)

		err = common.PushApplication(a.Application, repo, force)
		if err != nil {
			return err
		}

		ctx.Info("Application '%s' pushed.", a.Application)
	}

	ctx.Succ("Project '%s' pushed to server %s.", project, ctx.CurrentOpenApiHost)
	return nil
}
