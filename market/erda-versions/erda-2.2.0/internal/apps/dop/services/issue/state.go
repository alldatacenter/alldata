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

package issue

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/strutil"
)

//// canTransferToOpenWithoutPerm 根据 issue 类型和当前状态计算出是否可以被推进到 Open 状态，不做权限判断
//// 下同
//func canTransferToOpenWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateOpen {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeRequirement:
//		// 需求可以任意拖动
//		return true
//	default:
//		// OPEN 为初始状态，一旦修改，无法回退
//		return false
//	}
//}
//func canTransferToWorkingWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateWorking {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeRequirement:
//		// 需求可以任意拖动
//		return true
//	case apistructs.IssueTypeTask:
//		// 只有 OPEN 状态可以推进到 WORKING
//		return issue.State == apistructs.IssueStateOpen
//	case apistructs.IssueTypeBug:
//		// bug 没有 WORKING 状态
//		return false
//	case apistructs.IssueTypeTicket:
//		// ticket 没有 WORKING 状态
//		return false
//	default:
//		return false
//	}
//}
//func canTransferToTestingWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateTesting {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeRequirement:
//		// 需求可以任意拖动
//		return true
//	default:
//		// 只有 需求 有 TESTING
//		return false
//	}
//}
//func canTransferToDoneWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateDone {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeRequirement:
//		// 需求可以任意拖动
//		return true
//	case apistructs.IssueTypeTask:
//		// 只有 WORKING 状态可以推进到 DONE
//		return issue.State == apistructs.IssueStateWorking
//	default:
//		// 其他类型没有 WORKING 状态
//		return false
//	}
//}
//func canTransferToResolvedWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateResolved {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeBug:
//		return issue.State == apistructs.IssueStateOpen ||
//			issue.State == apistructs.IssueStateReopen
//	case apistructs.IssueTypeTicket:
//		return issue.State == apistructs.IssueStateOpen ||
//			issue.State == apistructs.IssueStateReopen
//	default:
//		return false
//	}
//}
//func canTransferToReopenWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateReopen {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeBug:
//		return issue.State == apistructs.IssueStateResolved ||
//			issue.State == apistructs.IssueStateWontfix ||
//			issue.State == apistructs.IssueStateDup
//	case apistructs.IssueTypeTicket:
//		return issue.State == apistructs.IssueStateResolved ||
//			issue.State == apistructs.IssueStateWontfix ||
//			issue.State == apistructs.IssueStateDup
//	default:
//		return false
//	}
//}
//func canTransferToWontfixWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateWontfix {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeBug:
//		return issue.State == apistructs.IssueStateOpen ||
//			issue.State == apistructs.IssueStateReopen
//	case apistructs.IssueTypeTicket:
//		return issue.State == apistructs.IssueStateOpen ||
//			issue.State == apistructs.IssueStateReopen
//	default:
//		return false
//	}
//}
//func canTransferToDupWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateDup {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeBug:
//		return issue.State == apistructs.IssueStateOpen ||
//			issue.State == apistructs.IssueStateReopen
//	case apistructs.IssueTypeTicket:
//		return issue.State == apistructs.IssueStateOpen ||
//			issue.State == apistructs.IssueStateReopen
//	default:
//		return false
//	}
//}
//func canTransferToClosedWithoutPerm(issue dao.Issue) bool {
//	if issue.State == apistructs.IssueStateClosed {
//		return false
//	}
//	switch issue.Type {
//	case apistructs.IssueTypeBug:
//		return issue.State == apistructs.IssueStateResolved ||
//			issue.State == apistructs.IssueStateWontfix ||
//			issue.State == apistructs.IssueStateDup
//	case apistructs.IssueTypeTicket:
//		return issue.State == apistructs.IssueStateResolved ||
//			issue.State == apistructs.IssueStateWontfix ||
//			issue.State == apistructs.IssueStateDup
//	default:
//		return false
//	}
//}
//
// makeButtonCheckPermItem 用于生成 开关 CanXxx 在 true 的基础上，再次校验用户权限时需要的 key
// key: ${issue-type}/CanXxx 开关项二次检查只与 issue 类型和 目标状态 有关，与当前状态无关 (permission.yml 定义)
func makeButtonCheckPermItem(issue dao.Issue, newStateID int64) string {

	return fmt.Sprintf("%s/%s", issue.Type, strconv.FormatInt(newStateID, 10))
}

// getCanXXXFieldName 根据传入的 state 生成对应的 CanXxx fieldName
func getCanXXXFieldName(state apistructs.IssueState) string {
	stateStr := strings.ToLower(string(state))
	return strutil.Concat("Can", strings.ToUpper(string(stateStr[0]))+stateStr[1:])
}

// getStateFromField 从 CanXxx fieldName 中获取对应 state: CanXxx -> XXX
func getStateFromField(field string) apistructs.IssueState {
	return apistructs.IssueState(strings.ToUpper(string(field[3:])))
}
