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
	"os"
	"runtime/debug"
	"strconv"

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
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common/gshelper"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/i18n"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
)

const (
	CompWorkList = "workList"

	DefaultPageNo   uint64 = 1
	DefaultPageSize uint64 = 10
)

type WorkList struct {
	impl.DefaultList

	sdk       *cptype.SDK
	bdl       *bundle.Bundle
	wbSvc     *workbench.Workbench
	State     State
	identity  apistructs.Identity
	filterReq apistructs.WorkbenchProjAppRequest
}

type State struct {
	Tabs apistructs.WorkbenchItemType `json:"tabs"`
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, CompWorkList, func() servicehub.Provider {
		return &WorkList{}
	})
}

func (l *WorkList) Initialize(sdk *cptype.SDK) {}

func (l *WorkList) Finalize(sdk *cptype.SDK) {}

func (l *WorkList) BeforeHandleOp(sdk *cptype.SDK) {
	// get svc info
	l.sdk = sdk
	l.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	l.wbSvc = sdk.Ctx.Value(types.WorkbenchSvc).(*workbench.Workbench)

	// get identity info
	l.identity = apistructs.Identity{
		UserID: sdk.Identity.UserID,
		OrgID:  sdk.Identity.OrgID,
	}

	// get global related stat info
	gh := gshelper.NewGSHelper(sdk.GlobalState)
	tp, _ := gh.GetWorkbenchItemType()
	query, _ := gh.GetFilterName()
	// construct filter info, check & set default value
	l.filterReq = apistructs.WorkbenchProjAppRequest{
		Type:  tp,
		Query: query,
		PageRequest: apistructs.PageRequest{
			PageNo:   DefaultPageNo,
			PageSize: DefaultPageSize,
		},
	}
	if l.filterReq.Type.IsEmpty() {
		l.filterReq.Type = apistructs.WorkbenchItemDefault
	}
}

func (l *WorkList) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		l.StdDataPtr = l.doFilter()
		return nil
	}
}

func (l *WorkList) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return l.RegisterInitializeOp()
}

// RegisterChangePage when change page, filter needed
func (l *WorkList) RegisterChangePage(opData list.OpChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		logrus.Infof("change page client data: %+v", opData)
		if opData.ClientData.PageNo > 0 {
			l.filterReq.PageNo = opData.ClientData.PageNo
		}
		if opData.ClientData.PageSize > 0 {
			l.filterReq.PageSize = opData.ClientData.PageSize
		}
		l.StdDataPtr = l.doFilter()
		return nil
	}
}

// RegisterItemStarOp when item stared, filter is unnecessary
func (l *WorkList) RegisterItemStarOp(opData list.OpItemStar) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		var (
			tp      apistructs.SubscribeType
			tpID    uint64
			star    bool
			updated bool
		)

		if l.filterReq.Type == apistructs.WorkbenchItemProj {
			tp = apistructs.ProjectSubscribe
		} else {
			tp = apistructs.AppSubscribe
		}

		id, err := strconv.Atoi(opData.ClientData.DataRef.ID)
		if err != nil {
			logrus.Errorf("star operation, format ClientData id failed, id: %v, error: %v", opData.ClientData.DataRef.ID, err)
			return nil
		}
		tpID = uint64(id)

		// if not star, create subscribe & unstar; else delete subscribe & set state
		if opData.ClientData.DataRef.Star == nil {
			logrus.Errorf("nil star value")
			return nil
		}

		if !*opData.ClientData.DataRef.Star {
			req := apistructs.CreateSubscribeReq{
				Type:   tp,
				TypeID: tpID,
				Name:   opData.ClientData.DataRef.Title,
				UserID: l.identity.UserID,
			}
			_, err = l.bdl.CreateSubscribe(l.identity.UserID, l.identity.OrgID, req)
			if err != nil {
				logrus.Errorf("star %v %v failed, id: %v, error: %v", req.Type, req.Name, req.TypeID, err)
				return nil
			}
			star = true
		} else {
			req := apistructs.UnSubscribeReq{
				Type:   tp,
				TypeID: tpID,
				UserID: l.identity.UserID,
			}
			err = l.bdl.DeleteSubscribe(l.identity.UserID, l.identity.OrgID, req)
			if err != nil {
				logrus.Errorf("unstar failed, id: %v, error: %v", req.TypeID, err)
				return nil
			}
			star = false
		}
		// TODO: update data in place, do not need reload
		if l.StdDataPtr == nil {
			logrus.Errorf("std data prt is nil")
			return nil
		}
		for i := range l.StdDataPtr.List {
			item := l.StdDataPtr.List[i]
			if item.ID == opData.ClientData.DataRef.ID {
				l.StdDataPtr.List[i].Star = &star
				updated = true
				break
			}
		}
		if !updated {
			logrus.Errorf("cannot update star info in local data")
		}
		return nil
	}
}

func (l *WorkList) RegisterItemClickGotoOp(opData list.OpItemClickGoto) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *WorkList) RegisterItemClickOp(opData list.OpItemClick) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *WorkList) RegisterBatchOp(opData list.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *WorkList) doFilter() *list.Data {
	switch l.filterReq.Type {
	case apistructs.WorkbenchItemProj:
		return l.doFilterProj()
	case apistructs.WorkbenchItemApp:
		return l.doFilterApp()
	default:
		return l.doFilterProj()
	}
}

func (l *WorkList) doFilterProj() (data *list.Data) {
	data = &list.Data{
		PageNo:   l.filterReq.PageNo,
		PageSize: l.filterReq.PageSize,
		Title:    l.sdk.I18n(i18n.I18nKeyMyProject),
		List:     make([]list.Item, 0),
		UserIDs:  make([]string, 0),
		Operations: map[cptype.OperationKey]cptype.Operation{
			list.OpChangePage{}.OpKey(): cputil.NewOpBuilder().Build(),
		},
	}

	defer func() {
		if err := recover(); err != nil {
			logrus.Errorf("do filter project recover failed, error: %v", err)
			logrus.Errorf("%s", debug.Stack())
		}
	}()

	req := apistructs.GetSubscribeReq{Type: apistructs.ProjectSubscribe}
	subProjs, err := l.bdl.ListSubscribes(l.identity.UserID, l.identity.OrgID, req)
	if err != nil {
		logrus.Errorf("list subscribes failed, identity: %+v,request: %+v, error:%v", l.identity, req, err)
		return
	}

	maxSub, err := strconv.ParseInt(os.Getenv("SUBSCRIBE_LIMIT_NUM"), 10, 64)
	if err != nil {
		maxSub = 6
		logrus.Warnf("get env SUBSCRIBE_LIMIT_NUM failed ,%v set default max count is 6", err)
	}
	reachLimit := false
	if subProjs != nil && int64(len(subProjs.List)) == maxSub {
		reachLimit = true
	}

	subProjMap := make(map[uint64]bool)
	if subProjs != nil {
		for _, v := range subProjs.List {
			id := v.TypeID
			subProjMap[id] = true
		}
	}

	// list my project workbench data
	projs, err := l.wbSvc.ListQueryProjWbData(l.identity, l.filterReq.PageRequest, l.filterReq.Query)
	if err != nil {
		logrus.Errorf("list query projct workbench data failed, error: %v", err)
		return
	}
	if len(projs.List) == 0 {
		return data
	}

	data.Total = uint64(projs.Total)
	data.TitleSummary = strconv.FormatInt(int64(projs.Total), 10)

	// get msp url params
	var projIDs []uint64
	for _, v := range projs.List {
		projIDs = append(projIDs, v.ProjectDTO.ID)
	}

	mspParams, err := l.wbSvc.GetMspUrlParamsMap(l.identity, projIDs, 0)
	if err != nil {
		logrus.Errorf("get msp common params failed, error: %v", err)
		return
	}

	projQueries, err := l.wbSvc.GetProjIssueQueries(l.identity.UserID, projIDs, 0)
	if err != nil {
		logrus.Errorf("get projects issue queries failed, ids: %v, error: %v", projIDs, err)
		return
	}

	for _, p := range projs.List {
		projID := strconv.FormatUint(p.ProjectDTO.ID, 10)

		params := make(map[string]interface{})
		err = common.Transfer(mspParams[projID], &params)
		if err != nil {
			logrus.Errorf("transfer msp params failed, msp params: %+v, error: %v", mspParams, err)
			return
		}
		params["projectId"] = p.ProjectDTO.ID

		// get click goto issue query url
		queries := projQueries[p.ProjectDTO.ID]
		kvs, columns := l.GenProjKvColumnInfo(p, queries, params)
		star := subProjMap[p.ProjectDTO.ID]
		starTip := l.sdk.I18n(i18n.GenStarTip(apistructs.WorkbenchItemProj, star))
		// if starDisable and not collected, cover tip and star
		starDisable := false
		if reachLimit && !star {
			starDisable = true
			starTip = l.sdk.I18n(i18n.I18nStarProject) + l.sdk.I18n(i18n.I18nReachLimit)
		}
		target := ""
		switch p.ProjectDTO.Type {
		case common.DevOpsProject, common.DefaultProject:
			target = "project"
		case common.MspProject:
			target = common.OpValTargetMspOverview
		}

		ts, _ := l.GenProjTitleState(p.ProjectDTO.Type)
		item := list.Item{
			ID:          strconv.FormatUint(p.ProjectDTO.ID, 10),
			Icon:        &commodel.Icon{URL: p.ProjectDTO.Logo},
			Title:       p.ProjectDTO.DisplayName,
			TitleState:  ts,
			Star:        &star,
			Selectable:  true,
			KvInfos:     kvs,
			ColumnsInfo: columns,
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemStar{}.OpKey(): cputil.NewOpBuilder().WithDisable(starDisable, starTip).Build(),
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: params,
							Target: target,
						},
					}).
					Build(),
			},
		}
		data.List = append(data.List, item)
	}
	return
}

func (l *WorkList) doFilterApp() (data *list.Data) {
	data = &list.Data{}

	defer func() {
		if err := recover(); err != nil {
			logrus.Errorf("do filter app recover failed, error: %v", err)
			logrus.Errorf("%s", debug.Stack())
		}
	}()

	// list my subscribed apps
	sr := apistructs.GetSubscribeReq{Type: apistructs.AppSubscribe}
	subs, err := l.bdl.ListSubscribes(l.identity.UserID, l.identity.OrgID, sr)
	if err != nil {
		logrus.Errorf("list subscribes failed, identity: %+v,request: %+v, error:%v", l.identity, sr, err)
		return
	}
	subMap := make(map[uint64]bool)
	if subs != nil {
		for _, v := range subs.List {
			id := v.TypeID
			subMap[id] = true
		}
	}
	maxSub, err := strconv.ParseInt(os.Getenv("SUBSCRIBE_LIMIT_NUM"), 10, 64)
	if err != nil {
		maxSub = 6
		logrus.Warnf("get env SUBSCRIBE_LIMIT_NUM failed ,%v", err)
	}
	reachLimit := false
	if int64(len(subs.List)) == maxSub {
		reachLimit = true
	}

	// list app workbench data
	lr := apistructs.ApplicationListRequest{
		Query:    l.filterReq.Query,
		PageSize: int(l.filterReq.PageSize),
		PageNo:   int(l.filterReq.PageNo),
	}

	apps, err := l.wbSvc.ListAppWbData(l.identity, lr, 0)
	if err != nil {
		logrus.Errorf("list query app workbench data failed, error: %v", err)
		return
	}

	data = &list.Data{
		Total:        uint64(apps.TotalApps),
		PageNo:       l.filterReq.PageNo,
		PageSize:     l.filterReq.PageSize,
		Title:        l.sdk.I18n(i18n.I18nKeyMyApp),
		TitleSummary: strconv.FormatInt(int64(apps.TotalApps), 10),
		Operations: map[cptype.OperationKey]cptype.Operation{
			list.OpChangePage{}.OpKey(): cputil.NewOpBuilder().Build(),
		},
	}

	for _, p := range apps.List {
		star := subMap[p.ID]
		starTip := l.sdk.I18n(i18n.GenStarTip(apistructs.WorkbenchItemApp, star))
		// if starDisable and not collected, cover tip and star
		starDisable := false
		if reachLimit && !star {
			starDisable = true
			starTip = l.sdk.I18n(i18n.I18nStarAPP) + l.sdk.I18n(i18n.I18nReachLimit)
		}
		ts, _ := l.GenAppTitleState(p.Mode)
		item := list.Item{
			ID:          strconv.FormatUint(p.ID, 10),
			Icon:        &commodel.Icon{URL: p.Logo},
			Title:       p.Name,
			TitleState:  ts,
			Star:        &star,
			Selectable:  true,
			KvInfos:     l.GenAppKvInfo(p),
			ColumnsInfo: l.GenAppColumnInfo(p),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemStar{}.OpKey(): cputil.NewOpBuilder().WithDisable(starDisable, starTip).Build(),
				list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
					WithSkipRender(true).
					WithServerDataPtr(list.OpItemClickGotoServerData{
						OpItemBasicServerData: list.OpItemBasicServerData{
							Params: map[string]interface{}{
								common.OpKeyProjectID: p.ProjectID,
								common.OpKeyAppID:     p.ID,
							},
							Target: common.OpValTargetRepo,
						},
					}).
					Build(),
			},
		}
		data.List = append(data.List, item)
	}
	return
}
