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
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
)

var ISSUECLOSE = command.Command{
	Name:       "close",
	ParentName: "ISSUE",
	ShortHelp:  "close an issue",
	Example:    "$ erda-cli close <issue-id>",
	Args: []command.Arg{
		command.IntArg{}.Name("issue-id"),
	},
	Flags: []command.Flag{
		command.StringFlag{Short: "", Name: "man-hour", Doc: "time for work, in format of 2m/2h/2d/2w", DefaultValue: ""},
	},
	ValidArgsFunction: CloseIssueCompletion,
	Run:               IssueClose,
}

func CloseIssueCompletion(ctx *cobra.Command, args []string, toComplete string, issueID int, manHour string) []string {
	return IssueCompletion(ctx, args, toComplete, issueID)
}

func IssueClose(ctx *command.Context, issueID int, manHour string) error {
	l := len(manHour)
	if l == 0 {
		return errors.Errorf("man-hour not set.")
	} else if l == 1 {
		return errors.Errorf("Invalid man-hour %s.", manHour)
	}
	number, err := strconv.ParseInt(manHour[:l-1], 10, 32)
	if err != nil {
		return errors.Errorf("Parse man-hour %s failed, error: %s.", manHour, err)
	}

	var manHourInMinutes int64
	switch manHour[l-1:] {
	case "m":
		manHourInMinutes = number
	case "h":
		manHourInMinutes = 60 * number
	case "d":
		manHourInMinutes = 8 * 60 * number
	case "w":
		manHourInMinutes = 5 * 8 * 60 * number
	default:
		return errors.Errorf("Parse man-hour %s failed, not end with m/h/d/w", manHour)
	}

	issue, err := common.GetIssue(ctx, ctx.CurrentOrg.ID, ctx.CurrentProject.ProjectID, uint64(issueID))
	if err != nil {
		return err
	}

	var reqState apistructs.IssueStateRelationGetRequest
	reqState.ProjectID = ctx.CurrentProject.ProjectID
	states, err := common.ListState(ctx, ctx.CurrentOrg.ID, reqState)
	if err != nil {
		return err
	}
	var state *apistructs.IssueStateRelation
	stateMap := map[int64]*apistructs.IssueStateRelation{}
	for i, s := range states {
		if s.StateID == issue.State {
			state = &states[i]
		}
		stateMap[s.StateID] = &states[i]
	}
	if state == nil {
		return errors.Errorf("Issue state %d not found in erda", issue.State)
	}
	nextStateId, err := getDoneState(state, stateMap)
	if err != nil {
		return err
	}

	req := &apistructs.IssueUpdateRequest{}
	req.ID = uint64(issue.ID)
	req.State = &nextStateId
	req.IterationID = &issue.IterationID

	issue.ManHour.ThisElapsedTime = manHourInMinutes
	if issue.ManHour.EstimateTime == 0 {
		issue.ManHour.EstimateTime = manHourInMinutes + issue.ManHour.ElapsedTime
	}
	if issue.ManHour.RemainingTime > manHourInMinutes {
		issue.ManHour.RemainingTime -= manHourInMinutes
	} else {
		issue.ManHour.RemainingTime = 0
	}
	issue.ManHour.IsModifiedRemainingTime = true

	req.ManHour = &issue.ManHour

	err = common.UpdateIssue(ctx, ctx.CurrentOrg.ID, req)
	if err != nil {
		return err
	}

	return nil
}

func getDoneState(state *apistructs.IssueStateRelation, stateMap map[int64]*apistructs.IssueStateRelation) (int64, error) {
	var nextStateId int64
	var done bool
	switch state.IssueType {
	case apistructs.IssueTypeRequirement:
		if state.StateBelong == apistructs.IssueStateBelongWorking {
			for _, next := range state.StateRelation {
				if n, ok := stateMap[next]; ok {
					if n.StateBelong == apistructs.IssueStateBelongDone {
						nextStateId = n.StateID
						done = true
					}
				}
			}
		}
	case apistructs.IssueTypeBug:
		if state.StateBelong == apistructs.IssueStateBelongOpen ||
			state.StateBelong == apistructs.IssueStateBelongReopen {
			for _, next := range state.StateRelation {
				if n, ok := stateMap[next]; ok {
					if n.StateBelong == apistructs.IssueStateBelongResolved {
						nextStateId = n.StateID
						done = true
					}
				}
			}
		}
	case apistructs.IssueTypeTask:
		if state.StateBelong == apistructs.IssueStateBelongWorking {
			for _, next := range state.StateRelation {
				if n, ok := stateMap[next]; ok {
					if n.StateBelong == apistructs.IssueStateBelongDone {
						nextStateId = n.StateID
						done = true
					}
				}
			}
		}
	}

	if !done {
		return 0, errors.Errorf("Issue type is %s, state is %s, could not change to done",
			state.IssueType.String(), state.StateName)
	}

	return nextStateId, nil
}
