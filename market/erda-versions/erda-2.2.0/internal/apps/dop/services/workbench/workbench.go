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

package workbench

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/issue"
)

// personal workbench issue expire days,Not specified, Expired, Due today , Due tomorrow, Due within 7 days, Expires within 30 days, Future: 0
// display undone issue, issues order by priority
var (
	issueUnspecified = "unspecified"
	issueExpired     = "expired"
	issueOneDay      = "oneDay"
	issueTomorrow    = "tomorrow"
	issueSevenDay    = "sevenDay"
	issueThirtyDay   = "thirtyDay"
	issueFeature     = "feature"
	expireDays       = []string{issueUnspecified, issueExpired, issueOneDay, issueTomorrow, issueSevenDay, issueThirtyDay, issueFeature}
	IssuePriorities  = []apistructs.IssuePriority{
		apistructs.IssuePriorityUrgent,
		apistructs.IssuePriorityHigh,
		apistructs.IssuePriorityNormal,
		apistructs.IssuePriorityLow,
	}
	IssueTypes = []apistructs.IssueType{
		apistructs.IssueTypeRequirement,
		apistructs.IssueTypeBug,
		apistructs.IssueTypeTask,
	}
)

type Workbench struct {
	bdl      *bundle.Bundle
	issueSvc *issue.Issue
}

type Option func(*Workbench)

func New(options ...Option) *Workbench {
	is := &Workbench{}
	for _, op := range options {
		op(is)
	}
	return is
}

// WithIssue set issue service
func WithIssue(i *issue.Issue) Option {
	return func(w *Workbench) {
		w.issueSvc = i
	}
}

// WithBundle set bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(w *Workbench) {
		w.bdl = bdl
	}
}

func (w *Workbench) GetUndoneProjectItems(req apistructs.WorkbenchRequest, userID string) (*apistructs.WorkbenchResponse, error) {
	if len(req.ProjectIDs) == 0 {
		return &apistructs.WorkbenchResponse{}, nil
	}
	req.IssuePagingRequest = apistructs.IssuePagingRequest{
		OrgID:    int64(req.OrgID),
		PageNo:   1,
		PageSize: uint64(req.IssueSize),
		IssueListRequest: apistructs.IssueListRequest{
			StateBelongs: apistructs.UnfinishedStateBelongs,
			Assignees:    []string{userID},
			External:     true,
			OrderBy:      "plan_finished_at asc, FIELD(priority, 'URGENT', 'HIGH', 'NORMAL', 'LOW')",
			Priority:     IssuePriorities,
			Type:         IssueTypes,
			Asc:          true,
		},
	}
	projectMap, err := w.issueSvc.GetIssuesByStates(req)
	if err != nil {
		return nil, err
	}

	return &apistructs.WorkbenchResponse{Data: projectMap}, nil
}
