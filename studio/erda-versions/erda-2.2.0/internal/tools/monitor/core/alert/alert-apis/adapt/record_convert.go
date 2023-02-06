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

package adapt

import (
	"time"

	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/cql"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

// FromModel .
func (a *AlertRecord) FromModel(m *db.AlertRecord) *AlertRecord {
	a.GroupID = m.GroupID
	a.Scope = m.Scope
	a.ScopeKey = m.ScopeKey
	a.AlertGroup = m.AlertGroup
	a.Title = m.Title
	a.AlertState = m.AlertState
	a.AlertType = m.AlertType
	a.AlertIndex = m.AlertIndex
	a.ExpressionKey = m.ExpressionKey
	a.AlertID = m.AlertID
	a.AlertName = m.AlertName
	a.RuleID = m.RuleID
	a.IssueID = m.IssueID
	a.HandleState = m.HandleState
	a.HandlerID = m.HandlerID
	a.AlertTime = utils.ConvertTimeToMS(m.AlertTime)
	a.HandleTime = utils.ConvertTimeToMS(m.HandleTime)
	a.CreateTime = utils.ConvertTimeToMS(m.CreateTime)
	a.UpdateTime = utils.ConvertTimeToMS(m.UpdateTime)
	return a
}

func ToPBAlertRecord(m *db.AlertRecord) *pb.AlertRecord {
	a := &pb.AlertRecord{}
	a.GroupId = m.GroupID
	a.Scope = m.Scope
	a.ScopeKey = m.ScopeKey
	a.AlertGroup = m.AlertGroup
	a.Title = m.Title
	a.AlertState = m.AlertState
	a.AlertType = m.AlertType
	a.AlertIndex = m.AlertIndex
	a.ExpressionKey = m.ExpressionKey
	a.AlertId = m.AlertID
	a.AlertName = m.AlertName
	a.RuleId = m.RuleID
	a.IssueId = m.IssueID
	a.HandleState = m.HandleState
	a.HandlerId = m.HandlerID
	a.AlertTime = utils.ConvertTimeToMS(m.AlertTime)
	a.HandleTime = utils.ConvertTimeToMS(m.HandleTime)
	a.CreateTime = utils.ConvertTimeToMS(m.CreateTime)
	a.UpdateTime = utils.ConvertTimeToMS(m.UpdateTime)
	return a
}

func (a *AlertRecord) ToModel(m *db.AlertRecord) {
	m.GroupID = a.GroupID
	m.Scope = a.Scope
	m.ScopeKey = a.ScopeKey
	m.AlertGroup = a.AlertGroup
	m.Title = a.Title
	m.AlertState = a.AlertState
	m.AlertType = a.AlertType
	m.AlertIndex = a.AlertIndex
	m.ExpressionKey = a.ExpressionKey
	m.AlertID = a.AlertID
	m.AlertName = a.AlertName
	m.RuleID = a.RuleID
	m.IssueID = a.IssueID
	m.HandleState = a.HandleState
	m.HandlerID = a.HandlerID
	m.AlertTime = time.Unix(a.AlertTime/1000, 0)
}

// FromModel .
func (a *AlertHistory) FromModel(m *cql.AlertHistory) *AlertHistory {
	a.GroupID = m.GroupID
	a.Timestamp = m.Timestamp
	a.AlertState = m.AlertState
	a.Title = m.Title
	a.Content = m.Content
	a.DisplayURL = m.DisplayURL
	return a
}

func ToDBAlertHistory(m *cql.AlertHistory) *pb.AlertHistory {
	a := &pb.AlertHistory{}
	a.GroupId = m.GroupID
	a.Timestamp = m.Timestamp
	a.AlertState = m.AlertState
	a.Title = m.Title
	a.Content = m.Content
	a.DisplayUrl = m.DisplayURL
	return a
}
