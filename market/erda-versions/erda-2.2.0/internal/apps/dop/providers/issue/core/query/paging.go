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
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

func (p *provider) Paging(req pb.PagingIssueRequest) ([]*pb.Issue, uint64, error) {
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
		lrs, err := p.db.GetLabelRelationsByLabels(apistructs.LabelTypeIssue, req.Label)
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
	if len(req.RelatedIssueId) > 0 {
		isIssue = true
		// 获取事件关联关系
		irs, err := p.db.GetIssueRelationsByIDs(req.RelatedIssueId, []string{apistructs.IssueRelationConnection})
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
	issueModels, total, err := p.db.PagingIssues(req, isLabel || isIssue)
	if err != nil {
		return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
	}

	issues, err := p.BatchConvert(issueModels, req.Type)
	if err != nil {
		return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
	}

	// issue 填充需求标题
	requirementIDs := make([]int64, 0, len(issues))
	for _, v := range issues {
		if v.RequirementID > 0 {
			requirementIDs = append(requirementIDs, v.RequirementID)
		}
	}
	if len(requirementIDs) > 0 {
		requirements, err := p.db.ListIssueByIDs(requirementIDs)
		if err != nil {
			return nil, 0, apierrors.ErrPagingIssues.InternalError(err)
		}
		requirementTitleMap := make(map[int64]string, len(requirements))
		for _, r := range requirements {
			requirementTitleMap[int64(r.ID)] = r.Title
		}
		for i, v := range issues {
			if v.RequirementID > 0 {
				issues[i].RequirementTitle = requirementTitleMap[(v.RequirementID)]
			}
		}
	}

	// 需求进度统计
	if req.WithProcessSummary {
		stateBelongMap := make(map[int64]string)
		stateBelong, err := p.db.GetIssuesStatesByProjectID(req.ProjectID, "")
		if err != nil {
			return nil, 0, err
		}
		for _, v := range stateBelong {
			stateBelongMap[int64(v.ID)] = v.Belong
		}
		for _, t := range req.Type {
			if t == pb.IssueTypeEnum_REQUIREMENT.String() || t == pb.IssueTypeEnum_EPIC.String() {
				requirementRelateIssueIDsMap := make(map[uint64][]uint64)
				issueIndex := make(map[uint64]int)
				// 获取所有的需求id
				var requirementIDs []uint64
				for i, v := range issues {
					if v.Type == pb.IssueTypeEnum_REQUIREMENT || t == pb.IssueTypeEnum_EPIC.String() {
						id := uint64(v.Id)
						issueIndex[id] = i
						requirementIDs = append(requirementIDs, id)
					}
				}
				relationTypes := []string{apistructs.IssueRelationInclusion}
				if t == pb.IssueTypeEnum_REQUIREMENT.String() {
					relationTypes = []string{apistructs.IssueRelationInclusion}
				}
				// 获取需求id对应的关联事件ids
				relations, err := p.db.GetIssueRelationsByIDs(requirementIDs, relationTypes)
				if err != nil {
					return nil, 0, err
				}
				for _, v := range relations {
					requirementRelateIssueIDsMap[v.IssueID] = append(requirementRelateIssueIDsMap[v.IssueID], v.RelatedIssue)
				}

				// 获取每个需求的IssueSummary
				for requirementID, relatedIDs := range requirementRelateIssueIDsMap {
					reqResult, err := p.db.IssueStateCount2(relatedIDs)
					if err != nil {
						return nil, 0, err
					}

					var sum pb.IssueSummary
					for _, v := range reqResult {
						if stateBelongMap[v.State] == pb.IssueStateBelongEnum_DONE.String() || stateBelongMap[v.State] == pb.IssueStateBelongEnum_CLOSED.String() {
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
