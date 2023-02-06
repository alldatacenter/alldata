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
	"os/exec"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var CLONE = command.Command{
	Name:      "clone",
	ShortHelp: "clone project or application from Erda",
	Example:   "$ erda-cli clone https://erda.cloud/trial/dop/projects/599",
	Args: []command.Arg{
		command.StringArg{}.Name("url"),
	},
	Flags: []command.Flag{
		command.BoolFlag{Short: "", Name: "cloneApps", Doc: "if false, don't clone applications in the project", DefaultValue: true},
	},
	Run: Clone,
}

func Clone(ctx *command.Context, ustr string, cloneApps bool) error {
	var org string
	var orgID, projectID, applicationID uint64

	u, err := url.Parse(ustr)
	if err != nil {
		return err
	}

	t, paths, err := utils.ClassifyURL(u.Path)
	if err != nil {
		return err
	}
	switch t {
	case utils.ApplicatinURL:
		applicationID, err = strconv.ParseUint(paths[6], 10, 64)
		if err != nil {
			return errors.Errorf("Invalid erda url.")
		}
		fallthrough
	case utils.ProjectURL:
		org = paths[1]
		projectID, err = strconv.ParseUint(paths[4], 10, 64)
		if err != nil {
			return errors.Errorf("Invalid erda url.")
		}
		break
	default:
		return errors.Errorf("Invalid erda url.")
	}

	org, orgID, err = common.GetOrgID(ctx, org)
	if err != nil {
		return err
	}

	var pInfo *command.ProjectInfo
	var successInfo string

	_, _, err = command.GetProjectConfig()
	if err != nil && err != utils.NotExist {
		return err
	} else if err == nil && t == utils.ProjectURL {
		return errors.New("you are already in a erda project workspace.")
	}

	p, err := common.GetProjectDetail(ctx, orgID, projectID)
	if err != nil {
		return err
	}
	pInfo = &command.ProjectInfo{
		Version:   command.ConfigVersion,
		Server:    ctx.CurrentOpenApiHost,
		Org:       org,
		OrgID:     orgID,
		Project:   p.Name,
		ProjectID: projectID,
	}

	_, err = os.Stat(fmt.Sprintf("%s", p.Name))
	if err == nil {
		return errors.Errorf("Project '%s' already exists in current directory.", p.Name)
	}

	err = os.MkdirAll(fmt.Sprintf("%s/%s", p.Name, utils.GlobalErdaDir), 0755)
	if err != nil {
		return err
	}
	defer func() {
		if err != nil {
			os.RemoveAll(p.Name)
		}
	}()

	appList, err := common.GetMyApplications(ctx, orgID, projectID)
	if err != nil {
		return err
	}
	for _, a := range appList {
		var (
			sonarHost    string
			sonarToken   string
			sonarProject string
		)
		if a.SonarConfig != nil {
			sonarHost = a.SonarConfig.Host
			sonarToken = a.SonarConfig.Token
			sonarProject = a.SonarConfig.ProjectKey
		}
		aInfo := command.ApplicationInfo{a.Name, a.ID, a.Mode, a.Desc,
			sonarHost, sonarToken, sonarProject}
		pInfo.Applications = append(pInfo.Applications, aInfo)

		if t == utils.ProjectURL && cloneApps {
			repo := fmt.Sprintf("%s://%s", u.Scheme, a.GitRepoNew)
			dir := fmt.Sprintf("%s/%s", p.Name, a.Name)
			ctx.Info("Application '%s' cloning ...", a.Name)
			err = cloneApplication(pInfo, a, repo, dir)
			if err != nil {
				return err
			}
			ctx.Info("Application '%s' cloned.", a.Name)
		}
	}

	if cloneApps {
		successInfo = fmt.Sprintf("Project '%s' and your applications cloned.", p.Name)
	} else {
		successInfo = fmt.Sprintf("Project '%s' cloned.", p.Name)
	}

	if t == utils.ApplicatinURL { // init application
		a, err := common.GetApplicationDetail(ctx, orgID, projectID, applicationID)
		if err != nil {
			return err
		}

		repo := fmt.Sprintf("%s://%s", u.Scheme, a.GitRepoNew)

		dir := fmt.Sprintf("%s/%s", p.Name, a.Name)
		err = cloneApplication(pInfo, a, repo, dir)
		if err != nil {
			return err
		}

		var (
			sonarHost    string
			sonarToken   string
			sonarProject string
		)
		if a.SonarConfig != nil {
			sonarHost = a.SonarConfig.Host
			sonarToken = a.SonarConfig.Token
			sonarProject = a.SonarConfig.ProjectKey
		}

		pInfo.Applications = append(pInfo.Applications, command.ApplicationInfo{
			a.Name, a.ID, a.Mode, a.Desc,
			sonarHost, sonarToken, sonarProject,
		})

		successInfo = fmt.Sprintf("Application '%s/%s' cloned.", a.ProjectName, a.Name)
	}

	pconfig := fmt.Sprintf("%s/.erda.d/config", pInfo.Project)
	err = command.SetProjectConfig(pconfig, pInfo)
	if err != nil {
		return err
	}
	ctx.Succ(successInfo)

	return nil
}

func cloneApplication(pInfo *command.ProjectInfo, a apistructs.ApplicationDTO, repo, dir string) error {
	if pInfo.ProjectID != a.ProjectID || pInfo.OrgID != a.OrgID {
		return errors.Errorf("application %s/%s cloned is not belong to project %s in the current workspace",
			a.ProjectName, a.Name, pInfo.Project)
	}

	// clone code
	output, err := exec.Command("git", "clone", repo, dir).CombinedOutput()
	if err != nil {
		return errors.Errorf("git clone repo %s err, %s", repo, output)
	}

	return nil
}
