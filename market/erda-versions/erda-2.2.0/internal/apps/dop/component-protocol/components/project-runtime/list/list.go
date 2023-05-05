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

package page

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math"
	"strconv"
	"strings"
	"time"

	"github.com/gogap/errors"
	"github.com/recallsong/go-utils/container/slice"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/commodel"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/list"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/list/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-runtime/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type List struct {
	base.DefaultProvider
	impl.DefaultList
	PageNo   uint64
	PageSize uint64
	Total    uint64
	State    cptype.ExtraMap
	Bdl      *bundle.Bundle
	Sdk      *cptype.SDK
}

func (p *List) RegisterItemStarOp(opData list.OpItemStar) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (p *List) RegisterItemClickGotoOp(opData list.OpItemClickGoto) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (p *List) RegisterItemClickOp(opData list.OpItemClick) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		// rerender list after any operation
		req := apistructs.RuntimeScaleRecords{}
		idStr := opData.ClientData.DataRef.ID
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			logrus.Errorf("failed to parse id ,err: %v", err)
		}
		req.IDs = []uint64{id}

		orgId, err := strconv.ParseUint(p.Sdk.Identity.OrgID, 10, 64)
		if err != nil {
			logrus.Errorf("failed to parse org-id ,err: %v", err)
		}
		switch opData.ClientData.OperationRef.ID {
		case common.DeleteOp:
			var resp apistructs.BatchRuntimeDeleteResults
			resp, err = p.Bdl.BatchUpdateDelete(req, orgId, p.Sdk.Identity.UserID, "delete")
			if err != nil {
				logrus.Errorf("delete runtime err %v", err)
			}
			if len(resp.UnDeletedIds) != 0 {
				logrus.Errorf("failed to %s runtimes ,ids :%v", opData.ClientData.OperationRef.ID, resp.UnDeletedIds)
			}
		case common.ReStartOp:
			var resp apistructs.BatchRuntimeReDeployResults
			resp, err = p.Bdl.BatchUpdateReDeploy(req, orgId, p.Sdk.Identity.UserID, "reDeploy")
			if err != nil {
				logrus.Errorf("redeploy runtime err %v", err)
			}
			if len(resp.UnReDeployedIds) != 0 {
				logrus.Errorf("failed to %s runtimes ,ids :%v", opData.ClientData.OperationRef.ID, resp.UnReDeployedIds)
			}
		}
		p.StdDataPtr = p.getData()
		return nil
	}
}

type ClickClientData struct {
	SelectedRowID string
	DataRef       list.MoreOpItem
}

func (p *List) RegisterBatchOp(opData list.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		req := apistructs.RuntimeScaleRecords{}
		for _, idStr := range opData.ClientData.SelectedRowIDs {
			id, err := strconv.ParseUint(idStr, 10, 64)
			if err != nil {
				logrus.Errorf("failed to parse id ,err: %v", err)
			}
			req.IDs = append(req.IDs, id)
		}

		orgId, err := strconv.ParseUint(p.Sdk.Identity.OrgID, 10, 64)
		if err != nil {
			logrus.Errorf("failed to parse org-id ,err: %v", err)
		}
		switch opData.ClientData.SelectedOptionsID {
		case common.DeleteOp:
			var resp apistructs.BatchRuntimeDeleteResults
			resp, err = p.Bdl.BatchUpdateDelete(req, orgId, p.Sdk.Identity.UserID, "delete")
			if err != nil {
				logrus.Errorf("delete runtime err %v", err)
			}
			if len(resp.UnDeletedIds) != 0 {
				logrus.Errorf("failed to %s runtimes ,ids :%v", opData.ClientData.SelectedOptionsID, resp.UnDeletedIds)
			}
		case common.ReStartOp:
			var resp apistructs.BatchRuntimeReDeployResults
			resp, err = p.Bdl.BatchUpdateReDeploy(req, orgId, p.Sdk.Identity.UserID, "reDeploy")
			if err != nil {
				logrus.Errorf("redeploy runtime err %v", err)
			}
			if len(resp.UnReDeployedIds) != 0 {
				logrus.Errorf("failed to %s runtimes ,ids :%v", opData.ClientData.SelectedOptionsID, resp.UnReDeployedIds)
			}
		}
		p.StdDataPtr = p.getData()
		return nil
	}
}

func (p *List) BeforeHandleOp(sdk *cptype.SDK) {
	p.State = cptype.ExtraMap{}
	p.Sdk = sdk
	p.Bdl = p.Sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	p.PageNo = 1
	p.PageSize = 10
}

func (p *List) RegisterChangePage(opData list.OpChangePage) (opFunc cptype.OperationFunc) {
	logrus.Infof("change page client data: %+v", opData)
	if opData.ClientData.PageNo > 0 {
		p.PageNo = opData.ClientData.PageNo
		p.State["pageNo"] = p.PageNo
	}
	if opData.ClientData.PageSize > 0 {
		p.PageSize = opData.ClientData.PageSize
		p.State["pageSize"] = p.PageSize
	}
	return p.RegisterRenderingOp()
}

func (p *List) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		logrus.Debug("list component init")
		if urlquery := sdk.InParams.String("list__urlQuery"); urlquery != "" {
			if page, err := p.flushOptsByFilter(urlquery); err != nil {
				logrus.Errorf("failed to transfer values in component advance filter")
				return nil
			} else {
				if page.PageNo == 0 || page.PageSize == 0 {
					p.PageSize = 10
					p.PageNo = 1
					p.State["pageSize"] = 10
					p.State["pageNo"] = 1
				} else {
					p.PageNo = page.PageNo
					p.PageSize = page.PageSize
					p.State["pageSize"] = p.PageSize
					p.State["pageNo"] = p.PageNo
				}
			}
		}
		urlParam, err := p.generateUrlQueryParams(p.State)
		if err != nil {
			logrus.Errorf("fail to parse list url")
			return nil
		}
		p.State["list__urlQuery"] = urlParam
		p.Sdk = sdk
		p.StdDataPtr = p.getData()
		p.StdStatePtr = &p.State
		return nil
	}
}

type Page struct {
	PageSize uint64
	PageNo   uint64
}

func (p *List) flushOptsByFilter(filterEntity string) (*Page, error) {
	page := &Page{}
	b, err := base64.StdEncoding.DecodeString(filterEntity)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(b, page)
	if err != nil {
		return nil, err
	}
	return page, nil
}
func (p *List) generateUrlQueryParams(Values cptype.ExtraMap) (string, error) {
	fb, err := json.Marshal(Values)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(fb), nil
}

func (p *List) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		logrus.Debug("list component rendering")
		p.Sdk = sdk
		urlParam, err := p.generateUrlQueryParams(p.State)
		if err != nil {
			logrus.Errorf("fail to parse list url")
			return nil
		}
		p.State["list__urlQuery"] = urlParam
		p.StdDataPtr = p.getData()
		p.StdStatePtr = &p.State
		return nil
	}
}

func (p *List) Init(ctx servicehub.Context) error {
	return p.DefaultProvider.Init(ctx)
}

func (p *List) initMyApps(sdk *cptype.SDK, oid, projectId uint64) (map[uint64]string, error) {
	myApp := make(map[uint64]string)
	apps, err := p.Bdl.GetMyApps(sdk.Identity.UserID, oid)
	if err != nil {
		logrus.Errorf("get my app failed,%v", err)
		return myApp, errors.Errorf("get my app failed,%v", err)
	}
	for i := 0; i < len(apps.List); i++ {
		if apps.List[i].ProjectID != projectId {
			continue
		}
		myApp[apps.List[i].ID] = apps.List[i].Name
	}
	return myApp, nil
}

func (p *List) getData() *list.Data {
	data := &list.Data{}
	data.PageNo = p.PageNo
	data.PageSize = p.PageSize
	var runtimes []bundle.GetApplicationRuntimesDataEle
	var runtimeIdToAppNameMap map[uint64]string
	var myApp map[uint64]string

	if gsRuntimes, ok := (*p.Sdk.GlobalState)["runtimes"]; !ok {
		logrus.Infof("not found runtimes")
		getEnv, ok := p.Sdk.InParams["env"].(string)
		if !ok {
			logrus.Errorf("env is empty")
			return data
		}
		projectId, err := strconv.ParseUint(p.Sdk.InParams["projectId"].(string), 10, 64)
		if err != nil {
			logrus.Errorf("parse oid failed,%v", err)
			return data
		}
		oid, err := strconv.ParseUint(p.Sdk.Identity.OrgID, 10, 64)
		if err != nil {
			logrus.Errorf("parse oid failed,%v", err)
			return data
		}
		appIds := make([]uint64, 0)
		appIdToName := make(map[uint64]string)

		allApps, err := p.Bdl.GetAppList(p.Sdk.Identity.OrgID, p.Sdk.Identity.UserID, apistructs.ApplicationListRequest{
			ProjectID: projectId,
			IsSimple:  true,
			PageSize:  math.MaxInt32,
			PageNo:    1})
		if err != nil {
			logrus.Errorf("failed to get app list ,err =%+v", err)
			return nil
		}
		for i := 0; i < len(allApps.List); i++ {
			appIds = append(appIds, allApps.List[i].ID)
			appIdToName[allApps.List[i].ID] = allApps.List[i].Name
		}
		myApp, err = p.initMyApps(p.Sdk, oid, projectId)
		if err != nil {
			logrus.Errorf("get my app failed,%v", err)
			return data
		}

		logrus.Infof("start load runtimes by app %v", time.Now())

		runtimesByApp, err := p.Bdl.ListRuntimesGroupByApps(oid, p.Sdk.Identity.UserID, appIds, getEnv)
		if err != nil {
			logrus.Errorf("get my app failed,%v", err)
			return data
		}
		logrus.Infof("finish load runtimes by app %v", time.Now())

		runtimeIdToAppNameMap = make(map[uint64]string)
		for _, v := range runtimesByApp {
			for _, appRuntime := range v {
				if getEnv == appRuntime.Extra.Workspace {
					runtimes = append(runtimes, *appRuntime)
					runtimeIdToAppNameMap[appRuntime.ID] = appIdToName[appRuntime.ApplicationID]
				}
			}
		}
	} else {
		logrus.Infof("found runtimes")
		err := common.Transfer(gsRuntimes, &runtimes)
		if err != nil {
			logrus.Errorf("failed to transfer runtimes, gsruntimes %v", err)
			return data
		}
		runtimeIdToAppNameMap = make(map[uint64]string)
		if gsMap, ok := (*p.Sdk.GlobalState)["runtimeIdToAppName"]; ok {
			err = common.Transfer(gsMap, &runtimeIdToAppNameMap)
			if err != nil {
				logrus.Errorf("failed to transfer runtimeMap, runtimeMap %v,err %v", (*p.Sdk.GlobalState)["runtimeIdToAppName"], err)
				return data
			}
		} else {
			logrus.Errorf("failed to transfer runtimeMap, runtimeMap %#v", (*p.Sdk.GlobalState)["runtimeIdToAppName"])
			return data
		}
		myApp = (*p.Sdk.GlobalState)["myApp"].(map[uint64]string)
	}
	logrus.Infof("runtimes:%v", runtimes)
	//oid, err := strconv.ParseUint(p.Sdk.Identity.OrgID, 10, 64)
	//if err != nil {
	//	logrus.Errorf("failed to get oid ,%v", err)
	//	return data
	//}
	myAppNames := make(map[string]bool)

	userReq := apistructs.UserListRequest{}
	for _, runtime := range runtimes {
		userReq.UserIDs = append(userReq.UserIDs, runtime.LastOperator)
		for _, appName2 := range myApp {
			if runtimeIdToAppNameMap[runtime.ID] == appName2 {
				myAppNames[appName2] = true
			}
		}
	}
	logrus.Infof("start load users %v", time.Now())

	users, err := p.Bdl.ListUsers(userReq)
	if err != nil {
		logrus.Errorf("failed to load users,err:%v", err)
		return data
	}
	logrus.Infof("finish load users %v", time.Now())

	uidToName := make(map[string]string)
	for _, user := range users.Users {
		if user.Nick == "" {
			uidToName[user.ID] = user.Name
		} else {
			uidToName[user.ID] = user.Nick
		}
		logrus.Infof("%s : %s", user.ID, uidToName[user.ID])
	}
	ids := make([]string, 0)
	deployId := p.Sdk.InParams["deployId"]
	runtimeMap := make(map[string]bundle.GetApplicationRuntimesDataEle)
	//oid, _ := strconv.ParseUint(p.Sdk.Identity.OrgID, 10, 64)
	for _, appRuntime := range runtimes {
		//healthyMap := make(map[string]int)
		deployIdStr := strconv.FormatUint(appRuntime.LastOperatorId, 10)
		if deployId != nil && deployId != "" {
			if deployId != deployIdStr {
				continue
			}
		}
		//healthyMap[appRuntime.Name] = healthyCnt
		//healthyCnt := 0
		//for _, s := range appRuntime.Services {
		//	if s.Status == "Healthy" {
		//		healthyCnt++
		//	}
		//}
		//var healthStr = ""
		//if len(appRuntime.Services) != 0 {
		//	healthStr = fmt.Sprintf("%d/%d", healthyCnt, len(appRuntime.Services))
		//}
		idStr := strconv.FormatUint(appRuntime.ID, 10)
		appIdStr := strconv.FormatUint(appRuntime.ApplicationID, 10)
		nameStr := appRuntime.Name
		if runtimeIdToAppNameMap[appRuntime.ID] != nameStr {
			nameStr = runtimeIdToAppNameMap[appRuntime.ID] + "/" + nameStr
		}
		logrus.Infof("%s : %s", appRuntime.Name, appRuntime.LastOperator)
		_, isMyApp := myApp[appRuntime.ApplicationID]
		item := list.Item{
			ID:         idStr,
			Title:      nameStr,
			TitleState: getTitleState(p.Sdk, appRuntime.RawDeploymentStatus, deployIdStr, appIdStr, appRuntime.DeleteStatus, isMyApp),
			Selectable: isMyApp,
			//KvInfos:        getKvInfos(p.Sdk, runtimeIdToAppNameMap[appRuntime.ID], uidToName[appRuntime.LastOperator], appRuntime.DeploymentOrderName, appRuntime.ReleaseVersion, healthStr, appRuntime, appRuntime.LastOperateTime),
			Operations:     getOperations(p.Sdk, appRuntime.ProjectID, appRuntime.ApplicationID, appRuntime.ID, isMyApp),
			MoreOperations: getMoreOperations(p.Sdk, fmt.Sprintf("%d", appRuntime.ID)),
		}
		data.List = append(data.List, item)
		ids = append(ids, idStr)
		runtimeMap[idStr] = appRuntime
	}

	data.Operations = p.getBatchOperation(p.Sdk, ids)
	// filter by name and advanced condition
	var advancedFilter map[string][]string
	if advancedFilterMap, ok := (*p.Sdk.GlobalState)["advanceFilter"]; ok {
		err := common.Transfer(advancedFilterMap, &advancedFilter)
		if err != nil {
			logrus.Errorf("parse advanced filter failed err :%v", err)
		}
		for k, v := range advancedFilter {
			if len(v) == 0 {
				delete(advancedFilter, k)
			}
		}
	}
	var filterName string
	if nameFilterValue, ok := (*p.Sdk.GlobalState)["nameFilter"]; ok {
		cputil.MustObjJSONTransfer(nameFilterValue, &filterName)
		filterName = strings.Trim(filterName, " ")
	}
	logrus.Infof("inputFilter: %v", filterName)
	logrus.Infof("advanceFilter: %#v", advancedFilter)
	filter := make(map[string]map[string]bool)
	for k, v := range advancedFilter {
		filter[k] = make(map[string]bool)
		for _, value := range v {
			if k == common.FilterApp && value == common.ALLINVOLVEAPP {
				filter[k] = make(map[string]bool)
				for str := range myAppNames {
					filter[k][str] = true
				}
			}
			filter[k][value] = true
		}
	}
	var needFilter = data.List
	data.List = make([]list.Item, 0)

	for i := 0; i < len(needFilter); i++ {
		runtime := runtimeMap[needFilter[i].ID]
		if filterName != "" {
			if !common.ExitsWithoutCase(needFilter[i].Title, filterName) {
				continue
			}
		}
		if p.doFilter(filter, runtime, runtime.LastOperateTime.UnixNano()/1e6, runtimeIdToAppNameMap[runtime.ID], runtime.DeploymentOrderName) {
			data.List = append(data.List, needFilter[i])
		}
	}
	//logrus.Infof("list after filter: %#v", data.List)

	slice.Sort(data.List, func(i, j int) bool {
		return data.List[i].Title < data.List[j].Title
	})
	data.Total = uint64(len(data.List))
	start := (p.PageNo - 1) * p.PageSize
	if start >= data.Total {
		start = 0
	}
	//logrus.Infof("list after sort: %#v", data.List)

	end := uint64(math.Min(float64((p.PageNo)*p.PageSize), float64(data.Total)))

	data.List = data.List[start:end]
	serviceRuntimes := make([]uint64, 0)
	for _, r := range data.List {
		rid, err := strconv.ParseUint(r.ID, 10, 64)
		if err != nil {
			continue
		}
		serviceRuntimes = append(serviceRuntimes, rid)
	}
	services, err := p.Bdl.BatchGetRuntimeServices(serviceRuntimes, p.Sdk.Identity.OrgID, p.Sdk.Identity.UserID)
	if err != nil {
		logrus.Errorf("failed to get services err = %v", err)
	}
	healthyCntMap := make(map[uint64]int)
	serviceCntMap := make(map[uint64]int)
	for rid, s := range services {
		serviceCntMap[rid] = len(s.Services)
		logrus.Infof("services %d:%v", rid, s)
		healthyCnt := 0
		for _, ss := range s.Services {
			if ss.Status == "Healthy" {
				healthyCnt++
			}
		}
		healthyCntMap[rid] = healthyCnt

	}

	for i := 0; i < len(data.List); i++ {
		var healthyCnt int
		var serviceCnt int
		rid, _ := strconv.ParseUint(data.List[i].ID, 10, 64)
		if c, ok := healthyCntMap[rid]; ok {
			healthyCnt = c
		}
		if c, ok := serviceCntMap[rid]; ok {
			serviceCnt = c
		}
		healthStr := fmt.Sprintf("%d/%d", healthyCnt, serviceCnt)
		appRuntime := runtimeMap[data.List[i].ID]
		// set icon once without service.
		data.List[i].KvInfos = getKvInfos(p.Sdk, runtimeIdToAppNameMap[appRuntime.ID], uidToName[appRuntime.Creator], appRuntime.DeploymentOrderName, appRuntime.ReleaseVersion, healthStr, appRuntime, appRuntime.LastOperateTime)
		data.List[i].Icon = getIconByServiceCnt(healthyCnt, serviceCnt)
	}
	return data
}

func (p *List) doFilter(conds map[string]map[string]bool, appRuntime bundle.GetApplicationRuntimesDataEle, deployAt int64, appName, deploymentOrderName string) bool {
	if len(conds) == 0 {
		return true
	}
	for k, v := range conds {
		switch k {
		case common.FilterApp:
			if _, ok := v[appName]; !ok {
				return false
			}
		//case common.FilterRuntimeStatus:
		//	for _, value := range v {
		//		if value == appRuntime.RawStatus {
		//			return true
		//		}
		//	}
		case common.FilterDeployStatus:
			if _, ok := v[appRuntime.RawDeploymentStatus]; !ok {
				return false
			}
		case common.FilterDeployOrderName:
			if _, ok := v[deploymentOrderName]; !ok {
				return false
			}
			//case common.FilterDeployTime:
			//	startTime, err := strconv.ParseInt(v[0], 10, 64)
			//	if err != nil {
			//		logrus.Errorf("parse filter time range failed ,err :%v", err)
			//	}
			//	endTime, err := strconv.ParseInt(v[1], 10, 64)
			//	if err != nil {
			//		logrus.Errorf("parse filter time range failed ,err :%v", err)
			//	}
			//	if startTime <= deployAt && endTime >= deployAt {
			//		return true
			//	}
		}
	}
	return true
}

func getIcon(runtimeStatus string) *commodel.Icon {
	var (
		statusStr string
	)
	switch runtimeStatus {
	case apistructs.RuntimeStatusHealthy:
		statusStr = common.FrontedIconBreathing
	//case apistructs.RuntimeStatusUnHealthy:
	//	statusStr = common.FrontedStatusError
	default:
		statusStr = common.FrontedIconLoading
	}

	return &commodel.Icon{URL: statusStr}
}

func getIconByServiceCnt(svcCnt, allCnt int) *commodel.Icon {
	var (
		statusStr string
	)
	if svcCnt == 0 || svcCnt < allCnt {
		statusStr = common.FrontedIconLoading
	} else {
		statusStr = common.FrontedIconBreathing
	}
	return &commodel.Icon{URL: statusStr}
}

func getTitleState(sdk *cptype.SDK, deployStatus, deploymentId, appId, dStatus string, isMy bool) []list.StateInfo {
	if dStatus == "" {
		var deployStr list.ItemCommStatus
		switch deployStatus {
		case string(apistructs.DeploymentStatusInit), string(apistructs.DeploymentStatusDeploying), string(apistructs.DeploymentStatusWaiting):
			deployStr = common.FrontedStatusProcessing
		case string(apistructs.DeploymentStatusOK):
			deployStr = common.FrontedStatusSuccess
		case string(apistructs.DeploymentStatusFailed):
			deployStr = common.FrontedStatusError
		case string(apistructs.DeploymentStatusCanceling):
			deployStr = common.FrontedStatusWarning
		case string(apistructs.DeploymentStatusCanceled):
			deployStr = common.FrontedStatusDefault
		}

		info := []list.StateInfo{
			{
				Status:     deployStr,
				Text:       sdk.I18n(deployStatus),
				SuffixIcon: "right",
			},
		}
		if isMy {
			info[0].Operations = map[cptype.OperationKey]cptype.Operation{
				"click": {
					SkipRender: true,
					ServerData: &cptype.OpServerData{
						"logId": deploymentId,
						"appId": appId,
					},
				},
			}
		}
		return info
	} else {
		return []list.StateInfo{
			{
				Status:     common.FrontedStatusProcessing,
				Text:       sdk.I18n(dStatus),
				Operations: nil,
			},
		}
	}
}

func getOperations(sdk *cptype.SDK, projectId, appId, runtimeId uint64, isMyApp bool) map[cptype.OperationKey]cptype.Operation {
	tip := ""
	if !isMyApp {
		tip = sdk.I18n("no authority found")
	}
	projectIdStr := fmt.Sprintf("%d", projectId)
	appIdStr := fmt.Sprintf("%d", appId)
	runtimeIdStr := fmt.Sprintf("%d", runtimeId)
	return map[cptype.OperationKey]cptype.Operation{
		"clickGoto": {
			Disabled: !isMyApp,
			Tip:      tip,
			ServerData: &cptype.OpServerData{
				"target": "projectDeployRuntime",
				"params": map[string]string{
					"projectId": projectIdStr,
					"appId":     appIdStr,
					"runtimeId": runtimeIdStr,
				},
			},
		},
	}
}

// getBatchOperation different runtime need different operation.
func (p List) getBatchOperation(sdk *cptype.SDK, ids []string) map[cptype.OperationKey]cptype.Operation {
	return map[cptype.OperationKey]cptype.Operation{
		"changePage": {},
		"batchRowsHandle": {
			ServerData: &cptype.OpServerData{
				"options": []list.OpBatchRowsHandleOptionServerData{
					{
						AllowedRowIDs: ids, Icon: &commodel.Icon{Type: "chongxinqidong"}, ID: common.ReStartOp, Text: sdk.I18n("restart"), // allowedRowIDs = null 或不传这个key，表示所有都可选，allowedRowIDs=[]表示当前没有可选择，此处应该可以不传
					},
					{
						AllowedRowIDs: ids, Icon: &commodel.Icon{Type: "remove"}, ID: common.DeleteOp, Text: sdk.I18n("delete"),
					},
				},
			},
		},
	}
}
func getMoreOperations(sdk *cptype.SDK, id string) []list.MoreOpItem {
	return []list.MoreOpItem{
		{
			ID:   common.DeleteOp,
			Icon: "remove",
			Text: sdk.I18n("delete"),
			Operations: map[cptype.OperationKey]cptype.Operation{
				"click": {
					Confirm:    sdk.I18n("delete confirm") + "?",
					ClientData: &cptype.OpClientData{},
				},
			},
		},
		{
			ID:   common.ReStartOp,
			Icon: "chongxinqidong",
			Text: sdk.I18n("restart"),
			Operations: map[cptype.OperationKey]cptype.Operation{
				"click": {
					ClientData: &cptype.OpClientData{},
				},
			},
		},
		//{
		//	ID:   id,
		//	Icon: "shuaxin",
		//	// todo
		//	Text: sdk.I18n("delete"),
		//	Operations: map[cptype.OperationKey]cptype.Operation{
		//		"click": {
		//			// todo
		//			Confirm:    sdk.I18n("delete confirm"),
		//			ClientData: &cptype.OpClientData{},
		//		},
		//	},
		//},
	}
}

func getKvInfos(sdk *cptype.SDK, appName, creatorName, deployOrderName, deployVersion, healthyStr string, runtime bundle.GetApplicationRuntimesDataEle, lastOperatorTime time.Time) []list.KvInfo {
	kvs := make([]list.KvInfo, 0)
	if healthyStr != "" {
		// service
		kvs = append(kvs, list.KvInfo{
			Key:   sdk.I18n("service"),
			Value: healthyStr,
		})
	}
	if deployOrderName != "" {
		tip := ""
		tip += fmt.Sprintf("%s: %s\n", sdk.I18n("release product"), deployVersion)
		tip += fmt.Sprintf("%s: %s\n", sdk.I18n("deployer"), creatorName)
		tip += fmt.Sprintf("%s: %s", sdk.I18n("deployAt"), runtime.LastOperateTime.Format("2006-01-02 15:04:05"))
		// deployment order name
		kvs = append(kvs, list.KvInfo{
			Key:   sdk.I18n("deploymentOrderName"),
			Value: deployOrderName,
			Tip:   tip,
		})
	}
	minutes := int64(time.Now().Sub(lastOperatorTime).Minutes())
	day := minutes / 1440
	hour := (minutes - (1440 * day)) / 60
	minute := minutes - (1440 * day) - (60 * hour)
	timeStr := ""
	if day == 0 {
		if hour == 0 {
			timeStr = fmt.Sprintf("%dm ", minute) + sdk.I18n("ago")
		} else {
			timeStr = fmt.Sprintf("%dh ", hour) + sdk.I18n("ago")
		}
	} else {
		timeStr = fmt.Sprintf("%dd ", day) + sdk.I18n("ago")
	}
	// running duration
	kvs = append(kvs, list.KvInfo{
		Key:   sdk.I18n("running duration"),
		Value: timeStr,
	})

	// app
	kvs = append(kvs, list.KvInfo{
		Key:   sdk.I18n("app"),
		Value: appName,
	})
	return kvs
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "list", func() servicehub.Provider {
		return &List{}
	})
}
