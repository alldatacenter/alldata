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

	"github.com/pkg/errors"
	"github.com/spf13/cobra"

	"github.com/erda-project/erda-proto-go/dop/issue/stream/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var MRCREATE = command.Command{
	Name:       "create",
	ParentName: "MR",
	ShortHelp:  "create merge request",
	Example:    "$ erda-cli mr create --application=<name> --from=<feature-branch> --to=<master>",
	Flags: []command.Flag{
		command.StringFlag{Short: "", Name: "application", Doc: "name of the application", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "from", Doc: "branch contains source code", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "to", Doc: "branch contains base code", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "title", Doc: "title of merge request", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "description", Doc: "desc of merge request", DefaultValue: ""},
		command.Uint64Flag{Short: "", Name: "issue-id", Doc: "relate issue id", DefaultValue: 0},
		command.BoolFlag{Short: "", Name: "remove", Doc: "if true, remove merge source branch", DefaultValue: true},
		command.BoolFlag{Short: "", Name: "open", Doc: "if true, open merge request page in browser", DefaultValue: false},
	},
	RegisterFlagCompletionFunc: map[string]interface{}{
		"application": MrCreateApplicationCompletion,
		"from":        MrCreateBranchCompletion,
		"to":          MrCreateBranchCompletion,
		"issue-id":    MrCreateIssueCompletion,
	},
	Run: MrCreate,
}

func MrCreateApplicationCompletion(ctx *cobra.Command, args []string, toComplete string, application, from, to, title, desc string, issueID uint64, remove, open bool) []string {
	var comps []string
	err := command.PrepareCtx(ctx, args)
	if err != nil {
		return comps
	}

	c := command.GetContext()
	for _, a := range c.Applications {
		comps = append(comps, a.Application)
	}
	return comps
}

func MrCreateBranchCompletion(ctx *cobra.Command, args []string, toComplete string, application, from, to, title, desc string, issueID uint64, remove, open bool) []string {
	workDir := "."
	if application != "" {
		workDir = application
	}
	return applicationBranches(workDir)
}

func MrCreateIssueCompletion(ctx *cobra.Command, args []string, toComplete string, application, from, to, title, desc string, issueID uint64, remove, open bool) []string {
	return IssueCompletion(ctx, args, toComplete, int(issueID))
}

func MrCreate(ctx *command.Context, application, from, to, title, desc string, issueID uint64, remove, open bool) error {
	if title == "" {
		return errors.Errorf("No title set, which is required.")
	}

	if desc == "" {
		return errors.Errorf("No description set, which is required.")
	}

	if from == "" {
		workDir := "."
		if application != "" {
			workDir = application
		}
		fromB, err := utils.GetWorkspaceBranch(workDir)
		if err != nil {
			return errors.Errorf("No source branch set, and get branch from workspace failed %v", err)
		}
		from = fromB
		ctx.Warn("No source branch set, use current branch '%s'", from)
	}
	if to == "" {
		to = "master"
		ctx.Warn("No target branch set, use 'master'.")
	}

	if from == to {
		return errors.Errorf("Invalid branch, source and target both are %s", from)
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

	application, applicationID, err := common.GetApplicationID(ctx, orgID, projectID, application)
	if err != nil {
		return err
	}

	p, err := common.GetProjectDetail(ctx, orgID, projectID)
	if err != nil {
		return err
	}

	assignee := ctx.GetUserID()
	if len(p.Owners) > 0 {
		assignee = p.Owners[0]
	}

	var request = &apistructs.GittarCreateMergeRequest{
		Title:              title,
		Description:        desc,
		SourceBranch:       from,
		TargetBranch:       to,
		AssigneeID:         assignee,
		RemoveSourceBranch: remove,
	}

	ctx.Info("source branch %s, target branch %s", from, to)
	mr, err := common.CreateMr(ctx, orgID, project, application, request)
	if err != nil {
		return err
	}

	if issueID != 0 {
		mrInfo := pb.MRCommentInfo{
			AppID:   int64(applicationID),
			MrID:    int64(mr.RepoMergeId),
			MrTitle: mr.Title,
		}
		comment := pb.CommentIssueStreamCreateRequest{
			IssueID: int64(issueID),
			Type:    string(apistructs.ISTRelateMR),
			UserID:  ctx.GetUserID(),
			MrInfo:  &mrInfo,
		}
		relateReq := pb.CommentIssueStreamBatchCreateRequest{
			IssueStreams: []*pb.CommentIssueStreamCreateRequest{&comment},
		}

		err = common.CreateIssueComment(ctx, orgID, &relateReq)
		if err != nil {
			return err
		}
	}

	entity := common.ErdaEntity{
		Type:           common.MrEntity,
		Org:            org,
		OrgID:          orgID,
		ProjectID:      projectID,
		ApplicationID:  applicationID,
		MergeRequestID: uint64(mr.RepoMergeId),
	}

	if open {
		err = common.Open(ctx, entity, url.Values{})
		ctx.Info("Merge request page opened in browser.")
	} else {
		url, err := common.ConstructURL(ctx, entity, url.Values{})
		if err != nil {
			return err
		}
		ctx.Info("Merge request created.")
		ctx.Info("To %s", url)
	}

	return nil
}
