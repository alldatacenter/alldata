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
	"encoding/json"
	"fmt"
	"strconv"
	"sync"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/filehelper"
)

// BatchConvert 批量转换
func (svc *Issue) BatchConvert(models []dao.Issue, issueTypes []apistructs.IssueType, identityInfo apistructs.IdentityInfo) ([]apistructs.Issue, error) {
	if len(models) == 0 {
		return nil, nil
	}
	// todo 老的issue button 先不删除了。等新的issue button map 权限加上后再删除老的代码
	// 批量处理按钮
	// buttons := make(map[dao.Issue][]apistructs.IssueStateButton)
	// for _, model := range models {
	// 	buttons[model] = nil
	// }
	// if err := svc.batchGenerateButton(buttons, identityInfo); err != nil {
	// 	return nil, err
	// }

	buttons, err := svc.genrateButtonMap(models[0].ProjectID, issueTypes)
	if err != nil {
		return nil, err
	}

	// 查询项目下所有 labels
	resp, err := svc.bdl.ListLabel(apistructs.ProjectLabelListRequest{
		ProjectID: models[0].ProjectID,
		Type:      apistructs.LabelTypeIssue,
		PageNo:    1,
		PageSize:  1000,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to list project labels, err: %v", err)
	}
	labels := resp.List
	labelMap := make(map[uint64]apistructs.ProjectLabel, len(labels))
	for _, label := range labels {
		labelMap[uint64(label.ID)] = label
	}
	// 批量获取 models 的 labels
	var issueIDs []int64
	for _, model := range models {
		issueIDs = append(issueIDs, int64(model.ID))
	}
	issueLabelIDMap, err := svc.db.BatchQueryIssueLabelIDMap(issueIDs)
	if err != nil {
		return nil, fmt.Errorf("failed to batch query issue label relations, err: %v", err)
	}
	if issueLabelIDMap == nil {
		issueLabelIDMap = make(map[uint64][]uint64)
	}

	// 赋值回 models
	var lock sync.RWMutex
	issues := make([]apistructs.Issue, 0, len(models))
	var wait sync.WaitGroup
	for _, model := range models {
		model := model
		wait.Add(1)
		go func() {
			defer wait.Done()
			issue := svc.ConvertWithoutButton(model, false, issueLabelIDMap[model.ID], false, labelMap)

			issue.IssueButton = buttons[model.Type][model.State]
			lock.Lock()
			issues = append(issues, *issue)
			lock.Unlock()
		}()
	}
	wait.Wait()
	// issues 重新排序
	issueMap := make(map[int64]*apistructs.Issue)
	for i := range issues {
		issueMap[issues[i].ID] = &issues[i]
	}
	results := make([]apistructs.Issue, 0, len(models))
	for _, model := range models {
		results = append(results, *issueMap[int64(model.ID)])
	}
	return results, nil
}

// Convert: dao.Issue -> apistructs.Issue
func (svc *Issue) Convert(model dao.Issue, identityInfo apistructs.IdentityInfo) (*apistructs.Issue, error) {
	issue := svc.ConvertWithoutButton(model, true, nil, true, nil)
	button, err := svc.generateButton(model, identityInfo, nil, nil, nil, nil)
	if err != nil {
		return nil, err
	}
	issue.IssueButton = button

	subscribers, err := svc.db.GetIssueSubscribersByIssueID(issue.ID)
	if err != nil {
		return nil, err
	}
	for _, v := range subscribers {
		issue.Subscribers = append(issue.Subscribers, v.UserID)
	}

	return issue, nil
}

// ConvertWithoutButton 不包括 button
func (svc *Issue) ConvertWithoutButton(model dao.Issue,
	needQueryLabelRef bool, labelIDs []uint64,
	needQueryLabels bool, projectLabels map[uint64]apistructs.ProjectLabel,
) *apistructs.Issue {
	// 标签
	if needQueryLabelRef {
		lrs, _ := svc.db.GetLabelRelationsByRef(apistructs.LabelTypeIssue, strconv.FormatUint(model.ID, 10))
		labelIDs = make([]uint64, 0, len(lrs))
		for _, v := range lrs {
			labelIDs = append(labelIDs, v.LabelID)
		}
	}
	var labelNames []string
	var labels []apistructs.ProjectLabel
	if needQueryLabels {
		labels, _ = svc.bdl.ListLabelByIDs(labelIDs)
		labelNames = make([]string, 0, len(labels))
		for _, v := range labels {
			labelNames = append(labelNames, v.Name)
		}
	} else {
		for _, labelID := range labelIDs {
			label, ok := projectLabels[labelID]
			if ok {
				labelNames = append(labelNames, label.Name)
				labels = append(labels, label)
			}
		}
	}

	var manHour apistructs.IssueManHour
	_ = json.Unmarshal([]byte(model.ManHour), &manHour)

	var bugStage, taskType = "", ""
	if model.Type == apistructs.IssueTypeTask {
		taskType = model.Stage
	} else if model.Type == apistructs.IssueTypeBug {
		bugStage = model.Stage
	}
	return &apistructs.Issue{
		ID:             int64(model.ID),
		CreatedAt:      model.CreatedAt,
		UpdatedAt:      model.UpdatedAt,
		PlanStartedAt:  model.PlanStartedAt,
		PlanFinishedAt: model.PlanFinishedAt,
		ProjectID:      model.ProjectID,
		IterationID:    model.IterationID,
		AppID:          model.AppID,
		RequirementID:  model.RequirementID,
		Type:           model.Type,
		Title:          model.Title,
		Content:        filehelper.FilterAPIFileUrl(model.Content),
		State:          model.State,
		Priority:       model.Priority,
		Complexity:     model.Complexity,
		Severity:       model.Severity,
		Creator:        model.Creator,
		Assignee:       model.Assignee,
		Labels:         labelNames,
		LabelDetails:   labels,
		ManHour:        manHour.Clean(),
		Source:         model.Source,
		Owner:          model.Owner,
		BugStage:       bugStage,
		TaskType:       taskType,
		FinishTime:     model.FinishTime,
		ReopenCount:    model.ReopenCount,
	}
}
