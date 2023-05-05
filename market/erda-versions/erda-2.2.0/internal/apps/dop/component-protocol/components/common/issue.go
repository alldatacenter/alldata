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
	"time"

	"github.com/erda-project/erda/apistructs"
)

const (
	UIStatusWarning    = "warning"
	UIStatusProcessing = "processing"
	UIStatusSuccess    = "success"
	UIStatusError      = "error"
	UIStatusDefault    = "default"
)

func GetUIIssueState(stateBelong apistructs.IssueStateBelong) string {
	switch stateBelong {
	case apistructs.IssueStateBelongOpen:
		return UIStatusWarning
	case apistructs.IssueStateBelongWorking:
		return UIStatusProcessing
	case apistructs.IssueStateBelongDone:
		return UIStatusSuccess
	case apistructs.IssueStateBelongWontfix:
		return UIStatusDefault
	case apistructs.IssueStateBelongReopen:
		return UIStatusError
	case apistructs.IssueStateBelongResolved:
		return UIStatusSuccess
	case apistructs.IssueStateBelongClosed:
		return UIStatusSuccess
	default:
		return UIStatusDefault
	}
}

func MilliFromTime(t time.Time) int64 {
	return t.UnixNano() / int64(time.Millisecond)
}

type TimeMilliInDays struct {
	Today               int64
	Tomorrow            int64
	TheDayAfterTomorrow int64
	SevenDays           int64
	ThirtyDays          int64
}
