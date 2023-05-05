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
	"strconv"

	"github.com/spf13/cobra"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/terminal/table"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var ISSUE = command.Command{
	Name:      "issue",
	ShortHelp: "list issue already started to handle",
	Example:   "$ erda-cli issue",
	Flags: []command.Flag{
		command.BoolFlag{Short: "", Name: "no-headers", Doc: "if true, don't print headers (default print headers)", DefaultValue: false},
		command.BoolFlag{Short: "", Name: "requirement", Doc: "if true, list requirements", DefaultValue: false},
		command.BoolFlag{Short: "", Name: "task", Doc: "if true, list tasks", DefaultValue: false},
		command.BoolFlag{Short: "", Name: "bug", Doc: "if true, list bugs", DefaultValue: false},
		command.IntFlag{Short: "", Name: "page-size", Doc: "the number of page size", DefaultValue: 10},
	},
	Run: MyIssueList,
}

func MyIssueList(ctx *command.Context, noHeaders, requirement, task, bug bool, pageSize int) error {

	req, states, err := makeRequest(ctx, requirement, task, bug)
	if err != nil {
		return err
	}

	stateMap := map[int64]apistructs.IssueStateRelation{}
	for _, s := range states {
		stateMap[s.StateID] = s
	}
	num := 0
	err = utils.PagingView(func(pageNo, pageSize int) (bool, error) {
		req.PageNo = uint64(pageNo)
		req.PageSize = uint64(pageSize)

		pagingIssue, err := common.ListMyIssue(ctx, req)
		if err != nil {
			return false, err
		}

		tableView(pagingIssue.List, stateMap, noHeaders)

		num += len(pagingIssue.List)
		return int(pagingIssue.Total) > num, nil
	}, "Continue to display project?", pageSize, command.Interactive)
	return nil
}

func makeRequest(ctx *command.Context, requirement, task, bug bool) (*apistructs.IssuePagingRequest, []apistructs.IssueStateRelation, error) {
	var org, project string
	org, orgID, err := common.GetOrgID(ctx, org)
	if err != nil {
		return nil, nil, err
	}
	project, projectID, err := common.GetProjectID(ctx, orgID, project)
	if err != nil {
		return nil, nil, err
	}
	userID := ctx.GetUserID()
	if userID == "" {
		return nil, nil, errors.New("Invalid user")
	}

	var reqState apistructs.IssueStateRelationGetRequest
	reqState.ProjectID = projectID
	states, err := common.ListState(ctx, orgID, reqState)
	if err != nil {
		return nil, nil, err
	}

	todoStateIds, err := common.GetTodoStateIds(states)
	if err != nil {
		return nil, nil, err
	}

	var req apistructs.IssuePagingRequest
	req.Assignees = []string{userID}
	req.State = todoStateIds
	req.OrderBy = "planFinishedAt"
	req.ProjectID = projectID
	if requirement {
		req.Type = append(req.Type, "REQUIREMENT")
	}
	if task {
		req.Type = append(req.Type, "TASK")
	}
	if bug {
		req.Type = append(req.Type, "BUG")
	}

	return &req, states, nil
}

func tableView(issues []apistructs.Issue, stateMap map[int64]apistructs.IssueStateRelation, noHeaders bool) error {
	data := [][]string{}
	for _, i := range issues {
		finishDate := "-"
		if i.PlanFinishedAt != nil {
			finishDate = i.PlanFinishedAt.Format("2006-01-02")
		}
		state := ""
		if s, ok := stateMap[i.State]; ok {
			state = s.StateName
		}
		line := []string{
			strconv.FormatInt(i.ID, 10),
			finishDate,
			state,
			i.Title,
		}

		data = append(data, line)
	}

	t := table.NewTable()
	if !noHeaders {
		headers := []string{
			"IssueID", "FinishDate", "State", "IssueName",
		}

		t.Header(headers)
	}
	err := t.Data(data).Flush()
	if err != nil {
		return err
	}

	return nil
}

func IssueCompletion(ctx *cobra.Command, args []string, toComplete string, issueID int) []string {
	command.Interactive = false
	defer func() {
		command.Interactive = true
	}()

	var comps []string
	err := command.PrepareCtx(ctx, args)
	if err != nil {
		return comps
	}

	c := command.GetContext()
	req, _, err := makeRequest(c, true, true, true)
	if err != nil {
		return comps
	}

	req.PageNo = 1
	req.PageSize = 100

	pagingIssue, err := common.ListMyIssue(c, req)
	if err != nil {
		return comps
	}

	for _, i := range pagingIssue.List {
		comps = append(comps, strconv.FormatInt(i.ID, 10))
	}

	return comps
}
