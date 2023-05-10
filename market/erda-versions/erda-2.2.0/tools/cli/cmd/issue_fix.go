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
	"os/exec"

	"github.com/spf13/cobra"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/dop/issue/stream/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var ISSUEFIX = command.Command{
	Name:       "fix",
	ParentName: "ISSUE",
	ShortHelp:  "create new branch to fix issue",
	Example:    "$ erda-cli fix <issue-id> --branch=<new> --base-branch=<base> --application=<name>",
	Args: []command.Arg{
		command.IntArg{}.Name("issue-id"),
	},
	Flags: []command.Flag{
		command.StringFlag{Short: "", Name: "application", Doc: "name of the application", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "branch", Doc: "branch to create and checkout", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "base-branch", Doc: "branch as base to create from", DefaultValue: ""},
	},
	ValidArgsFunction: FixIssueCompletion,
	Run:               IssueFix,
}

func FixIssueCompletion(ctx *cobra.Command, args []string, toComplete string, issueID int, application, branch, baseBranch string) []string {
	return IssueCompletion(ctx, args, toComplete, issueID)
}

func IssueFix(ctx *command.Context, issueID int, application, branch, baseBranch string) error {
	if branch == "" {
		return errors.Errorf("--branch not set")
	}

	// workspace check
	workDir := "."
	if application != "" {
		workDir = application
	} else if ctx.CurrentApplication.Application == "" {
		return errors.Errorf("Application not set. Using --application to set or work in an application directory.")
	}

	gittarInfo, err := utils.GetWorkspaceInfo(workDir, command.Remote)
	if err != nil {
		return err
	}
	if gittarInfo.Org != ctx.CurrentOrg.Name || gittarInfo.Project != ctx.CurrentProject.Project {
		return errors.Errorf("Current application remote %s is not match with local coinfig for project %s",
			utils.GetRepo(command.Remote), ctx.CurrentProject.Project)
	}

	dirty, err := utils.IsWorkspaceDirty(workDir)
	if err != nil {
		return err
	}
	if dirty {
		return errors.Errorf("Changes should be committed first for application %s", gittarInfo.Application)
	}

	err = createBranch(ctx, workDir, baseBranch, branch)
	if err != nil {
		return errors.Errorf("checkout new branch %s failed, error: %v", branch, err)
	}
	ctx.Info("Branch '%s' created in application '%s' to fix issue '%d'.", branch, gittarInfo.Application, issueID)

	err = updateIssueStream(ctx, issueID, branch)
	if err != nil {
		return errors.Errorf("update issue comment failed, error: %v", err)
	}

	return nil
}

func createBranch(ctx *command.Context, workDir, baseBranch, branch string) error {
	if baseBranch != "" {
		baseBranchCmd := exec.Command("git", "checkout", baseBranch)
		baseBranchCmd.Dir = workDir
		out, err := baseBranchCmd.CombinedOutput()
		if err != nil {
			return err
		}
		ctx.Info(string(out))
	} else {
		baseBranch, err := utils.GetWorkspaceBranch(workDir)
		if err != nil {
			return err
		}
		ctx.Info("No base branch set, use branch '%s'.", baseBranch)
	}
	{
		newBranchCmd := exec.Command("git", "checkout", "-b", branch)
		newBranchCmd.Dir = workDir
		out, err := newBranchCmd.CombinedOutput()
		if err != nil {
			ctx.Error(string(out))
			return err
		}
		ctx.Info(string(out))
	}
	{
		comment := fmt.Sprintf("init branch '%s'", branch)
		commitBranchCmd := exec.Command("git", "commit", "--allow-empty", "-m", comment)
		commitBranchCmd.Dir = workDir
		out, err := commitBranchCmd.CombinedOutput()
		if err != nil {
			ctx.Error(string(out))
			return err
		}
		ctx.Info(string(out))
	}
	{
		pushCmd := exec.Command("git", "push", "--set-upstream", command.Remote, branch)
		pushCmd.Dir = workDir
		out, err := pushCmd.CombinedOutput()
		if err != nil {
			ctx.Error(string(out))
			return err
		}
		ctx.Info(string(out))
	}
	return nil
}

func updateIssueStream(ctx *command.Context, issueID int, branch string) error {
	{
		entity := common.ErdaEntity{
			Type:          common.GittarBranch,
			Org:           ctx.CurrentOrg.Name,
			ProjectID:     ctx.CurrentProject.ProjectID,
			ApplicationID: ctx.CurrentApplication.ApplicationID,
			GittarBranch:  branch,
		}
		branchUrl, err := common.ConstructURL(ctx, entity, url.Values{})
		if err != nil {
			return err
		}
		content := fmt.Sprintf("Create [branch '%s'](%s) in application %s",
			branch, branchUrl, ctx.CurrentApplication.Application)
		comment := pb.CommentIssueStreamCreateRequest{
			IssueID: int64(issueID),
			Type:    string(apistructs.ISTComment),
			UserID:  ctx.GetUserID(),
			Content: content,
		}
		relateReq := pb.CommentIssueStreamBatchCreateRequest{
			IssueStreams: []*pb.CommentIssueStreamCreateRequest{&comment},
		}

		err = common.CreateIssueComment(ctx, ctx.CurrentOrg.ID, &relateReq)
		if err != nil {
			return err
		}
	}
	return nil
}
