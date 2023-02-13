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

package query

import (
	"encoding/json"
	"errors"
	"fmt"
	"reflect"
	"time"

	"github.com/sirupsen/logrus"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	stream "github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (p *provider) UpdateIssue(req *pb.UpdateIssueRequest) error {
	id := req.Id
	// 获取事件更新前的版本
	issueModel, err := p.db.GetIssue(int64(id))
	if err != nil {
		return apierrors.ErrGetIssue.InternalError(err)
	}

	if err := validPlanTime(req, &issueModel); err != nil {
		return err
	}

	cache, err := NewIssueCache(p.db)
	if err != nil {
		return apierrors.ErrUpdateIssue.InternalError(err)
	}
	// if state of bug is changed to resolved/wontfix, change owner to operator/creator
	if issueModel.Type == pb.IssueTypeEnum_BUG.String() {
		currentState, err := cache.TryGetState(issueModel.State)
		if err != nil {
			return apierrors.ErrGetIssue.InternalError(err)
		}
		if req.State != nil {
			newState, err := cache.TryGetState(*req.State)
			if err != nil {
				return apierrors.ErrGetIssue.InternalError(err)
			}
			if (currentState.Belong != pb.IssueStateBelongEnum_RESOLVED.String()) && newState.Belong == pb.IssueStateBelongEnum_RESOLVED.String() {
				req.Owner = &req.IdentityInfo.UserID
			} else if (currentState.Belong != pb.IssueStateBelongEnum_WONTFIX.String()) && newState.Belong == pb.IssueStateBelongEnum_WONTFIX.String() {
				req.Owner = &issueModel.Creator
			}
		}
	}
	canUpdateFields := issueModel.GetCanUpdateFields()
	// 请求传入的需要更新的字段
	changedFields := getChangedFields(req, canUpdateFields["man_hour"].(string))
	planStartedAt := asTime(req.PlanStartedAt)
	planFinishedAt := asTime(req.PlanFinishedAt)
	if req.PlanFinishedAt != nil {
		// change plan finished at, update expiry status
		now := time.Date(time.Now().Year(), time.Now().Month(), time.Now().Day(), 0, 0, 0, 0, time.Now().Location())
		changedFields["expiry_status"] = dao.GetExpiryStatus(planFinishedAt, now)
	}
	// 检查修改的字段合法性
	if err := p.checkChangeFields(changedFields); err != nil {
		return apierrors.ErrUpdateIssue.InvalidParameter(err)
	}

	// issueStreamFields 保存字段更新前后的值，用于生成活动记录
	issueStreamFields := make(map[string][]interface{})

	// 遍历请求更新的字段，删除无需更新的字段
	for field, v := range changedFields {
		// man_hour 是否更新已提前判断，跳过是否需要更新的判断
		if field == "man_hour" {
			goto STREAM
		}
		// if field == "plan_started_at" {
		// 	// 如已有开始时间，跳过
		// 	if issueModel.PlanStartedAt != nil {
		// 		delete(changedFields, field)
		// 	}
		// 	continue
		// }
		if reflect.DeepEqual(v, canUpdateFields[field]) || v == canUpdateFields[field] {
			delete(changedFields, field)
			continue
		}
		if field == "state" {
			currentBelong, err := cache.TryGetState(issueModel.State)
			if err != nil {
				return apierrors.ErrGetIssue.InternalError(err)
			}
			newBelong, err := cache.TryGetState(*req.State)
			if err != nil {
				return apierrors.ErrGetIssueState.InternalError(err)
			}
			if currentBelong.Belong != pb.IssueStateBelongEnum_DONE.String() && newBelong.Belong == pb.IssueStateBelongEnum_DONE.String() {
				changedFields["finish_time"] = time.Now()
			} else if currentBelong.Belong != pb.IssueStateBelongEnum_CLOSED.String() && newBelong.Belong == pb.IssueStateBelongEnum_CLOSED.String() {
				changedFields["finish_time"] = time.Now()
			}
			if currentBelong.Belong == pb.IssueStateBelongEnum_DONE.String() && newBelong.Belong != pb.IssueStateBelongEnum_DONE.String() {
				var nilTime *time.Time
				nilTime = nil
				changedFields["finish_time"] = nilTime
			} else if currentBelong.Belong == pb.IssueStateBelongEnum_CLOSED.String() && newBelong.Belong != pb.IssueStateBelongEnum_CLOSED.String() {
				var nilTime *time.Time
				nilTime = nil
				changedFields["finish_time"] = nilTime
			}

			if currentBelong.Belong != pb.IssueStateBelongEnum_REOPEN.String() && newBelong.Belong == pb.IssueStateBelongEnum_REOPEN.String() {
				changedFields["reopen_count"] = issueModel.ReopenCount + 1
			}
			if currentBelong.Belong == pb.IssueStateBelongEnum_OPEN.String() && newBelong.Belong != currentBelong.Belong && issueModel.StartTime == nil {
				changedFields["start_time"] = time.Now()
			}
		}
	STREAM:
		issueStreamFields[field] = []interface{}{canUpdateFields[field], v}
	}

	c := &issueValidationConfig{}
	if req.IterationID != nil {
		iteration, err := cache.TryGetIteration(*req.IterationID)
		if err != nil {
			return err
		}
		c.iteration = iteration
	}
	if req.State != nil {
		state, err := cache.TryGetState(*req.State)
		if err != nil {
			return err
		}
		c.state = state
	}
	v := issueValidator{}
	if err = v.validateChangedFields(req, c, changedFields); err != nil {
		return err
	}

	// 校验实际需要更新的字段
	// 校验 state
	if err := p.checkUpdateStatePermission(issueModel, changedFields, req.IdentityInfo); err != nil {
		return err
	}

	// 更新实际需要更新的字段
	if err := p.db.UpdateIssue(id, changedFields); err != nil {
		return apierrors.ErrUpdateIssue.InternalError(err)
	}

	// 需求迭代变更时，集联变更需求下的任务迭代
	// if issueModel.Type == apistructs.IssueTypeRequirement {
	// 	taskFields := map[string]interface{}{"iteration_id": req.IterationID}
	// 	if err := svc.db.UpdateIssues(req.ID, taskFields); err != nil {
	// 		return apierrors.ErrUpdateIssue.InternalError(err)
	// 	}
	// }

	// create stream and send issue update event
	if err := p.Stream.CreateStream(req, issueStreamFields); err != nil {
		logrus.Errorf("create issue %d stream err: %v", req.Id, err)
	}

	// create issue state transition
	if req.State != nil && issueModel.State != *req.State {
		if err = p.db.CreateIssueStateTransition(&dao.IssueStateTransition{
			ProjectID: issueModel.ProjectID,
			IssueID:   issueModel.ID,
			StateFrom: uint64(issueModel.State),
			StateTo:   uint64(*req.State),
			Creator:   req.IdentityInfo.UserID,
		}); err != nil {
			return err
		}
	}

	currentBelong, err := cache.TryGetState(issueModel.State)
	if err != nil {
		return apierrors.ErrUpdateIssue.InternalError(err)
	}
	u := &IssueUpdated{
		Id:                      issueModel.ID,
		stateOld:                currentBelong.Belong,
		PlanStartedAt:           planStartedAt,
		PlanFinishedAt:          planFinishedAt,
		updateChildrenIteration: req.WithChildrenIteration && issueModel.Type == pb.IssueTypeEnum_REQUIREMENT.String(),
		projectID:               issueModel.ProjectID,
	}
	if req.IterationID != nil {
		u.IterationID = *req.IterationID
	}
	if req.State != nil {
		newBelong, err := cache.TryGetState(*req.State)
		if err != nil {
			return apierrors.ErrUpdateIssue.InternalError(err)
		}
		u.stateNew = newBelong.Belong
	}
	if err := p.AfterIssueUpdate(u); err != nil {
		return fmt.Errorf("after issue update failed when issue id: %v update, err: %v", issueModel.ID, err)
	}

	return nil
}

// IsEmpty 判断更新请求里的字段是否均为空
func IsEmpty(r *pb.UpdateIssueRequest) bool {
	return r.Title == nil && r.Content == nil && r.State == nil &&
		r.Priority == nil && r.Complexity == nil && r.Severity == nil &&
		r.PlanStartedAt == nil && r.PlanFinishedAt == nil &&
		r.Assignee == nil && r.IterationID == nil && r.IssueManHour == nil
}

type IssueUpdated struct {
	Id                      uint64
	stateOld                string
	stateNew                string
	PlanStartedAt           *time.Time
	PlanFinishedAt          *time.Time
	IterationID             int64
	iterationOld            int64
	projectID               uint64
	updateChildrenIteration bool
	withIteration           bool
}

func validPlanTime(req *pb.UpdateIssueRequest, issue *dao.Issue) error {
	started := asTime(req.PlanStartedAt)
	finished := asTime(req.PlanFinishedAt)
	if started != nil && finished != nil {
		if started.After(*finished) {
			return fmt.Errorf("plan started is after plan finished time")
		}
	} else {
		if finished != nil && issue.PlanStartedAt != nil && issue.PlanStartedAt.After(*finished) {
			return apierrors.ErrUpdateIssue.InvalidParameter("plan finished at")
		}
		if started != nil && issue.PlanFinishedAt != nil && started.After(*issue.PlanFinishedAt) {
			return apierrors.ErrUpdateIssue.InvalidParameter("plan started at")
		}
	}
	return nil
}

// Convert to local time for comparing date without location, timestamppb uses UTC different with db global time zone.
func asTime(s *string) *time.Time {
	if s == nil || *s == "" {
		return nil
	}
	t, err := time.Parse(time.RFC3339, *s)
	if err != nil {
		return nil
	}
	localTime := t.Local()
	return &localTime
}

// GetChangedFields 从 IssueUpdateRequest 中找出需要更新(不为空)的字段
// 注意：map 的 value 需要与 dao.Issue 字段类型一致
func getChangedFields(r *pb.UpdateIssueRequest, manHour string) map[string]interface{} {
	fields := make(map[string]interface{})
	if r.Title != nil {
		fields["title"] = *r.Title
	}
	if r.Content != nil {
		fields["content"] = *r.Content
	}
	if r.State != nil {
		fields["state"] = *r.State
	}
	if r.Priority != nil {
		fields["priority"] = *r.Priority
	}
	if r.Complexity != nil {
		fields["complexity"] = *r.Complexity
	}
	if r.Severity != nil {
		fields["severity"] = *r.Severity
	}
	if r.PlanStartedAt != nil {
		fields["plan_started_at"] = asTime(r.PlanStartedAt)
	}
	if r.PlanFinishedAt != nil {
		fields["plan_finished_at"] = asTime(r.PlanFinishedAt)
	}
	if r.Assignee != nil {
		fields["assignee"] = *r.Assignee
	}
	if r.IterationID != nil {
		fields["iteration_id"] = *r.IterationID
	}
	if r.Source != nil {
		fields["source"] = *r.Source
	}
	if r.Owner != nil {
		fields["owner"] = *r.Owner
	}
	// TaskType和BugStage必定有一个为nil
	if r.BugStage != nil && len(*r.BugStage) != 0 {
		fields["stage"] = *r.BugStage
	} else if r.TaskType != nil && len(*r.TaskType) > 0 {
		fields["stage"] = *r.TaskType
	}
	if r.IssueManHour != nil {
		// if r.IssueManHour.ThisElapsedTime != 0 {
		// 	// 开始时间为当天0点
		// 	timeStr := time.Now().Format("2006-01-02")
		// 	t, _ := time.ParseInLocation("2006-01-02", timeStr, time.Local)
		// 	fields["plan_started_at"] = t
		// }
		// IssueManHour 是否改变提前特殊处理
		// 只有当预期时间或剩余时间发生改变时，才认为工时信息发生了改变
		// 所花时间，开始时间，工作内容是实时内容，只在事件动态里记录就好，在dice_issue表中是没有意义的数据
		var oldManHour pb.IssueManHour
		json.Unmarshal([]byte(manHour), &oldManHour)
		if r.IssueManHour.ThisElapsedTime != 0 || r.IssueManHour.StartTime != "" || r.IssueManHour.WorkContent != "" ||
			r.IssueManHour.EstimateTime != oldManHour.EstimateTime || r.IssueManHour.RemainingTime != oldManHour.RemainingTime {
			// 剩余时间被修改过的话，需要标记一下
			if r.IssueManHour.RemainingTime != oldManHour.RemainingTime {
				r.IssueManHour.IsModifiedRemainingTime = true
			}
			// 已用时间累加上
			r.IssueManHour.ElapsedTime = oldManHour.ElapsedTime + r.IssueManHour.ThisElapsedTime
			fields["man_hour"] = common.GetDBManHour(r.IssueManHour)
		}
	}
	return fields
}

// checkChangeFields 更新事件时检查需要更新的字段
func (p *provider) checkChangeFields(fields map[string]interface{}) error {
	// 检查迭代是否存在
	if _, ok := fields["iteration_id"]; !ok {
		return nil
	}

	iterationID := fields["iteration_id"].(int64)
	if iterationID == -1 {
		return nil
	}

	iteration, err := p.db.GetIteration(uint64(iterationID))
	if err != nil || iteration == nil {
		return errors.New("the iteration does not exit")
	}

	return nil
}

// checkUpdateStatePermission 判断当前状态是否能更新至新状态，如果能，再判断是否有权限更新至新状态
func (p *provider) checkUpdateStatePermission(model dao.Issue, changedFields map[string]interface{}, identityInfo *commonpb.IdentityInfo) error {
	// 校验新状态是否合法
	newStateInterface, ok := changedFields["state"]
	if !ok {
		return nil
	}
	newState, ok := newStateInterface.(int64)
	if !ok {
		return apierrors.ErrUpdateIssueState.InvalidParameter(fmt.Sprintf("%v", changedFields["state_id"]))
	}
	//if !svc.ConvertWithoutButton(model).ValidState(string(newState)) {
	//	return apierrors.ErrUpdateIssueState.InvalidParameter(newState)
	//}

	// 权限校验只校验目标状态
	permCheckItem := makeButtonCheckPermItem(model, newState)
	permCheckItems := map[string]bool{permCheckItem: false}

	_, err := p.GenerateButton(model, identityInfo, permCheckItems, nil, nil, nil)
	if err != nil {
		return err
	}

	if !permCheckItems[permCheckItem] {
		return apierrors.ErrUpdateIssueState.InvalidParameter(newState)
	}

	return nil
}

func (p *provider) BatchUpdateIssue(req *pb.BatchUpdateIssueRequest) error {
	if err := CheckValid(req); err != nil {
		return apierrors.ErrBatchUpdateIssue.InvalidParameter(err)
	}
	// 获取待批量更新记录
	issues, err := p.db.GetBatchUpdateIssues(req)
	if err != nil {
		return apierrors.ErrBatchUpdateIssue.InternalError(err)
	}

	//如果更新的是状态，单独鉴权
	if req.State != 0 {
		changedFields := map[string]interface{}{"state": req.State}
		for _, v := range issues {
			if err := p.checkUpdateStatePermission(v, changedFields, req.IdentityInfo); err != nil {
				return apierrors.ErrBatchUpdateIssue.InternalError(fmt.Errorf(
					"update to %v failed: no permission or some issue's state couldn't update to %v directly",
					req.State, req.State))
			}
		}
	}

	// 批量更新
	if err := p.db.BatchUpdateIssues(req); err != nil {
		return apierrors.ErrBatchUpdateIssue.InternalError(err)
	}

	if req.NewIterationID != 0 {
		for _, v := range issues {
			// 需求迭代变更时，集联变更需求下的任务迭代
			if v.Type == pb.IssueTypeEnum_REQUIREMENT.String() {
				taskFields := map[string]interface{}{"iteration_id": req.NewIterationID}
				if err := p.db.UpdateIssues(uint64(v.ID), taskFields); err != nil {
					logrus.Errorf("[alert] failed to update task iteration, reqID: %+v, err: %v", v.ID, err)
					continue
				}
			}
		}
	}

	if req.State != 0 {
		p.batchCreateStateChaningStream(req, issues)
	} else if req.Assignee != "" {
		if err := p.batchCreateAssignChaningStream(req, issues); err != nil {
			return apierrors.ErrBatchUpdateIssue.InternalError(err)
		}
	}
	return nil
}

// CheckValid 仅需求、缺陷的处理人/状态可批量更新
func CheckValid(r *pb.BatchUpdateIssueRequest) error {
	if !r.All && len(r.Ids) == 0 {
		return errors.New("none selected")
	}

	if r.Assignee == "" && r.State == 0 && r.NewIterationID == 0 {
		return errors.New("none updated")
	}

	if r.Type != pb.IssueTypeEnum_REQUIREMENT && r.Type != pb.IssueTypeEnum_BUG {
		return errors.New("only requirement/bug can batch update")
	}

	return nil
}

// batchCreateStateChaningStream 批量创建状态转换活动记录
func (p *provider) batchCreateStateChaningStream(req *pb.BatchUpdateIssueRequest, issues []dao.Issue) {
	// 批量生成状态转换活动记录
	for _, v := range issues {
		streamReq := stream.IssueStreamCreateRequest{
			IssueID:      int64(v.ID),
			Operator:     req.IdentityInfo.UserID,
			StreamType:   stream.ISTTransferState,
			StreamParams: stream.ISTParam{CurrentState: fmt.Sprintf("%d", v.State), NewState: fmt.Sprintf("%d", v.State)},
		}
		// create stream
		if _, err := p.Stream.Create(&streamReq); err != nil {
			logrus.Errorf("[alert] failed to create issueStream when update issue, req: %+v, err: %v", streamReq, err)
			continue
		}
	}
}

// batchCreateAssignChaningStream 批量生成处理人变更活动记录
func (p *provider) batchCreateAssignChaningStream(req *pb.BatchUpdateIssueRequest, issues []dao.Issue) error {
	// 批量生成处理人变更活动记录
	userIds := make([]string, 0, len(issues))
	for _, v := range issues {
		userIds = append(userIds, v.Assignee)
	}
	userIds = append(userIds, req.Assignee)

	users, err := p.uc.FindUsers(userIds)
	if err != nil {
		return err
	}
	userInfo := make(map[string]string, len(users))
	for _, v := range users {
		userInfo[v.ID] = v.Nick
	}

	for _, v := range issues {
		streamReq := stream.IssueStreamCreateRequest{
			IssueID:      int64(v.ID),
			Operator:     req.IdentityInfo.UserID,
			StreamType:   stream.ISTChangeAssignee,
			StreamParams: stream.ISTParam{CurrentAssignee: userInfo[v.Assignee], NewAssignee: userInfo[req.Assignee]},
		}
		// create stream
		if _, err := p.Stream.Create(&streamReq); err != nil {
			logrus.Errorf("[alert] failed to create issueStream when update issue, req: %+v, err: %v", streamReq, err)
			continue
		}
	}

	return nil
}
