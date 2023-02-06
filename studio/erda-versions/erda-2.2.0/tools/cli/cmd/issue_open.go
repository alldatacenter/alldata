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
	"strconv"

	"github.com/spf13/cobra"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
)

var ISSUETOPEN = command.Command{
	Name:       "open",
	ParentName: "ISSUE",
	ShortHelp:  "open the issue page in browser",
	Example:    "$ erda-cli issue open <issue-id>",
	Args: []command.Arg{
		command.IntArg{}.Name("issue-id"),
	},
	ValidArgsFunction: OpenIssueCompletion,
	Run:               IssueOpen,
}

func OpenIssueCompletion(ctx *cobra.Command, args []string, toComplete string, issueID int) []string {
	return IssueCompletion(ctx, args, toComplete, issueID)
}

func IssueOpen(ctx *command.Context, issueID int) error {
	if issueID <= 0 {
		return errors.Errorf("Invalid issue id %d", issueID)
	}

	var org, project string
	org, orgID, err := common.GetOrgID(ctx, org)
	if err != nil {
		return err
	}
	project, projectID, err := common.GetProjectID(ctx, orgID, project)
	if err != nil {
		return err
	}

	issue, err := common.GetIssue(ctx, orgID, projectID, uint64(issueID))
	if err != nil {
		return err
	}

	params := url.Values{}
	params.Add("id", strconv.FormatUint(uint64(issueID), 10))
	params.Add("type", issue.Type.String())
	entity := common.ErdaEntity{
		Type:      common.IssueEntity,
		Org:       org,
		OrgID:     orgID,
		ProjectID: projectID,
	}
	err = common.Open(ctx, entity, params)
	if err != nil {
		return err
	}

	ctx.Succ("Open issue '%d' in browser.", issueID)
	return nil
}
