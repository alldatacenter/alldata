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

package workList

import (
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/list"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/i18n"
	wb "github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
)

// GenProjKvColumnInfo show type: DevOps, MSP, DevOps(primary)/MSP
func (l *WorkList) GenProjKvColumnInfo(project apistructs.WorkbenchProjOverviewItem, q wb.IssueUrlQueries, mspParams map[string]interface{}) (kvs []list.KvInfo, columns map[string]interface{}) {
	switch project.ProjectDTO.Type {
	case common.DevOpsProject, common.DefaultProject:
		// kv issue info
		kvs = l.GenProjDopKvInfo(project, q, mspParams)
		columns = l.GenProjDopColumnInfo(project, q, mspParams)

	case common.MspProject:
		kvs = l.GenProjMspKvInfo(project, q, mspParams)
		columns = l.GenProjMspColumnInfo(project, q, mspParams)
	}
	return
}

func (l *WorkList) GenProjDopKvInfo(proj apistructs.WorkbenchProjOverviewItem, q wb.IssueUrlQueries, mspParams map[string]interface{}) (kvs []list.KvInfo) {
	// kv issue info
	if proj.IssueInfo == nil {
		proj.IssueInfo = &apistructs.ProjectIssueInfo{}
	}
	kvs = []list.KvInfo{
		// issue expired
		{
			ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyIssueExpired),
			Value: strconv.FormatInt(int64(proj.IssueInfo.ExpiredIssueNum), 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Query: map[string]interface{}{
								common.OpKeyIssueFilterUrlQuery: q.ExpiredQuery,
							},
							Params: map[string]interface{}{
								common.OpKeyProjectID: proj.ProjectDTO.ID,
							},
							Target: common.OpValTargetProjAllIssue,
						},
					}).
					Build(),
			},
		},
		// issue will expire today
		{
			ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyIssueExpireToday),
			Value: strconv.FormatInt(int64(proj.IssueInfo.ExpiredOneDayNum), 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Query: map[string]interface{}{
								common.OpKeyIssueFilterUrlQuery: q.TodayExpireQuery,
							},
							Params: map[string]interface{}{
								common.OpKeyProjectID: proj.ProjectDTO.ID,
							},
							Target: common.OpValTargetProjAllIssue,
						},
					}).
					Build(),
			},
		},
		// issue undo
		{
			ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyIssueUndo),
			Value: strconv.FormatInt(int64(proj.IssueInfo.TotalIssueNum), 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Query: map[string]interface{}{
								common.OpKeyIssueFilterUrlQuery: q.UndoQuery,
							},
							Params: map[string]interface{}{
								common.OpKeyProjectID: proj.ProjectDTO.ID,
							},
							Target: common.OpValTargetProjAllIssue,
						},
					}).
					Build(),
			},
		},
	}
	if proj.StatisticInfo != nil {
		// msp alert info
		altKv := list.KvInfo{
			ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyMspLast24HAlertCount),
			Value: strconv.FormatInt(proj.StatisticInfo.Last24HAlertCount, 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMicroServiceAlarmRecord,
						},
					}).
					Build(),
			},
		}
		kvs = append(kvs, altKv)
	}
	return
}

func (l *WorkList) GenProjMspKvInfo(proj apistructs.WorkbenchProjOverviewItem, q wb.IssueUrlQueries, mspParams map[string]interface{}) (kvs []list.KvInfo) {
	if proj.StatisticInfo == nil {
		return []list.KvInfo{
			{
				ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
				Key:   l.sdk.I18n(i18n.I18nKeyMspServiceCount),
				Value: "0",
			},
			{
				ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
				Key:   l.sdk.I18n(i18n.I18nKeyMspLast24HAlertCount),
				Value: "0",
			},
		}
	}
	kvs = []list.KvInfo{
		// msp service info
		{
			ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyMspServiceCount),
			Value: strconv.FormatInt(proj.StatisticInfo.ServiceCount, 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMspOverview,
						},
					}).
					Build(),
			},
		},
		// msp alert info
		{
			ID:    strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyMspLast24HAlertCount),
			Value: strconv.FormatInt(proj.StatisticInfo.Last24HAlertCount, 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMicroServiceAlarmRecord,
						},
					}).
					Build(),
			},
		},
	}
	return
}

func (l *WorkList) GenProjDopColumnInfo(proj apistructs.WorkbenchProjOverviewItem, q wb.IssueUrlQueries, mspParams map[string]interface{}) (columns map[string]interface{}) {
	var hovers []list.KvInfo
	columns = make(map[string]interface{})
	hovers = []list.KvInfo{
		// 项目管理
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconProjManagement,
			Tip:  l.sdk.I18n(i18n.I18nKeyProjManagement),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: proj.ProjectDTO.ID,
							},
							Target: common.OpValTargetProjAllIssue,
						},
					}).
					Build(),
			},
		},
		// 应用开发
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconAppDevelop,
			Tip:  l.sdk.I18n(i18n.I18nKeyAppDevelop),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: proj.ProjectDTO.ID,
							},
							Target: common.OpValTargetProjApps,
						},
					}).
					Build(),
			},
		},
		// 测试管理
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconTestManagement,
			Tip:  l.sdk.I18n(i18n.I18nKeyTestManagement),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: proj.ProjectDTO.ID,
							},
							Target: common.OpValTargetProjectTestDashboard,
						},
					}).
					Build(),
			},
		},

		// 项目设置
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconProjSetting,
			Tip:  l.sdk.I18n(i18n.I18nKeyProjSetting),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: proj.ProjectDTO.ID,
							},
							Target: common.OpValTargetProjectSetting,
						},
					}).
					Build(),
			},
		},
	}
	if mspParams["terminusKey"] != "" {
		hovers = append(hovers, list.KvInfo{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconServiceObservation,
			Tip:  l.sdk.I18n(i18n.I18nKeyServiceObservation),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMspOverview,
						},
					}).
					Build(),
			},
		})
	}
	columns["hoverIcons"] = hovers
	return
}

func (l *WorkList) GenProjMspColumnInfo(proj apistructs.WorkbenchProjOverviewItem, q wb.IssueUrlQueries, mspParams map[string]interface{}) (columns map[string]interface{}) {
	var hovers []list.KvInfo
	columns = make(map[string]interface{})
	hovers = []list.KvInfo{
		// 服务列表
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconServiceList,
			Tip:  l.sdk.I18n(i18n.I18nKeyServiceList),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMspOverview,
						},
					}).
					Build(),
			},
		},
		// 服务监控
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconServiceMonitor,
			Tip:  l.sdk.I18n(i18n.I18nKeyServiceMonitor),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMspMonitorServiceAnalyze,
						},
					}).
					Build(),
			},
		},
		// 链路追踪
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconServiceTracing,
			Tip:  l.sdk.I18n(i18n.I18nKeyServiceTracing),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMicroTrace,
						},
					}).
					Build(),
			},
		},
		// 日志分析
		{
			ID:   strconv.FormatUint(proj.ProjectDTO.ID, 10),
			Icon: common.IconLogAnalysis,
			Tip:  l.sdk.I18n(i18n.I18nKeyLogAnalysis),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: mspParams,
							Target: common.OpValTargetMspLogAnalyze,
						},
					}).
					Build(),
			},
		},
	}
	columns["hoverIcons"] = hovers
	return
}

func (l *WorkList) GenProjTitleState(tp string) ([]list.StateInfo, bool) {
	switch tp {
	case common.MspProject:
		return []list.StateInfo{{Text: l.sdk.I18n(i18n.I18nKeyMspProject), Status: common.ProjMspStatus}}, true
	case common.DevOpsProject, common.DefaultProject:
		return []list.StateInfo{{Text: l.sdk.I18n(i18n.I18nKeyDevOpsProject), Status: common.ProjDevOpsStatus}}, true
	default:
		logrus.Warnf("wrong project type: %v", tp)
		return []list.StateInfo{}, false
	}
}
