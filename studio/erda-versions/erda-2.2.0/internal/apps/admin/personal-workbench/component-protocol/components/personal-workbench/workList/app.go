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
)

// GenAppKvInfo show: mr num, runtime num
func (l *WorkList) GenAppKvInfo(item apistructs.AppWorkBenchItem) (kvs []list.KvInfo) {
	kvs = []list.KvInfo{
		// project belong
		{
			ID:    strconv.FormatUint(item.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyProject),
			Value: item.ProjectName,
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: item.ProjectID,
							},
							Target: common.OpValTargetProject,
						},
					}).
					Build(),
			},
		},
		// mr count
		{
			ID:    strconv.FormatUint(item.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyMR),
			Value: strconv.FormatInt(int64(item.AppOpenMrNum), 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: item.ProjectID,
								common.OpKeyAppID:     item.ID,
							},
							Target: common.OpValTargetAppOpenMr,
						},
					}).
					Build(),
			},
		},
		// runtime count
		{
			ID:    strconv.FormatUint(item.ID, 10),
			Key:   l.sdk.I18n(i18n.I18nKeyRuntime),
			Value: strconv.FormatInt(int64(item.AppRuntimeNum), 10),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: item.ProjectID,
								common.OpKeyAppID:     item.ID,
								common.OpKeyWorkSpace: "DEV",
							},
							Target: common.OpValTargetAppDeploy,
						},
					}).
					Build(),
			},
		},
	}
	return
}

func (l *WorkList) GenAppColumnInfo(item apistructs.AppWorkBenchItem) (columns map[string]interface{}) {
	var hovers []list.KvInfo
	columns = make(map[string]interface{})
	hovers = []list.KvInfo{
		// 代码仓库
		{
			ID:   strconv.FormatUint(item.ID, 10),
			Icon: common.IconRepo,
			Tip:  l.sdk.I18n(i18n.I18nKeyGitRepo),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: item.ProjectID,
								common.OpKeyAppID:     item.ID,
							},
							Target: common.OpValTargetRepo,
						},
					}).
					Build(),
			},
		},
		// 流水线
		{
			ID:   strconv.FormatUint(item.ID, 10),
			Icon: common.IconPipeline,
			Tip:  l.sdk.I18n(i18n.I18nKeyPipeline),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: item.ProjectID,
								common.OpKeyAppID:     item.ID,
							},
							Target: common.OpValTargetPipelineRoot,
						},
					}).
					Build(),
			},
		},
		// API设计
		//{
		//	ID:   strconv.FormatUint(app.ID, 10),
		//	Icon: common.IconAppApiDesign,
		//	Tip:  l.sdk.I18n(i18n.I18nKeyAppApiDesign),
		//	Operations: map[cptype.OperationKey]cptype.Operation{
		//		list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
		//			WithSkipRender(true).
		//			WithServerDataPtr(list.OpItemClickGotoServerData{
		//				OpItemBasicServerData: list.OpItemBasicServerData{
		//					Params: map[string]interface{}{
		//						common.OpKeyProjectID: app.ProjectID,
		//						common.OpKeyAppID:     app.ID,
		//					},
		//					Target: common.OpValTargetAppApiDesign,
		//				},
		//			}).
		//			Build(),
		//	},
		//},
		// 部署中心
		{
			ID:   strconv.FormatUint(item.ID, 10),
			Icon: common.IconAppDeployCenter,
			Tip:  l.sdk.I18n(i18n.I18nKeyAppDeployCenter),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: item.ProjectID,
								common.OpKeyAppID:     item.ID,
								common.OpKeyWorkSpace: "DEV",
							},
							Target: common.OpValTargetAppDeploy,
						},
					}).
					Build(),
			},
		},
	}
	columns["hoverIcons"] = hovers
	return
}

// LIBRARY, SERVICE, BIGDATA

func (l *WorkList) GenAppTitleState(mode string) ([]list.StateInfo, bool) {
	switch mode {
	case "LIBRARY":
		return []list.StateInfo{{Text: l.sdk.I18n(i18n.I18nKeyAppModeLIBRARY), Status: common.AppLibraryStatus}}, true
	case "BIGDATA":
		return []list.StateInfo{{Text: l.sdk.I18n(i18n.I18nKeyAppModeBIGDATA), Status: common.AppBigdataStatus}}, true
	case "SERVICE":
		return []list.StateInfo{{Text: l.sdk.I18n(i18n.I18nKeyAppModeSERVICE), Status: common.AppServiceStatus}}, true
	case "MOBILE":
		return []list.StateInfo{{Text: l.sdk.I18n(i18n.I18nKeyAppModeMOBILE), Status: common.AppMobileStatus}}, true
	case "PROJECT_SERVICE":
		return []list.StateInfo{{Text: l.sdk.I18n(i18n.I18nAppModePROJECTSERVICE), Status: common.AppMobileStatus}}, true
	default:
		logrus.Warnf("wrong app mode: %v", mode)
		return []list.StateInfo{}, false
	}
}
