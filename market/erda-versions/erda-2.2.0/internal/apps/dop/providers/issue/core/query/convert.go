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
	"fmt"
	"strconv"
	"strings"
	"sync"

	"google.golang.org/protobuf/types/known/timestamppb"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	streamcommon "github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/filehelper"
	"github.com/erda-project/erda/pkg/strutil"
)

// Convert: dao.Issue -> apistructs.Issue
func (p *provider) Convert(model dao.Issue, identityInfo *commonpb.IdentityInfo) (*pb.Issue, error) {
	issue := p.ConvertWithoutButton(model, true, nil, true, nil)
	button, err := p.GenerateButton(model, identityInfo, nil, nil, nil, nil)
	if err != nil {
		return nil, err
	}
	issue.IssueButton = button

	subscribers, err := p.db.GetIssueSubscribersByIssueID(issue.Id)
	if err != nil {
		return nil, err
	}
	for _, v := range subscribers {
		issue.Subscribers = append(issue.Subscribers, v.UserID)
	}

	return issue, nil
}

func (p *provider) BatchConvert(models []dao.Issue, issueTypes []string) ([]*pb.Issue, error) {
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

	buttons, err := p.GenerateButtonMap(models[0].ProjectID, issueTypes)
	if err != nil {
		return nil, err
	}

	// 查询项目下所有 labels
	resp, err := p.bdl.ListLabel(apistructs.ProjectLabelListRequest{
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
	issueLabelIDMap, err := p.db.BatchQueryIssueLabelIDMap(issueIDs)
	if err != nil {
		return nil, fmt.Errorf("failed to batch query issue label relations, err: %v", err)
	}
	if issueLabelIDMap == nil {
		issueLabelIDMap = make(map[uint64][]uint64)
	}

	// 赋值回 models
	var lock sync.RWMutex
	issues := make([]pb.Issue, 0, len(models))
	var wait sync.WaitGroup
	for _, model := range models {
		model := model
		wait.Add(1)
		go func() {
			defer wait.Done()
			issue := p.ConvertWithoutButton(model, false, issueLabelIDMap[model.ID], false, labelMap)

			issue.IssueButton = buttons[model.Type][model.State]
			lock.Lock()
			issues = append(issues, *issue)
			lock.Unlock()
		}()
	}
	wait.Wait()
	// issues 重新排序
	issueMap := make(map[int64]*pb.Issue)
	for i := range issues {
		issueMap[issues[i].Id] = &issues[i]
	}
	results := make([]*pb.Issue, 0, len(models))
	for _, model := range models {
		results = append(results, issueMap[int64(model.ID)])
	}
	return results, nil
}

// ConvertWithoutButton 不包括 button
func (p *provider) ConvertWithoutButton(model dao.Issue,
	needQueryLabelRef bool, labelIDs []uint64,
	needQueryLabels bool, projectLabels map[uint64]apistructs.ProjectLabel,
) *pb.Issue {
	// 标签
	if needQueryLabelRef {
		lrs, _ := p.db.GetLabelRelationsByRef(pb.ProjectLabelTypeEnum_issue.String(), strconv.FormatUint(model.ID, 10))
		labelIDs = make([]uint64, 0, len(lrs))
		for _, v := range lrs {
			labelIDs = append(labelIDs, v.LabelID)
		}
	}
	var labelNames []string
	var labels []apistructs.ProjectLabel
	if needQueryLabels {
		labels, _ = p.bdl.ListLabelByIDs(labelIDs)
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

	var manHour pb.IssueManHour
	_ = json.Unmarshal([]byte(model.ManHour), &manHour)

	var bugStage, taskType = "", ""
	if model.Type == pb.IssueTypeEnum_TASK.String() {
		taskType = model.Stage
	} else if model.Type == pb.IssueTypeEnum_BUG.String() {
		bugStage = model.Stage
	}
	issue := &pb.Issue{
		Id:           int64(model.ID),
		CreatedAt:    timestamppb.New(model.CreatedAt),
		UpdatedAt:    timestamppb.New(model.UpdatedAt),
		ProjectID:    model.ProjectID,
		IterationID:  model.IterationID,
		Type:         pb.IssueTypeEnum_Type(pb.IssueTypeEnum_Type_value[model.Type]),
		Title:        model.Title,
		Content:      filehelper.FilterAPIFileUrl(model.Content),
		State:        model.State,
		Priority:     pb.IssuePriorityEnum_Priority(pb.IssuePriorityEnum_Priority_value[model.Priority]),
		Complexity:   pb.IssueComplexityEnum_Complextity(pb.IssueComplexityEnum_Complextity_value[model.Complexity]),
		Severity:     pb.IssueSeverityEnum_Severity(pb.IssueSeverityEnum_Severity_value[model.Severity]),
		Creator:      model.Creator,
		Assignee:     model.Assignee,
		Labels:       labelNames,
		LabelDetails: getPbLabelDetails(labels),
		IssueManHour: cleanManHour(manHour),
		Source:       model.Source,
		Owner:        model.Owner,
		BugStage:     bugStage,
		TaskType:     taskType,
		ReopenCount:  int32(model.ReopenCount),
	}
	if model.PlanStartedAt != nil {
		issue.PlanStartedAt = timestamppb.New(*model.PlanStartedAt)
	}
	if model.PlanFinishedAt != nil {
		issue.PlanFinishedAt = timestamppb.New(*model.PlanFinishedAt)
	}
	if model.FinishTime != nil {
		issue.FinishTime = timestamppb.New(*model.FinishTime)
	}
	if model.AppID != nil {
		issue.AppID = *model.AppID
	}
	if model.RequirementID != nil {
		issue.RequirementID = *model.RequirementID
	}
	return issue
}

func cleanManHour(imh pb.IssueManHour) *pb.IssueManHour {
	imh.ThisElapsedTime = 0
	imh.StartTime = ""
	imh.WorkContent = ""
	return &imh
}

func getPbLabelDetails(labels []apistructs.ProjectLabel) []*pb.ProjectLabel {
	l := make([]*pb.ProjectLabel, 0, len(labels))
	for _, i := range labels {
		l = append(l, GetPbLabelDetail(i))
	}
	return l
}

func GetPbLabelDetail(l apistructs.ProjectLabel) *pb.ProjectLabel {
	return &pb.ProjectLabel{
		Id:        l.ID,
		Name:      l.Name,
		Type:      pb.ProjectLabelTypeEnum_ProjectLabelType(pb.ProjectLabelTypeEnum_ProjectLabelType_value[string(l.Type)]),
		Color:     l.Color,
		ProjectID: l.ProjectID,
		Creator:   l.Creator,
		CreatedAt: timestamppb.New(l.CreatedAt),
		UpdatedAt: timestamppb.New(l.UpdatedAt),
	}
}

type IssueStage struct {
	Type  string
	Value string
}

func (p *provider) getIssueExportDataI18n(locale, i18nKey string) []string {
	l := p.bdl.GetLocale(locale)
	t := l.Get(i18nKey)
	return strutil.Split(t, ",")
}

func (p *provider) convertIssueToExcelList(issues []*pb.Issue, property []*pb.IssuePropertyIndex, projectID uint64, isDownload bool, stageMap map[IssueStage]string, locale string) ([][]string, error) {
	// 默认字段列名
	r := [][]string{p.getIssueExportDataI18n(locale, i18n.I18nKeyIssueExportTitles)}
	// 自定义字段列名
	for _, pro := range property {
		r[0] = append(r[0], pro.DisplayName)
	}
	// 下载模版
	if isDownload {
		return r, nil
	}
	type pair struct {
		PropertyID int64
		valueID    int64
	}
	// 构建自定义字段枚举值map
	mp := make(map[pair]string)
	for _, v := range property {
		if common.IsOptions(v.PropertyType.String()) == true {
			for _, val := range v.EnumeratedValues {
				mp[pair{PropertyID: v.PropertyID, valueID: val.Id}] = val.Name
			}
		}
	}
	// 状态名map
	stateMap := make(map[int64]string)
	states, err := p.db.GetIssuesStatesByProjectID(projectID, "")
	if err != nil {
		return nil, err
	}
	for _, v := range states {
		stateMap[int64(v.ID)] = v.Name
	}
	// 迭代map
	iterationMap := make(map[int64]string)
	iterations, _, err := p.db.PagingIterations(apistructs.IterationPagingRequest{
		PageNo:    1,
		PageSize:  10000,
		ProjectID: projectID,
	})
	if err != nil {
		return nil, err
	}
	for _, v := range iterations {
		iterationMap[int64(v.ID)] = v.Title
	}
	iterationMap[-1] = "待办事项"
	// 自定义字段map
	var issueIDs []int64
	for _, v := range issues {
		issueIDs = append(issueIDs, v.Id)
	}
	propertyMap := make(map[int64][]dao.IssuePropertyRelation)
	properties, err := p.db.PagingPropertyRelationByIDs(issueIDs)
	for _, v := range properties {
		propertyMap[v.IssueID] = append(propertyMap[v.IssueID], v)
	}
	for index, i := range issues {
		planFinishedAt := ""
		if i.PlanFinishedAt != nil {
			planFinishedAt = i.PlanFinishedAt.AsTime().Format("2006-01-02 15:04:05")
		}
		planStartedAt := ""
		if i.PlanStartedAt != nil {
			planStartedAt = i.PlanStartedAt.AsTime().Format("2006-01-02 15:04:05")
		}
		iterationName := iterationMap[i.IterationID]
		stage := IssueStage{
			Type:  i.Type.String(),
			Value: common.GetStage(i),
		}
		finishTime := ""
		if i.FinishTime != nil {
			finishTime = i.FinishTime.AsTime().Format("2006-01-02 15:04:05")
		}

		_, relatedIssueIDs, err := p.GetIssueRelationsByIssueIDs(uint64(i.Id), []string{apistructs.IssueRelationConnection})
		if err != nil {
			return nil, err
		}
		relatedIssueIDStrs := make([]string, 0)
		for _, id := range relatedIssueIDs {
			relatedIssueIDStrs = append(relatedIssueIDStrs, strconv.FormatUint(id, 10))
		}

		r = append(r, append([]string{
			strconv.FormatInt(i.Id, 10),
			i.Title,
			i.Content,
			stateMap[i.State],
			i.Creator,
			i.Assignee,
			i.Owner,
			stageMap[stage],
			map[pb.IssuePriorityEnum_Priority]string{
				pb.IssuePriorityEnum_LOW:    "低",
				pb.IssuePriorityEnum_HIGH:   "高",
				pb.IssuePriorityEnum_NORMAL: "中",
				pb.IssuePriorityEnum_URGENT: "紧急",
			}[i.Priority],
			iterationName,
			map[pb.IssueComplexityEnum_Complextity]string{
				pb.IssueComplexityEnum_EASY:   "容易",
				pb.IssueComplexityEnum_NORMAL: "中",
				pb.IssueComplexityEnum_HARD:   "复杂",
			}[i.Complexity],
			map[pb.IssueSeverityEnum_Severity]string{
				pb.IssueSeverityEnum_FATAL:   "致命",
				pb.IssueSeverityEnum_SERIOUS: "严重",
				pb.IssueSeverityEnum_NORMAL:  "一般",
				pb.IssueSeverityEnum_SLIGHT:  "轻微",
				pb.IssueSeverityEnum_SUGGEST: "建议",
			}[i.Severity],
			strutil.Join(i.Labels, ",", true),
			map[pb.IssueTypeEnum_Type]string{
				pb.IssueTypeEnum_REQUIREMENT: "需求",
				pb.IssueTypeEnum_TASK:        "任务",
				pb.IssueTypeEnum_BUG:         "缺陷",
				pb.IssueTypeEnum_EPIC:        "史诗",
			}[i.Type],
			planFinishedAt,
			i.CreatedAt.AsTime().Format("2006-01-02 15:04:05"),
			strings.Join(relatedIssueIDStrs, ","),
			streamcommon.GetFormartTime(i.IssueManHour, "EstimateTime"),
			finishTime,
			planStartedAt,
			fmt.Sprintf("%d", i.ReopenCount),
		}))
		relations := propertyMap[i.Id]
		// 获取每个自定义字段的值
		for _, pro := range property {
			// 根据字段类型将数据放入表格
			if common.IsOptions(pro.PropertyType.String()) == false {
				for _, rel := range relations {
					if rel.PropertyID == pro.PropertyID {
						r[index+1] = append(r[index+1], rel.ArbitraryValue)
						break
					}
				}
			} else if pro.PropertyType == pb.PropertyTypeEnum_Select {
				for _, rel := range relations {
					if rel.PropertyID == pro.PropertyID {
						r[index+1] = append(r[index+1], mp[pair{PropertyID: pro.PropertyID, valueID: rel.PropertyValueID}])
						break
					}
				}
			} else if pro.PropertyType == pb.PropertyTypeEnum_MultiSelect || pro.PropertyType == pb.PropertyTypeEnum_CheckBox {
				// 多选类型的全部已选项的名字拼接成一个字符串放入表格
				var str []string
				for _, rel := range relations {
					if rel.PropertyID == pro.PropertyID {
						str = append(str, mp[pair{PropertyID: pro.PropertyID, valueID: rel.PropertyValueID}])
					}
				}
				r[index+1] = append(r[index+1], strutil.Join(str, ","))
			}
		}
	}
	return r, nil
}
