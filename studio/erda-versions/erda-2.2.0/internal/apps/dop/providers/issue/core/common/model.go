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

package common

import (
	"encoding/json"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
)

func GetDBManHour(i *pb.IssueManHour) string {
	if i == nil {
		return ""
	}
	mh, _ := json.Marshal(*i)
	return string(mh)
}

func GetUserIDs(req *pb.PagingIssueRequest) []string {
	var userIDs []string
	userIDs = append(userIDs, req.Assignee...)
	userIDs = append(userIDs, req.Creator...)
	userIDs = append(userIDs, req.Owner...)
	return userIDs
}

func IsOptions(pt string) bool {
	if pt == pb.PropertyTypeEnum_Select.String() || pt == pb.PropertyTypeEnum_MultiSelect.String() || pt == pb.PropertyTypeEnum_CheckBox.String() {
		return true
	}
	return false
}

func GetStage(s *pb.Issue) string {
	var stage string
	if s.Type == pb.IssueTypeEnum_TASK {
		stage = s.TaskType
	} else if s.Type == pb.IssueTypeEnum_BUG {
		stage = s.BugStage
	}
	return stage
}

var UnfinishedStateBelongs = []string{
	pb.IssueStateBelongEnum_OPEN.String(),
	pb.IssueStateBelongEnum_WORKING.String(),
	pb.IssueStateBelongEnum_WONTFIX.String(),
	pb.IssueStateBelongEnum_REOPEN.String(),
	pb.IssueStateBelongEnum_RESOLVED.String(),
}

var UnclosedStateBelongs = []string{
	pb.IssueStateBelongEnum_OPEN.String(),
	pb.IssueStateBelongEnum_WORKING.String(),
	pb.IssueStateBelongEnum_DONE.String(),
	pb.IssueStateBelongEnum_WONTFIX.String(),
	pb.IssueStateBelongEnum_REOPEN.String(),
	pb.IssueStateBelongEnum_RESOLVED.String(),
}
