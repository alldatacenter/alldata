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

package workCards

import (
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/cardlist"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/cardlist/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/i18n"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
)

type WorkCards struct {
	impl.DefaultCard
	filterReq apistructs.IssuePagingRequest
	State     State
	Bdl       *bundle.Bundle
	Wb        *workbench.Workbench
}

const DefaultCardListSize int = 6

func (wc *WorkCards) BeforeHandleOp(sdk *cptype.SDK) {
	wc.Wb = sdk.Ctx.Value(types.WorkbenchSvc).(*workbench.Workbench)
	wc.Bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
}

func (wc *WorkCards) RegisterCardListStarOp(opData cardlist.OpCardListStar) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		card := cardlist.Card{}
		err := common.Transfer(opData.ClientData.DataRef, &card)
		if err != nil {
			return nil
		}
		tabName := wc.getTableName(sdk)
		id, err := strconv.ParseUint(card.ID, 10, 64)
		if err != nil {
			logrus.Error(err)
			return nil
		}
		req := apistructs.UnSubscribeReq{
			Type:   apistructs.SubscribeType(tabName),
			TypeID: id,
			UserID: sdk.Identity.UserID,
		}
		err = wc.Bdl.DeleteSubscribe(sdk.Identity.UserID, sdk.Identity.OrgID, req)
		if err != nil {
			logrus.Errorf("star %v failed, id: %v, error: %v", req.Type, req.TypeID, err)
			return nil
		}

		cards := wc.StdDataPtr.Cards
		//wc.LoadList(sdk)
		for i := 0; i < len(cards); i++ {
			if card.ID == cards[i].ID {
				cards = append(cards[:i], cards[i+1:]...)
				break
			}
		}
		wc.StdDataPtr.Cards = cards
		return nil
	}
}

type State struct {
	TabName string `json:"tabName"`
}

func (wc *WorkCards) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		wc.Wb = sdk.Ctx.Value(types.WorkbenchSvc).(*workbench.Workbench)
		wc.Bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
		wc.LoadList(sdk)
		return nil
	}
}

func (wc *WorkCards) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return wc.RegisterInitializeOp()
}

func (wc *WorkCards) getAppTextMeta(app apistructs.AppWorkBenchItem) (metas []cardlist.TextMeta) {
	mrData := make(cptype.OpServerData)
	runtimeData := make(cptype.OpServerData)

	mrOps := common.Operation{
		JumpOut: false,
		Target:  common.OpValTargetAppOpenMr,
		Params:  map[string]interface{}{"projectId": app.ProjectID, "appId": app.ID},
	}
	runtimeOps := common.Operation{
		JumpOut: false,
		Target:  common.OpValTargetAppDeploy,
		Params:  map[string]interface{}{"projectId": app.ProjectID, "appId": app.ID, common.OpKeyWorkSpace: "DEV"},
	}
	err := common.Transfer(mrOps, &mrData)
	if err != nil {
		logrus.Error(err)
		return
	}
	err = common.Transfer(runtimeOps, &runtimeData)
	if err != nil {
		logrus.Error(err)
		return
	}
	metas = []cardlist.TextMeta{
		{
			MainText: float64(app.AppOpenMrNum),
			SubText:  "MR ",
			Operations: map[cptype.OperationKey]cptype.Operation{"clickGoto": {
				ServerData: &mrData,
			}},
		},
		{
			MainText: float64(app.AppRuntimeNum),
			SubText:  "Runtime ",
			Operations: map[cptype.OperationKey]cptype.Operation{"clickGoto": {
				ServerData: &runtimeData,
			}},
		},
	}
	return
}
func (wc *WorkCards) getProjTextMeta(sdk *cptype.SDK, project apistructs.WorkbenchProjOverviewItem, queries workbench.IssueUrlQueries, urlParam workbench.UrlParams) (metas []cardlist.TextMeta) {

	metas = make([]cardlist.TextMeta, 0)
	switch project.ProjectDTO.Type {
	case common.DevOpsProject, common.DefaultProject:
		todayData := make(cptype.OpServerData)
		expireData := make(cptype.OpServerData)
		todayOp := common.Operation{
			JumpOut: false,
			Target:  "projectAllIssue",
			Query:   map[string]interface{}{"issueFilter__urlQuery": queries.TodayExpireQuery},
			Params:  map[string]interface{}{"projectId": project.ProjectDTO.ID},
		}
		expireOp := common.Operation{
			JumpOut: false,
			Target:  "projectAllIssue",
			Query:   map[string]interface{}{"issueFilter__urlQuery": queries.ExpiredQuery},
			Params:  map[string]interface{}{"projectId": project.ProjectDTO.ID},
		}

		err := common.Transfer(expireOp, &expireData)
		if err != nil {
			logrus.Error(err)
			return
		}
		err = common.Transfer(todayOp, &todayData)
		if err != nil {
			logrus.Error(err)
			return
		}
		if project.IssueInfo == nil {
			project.IssueInfo = &apistructs.ProjectIssueInfo{}
		}

		metas = []cardlist.TextMeta{
			{
				MainText: float64(project.IssueInfo.ExpiredIssueNum),
				SubText:  sdk.I18n(i18n.I18nKeyIssueExpired),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &expireData,
					},
				},
			},
			{
				MainText: float64(project.IssueInfo.ExpiredOneDayNum),
				SubText:  sdk.I18n(i18n.I18nKeyIssueExpireToday),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &todayData,
					},
				},
			},
		}
		return
	case common.MspProject:
		if project.IssueInfo == nil {
			project.IssueInfo = &apistructs.ProjectIssueInfo{}
		}
		serviceCnt := make(cptype.OpServerData)
		lastDayWarning := make(cptype.OpServerData)
		err := common.Transfer(urlParam, &serviceCnt)
		if err != nil {
			logrus.Error(err)
			return
		}
		err = common.Transfer(urlParam, &lastDayWarning)
		if err != nil {
			logrus.Error(err)
			return
		}
		serviceCnt["projectId"] = project.ProjectDTO.ID
		lastDayWarning["projectId"] = project.ProjectDTO.ID

		serviceOp := common.Operation{
			Target: common.OpValTargetMspOverview,
			Params: serviceCnt,
		}
		lastDayOp := common.Operation{
			Target: "microServiceAlarmRecord",
			Params: lastDayWarning,
		}

		err = common.Transfer(serviceOp, &serviceCnt)
		if err != nil {
			logrus.Error(err)
			return
		}
		err = common.Transfer(lastDayOp, &lastDayWarning)
		if err != nil {
			logrus.Error(err)
			return
		}
		if project.StatisticInfo == nil {
			metas = []cardlist.TextMeta{
				{
					MainText: 0,
					SubText:  sdk.I18n(i18n.I18nKeyMspServiceCount),
				},
				{
					MainText: 0,
					SubText:  sdk.I18n(i18n.I18nKeyMspLast24HAlertCount),
				},
			}
			return
		}
		metas = []cardlist.TextMeta{
			{
				MainText: float64(project.StatisticInfo.ServiceCount),
				SubText:  sdk.I18n(i18n.I18nKeyMspServiceCount),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &serviceCnt,
					},
				},
			},
			{
				MainText: float64(project.StatisticInfo.Last24HAlertCount),
				SubText:  sdk.I18n(i18n.I18nKeyMspLast24HAlertCount),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &lastDayWarning,
					},
				},
			},
		}
		return
	default:
		return
	}
}
func (wc *WorkCards) getProjectCardOps(sdk *cptype.SDK, params workbench.UrlParams, project apistructs.WorkbenchProjOverviewItem) (ops map[cptype.OperationKey]cptype.Operation) {
	ops = make(map[cptype.OperationKey]cptype.Operation)
	ops["star"] = cptype.Operation{
		Tip: sdk.I18n("cancel star"),
	}
	serviceOp := common.Operation{
		JumpOut: false,
		Params:  map[string]interface{}{},
	}
	err := common.Transfer(params, &serviceOp.Params)
	if err != nil {
		logrus.Errorf("card operation error :%v", err)
		return
	}
	target := ""
	switch project.ProjectDTO.Type {
	case common.DevOpsProject, common.DefaultProject:
		target = "project"
	case common.MspProject:
		target = common.OpValTargetMspOverview
	}
	serviceOp.Target = target
	serviceOp.Params["projectId"] = project.ProjectDTO.ID
	serverData := make(cptype.OpServerData)

	err = common.Transfer(serviceOp, &serverData)
	if err != nil {
		logrus.Error(err)
		return
	}
	ops["clickGoto"] = cptype.Operation{ServerData: &serverData, Async: true}
	return
}

func (wc *WorkCards) getAppCardOps(sdk *cptype.SDK, app apistructs.AppWorkBenchItem) (ops map[cptype.OperationKey]cptype.Operation) {
	ops = make(map[cptype.OperationKey]cptype.Operation)
	ops["star"] = cptype.Operation{
		Tip: sdk.I18n("cancel star"),
	}
	serviceOp := common.Operation{
		JumpOut: false,
		Target:  "repo",
		Params:  map[string]interface{}{"projectId": app.ProjectID, "appId": app.ID},
	}
	serverData := make(cptype.OpServerData)

	err := common.Transfer(serviceOp, &serverData)
	if err != nil {
		logrus.Error(err)
		return
	}
	ops["clickGoto"] = cptype.Operation{ServerData: &serverData}
	return
}
func (wc *WorkCards) getAppIconOps(sdk *cptype.SDK, app apistructs.AppWorkBenchItem) (ops []cardlist.IconOperations) {
	ops = make([]cardlist.IconOperations, 0)
	gotoData := cardlist.OpCardListGotoData{
		JumpOut: false,
		Params: cptype.ExtraMap{
			"projectId": app.ProjectID,
			"appId":     app.ID,
		},
	}
	pipelineServerData := make(cptype.OpServerData)
	//apiDesignServerData := make(cptype.OpServerData)
	deployData := make(cptype.OpServerData)
	repositoryServerData := make(cptype.OpServerData)
	common.Transfer(gotoData, &pipelineServerData)
	//common.Transfer(gotoData, &apiDesignServerData)
	common.Transfer(gotoData, &deployData)
	common.Transfer(gotoData, &repositoryServerData)
	pipelineServerData["target"] = "pipelineRoot"
	//apiDesignServerData["target"] = "appApiDesign"
	deployData["target"] = "deploy"
	repositoryServerData["target"] = "repo"
	ops = []cardlist.IconOperations{
		{
			Icon: "daimacangku",
			Tip:  sdk.I18n(i18n.I18nKeyGitRepo),
			Operations: map[cptype.OperationKey]cptype.Operation{
				"clickGoto": {ServerData: &repositoryServerData},
			}},
		{
			Icon: "liushuixian",
			Tip:  sdk.I18n(i18n.I18nKeyPipeline),
			Operations: map[cptype.OperationKey]cptype.Operation{
				"clickGoto": {ServerData: &pipelineServerData},
			}},
		//{
		//	Icon: "apisheji",
		//	Tip:  sdk.I18n("api design"),
		//	Operations: map[cptype.OperationKey]cptype.Operation{
		//		"clickGoto": {ServerData: &apiDesignServerData},
		//	}},
		{
			Icon: "bushuzhongxin",
			Tip:  sdk.I18n(i18n.I18nKeyAppDeployCenter),
			Operations: map[cptype.OperationKey]cptype.Operation{
				"clickGoto": {ServerData: &deployData},
			}},
	}
	return
}
func (wc *WorkCards) getProjIconOps(sdk *cptype.SDK, project apistructs.WorkbenchProjOverviewItem, params workbench.UrlParams) []cardlist.IconOperations {

	gotoData := cardlist.OpCardListGotoData{
		JumpOut: false,
		Params:  make(cptype.ExtraMap),
	}
	err := common.Transfer(params, &gotoData.Params)
	if err != nil {
		logrus.Error(err)
		return nil
	}
	gotoData.Params["projectId"] = project.ProjectDTO.ID
	switch project.ProjectDTO.Type {
	case common.MspProject:
		serviceListServerData := make(cptype.OpServerData)
		monitorServerData := make(cptype.OpServerData)
		traceServerData := make(cptype.OpServerData)
		logAnalysisServerData := make(cptype.OpServerData)
		common.Transfer(gotoData, &serviceListServerData)
		common.Transfer(gotoData, &monitorServerData)
		common.Transfer(gotoData, &traceServerData)
		common.Transfer(gotoData, &logAnalysisServerData)
		serviceListServerData["target"] = common.OpValTargetMspOverview
		monitorServerData["target"] = "mspMonitorServiceAnalyze"
		traceServerData["target"] = "microTrace"
		logAnalysisServerData["target"] = "mspLogAnalyze"
		return []cardlist.IconOperations{
			{
				Icon: "fuwuliebiao",
				Tip:  sdk.I18n(i18n.I18nKeyServiceList),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &serviceListServerData,
					},
				},
			},
			{
				Icon: "fuwujiankong",
				Tip:  sdk.I18n(i18n.I18nKeyServiceMonitor),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &monitorServerData,
					},
				},
			},
			{
				Icon: "lianluzhuizong",
				Tip:  sdk.I18n(i18n.I18nKeyServiceTracing),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &traceServerData,
					},
				},
			},
			{
				Icon: "rizhifenxi",
				Tip:  sdk.I18n(i18n.I18nKeyLogAnalysis),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {
						ServerData: &logAnalysisServerData,
					},
				},
			},
		}
	case common.DevOpsProject, common.DefaultProject:
		projectManageServerData := make(cptype.OpServerData)
		appDevelopServerData := make(cptype.OpServerData)
		testManageServerData := make(cptype.OpServerData)
		serviceMonitorServerData := make(cptype.OpServerData)
		projectSettingServerData := make(cptype.OpServerData)

		common.Transfer(gotoData, &projectManageServerData)
		common.Transfer(gotoData, &appDevelopServerData)
		common.Transfer(gotoData, &testManageServerData)
		common.Transfer(gotoData, &serviceMonitorServerData)
		common.Transfer(gotoData, &projectSettingServerData)
		projectManageServerData["target"] = "projectAllIssue"
		appDevelopServerData["target"] = "projectApps"
		testManageServerData["target"] = "projectTestDashboard"
		serviceMonitorServerData["target"] = common.OpValTargetMspOverview
		projectSettingServerData["target"] = "projectSetting"
		ios := []cardlist.IconOperations{
			{
				Icon: "xiangmuguanli",
				Tip:  sdk.I18n(i18n.I18nKeyProjManagement),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {ServerData: &projectManageServerData},
				},
			},
			{
				Icon: "yingyongkaifa",
				Tip:  sdk.I18n(i18n.I18nKeyAppDevelop),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {ServerData: &appDevelopServerData},
				},
			},
			{
				Icon: "ceshiguanli",
				Tip:  sdk.I18n(i18n.I18nKeyTestManagement),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {ServerData: &testManageServerData},
				},
			},
			{
				Icon: "xiangmushezhi",
				Tip:  sdk.I18n(i18n.I18nKeyProjSetting),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {ServerData: &projectSettingServerData},
				},
			},
		}
		if params.TerminusKey != "" {
			ios = append(ios, cardlist.IconOperations{
				Icon: "fuwuguance",
				Tip:  sdk.I18n(i18n.I18nKeyServiceObservation),
				Operations: map[cptype.OperationKey]cptype.Operation{
					"clickGoto": {ServerData: &serviceMonitorServerData},
				},
			})
		}
		return ios
	default:
		return []cardlist.IconOperations{}
	}
}

func (wc *WorkCards) getProjectTitleState(sdk *cptype.SDK, kind string) []cardlist.TitleState {
	switch kind {
	case common.MspProject:
		return []cardlist.TitleState{{Text: sdk.I18n(i18n.I18nKeyMspProject), Status: common.ProjMspStatus}}
	case common.DevOpsProject, common.DefaultProject:
		return []cardlist.TitleState{{Text: sdk.I18n(i18n.I18nKeyDevOpsProject), Status: common.ProjDevOpsStatus}}
	default:
		logrus.Warnf("wrong project type: %v", kind)
		return []cardlist.TitleState{}
	}
}

func (wc *WorkCards) getAppTitleState(sdk *cptype.SDK, mode string) []cardlist.TitleState {
	switch mode {
	case "LIBRARY":
		return []cardlist.TitleState{{Text: sdk.I18n(i18n.I18nKeyAppModeLIBRARY), Status: common.AppLibraryStatus}}
	case "BIGDATA":
		return []cardlist.TitleState{{Text: sdk.I18n(i18n.I18nKeyAppModeBIGDATA), Status: common.AppBigdataStatus}}
	case "SERVICE":
		return []cardlist.TitleState{{Text: sdk.I18n(i18n.I18nKeyAppModeSERVICE), Status: common.AppServiceStatus}}
	case "MOBILE":
		return []cardlist.TitleState{{Text: sdk.I18n(i18n.I18nKeyAppModeMOBILE), Status: common.AppMobileStatus}}
	case "PROJECT_SERVICE":
		return []cardlist.TitleState{{Text: sdk.I18n(i18n.I18nAppModePROJECTSERVICE), Status: common.AppMobileStatus}}
	default:
		logrus.Warnf("wrong app mode: %v", mode)
		return []cardlist.TitleState{}
	}

}

func (wc *WorkCards) getTableName(sdk *cptype.SDK) string {
	tabStr := ""
	if tab, ok := (*sdk.GlobalState)[common.WorkTabKey]; !ok {
		tabStr = wc.State.TabName
	} else {
		tabStr = tab.(string)
	}
	if tabStr == "" {
		tabStr = apistructs.WorkbenchItemProj.String()
	}
	return tabStr
}

func (wc *WorkCards) LoadList(sdk *cptype.SDK) {
	tabStr := wc.getTableName(sdk)
	data := cardlist.Data{}
	apiIdentity := apistructs.Identity{}
	apiIdentity.OrgID = sdk.Identity.OrgID
	apiIdentity.UserID = sdk.Identity.UserID
	cnt := 0
	switch tabStr {
	case apistructs.WorkbenchItemApp.String():
		data.Title = sdk.I18n("star application")
		apps, err := wc.Wb.ListSubAppWbData(apiIdentity, 0)
		if err != nil {
			return
		}
		data.TitleSummary = fmt.Sprintf("%d", len(apps.List))
		for _, app := range apps.List {
			if cnt >= DefaultCardListSize {
				break
			}
			cnt++
			data.Cards = append(data.Cards, cardlist.Card{
				ID:             fmt.Sprintf("%d", app.ID),
				ImgURL:         app.Logo,
				Title:          app.Name,
				TitleState:     wc.getAppTitleState(sdk, app.Mode),
				Star:           true,
				IconOperations: wc.getAppIconOps(sdk, app),
				TextMeta:       wc.getAppTextMeta(app),
				Operations:     wc.getAppCardOps(sdk, app),
			})
		}
	case apistructs.WorkbenchItemProj.String():
		data.Title = sdk.I18n("star project")
		projects, err := wc.Wb.ListSubProjWbData(apiIdentity)
		if err != nil {
			return
		}
		data.TitleSummary = fmt.Sprintf("%d", len(projects.List))
		ids := make([]uint64, 0)
		for _, project := range projects.List {
			ids = append(ids, project.ProjectDTO.ID)
		}

		params, err := wc.Wb.GetMspUrlParamsMap(apistructs.Identity{UserID: sdk.Identity.UserID, OrgID: sdk.Identity.OrgID}, ids, 0)
		if err != nil {
			logrus.Errorf("card list fail to get url params ,err :%v", err)
		}

		qMap, err := wc.Wb.GetProjIssueQueries(apiIdentity.UserID, ids, 0)
		if err != nil {
			logrus.Errorf("get project issue queries failed, project ids: %v, error:%v", ids, err)
			return
		}

		for _, project := range projects.List {
			if cnt >= DefaultCardListSize {
				break
			}
			projID := strconv.FormatUint(project.ProjectDTO.ID, 10)
			cnt++
			data.Cards = append(data.Cards, cardlist.Card{
				ID:             fmt.Sprintf("%d", project.ProjectDTO.ID),
				ImgURL:         project.ProjectDTO.Logo,
				Title:          project.ProjectDTO.DisplayName,
				TitleState:     wc.getProjectTitleState(sdk, project.ProjectDTO.Type),
				Star:           true,
				TextMeta:       wc.getProjTextMeta(sdk, project, qMap[project.ProjectDTO.ID], params[projID]),
				IconOperations: wc.getProjIconOps(sdk, project, params[projID]),
				Operations:     wc.getProjectCardOps(sdk, params[projID], project),
			})
		}
	}
	wc.StdDataPtr = &data
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "workCards", func() servicehub.Provider {
		return &WorkCards{}
	})
}
