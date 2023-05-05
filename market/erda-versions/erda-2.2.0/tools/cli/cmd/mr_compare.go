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
	"net/url"

	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
)

var MRCOMPARE = command.Command{
	Name:       "compare",
	ParentName: "MR",
	ShortHelp:  "compare merge request in browser",
	Example:    "$ erda-cli mr --application=<name> --from=<feature/new> --to=<master>",
	Flags: []command.Flag{
		command.StringFlag{Short: "", Name: "application", Doc: "name of the application", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "from", Doc: "branch contains source code", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "to", Doc: "branch contains base code", DefaultValue: ""},
	},
	Run: MrCompare,
}

func MrCompare(ctx *command.Context, application, from, to string) error {
	var org, project string
	org, orgID, err := common.GetOrgID(ctx, org)
	if err != nil {
		return err
	}
	project, projectID, err := common.GetProjectID(ctx, orgID, project)
	if err != nil {
		return err
	}
	application, applicationID, err := common.GetApplicationID(ctx, orgID, projectID, application)
	if err != nil {
		return err
	}

	entity := common.ErdaEntity{
		Type:          common.MrCompaireEntity,
		Org:           org,
		OrgID:         orgID,
		ProjectID:     projectID,
		ApplicationID: applicationID,
	}
	err = common.Open(ctx, entity, url.Values{})
	if err != nil {
		return err
	}

	ctx.Succ("Open merge request compare page in browser.")
	return nil
}
