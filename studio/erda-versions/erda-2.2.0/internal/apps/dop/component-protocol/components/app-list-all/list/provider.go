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

package list

import (
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
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/app-list-all/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

const (
	DefaultPageNo   = 1
	DefaultPageSize = 20
)

type List struct {
	impl.DefaultList

	sdk       *cptype.SDK
	bdl       *bundle.Bundle
	identity  apistructs.Identity
	filterReq *apistructs.ApplicationListRequest
}

func init() {
	base.InitProviderWithCreator("app-list-all", "list", func() servicehub.Provider {
		return &List{}
	})
}

func (l *List) Initialize(sdk *cptype.SDK) {}

func (l *List) Finalize(sdk *cptype.SDK) {}

func (l *List) BeforeHandleOp(sdk *cptype.SDK) {
	l.sdk = sdk
	l.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	l.identity = apistructs.Identity{
		UserID: sdk.Identity.UserID,
		OrgID:  sdk.Identity.OrgID,
	}
	gh := gshelper.NewGSHelper(sdk.GlobalState)
	req, _ := gh.GetAppPagingRequest()
	req.PageNo = DefaultPageNo
	req.PageSize = DefaultPageSize
	req.ProjectID = l.StdInParamsPtr.Uint64("projectId")
	l.filterReq = req
}

func (l *List) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		l.StdDataPtr = l.doFilterApp()
		return nil
	}
}

func (l *List) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return l.RegisterInitializeOp()
}

func (l *List) RegisterChangePage(opData list.OpChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		if opData.ClientData.PageNo > 0 {
			l.filterReq.PageNo = int(opData.ClientData.PageNo)
		}
		if opData.ClientData.PageSize > 0 {
			l.filterReq.PageSize = int(opData.ClientData.PageSize)
		}
		l.StdDataPtr = l.doFilterApp()
		return nil
	}
}

func (l *List) RegisterItemClickGotoOp(opData list.OpItemClickGoto) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *List) RegisterItemStarOp(opData list.OpItemStar) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *List) RegisterItemClickOp(opData list.OpItemClick) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *List) RegisterBatchOp(opData list.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *List) doFilterApp() (data *list.Data) {
	data = &list.Data{}
	gh := gshelper.NewGSHelper(l.sdk.GlobalState)
	selectedOption := gh.GetOption()
	apps, err := l.appListRetriever(selectedOption)
	if err != nil {
		logrus.Errorf("list query app workbench data failed, error: %v", err)
		panic(err)
	}

	data = &list.Data{
		Total:    uint64(apps.Total),
		PageNo:   uint64(l.filterReq.PageNo),
		PageSize: uint64(l.filterReq.PageSize),
		Operations: map[cptype.OperationKey]cptype.Operation{
			list.OpChangePage{}.OpKey(): cputil.NewOpBuilder().Build(),
		},
	}

	myAppMap := make(map[uint64]bool)
	if selectedOption != "my" {
		orgID, err := strconv.Atoi(l.identity.OrgID)
		if err != nil {
			panic(err)
		}
		myApps, err := l.bdl.GetAllMyApps(l.identity.UserID, uint64(orgID), apistructs.ApplicationListRequest{
			ProjectID: l.filterReq.ProjectID,
			IsSimple:  true,
			Query:     l.filterReq.Query,
			PageNo:    1,
			PageSize:  1000,
		})
		if err != nil {
			panic(err)
		}
		for i := 0; i < len(myApps.List); i++ {
			myAppMap[myApps.List[i].ID] = true
		}
	}
	var appIDs []uint64
	for i := range apps.List {
		appIDs = append(appIDs, apps.List[i].ID)
	}

	mrResult, err := l.bdl.MergeRequestCount(l.identity.UserID, apistructs.MergeRequestCountRequest{
		AppIDs: appIDs,
		State:  "open",
	})
	if err != nil {
		logrus.Errorf("list open mr failed, appIDs: %v, error: %v", appIDs, err)
		return
	}

	for _, p := range apps.List {
		_, ok := myAppMap[p.ID]
		authorized := selectedOption == "my" || ok
		item := list.Item{
			ID:          strconv.FormatUint(p.ID, 10),
			Icon:        &commodel.Icon{URL: p.Logo},
			Title:       p.Name,
			Selectable:  authorized,
			KvInfos:     l.GenAppKvInfo(p, mrResult[strconv.FormatUint(p.ID, 10)]),
			Description: l.appDescription(p.Desc),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): func() cptype.Operation {
					builder := cputil.NewOpBuilder().
						WithSkipRender(true).
						WithServerDataPtr(list.OpItemClickGotoServerData{
							OpItemBasicServerData: list.OpItemBasicServerData{
								Params: map[string]interface{}{
									gshelper.OpKeyProjectID: p.ProjectID,
									gshelper.OpKeyAppID:     p.ID,
								},
								Target: gshelper.OpValTargetRepo,
							},
						})
					if !authorized {
						builder = builder.WithDisable(true, l.sdk.I18n("appNotAuthorized"))
					}
					return builder.Build()
				}(),
			},
		}
		data.List = append(data.List, item)
	}
	return
}

func (l *List) appDescription(desc string) string {
	if len(desc) == 0 {
		return l.sdk.I18n("defaultAppDescription")
	}
	return desc
}

func (l *List) appListRetriever(option string) (*apistructs.ApplicationListResponseData, error) {
	if option == "my" {
		orgID, err := strconv.Atoi(l.identity.OrgID)
		if err != nil {
			return nil, err
		}
		return l.bdl.GetAllMyApps(l.identity.UserID, uint64(orgID), *l.filterReq)
	}
	return l.bdl.GetAppList(l.identity.OrgID, l.identity.UserID, *l.filterReq)
}
