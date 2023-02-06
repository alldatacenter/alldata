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
	"fmt"
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
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/i18n"
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
	filterReq *apistructs.ProjectListRequest
	visible   bool
}

func init() {
	base.InitProviderWithCreator("project-list-all", "list", func() servicehub.Provider {
		return &List{}
	})
}

func (l *List) Initialize(sdk *cptype.SDK) {}

func (l *List) Finalize(sdk *cptype.SDK) {}

func (l *List) BeforeHandleOp(sdk *cptype.SDK) {
	l.sdk = sdk
	l.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	orgID, err := strconv.Atoi(l.sdk.Identity.OrgID)
	if err != nil {
		panic(err)
	}
	gh := gshelper.NewGSHelper(sdk.GlobalState)
	req, _ := gh.GetProjectPagingRequest()
	req.PageNo = DefaultPageNo
	req.PageSize = DefaultPageSize
	req.OrgID = uint64(orgID)
	req.OrderBy = "activeTime"
	l.filterReq = req
}

func (l *List) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		l.StdDataPtr = l.doFilterProject()
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
		l.StdDataPtr = l.doFilterProject()
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
		projectID := opData.ClientData.DataRef.ID
		switch opData.ClientData.OperationRef.ID {
		case "exit":
			req := apistructs.MemberRemoveRequest{
				Scope: apistructs.Scope{
					Type: apistructs.ProjectScope,
					ID:   projectID,
				},
				UserIDs: []string{l.sdk.Identity.UserID},
			}
			req.UserID = l.sdk.Identity.UserID
			fmt.Println("exit")
			if err := l.bdl.DeleteMember(req); err != nil {
				panic(err)
			}
		}
		l.StdDataPtr = l.doFilterProject()
		return nil
	}
}

func (l *List) RegisterBatchOp(opData list.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *List) Visible(sdk *cptype.SDK) bool {
	return l.visible
}

func (l *List) SkipOp(sdk *cptype.SDK) bool {
	return false
}

func (l *List) doFilterProject() (data *list.Data) {
	data = &list.Data{}
	gh := gshelper.NewGSHelper(l.sdk.GlobalState)
	selectedOption := gh.GetOption()
	projects, err := l.projectListRetriever(selectedOption)
	if err != nil {
		logrus.Errorf("list query app workbench data failed, error: %v", err)
		panic(err)
	}
	if projects == nil {
		data.Total = 0
	} else {
		data = &list.Data{
			Total:    uint64(projects.Total),
			PageNo:   uint64(l.filterReq.PageNo),
			PageSize: uint64(l.filterReq.PageSize),
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpChangePage{}.OpKey(): cputil.NewOpBuilder().Build(),
			},
		}
	}

	visible, err := l.CheckVisible(data)
	if err != nil {
		panic(err)
	}
	l.visible = visible
	gh.SetIsEmpty(!visible)
	if !visible || data.Total == 0 {
		return
	}

	for _, p := range projects.List {
		// authorized := selectedOption == "my"
		item := list.Item{
			ID: strconv.FormatUint(p.ID, 10),
			Icon: &commodel.Icon{URL: func() string {
				if len(p.Logo) == 0 {
					return "frontImg_default_project_icon"
				}
				return p.Logo
			}()},
			Title:       p.DisplayName,
			Selectable:  true,
			KvInfos:     l.GenProjectKvInfo(p),
			Description: p.Desc,
			Operations: map[cptype.OperationKey]cptype.Operation{
				list.OpItemClickGoto{}.OpKey(): func() cptype.Operation {
					builder := cputil.NewOpBuilder().
						WithSkipRender(true).
						WithServerDataPtr(list.OpItemClickGotoServerData{
							OpItemBasicServerData: list.OpItemBasicServerData{
								Params: map[string]interface{}{
									"projectId": p.ID,
								},
								Target: "project",
							},
						})
					// if !authorized {
					// 	builder = builder.WithDisable(true, l.sdk.I18n("appNotAuthorized"))
					// }
					return builder.Build()
				}(),
			},
		}
		if selectedOption == "my" {
			if p.CanManage && (p.BlockStatus == "unblocked" || p.BlockStatus == "unblocking" || p.BlockStatus == "blocked") {
				item.MoreOperations = append(item.MoreOperations,
					list.MoreOpItem{
						ID:   "applyDeploy",
						Text: l.sdk.I18n("applyDeploy"),
						Operations: map[cptype.OperationKey]cptype.Operation{
							"click": {
								ClientData: &cptype.OpClientData{},
							},
						},
					},
				)
			}
			item.MoreOperations = append(item.MoreOperations,
				list.MoreOpItem{
					ID:   "exit",
					Text: l.sdk.I18n("exit"),
					Operations: map[cptype.OperationKey]cptype.Operation{
						"click": {
							Confirm:    l.sdk.I18n("exitProjectConfirm"),
							ClientData: &cptype.OpClientData{},
						},
					},
				},
			)
		}
		data.List = append(data.List, item)
	}
	return
}

func (l *List) projectListRetriever(option string) (*apistructs.PagingProjectDTO, error) {
	if option == "my" {
		return l.bdl.ListMyProject(l.sdk.Identity.UserID, *l.filterReq)
	}
	return l.bdl.ListPublicProject(l.sdk.Identity.UserID, *l.filterReq)
}

func (l *List) GenProjectKvInfo(item apistructs.ProjectDTO) (kvs []list.KvInfo) {
	var isPublic = "privateProject"
	var publicIcon = "private"
	if item.IsPublic {
		isPublic = "publicProject"
		publicIcon = "public"
	}
	updated := common.UpdatedTime(l.sdk.Ctx, item.UpdatedAt)
	kvs = []list.KvInfo{
		{
			Icon:  publicIcon,
			Value: l.sdk.I18n(isPublic),
			Tip:   l.sdk.I18n("publicProperty"),
		},
		{
			Icon:  "application-one",
			Tip:   l.sdk.I18n(i18n.I18nAppNumber),
			Value: strconv.Itoa(item.Stats.CountApplications),
		},
		{
			Icon:  "time",
			Tip:   l.sdk.I18n("updatedAt") + ": " + item.UpdatedAt.Format("2006-01-02 15:04:05"),
			Value: updated,
		},
	}
	if item.BlockStatus == "unblocking" {
		kvs = append(kvs, list.KvInfo{
			Icon:  "link-cloud-faild",
			Value: l.sdk.I18n(i18n.I18nUnblocking),
			Tip:   l.sdk.I18n(i18n.I18nUnblocking),
		})
	} else if item.BlockStatus == "unblocked" {
		kvs = append(kvs, list.KvInfo{
			Icon:  "link-cloud-sucess",
			Value: l.sdk.I18n(i18n.I18nUnblocked),
		})
	}
	return
}

func (i *List) CheckVisible(data *list.Data) (bool, error) {
	if data.Total != 0 {
		return true, nil
	}
	if i.filterReq.Query == "" {
		return false, nil
	}
	req := apistructs.ProjectListRequest{
		OrgID:    i.filterReq.OrgID,
		PageNo:   1,
		PageSize: 1,
		OrderBy:  "activeTime",
	}
	projectDTO, err := i.bdl.ListMyProject(i.sdk.Identity.UserID, req)
	if err != nil {
		return false, err
	}
	if projectDTO == nil || projectDTO.Total == 0 {
		return false, nil
	}
	return true, nil
}
