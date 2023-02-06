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

package core

import (
	"encoding/json"
	"fmt"
	"reflect"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
)

func (p *provider) CreateIssueStreamBySystem(id uint64, streamFields map[string][]interface{}) error {
	streams := make([]dao.IssueStream, 0, len(streamFields))
	for field, v := range streamFields {
		if len(v) < 3 {
			logrus.Warnf("issue stream input: %v format is invalid", v)
			continue
		}
		streamReq := dao.IssueStream{
			IssueID:  int64(id),
			Operator: apistructs.SystemOperator,
		}
		reason, ok := v[2].(string)
		if !ok {
			logrus.Warnf("issue stream input field: %v type is invalid", v[2])
			continue
		}
		switch field {
		case "state":
			CurrentState, err := p.db.GetIssueStateByID(v[0].(int64))
			if err != nil {
				return err
			}
			NewState, err := p.db.GetIssueStateByID(v[1].(int64))
			if err != nil {
				return err
			}
			streamReq.StreamType = common.ISTTransferState
			streamReq.StreamParams = common.ISTParam{
				CurrentState: CurrentState.Name,
				NewState:     NewState.Name,
				ReasonDetail: reason,
			}
		case "plan_finished_at":
			streamReq.StreamType = common.ISTChangePlanFinishedAt
			streamReq.StreamParams = common.ISTParam{
				CurrentPlanFinishedAt: formatTime(v[0]),
				NewPlanFinishedAt:     formatTime(v[1]),
				ReasonDetail:          reason,
			}
		case "plan_started_at":
			streamReq.StreamType = common.ISTChangePlanStartedAt
			streamReq.StreamParams = common.ISTParam{
				CurrentPlanStartedAt: formatTime(v[0]),
				NewPlanStartedAt:     formatTime(v[1]),
				ReasonDetail:         reason,
			}
		case "label":
			streamReq.StreamType = common.ISTChangeLabel
			streamReq.StreamParams = common.ISTParam{
				ReasonDetail: reason,
			}
		case "iteration_id":
			streamType, params, err := p.HandleIssueStreamChangeIteration(nil, v[0].(int64), v[1].(int64))
			if err != nil {
				return err
			}
			streamReq.StreamType = streamType
			params.ReasonDetail = reason
			streamReq.StreamParams = params
		}
		streams = append(streams, streamReq)
	}
	return p.db.BatchCreateIssueStream(streams)
}

func (p *provider) HandleIssueStreamChangeIteration(lang i18n.LanguageCodes, currentIterationID, newIterationID int64) (
	streamType string, params common.ISTParam, err error) {
	// init default iteration
	unassignedIteration := &dao.Iteration{Title: p.I18n.Text(lang, "unassigned iteration")}
	currentIteration, newIteration := unassignedIteration, unassignedIteration
	streamType = common.ISTChangeIteration

	// current iteration
	if currentIterationID == apistructs.UnassignedIterationID {
		streamType = common.ISTChangeIterationFromUnassigned
	} else {
		currentIteration, err = p.db.GetIteration(uint64(currentIterationID))
		if err != nil {
			return streamType, params, err
		}
	}

	// to iteration
	if newIterationID == apistructs.UnassignedIterationID {
		streamType = common.ISTChangeIterationToUnassigned
	} else {
		newIteration, err = p.db.GetIteration(uint64(newIterationID))
		if err != nil {
			return streamType, params, err
		}
	}

	params = common.ISTParam{CurrentIteration: currentIteration.Title, NewIteration: newIteration.Title}

	return streamType, params, nil
}

func formatTime(input interface{}, format ...string) string {
	if reflect.ValueOf(input).IsNil() {
		return ""
	}
	if len(format) > 0 {
		return input.(*time.Time).Format(format[0])
	}
	return input.(*time.Time).Format("2006-01-02")
}

func (p *provider) CreateStream(updateReq *pb.UpdateIssueRequest, streamFields map[string][]interface{}) error {
	defer func() {
		if r := recover(); r != nil {
			logrus.Errorf("[alert] failed to create issueStream when update issue: %v err: %v", updateReq.Id, fmt.Sprintf("%+v", r))
		}
	}()

	id := int64(updateReq.Id)
	eventReq := common.IssueStreamCreateRequest{
		IssueID:      id,
		Operator:     updateReq.IdentityInfo.UserID,
		StreamTypes:  make([]string, 0),
		StreamParams: common.ISTParam{},
	}
	for field, v := range streamFields {
		streamReq := common.IssueStreamCreateRequest{
			IssueID:  id,
			Operator: updateReq.IdentityInfo.UserID,
		}
		switch field {
		case "title":
			streamReq.StreamType = common.ISTChangeTitle
			streamReq.StreamParams = common.ISTParam{CurrentTitle: v[0].(string), NewTitle: v[1].(string)}
		case "state":
			CurrentState, err := p.db.GetIssueStateByID(v[0].(int64))
			if err != nil {
				return err
			}
			NewState, err := p.db.GetIssueStateByID(v[1].(int64))
			if err != nil {
				return err
			}

			streamReq.StreamType = common.ISTTransferState
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTTransferState)
			streamReq.StreamParams = common.ISTParam{CurrentState: CurrentState.Name, NewState: NewState.Name}
			eventReq.StreamParams.CurrentState = CurrentState.Name
			eventReq.StreamParams.NewState = NewState.Name
		case "plan_started_at":
			streamReq.StreamType = common.ISTChangePlanStartedAt
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangePlanStartedAt)
			streamReq.StreamParams = common.ISTParam{
				CurrentPlanStartedAt: formatTime(v[0]),
				NewPlanStartedAt:     formatTime(v[1])}
			eventReq.StreamParams.CurrentPlanStartedAt = formatTime(v[0])
			eventReq.StreamParams.NewPlanStartedAt = formatTime(v[1])
		case "plan_finished_at":
			streamReq.StreamType = common.ISTChangePlanFinishedAt
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangePlanFinishedAt)
			streamReq.StreamParams = common.ISTParam{
				CurrentPlanFinishedAt: formatTime(v[0]),
				NewPlanFinishedAt:     formatTime(v[1])}
			eventReq.StreamParams.CurrentPlanFinishedAt = formatTime(v[0])
			eventReq.StreamParams.NewPlanFinishedAt = formatTime(v[1])
		case "owner":
			userIds := make([]string, 0, len(v))
			for _, uid := range v {
				userIds = append(userIds, uid.(string))
			}
			users, err := p.uc.FindUsers(userIds)
			if err != nil {
				return err
			}
			if len(users) != 2 {
				return errors.Errorf("failed to fetch userInfo when create issue stream, %v", v)
			}
			streamReq.StreamType = common.ISTChangeOwner
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangeOwner)
			streamReq.StreamParams = common.ISTParam{CurrentOwner: users[0].Nick, NewOwner: users[1].Nick}
			eventReq.StreamParams.CurrentOwner = users[0].Nick
			eventReq.StreamParams.NewOwner = users[1].Nick
		case "stage":
			issue, err := p.db.GetIssue(id)
			if err != nil {
				return err
			}
			if issue.Type == pb.IssueTypeEnum_TASK.String() {
				streamReq.StreamType = common.ISTChangeTaskType
			} else if issue.Type == pb.IssueTypeEnum_BUG.String() {
				streamReq.StreamType = common.ISTChangeBugStage
			} else {
				continue
			}
			project, err := p.bdl.GetProject(issue.ProjectID)
			if err != nil {
				return err
			}
			stage, err := p.db.GetIssuesStage(int64(project.OrgID), issue.Type)
			for _, s := range stage {
				if v[0].(string) == s.Value {
					v[0] = s.Name
				}
				if v[1].(string) == s.Value {
					v[1] = s.Name
				}
			}
			streamReq.StreamParams = common.ISTParam{
				CurrentStage: v[0].(string),
				NewStage:     v[1].(string)}
		case "priority":
			streamReq.StreamType = common.ISTChangePriority
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangePriority)
			streamReq.StreamParams = common.ISTParam{
				CurrentPriority: v[0].(string),
				NewPriority:     v[1].(string)}
			eventReq.StreamParams.CurrentPriority = v[0].(string)
			eventReq.StreamParams.NewPriority = v[1].(string)
		case "complexity":
			streamReq.StreamType = common.ISTChangeComplexity
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangeComplexity)
			streamReq.StreamParams = common.ISTParam{
				CurrentComplexity: v[0].(string),
				NewComplexity:     v[1].(string)}
			eventReq.StreamParams.CurrentComplexity = v[0].(string)
			eventReq.StreamParams.NewComplexity = v[1].(string)
		case "severity":
			streamReq.StreamType = common.ISTChangeSeverity
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangeSeverity)
			streamReq.StreamParams = common.ISTParam{
				CurrentSeverity: v[0].(string),
				NewSeverity:     v[1].(string)}
			eventReq.StreamParams.CurrentSeverity = v[0].(string)
			eventReq.StreamParams.NewSeverity = v[1].(string)
		case "content":
			// 不显示修改详情
			streamReq.StreamType = common.ISTChangeContent
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangeContent)
		case "label":
			// 不显示修改详情
			streamReq.StreamType = common.ISTChangeLabel
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangeLabel)
		case "assignee":
			userIds := make([]string, 0, len(v))
			for _, uid := range v {
				userIds = append(userIds, uid.(string))
			}
			users, err := p.uc.FindUsers(userIds)
			if err != nil {
				return err
			}
			if len(users) != 2 {
				return errors.Errorf("failed to fetch userInfo when create issue stream, %v", v)
			}
			streamReq.StreamType = common.ISTChangeAssignee
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangeAssignee)
			streamReq.StreamParams = common.ISTParam{CurrentAssignee: users[0].Nick, NewAssignee: users[1].Nick}
			eventReq.StreamParams.CurrentAssignee = users[0].Nick
			eventReq.StreamParams.NewAssignee = users[1].Nick
		case "iteration_id":
			// streamType, params, err := p.HandleIssueStreamChangeIteration(updateReq.Lang, v[0].(int64), v[1].(int64))
			streamType, params, err := p.HandleIssueStreamChangeIteration(nil, v[0].(int64), v[1].(int64))
			if err != nil {
				return err
			}
			streamReq.StreamType = streamType
			eventReq.StreamTypes = append(eventReq.StreamTypes, streamType)
			streamReq.StreamParams = params
			eventReq.StreamParams.CurrentIteration = params.CurrentIteration
			eventReq.StreamParams.NewIteration = params.NewIteration
		case "man_hour":
			// 工时
			var currentManHour, newManHour pb.IssueManHour
			json.Unmarshal([]byte(v[0].(string)), &currentManHour)
			json.Unmarshal([]byte(v[1].(string)), &newManHour)
			streamReq.StreamType = common.ISTChangeManHour
			eventReq.StreamTypes = append(eventReq.StreamTypes, common.ISTChangeManHour)
			streamReq.StreamParams = common.ISTParam{
				CurrentEstimateTime:  common.GetFormartTime(&currentManHour, "EstimateTime"),
				CurrentElapsedTime:   common.GetFormartTime(&currentManHour, "ElapsedTime"),
				CurrentRemainingTime: common.GetFormartTime(&currentManHour, "RemainingTime"),
				CurrentStartTime:     currentManHour.StartTime,
				CurrentWorkContent:   currentManHour.WorkContent,
				NewEstimateTime:      common.GetFormartTime(&newManHour, "EstimateTime"),
				NewElapsedTime:       common.GetFormartTime(&newManHour, "ElapsedTime"),
				NewRemainingTime:     common.GetFormartTime(&newManHour, "RemainingTime"),
				NewStartTime:         newManHour.StartTime,
				NewWorkContent:       newManHour.WorkContent,
			}
			eventReq.StreamParams.CurrentEstimateTime = common.GetFormartTime(&currentManHour, "EstimateTime")
			eventReq.StreamParams.CurrentElapsedTime = common.GetFormartTime(&currentManHour, "ElapsedTime")
			eventReq.StreamParams.CurrentRemainingTime = common.GetFormartTime(&currentManHour, "RemainingTime")
			eventReq.StreamParams.CurrentStartTime = currentManHour.StartTime
			eventReq.StreamParams.CurrentWorkContent = currentManHour.WorkContent
			eventReq.StreamParams.NewEstimateTime = common.GetFormartTime(&newManHour, "EstimateTime")
			eventReq.StreamParams.NewElapsedTime = common.GetFormartTime(&newManHour, "ElapsedTime")
			eventReq.StreamParams.NewRemainingTime = common.GetFormartTime(&newManHour, "RemainingTime")
			eventReq.StreamParams.NewStartTime = newManHour.StartTime
			eventReq.StreamParams.NewWorkContent = newManHour.WorkContent
		default:
			continue
		}

		// create stream and send issue event
		if _, err := p.Create(&streamReq); err != nil {
			logrus.Errorf("[alert] failed to create issueStream when update issue, req: %+v, err: %v", streamReq, err)
		}
	}
	// send issue create or update event
	go func() {
		if err := p.CreateIssueEvent(&eventReq); err != nil {
			logrus.Errorf("create issue %d event err: %v", eventReq.IssueID, err)
		}
	}()

	return nil
}
