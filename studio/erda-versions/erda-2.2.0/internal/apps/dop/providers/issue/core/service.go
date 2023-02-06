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
	"context"
	"fmt"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	stream "github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/core"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/testcase"
	mttestplan "github.com/erda-project/erda/internal/apps/dop/services/testplan"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

type IssueService struct {
	logger logs.Logger

	db            *dao.DBClient
	bdl           *bundle.Bundle
	uc            *ucauth.UCClient
	stream        core.Interface
	query         query.Interface
	mttestPlan    *mttestplan.TestPlan
	testcase      *testcase.Service
	ExportChannel chan uint64
	ImportChannel chan uint64
}

func (i *IssueService) WithUc(uc *ucauth.UCClient) {
	i.uc = uc
}

func (i *IssueService) WithTestplan(testPlan *mttestplan.TestPlan) {
	i.mttestPlan = testPlan
}

func (i *IssueService) WithTestcase(testcase *testcase.Service) {
	i.testcase = testcase
}

func (i *IssueService) WithChannel(export, im chan uint64) {
	i.ExportChannel = export
	i.ImportChannel = im
}

func (i *IssueService) CreateIssue(ctx context.Context, req *pb.IssueCreateRequest) (*pb.IssueCreateResponse, error) {
	req.IdentityInfo = apis.GetIdentityInfo(ctx)
	if req.IdentityInfo == nil {
		return nil, apierrors.ErrCreateIssue.NotLogin()
	}
	if !apis.IsInternalClient(ctx) {
		req.External = true
	}

	if req.Type == pb.IssueTypeEnum_BUG {
		req.Owner = req.Assignee
	}
	if req.ProjectID == 0 {
		return nil, apierrors.ErrCreateIssue.MissingParameter("projectID")
	}
	if req.Title == "" {
		return nil, apierrors.ErrCreateIssue.MissingParameter("title")
	}
	// 不归属任何迭代时，IterationID=-1
	if req.IterationID == 0 {
		return nil, apierrors.ErrCreateIssue.MissingParameter("iterationID")
	}
	// 工单允许处理人为空
	if req.Assignee == "" && req.Type != pb.IssueTypeEnum_TICKET {
		return nil, apierrors.ErrCreateIssue.MissingParameter("assignee")
	}
	// 显式指定了创建人，则覆盖
	if req.Creator != "" {
		req.IdentityInfo.UserID = req.Creator
	}
	planStartedAt, planFinishedAt := common.ToIssueTime(req.PlanStartedAt), common.ToIssueTime(req.PlanFinishedAt)
	// 初始状态为排序级最高的状态
	initState, err := i.db.GetIssuesStatesByProjectID(req.ProjectID, req.Type.String())
	if err != nil {
		return nil, err
	}
	if len(initState) == 0 {
		return nil, apierrors.ErrCreateIssue.InvalidParameter("缺少默认事件状态")
	}
	now := time.Date(time.Now().Year(), time.Now().Month(), time.Now().Day(), 0, 0, 0, 0, time.Now().Location())
	// 创建 issue
	create := dao.Issue{
		PlanStartedAt:  planStartedAt,
		PlanFinishedAt: planFinishedAt,
		ProjectID:      req.ProjectID,
		IterationID:    req.IterationID,
		AppID:          req.AppID,
		Type:           req.Type.String(),
		Title:          req.Title,
		Content:        req.Content,
		State:          int64(initState[0].ID),
		Priority:       req.Priority.String(),
		Complexity:     req.Complexity.String(),
		Severity:       req.Severity.String(),
		Creator:        req.IdentityInfo.UserID,
		Assignee:       req.Assignee,
		Source:         req.Source,
		ManHour:        common.GetDBManHour(req.IssueManHour),
		External:       req.External,
		Stage:          getStage(req),
		Owner:          req.Owner,
		ExpiryStatus:   dao.GetExpiryStatus(planFinishedAt, now),
	}
	if err := i.db.CreateIssue(&create); err != nil {
		return nil, apierrors.ErrCreateIssue.InternalError(err)
	}

	// create subscribers
	issueID := int64(create.ID)
	req.Subscribers = append(req.Subscribers, create.Creator)
	req.Subscribers = strutil.DedupSlice(req.Subscribers)
	var subscriberModels []dao.IssueSubscriber
	for _, v := range req.Subscribers {
		subscriberModels = append(subscriberModels, dao.IssueSubscriber{IssueID: issueID, UserID: v})
	}
	if err := i.db.BatchCreateIssueSubscribers(subscriberModels); err != nil {
		return nil, apierrors.ErrCreateIssue.InternalError(err)
	}

	// 生成活动记录
	users, err := i.uc.FindUsers([]string{req.IdentityInfo.UserID})
	if err != nil {
		return nil, err
	}
	if len(users) != 1 {
		return nil, errors.Errorf("not found user info")
	}
	streamReq := stream.IssueStreamCreateRequest{
		IssueID:      int64(create.ID),
		Operator:     req.IdentityInfo.UserID,
		StreamType:   stream.ISTCreate,
		StreamParams: stream.ISTParam{UserName: users[0].Nick},
	}
	// create stream and send issue create event
	if _, err := i.stream.Create(&streamReq); err != nil {
		return nil, err
	}

	// create issue state transition
	if err = i.db.CreateIssueStateTransition(&dao.IssueStateTransition{
		ProjectID: create.ProjectID,
		IssueID:   create.ID,
		StateFrom: 0,
		StateTo:   uint64(create.State),
		Creator:   create.Creator,
	}); err != nil {
		return nil, err
	}

	u := &query.IssueUpdated{
		Id:             create.ID,
		IterationID:    req.IterationID,
		PlanStartedAt:  planStartedAt,
		PlanFinishedAt: planFinishedAt,
	}

	if err := i.query.AfterIssueUpdate(u); err != nil {
		return nil, fmt.Errorf("after issue update failed when issue id: %v create, err: %v", issueID, err)
	}

	go func() {
		if err := i.stream.CreateIssueEvent(&streamReq); err != nil {
			logrus.Errorf("create issue %d event err: %v", streamReq.IssueID, err)
		}
	}()

	if err := i.bdl.UpdateProjectActiveTime(apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  req.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	// 关联 测试计划用例
	if len(req.TestPlanCaseRelIDs) > 0 {
		// 批量查询测试计划用例
		testPlanCaseRels, err := i.mttestPlan.ListTestPlanCaseRels(apistructs.TestPlanCaseRelListRequest{IDs: req.TestPlanCaseRelIDs})
		if err != nil {
			return nil, err
		}
		var issueCaseRels []dao.IssueTestCaseRelation
		for _, rel := range testPlanCaseRels {
			issueCaseRels = append(issueCaseRels, dao.IssueTestCaseRelation{
				IssueID:           create.ID,
				TestPlanID:        rel.TestPlanID,
				TestPlanCaseRelID: rel.ID,
				TestCaseID:        rel.TestCaseID,
				CreatorID:         req.IdentityInfo.UserID,
			})
		}
		if err := i.db.BatchCreateIssueTestCaseRelations(issueCaseRels); err != nil {
			return nil, apierrors.ErrBatchCreateIssueTestCaseRel.InternalError(err)
		}
	}

	labels, err := i.bdl.ListLabelByNameAndProjectID(create.ProjectID, req.Labels)
	if err != nil {
		return nil, apierrors.ErrCreateIssue.InternalError(err)
	}
	for _, v := range labels {
		lr := &dao.LabelRelation{
			LabelID: uint64(v.ID),
			RefType: apistructs.LabelTypeIssue,
			RefID:   strconv.FormatUint(create.ID, 10),
		}
		if err := i.db.CreateLabelRelation(lr); err != nil {
			return nil, apierrors.ErrCreateIssue.InternalError(err)
		}
	}

	return &pb.IssueCreateResponse{
		Data: create.ID,
	}, nil
}

func getStage(req *pb.IssueCreateRequest) string {
	var stage string
	if req.Type == pb.IssueTypeEnum_TASK {
		stage = req.TaskType
	} else if req.Type == pb.IssueTypeEnum_BUG {
		stage = req.BugStage
	}
	return stage
}

func (i *IssueService) PagingIssue(ctx context.Context, req *pb.PagingIssueRequest) (*pb.PagingIssueResponse, error) {
	switch req.OrderBy {
	case "":
	case "planStartedAt":
		req.OrderBy = "plan_started_at"
	case "planFinishedAt":
		req.OrderBy = "plan_finished_at"
	case "assignee":
		req.OrderBy = "assignee"
	case "updatedAt", "updated_at":
		req.OrderBy = "updated_at"
	default:
		return nil, apierrors.ErrPagingIssues.InvalidParameter("orderBy")
	}

	req.IdentityInfo = apis.GetIdentityInfo(ctx)
	if req.IdentityInfo == nil {
		return nil, apierrors.ErrPagingIssues.NotLogin()
	}
	if !apis.IsInternalClient(ctx) {
		req.External = true
	}

	issues, total, err := i.query.Paging(*req)
	if err != nil {
		return nil, apierrors.ErrPagingIssues.InternalError(err)
	}
	// userIDs
	userIDs := common.GetUserIDs(req)
	for _, issue := range issues {
		userIDs = append(userIDs, issue.Creator, issue.Assignee, issue.Owner)
	}
	userIDs = strutil.DedupSlice(userIDs, true)

	return &pb.PagingIssueResponse{
		Data: &pb.IssuePagingResponseData{
			Total: total,
			List:  issues,
		},
		UserIDs: userIDs,
	}, nil
}

func (i *IssueService) GetIssue(ctx context.Context, req *pb.GetIssueRequest) (*pb.GetIssueResponse, error) {
	id, err := strconv.Atoi(req.Id)
	if err != nil {
		return nil, err
	}
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrGetIssue.NotLogin()
	}
	issue, err := i.query.GetIssue(int64(id), identityInfo)
	if err != nil {
		return nil, err
	}
	rels, err := i.GetTestPlanCaseRels(uint64(issue.Id))
	if err != nil {
		return nil, apierrors.ErrGetIssue.InternalError(err)
	}
	issue.TestPlanCaseRels = rels
	if !apis.IsInternalClient(ctx) {
		access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  issue.ProjectID,
			Resource: apistructs.ProjectResource,
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, apierrors.ErrGetIssue.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrGetIssue.AccessDenied()
		}
	}
	return &pb.GetIssueResponse{
		Data:    issue,
		UserIDs: strutil.DedupSlice(append(issue.Subscribers, issue.Creator, issue.Assignee, issue.Owner), true),
	}, nil
}

func (i *IssueService) UpdateIssue(ctx context.Context, req *pb.UpdateIssueRequest) (*pb.UpdateIssueResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrUpdateIssue.NotLogin()
	}
	req.IdentityInfo = identityInfo
	id := req.Id
	issueModel, err := i.db.GetIssue(int64(id))
	if err != nil {
		return nil, apierrors.ErrUpdateIssue.InvalidParameter(err)
	}
	// 鉴权
	if !apis.IsInternalClient(ctx) {
		if identityInfo.UserID != issueModel.Creator && identityInfo.UserID != issueModel.Assignee {
			access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.ProjectScope,
				ScopeID:  issueModel.ProjectID,
				Resource: "issue-" + strings.ToLower(issueModel.Type),
				Action:   apistructs.UpdateAction,
			})
			if err != nil {
				return nil, apierrors.ErrUpdateIssue.InternalError(err)
			}
			if !access.Access {
				return nil, apierrors.ErrUpdateIssue.AccessDenied()
			}
		}
	}

	if err := i.query.UpdateIssue(req); err != nil {
		return nil, apierrors.ErrUpdateIssue.InternalError(err)
	}

	// 更新项目活跃时间
	if err := i.bdl.UpdateProjectActiveTime(apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  issueModel.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
		return nil, apierrors.ErrUpdateIssue.InternalError(err)
	}

	// 更新 关联测试计划用例
	if len(req.TestPlanCaseRelIDs) > 0 && !req.RemoveTestPlanCaseRelIDs {
		// 批量查询测试计划用例
		testPlanCaseRels, err := i.mttestPlan.ListTestPlanCaseRels(apistructs.TestPlanCaseRelListRequest{IDs: req.TestPlanCaseRelIDs})
		if err != nil {
			return nil, apierrors.ErrUpdateIssue.InternalError(err)
		}
		// 批量删除原有关联
		if err := i.db.DeleteIssueTestCaseRelationsByIssueID(id); err != nil {
			return nil, apierrors.ErrUpdateIssue.InternalError(err)
		}
		// 批量创建关联
		var issueCaseRels []dao.IssueTestCaseRelation
		for _, rel := range testPlanCaseRels {
			issueCaseRels = append(issueCaseRels, dao.IssueTestCaseRelation{
				IssueID:           id,
				TestPlanID:        rel.TestPlanID,
				TestPlanCaseRelID: rel.ID,
				TestCaseID:        rel.TestCaseID,
				CreatorID:         req.IdentityInfo.UserID,
			})
		}
		if err := i.db.BatchCreateIssueTestCaseRelations(issueCaseRels); err != nil {
			return nil, apierrors.ErrUpdateIssue.InternalError(err)
		}
	}
	// 批量删除原有关联
	if req.RemoveTestPlanCaseRelIDs {
		if err := i.db.DeleteIssueTestCaseRelationsByIssueID(id); err != nil {
			return nil, apierrors.ErrUpdateIssue.InternalError(err)
		}
	}

	issue, err := i.query.GetIssue(int64(id), identityInfo)
	if err != nil {
		return nil, apierrors.ErrUpdateIssue.InternalError(err)
	}
	rels, err := i.GetTestPlanCaseRels(id)
	if err != nil {
		return nil, apierrors.ErrUpdateIssue.InternalError(err)
	}
	issue.TestPlanCaseRels = rels
	currentLabelMap := make(map[string]bool)
	newLabelMap := make(map[string]bool)
	for _, v := range issue.Labels {
		currentLabelMap[v] = true
	}
	for _, v := range req.Labels {
		newLabelMap[v] = true
	}
	if reflect.DeepEqual(currentLabelMap, newLabelMap) == false {
		if err := i.query.UpdateLabels(id, issueModel.ProjectID, req.Labels); err != nil {
			return nil, apierrors.ErrUpdateIssue.InternalError(err)
		}
		// 生成活动记录
		// issueStreamFields 保存字段更新前后的值，用于生成活动记录
		issueStreamFields := make(map[string][]interface{})
		issueStreamFields["label"] = []interface{}{"1", "2"}
		_ = i.stream.CreateStream(req, issueStreamFields)
	}

	return &pb.UpdateIssueResponse{Data: issueModel.ID}, nil
}

func (i *IssueService) DeleteIssue(ctx context.Context, req *pb.DeleteIssueRequest) (*pb.DeleteIssueResponse, error) {
	id, err := strconv.ParseUint(req.Id, 10, 64)
	if err != nil {
		return nil, apierrors.ErrDeleteIssue.InvalidParameter(err)
	}

	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrUpdateIssue.NotLogin()
	}

	issue, err := i.query.GetIssue(int64(id), identityInfo)
	if err != nil {
		return nil, err
	}
	rels, err := i.GetTestPlanCaseRels(uint64(issue.Id))
	if err != nil {
		return nil, apierrors.ErrDeleteIssue.InternalError(err)
	}
	issue.TestPlanCaseRels = rels

	if !apis.IsInternalClient(ctx) {
		if identityInfo.UserID != issue.Creator && identityInfo.UserID != issue.Assignee {
			access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.ProjectScope,
				ScopeID:  issue.ProjectID,
				Resource: "issue-" + strings.ToLower(issue.Type.String()),
				Action:   apistructs.DeleteAction,
			})
			if err != nil {
				return nil, apierrors.ErrDeleteIssue.InternalError(err)
			}
			if !access.Access {
				return nil, apierrors.ErrDeleteIssue.AccessDenied()
			}
		}
	}

	// 删除史诗前判断是否关联了事件
	if issue.Type == pb.IssueTypeEnum_EPIC {
		relatingIssueIDs, err := i.db.GetRelatingIssues(uint64(id), []string{apistructs.IssueRelationConnection})
		if err != nil {
			return nil, err
		}
		if len(relatingIssueIDs) > 0 {
			return nil, apierrors.ErrDeleteIssue.InvalidState("史诗下关联了事件,不可删除")
		}
	}

	if err := i.db.CleanIssueRelation(id); err != nil {
		return nil, apierrors.ErrDeleteIssue.InternalError(err)
	}
	// 删除自定义字段
	if err := i.db.DeletePropertyRelationByIssueID(int64(id)); err != nil {
		return nil, apierrors.ErrDeleteIssue.InternalError(err)
	}
	// 删除测试计划用例关联
	if issue.Type == pb.IssueTypeEnum_BUG {
		if err := i.db.DeleteIssueTestCaseRelationsByIssueIDs([]uint64{id}); err != nil {
			return nil, apierrors.ErrDeleteIssue.InternalError(err)
		}
	}
	// delete issue state transition
	if err = i.db.DeleteIssuesStateTransition(id); err != nil {
		return nil, apierrors.ErrDeleteIssue.InternalError(err)
	}

	if err = i.db.DeleteIssue(id); err != nil {
		return nil, err
	}

	if err := i.bdl.UpdateProjectActiveTime(apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  issue.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	return &pb.DeleteIssueResponse{Data: issue}, nil
}

func (i *IssueService) GetTestPlanCaseRels(issueID uint64) ([]*pb.TestPlanCaseRel, error) {
	// 查询关联的测试计划用例
	testPlanCaseRels := make([]*pb.TestPlanCaseRel, 0)
	issueTestCaseRels, err := i.db.ListIssueTestCaseRelations(apistructs.IssueTestCaseRelationsListRequest{IssueID: issueID})
	if err != nil {
		return nil, err
	}
	if len(issueTestCaseRels) > 0 {
		var relIDs []uint64
		for _, issueCaseRel := range issueTestCaseRels {
			relIDs = append(relIDs, issueCaseRel.TestPlanCaseRelID)
		}
		relIDs = strutil.DedupUint64Slice(relIDs, true)
		rels, err := i.mttestPlan.ListTestPlanCaseRels(apistructs.TestPlanCaseRelListRequest{IDs: relIDs})
		if err != nil {
			return nil, err
		}

		for _, rel := range rels {
			apiCount := &pb.TestCaseAPICount{
				Total:   rel.APICount.Total,
				Created: rel.APICount.Created,
				Running: rel.APICount.Running,
				Passed:  rel.APICount.Passed,
				Failed:  rel.APICount.Failed,
			}
			testPlanCaseRels = append(testPlanCaseRels, &pb.TestPlanCaseRel{
				Id:         rel.ID,
				Name:       rel.Name,
				Priority:   string(rel.Priority),
				TestPlanID: rel.TestPlanID,
				TestSetID:  rel.TestSetID,
				TestCaseID: rel.TestCaseID,
				ExecStatus: string(rel.ExecStatus),
				Creator:    rel.CreatorID,
				CreatedAt:  timestamppb.New(rel.CreatedAt),
				UpdaterID:  rel.UpdaterID,
				ExecutorID: rel.ExecutorID,
				UpdatedAt:  timestamppb.New(rel.UpdatedAt),
				APICount:   apiCount,
			})
		}
	}
	return testPlanCaseRels, nil
}

func (i *IssueService) BatchUpdateIssue(ctx context.Context, req *pb.BatchUpdateIssueRequest) (*pb.BatchUpdateIssueResponse, error) {
	if req.ProjectID == 0 {
		return nil, apierrors.ErrBatchUpdateIssue.MissingParameter("projectID")
	}
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrBatchUpdateIssue.NotLogin()
	}
	req.IdentityInfo = identityInfo

	if req.CurrentIterationID != 0 {
		req.CurrentIterationIDs = append(req.CurrentIterationIDs, req.CurrentIterationID)
	}
	req.CurrentIterationIDs = strutil.DedupInt64Slice(req.CurrentIterationIDs)
	if err := i.query.BatchUpdateIssue(req); err != nil {
		return nil, err
	}
	if err := i.bdl.UpdateProjectActiveTime(apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  req.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}
	return &pb.BatchUpdateIssueResponse{}, nil
}

func (i *IssueService) UpdateIssueType(ctx context.Context, req *pb.UpdateIssueTypeRequest) (*pb.UpdateIssueTypeResponse, error) {
	identity := apis.GetIdentityInfo(ctx)
	if !apis.IsInternalClient(ctx) {
		access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identity.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(req.ProjectID),
			Resource: apistructs.IssueTypeResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return nil, apierrors.ErrBatchUpdateIssue.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrBatchUpdateIssue.AccessDenied()
		}
	}
	issueModel, err := i.db.GetIssue(req.Id)
	if err != nil {
		return nil, err
	}
	states, err := i.db.GetIssuesStatesByProjectID(uint64(req.ProjectID), req.Type.String())
	if err != nil {
		return nil, err
	}
	if len(states) == 0 {
		return nil, apierrors.ErrUpdateIssue.InvalidParameter("该类型缺少默认事件状态")
	}
	issueModel.Type = req.Type.String()
	issueModel.State = int64(states[0].ID)
	if issueModel.Type == pb.IssueTypeEnum_REQUIREMENT.String() {
		issueModel.Stage = ""
		issueModel.Owner = ""
	} else if issueModel.Type == pb.IssueTypeEnum_BUG.String() {
		issueModel.Stage = "codeDevelopment"
		issueModel.Owner = issueModel.Assignee
	} else if issueModel.Type == pb.IssueTypeEnum_TASK.String() {
		issueModel.Stage = "dev"
		issueModel.Owner = issueModel.Assignee
	}
	err = i.db.UpdateIssueType(&issueModel)
	if err != nil {
		return nil, err
	}

	// 删除原有类型配置的自定义字段
	if err := i.db.DeletePropertyRelationByIssueID(int64(issueModel.ID)); err != nil {
		return nil, err
	}

	return &pb.UpdateIssueTypeResponse{Data: int64(issueModel.ID)}, nil
}
