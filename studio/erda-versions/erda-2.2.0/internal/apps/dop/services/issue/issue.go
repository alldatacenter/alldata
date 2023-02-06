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

// Package issue 封装 事件 相关操作
package issue

import (
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

// Issue 事件操作封装
type Issue struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
}

// Option 定义 Issue 配置选项
type Option func(*Issue)

// New 新建 Issue 实例
func New(options ...Option) *Issue {
	itr := &Issue{}
	for _, op := range options {
		op(itr)
	}
	return itr
}

// WithDBClient 配置 Issue 数据库选项
func WithDBClient(db *dao.DBClient) Option {
	return func(issue *Issue) {
		issue.db = db
	}
}

// WithBundle 配置 bdl
func WithBundle(bdl *bundle.Bundle) Option {
	return func(issue *Issue) {
		issue.bdl = bdl
	}
}

// Paging 分页查询事件
func (svc *Issue) Paging(req apistructs.IssuePagingRequest) ([]apistructs.Issue, uint64, error) {
	// 请求校验
	if req.ProjectID == 0 {
		return nil, 0, apierrors.ErrPagingIssues.MissingParameter("projectID")
	}
	// 待办事项允许迭代id为-1即只能看未纳入迭代的事项，默认按照优先级排序
	if (req.IterationID == -1 || (len(req.IterationIDs) == 1 && req.IterationIDs[0] == -1)) && req.OrderBy == "" {
		// req.Type = apistructs.IssueTypeRequirement
		req.OrderBy = "FIELD(priority, 'LOW', 'NORMAL', 'HIGH', 'URGENT')"
	}
	if req.IterationID != 0 {
		req.IterationIDs = append(req.IterationIDs, req.IterationID)
	}

	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 20
	}

	var (
		labelRelationIDs, issueRelationIDs []int64
		isLabel, isIssue                   bool
	)
	if len(req.Label) > 0 {
		isLabel = true
		// 获取标签关联关系
		lrs, err := svc.db.GetLabelRelationsByLabels(apistructs.LabelTypeIssue, req.Label)
		if err != nil {
			return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
		}
		for _, v := range lrs {
			id, err := strconv.ParseInt(v.RefID, 10, 64)
			if err != nil {
				logrus.Errorf("failed to parse refID for label relation %d, %v", v.ID, err)
				continue
			}
			labelRelationIDs = append(labelRelationIDs, id)
		}
	}
	if len(req.RelatedIssueIDs) > 0 {
		isIssue = true
		// 获取事件关联关系
		irs, err := svc.db.GetIssueRelationsByIDs(req.RelatedIssueIDs, []string{apistructs.IssueRelationConnection})
		if err != nil {
			return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
		}
		for _, v := range irs {
			issueRelationIDs = append(issueRelationIDs, int64(v.RelatedIssue))
		}
	}
	if isLabel || isIssue {
		req.IDs = strutil.DedupInt64Slice(append(getRelatedIDs(labelRelationIDs, issueRelationIDs, isLabel, isIssue), req.IDs...))
	}

	// 该项目下全部的state信息，之后不再查询state  key: stateID value:state
	// stateMap := make(map[int64]dao.IssueState)
	// 根据主状态过滤
	// if len(req.StateBelongs) > 0 {
	// 	err := svc.FilterByStateBelong(stateMap, &req)
	// 	if err != nil {
	// 		return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
	// 	}
	// }
	// 分页
	issueModels, total, err := svc.db.PagingIssues(req, isLabel || isIssue)
	if err != nil {
		return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
	}

	issues, err := svc.BatchConvert(issueModels, req.Type, req.IdentityInfo)
	if err != nil {
		return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
	}

	// issue 填充需求标题
	requirementIDs := make([]int64, 0, len(issues))
	for _, v := range issues {
		if v.RequirementID != nil && *v.RequirementID > 0 {
			requirementIDs = append(requirementIDs, *v.RequirementID)
		}
	}
	if len(requirementIDs) > 0 {
		requirements, err := svc.db.ListIssueByIDs(requirementIDs)
		if err != nil {
			return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
		}
		requirementTitleMap := make(map[int64]string, len(requirements))
		for _, r := range requirements {
			requirementTitleMap[int64(r.ID)] = r.Title
		}
		for i, v := range issues {
			if v.RequirementID != nil && *v.RequirementID > 0 {
				issues[i].RequirementTitle = requirementTitleMap[(*v.RequirementID)]
			}
		}
	}

	// 需求进度统计
	if req.WithProcessSummary {
		stateBelongMap := make(map[int64]apistructs.IssueStateBelong)
		stateBelong, err := svc.db.GetIssuesStatesByProjectID(req.ProjectID, "")
		if err != nil {
			return nil, 0, err
		}
		for _, v := range stateBelong {
			stateBelongMap[int64(v.ID)] = v.Belong
		}
		for _, t := range req.Type {
			if t == apistructs.IssueTypeRequirement || t == apistructs.IssueTypeEpic {
				requirementRelateIssueIDsMap := make(map[uint64][]uint64)
				issueIndex := make(map[uint64]int)
				// 获取所有的需求id
				var requirementIDs []uint64
				for i, v := range issues {
					if v.Type == apistructs.IssueTypeRequirement || t == apistructs.IssueTypeEpic {
						id := uint64(v.ID)
						issueIndex[id] = i
						requirementIDs = append(requirementIDs, id)
					}
				}
				relationTypes := []string{apistructs.IssueRelationInclusion}
				if t == apistructs.IssueTypeRequirement {
					relationTypes = []string{apistructs.IssueRelationInclusion}
				}
				// 获取需求id对应的关联事件ids
				relations, err := svc.db.GetIssueRelationsByIDs(requirementIDs, relationTypes)
				if err != nil {
					return nil, 0, err
				}
				for _, v := range relations {
					requirementRelateIssueIDsMap[v.IssueID] = append(requirementRelateIssueIDsMap[v.IssueID], v.RelatedIssue)
				}

				// 获取每个需求的IssueSummary
				for requirementID, relatedIDs := range requirementRelateIssueIDsMap {
					reqResult, err := svc.db.IssueStateCount2(relatedIDs)
					if err != nil {
						return nil, 0, err
					}

					var sum apistructs.IssueSummary
					for _, v := range reqResult {
						if stateBelongMap[v.State] == apistructs.IssueStateBelongDone || stateBelongMap[v.State] == apistructs.IssueStateBelongClosed {
							sum.DoneCount++
						} else {
							sum.ProcessingCount++
						}
					}

					issues[issueIndex[requirementID]].IssueSummary = &sum
				}
			}
		}
	}

	return issues, total, nil
}

// GetIssue 获取事件
func (svc *Issue) GetIssue(req apistructs.IssueGetRequest) (*apistructs.Issue, error) {
	// 请求校验
	if req.ID == 0 {
		return nil, apierrors.ErrGetIssue.MissingParameter("id")
	}
	// 查询事件
	model, err := svc.db.GetIssue(int64(req.ID))
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, apierrors.ErrGetIssue.NotFound()
		}
		return nil, apierrors.ErrGetIssue.InternalError(err)
	}
	issue, err := svc.Convert(model, req.IdentityInfo)
	if err != nil {
		return nil, apierrors.ErrGetIssue.InternalError(err)
	}
	return issue, nil
}

// get all undone issues by orgid
func (svc *Issue) PagingForWorkbench(req apistructs.IssuePagingRequest) ([]apistructs.Issue, uint64, error) {
	if (req.IterationID == -1 || (len(req.IterationIDs) == 1 && req.IterationIDs[0] == -1)) && req.OrderBy == "" {
		req.OrderBy = "FIELD(priority, 'LOW', 'NORMAL', 'HIGH', 'URGENT')"
	}
	if req.IterationID != 0 {
		req.IterationIDs = append(req.IterationIDs, req.IterationID)
	}

	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 20
	}

	var (
		labelRelationIDs, issueRelationIDs []int64
		isLabel, isIssue                   bool
	)
	if len(req.Label) > 0 {
		isLabel = true
		lrs, err := svc.db.GetLabelRelationsByLabels(apistructs.LabelTypeIssue, req.Label)
		if err != nil {
			return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
		}
		for _, v := range lrs {
			id, err := strconv.ParseInt(v.RefID, 10, 64)
			if err != nil {
				logrus.Errorf("failed to parse refID for label relation %d, %v", v.ID, err)
				continue
			}
			labelRelationIDs = append(labelRelationIDs, id)
		}
	}
	if len(req.RelatedIssueIDs) > 0 {
		isIssue = true
		irs, err := svc.db.GetIssueRelationsByIDs(req.RelatedIssueIDs, []string{apistructs.IssueRelationConnection})
		if err != nil {
			return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
		}
		for _, v := range irs {
			issueRelationIDs = append(issueRelationIDs, int64(v.RelatedIssue))
		}
	}
	if isLabel || isIssue {
		req.IDs = strutil.DedupInt64Slice(append(getRelatedIDs(labelRelationIDs, issueRelationIDs, isLabel, isIssue), req.IDs...))
	}

	// state  key: stateID value:state
	stateMap := make(map[int64]dao.IssueState)
	// filter by state
	if len(req.StateBelongs) > 0 {
		err := svc.FilterByStateBelong(stateMap, &req)
		if err != nil {
			return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
		}
	}
	// paging
	issueModels, total, err := svc.db.PagingIssues(req, isLabel || isIssue)
	if err != nil {
		return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
	}

	issues, err := svc.BatchConvert(issueModels, req.Type, req.IdentityInfo)
	if err != nil {
		return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
	}

	return issues, total, nil
}

type issueUpdated struct {
	id                      uint64
	stateOld                apistructs.IssueStateBelong
	stateNew                apistructs.IssueStateBelong
	planStartedAt           *time.Time
	planFinishedAt          *time.Time
	iterationID             int64
	iterationOld            int64
	projectID               uint64
	updateChildrenIteration bool
	withIteration           bool
}

// getRelatedIDs 当同时通过lable和issue关联关系过滤时，需要取交集
func getRelatedIDs(lableRelationIDs []int64, issueRelationIDs []int64, isLabel, isIssue bool) []int64 {
	// 取交集
	if isLabel && isIssue {
		return strutil.IntersectionInt64Slice(lableRelationIDs, issueRelationIDs)
	}

	if isLabel {
		return lableRelationIDs
	}

	if isIssue {
		return issueRelationIDs
	}

	return nil
}

// FilterByStateBelong 根据主状态过滤
func (svc *Issue) FilterByStateBelong(stateMap map[int64]dao.IssueState, req *apistructs.IssuePagingRequest) error {
	var states []int64
	belongMap := make(map[apistructs.IssueStateBelong]bool)
	for _, v := range req.StateBelongs {
		belongMap[v] = true
	}
	// 获取主状态下的子状态
	projectStates, err := svc.db.GetIssuesStatesByProjectID(req.ProjectID, "")

	if err != nil {
		return err
	}
	for _, s := range projectStates {
		stateMap[int64(s.ID)] = s
		if belongMap[s.Belong] {
			states = append(states, int64(s.ID))
		}
	}

	if len(req.State) == 0 {
		req.State = states
	} else {
		var newState []int64
		for _, v := range states {
			for _, vv := range req.State {
				if v == vv {
					newState = append(newState, vv)
					break
				}
			}
		}
		req.State = newState
	}
	return nil
}

// StateCheckPermission 事件状态Button鉴权
func (svc *Issue) StateCheckPermission(req *apistructs.PermissionCheckRequest, st int64, ed int64) (bool, error) {
	logrus.Debugf("invoke permission, time: %s, req: %+v", time.Now().Format(time.RFC3339), req)
	// 是否是内部服务账号
	resp, err := svc.bdl.StateCheckPermission(req)
	if err != nil {
		return false, err
	}
	if resp.Access {
		return true, nil
	}
	for _, role := range resp.Roles {
		rp, err := svc.db.GetIssueStatePermission(role, st, ed)
		if err != nil {
			return false, err
		}
		if rp != nil {
			return true, nil
		}
	}
	return false, nil
}

func (svc *Issue) GetIssuesByStates(req apistructs.WorkbenchRequest) (map[uint64]*apistructs.WorkbenchProjectItem, error) {
	stats, err := svc.db.GetIssueExpiryStatusByProjects(req)
	if err != nil {
		return nil, err
	}

	projectMap := make(map[uint64]*apistructs.WorkbenchProjectItem)
	for _, i := range stats {
		if _, ok := projectMap[i.ProjectID]; !ok {
			projectMap[i.ProjectID] = &apistructs.WorkbenchProjectItem{}
		}
		item := projectMap[i.ProjectID]
		num := int(i.IssueNum)
		switch i.ExpiryStatus {
		case dao.ExpireTypeUndefined:
			item.UnSpecialIssueNum = num
		case dao.ExpireTypeExpired:
			item.ExpiredIssueNum = num
		case dao.ExpireTypeExpireIn1Day:
			item.ExpiredOneDayNum = num
		case dao.ExpireTypeExpireIn2Days:
			item.ExpiredTomorrowNum = num
		case dao.ExpireTypeExpireIn7Days:
			item.ExpiredSevenDayNum = num
		case dao.ExpireTypeExpireIn30Days:
			item.ExpiredThirtyDayNum = num
		case dao.ExpireTypeExpireInFuture:
			item.FeatureDayNum = num
		}
		item.TotalIssueNum += num
	}

	return projectMap, nil
}
