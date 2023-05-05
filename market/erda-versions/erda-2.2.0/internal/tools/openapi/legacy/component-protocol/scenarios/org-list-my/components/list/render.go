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
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"

	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

const defaultOrgImage = "frontImg_default_org_icon"

func (i *ComponentList) unmarshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state State
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}
	i.State = state
	return nil
}

func (i *ComponentList) marshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(i.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	c.Data = map[string]interface{}{
		"list": i.Data,
	}
	c.State = state
	c.Props = i.Props
	c.Operations = i.Operations
	return nil
}

func (i *ComponentList) SetCtxBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil || b.I18nPrinter == nil {
		err := fmt.Errorf("invalie context bundle")
		return err
	}
	logrus.Infof("inParams:%+v, identity:%+v", b.InParams, b.Identity)
	i.CtxBdl = b
	return nil
}

func (i *ComponentList) Render(ctx context.Context, c *apistructs.Component, _ apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	if event.Operation != apistructs.InitializeOperation {
		err = i.unmarshal(c)
		if err != nil {
			return err
		}
	}

	defer func() {
		fail := i.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	if err := i.SetCtxBundle(bdl); err != nil {
		return err
	}

	i.initProperty()
	switch event.Operation {
	case apistructs.InitializeOperation, apistructs.RenderingOperation, apistructs.ChangeOrgsPageNoOperationKey, apistructs.ChangeOrgsPageSizeOperationKey:
		if err := i.RenderMyOrgs(); err != nil {
			return err
		}
	case apistructs.ExitOrgOperationKey:
		if err := i.ExitOrg(event); err != nil {
			return err
		}
		if err := i.RenderMyOrgs(); err != nil {
			return err
		}
	}
	return nil
}

func (i *ComponentList) RenderMyOrgs() error {
	if i.State.PageSize == 0 {
		i.State.PageSize = 10
		i.State.PageNo = 1
	}
	if i.State.SearchRefresh {
		i.State.PageNo = 1
	}
	req := apistructs.OrgSearchRequest{
		PageNo:   i.State.PageNo,
		PageSize: i.State.PageSize,
	}
	req.UserID = i.CtxBdl.Identity.UserID
	orgs, err := i.CtxBdl.Bdl.ListDopOrgs(&req)
	if err != nil {
		return err
	}
	if orgs.Total == 0 {
		i.State.IsEmpty = true
		i.Props.Visible = false
	}

	if i.State.SearchEntry != "" {
		req.Q = i.State.SearchEntry
		orgs, err = i.CtxBdl.Bdl.ListDopOrgs(&req)
		if err != nil {
			return err
		}
	}

	i.State.Total = orgs.Total
	id, err := strconv.Atoi(i.CtxBdl.Identity.OrgID)
	if err != nil {
		id = 0
	}
	curId := uint64(id)
	var first *Org
	data := []Org{}
	for _, org := range orgs.List {
		item := Org{
			Id:          org.ID,
			Title:       org.DisplayName,
			Description: org.Desc,
			PrefixImg:   defaultOrgImage,
			ExtraInfos: []ExtraInfo{
				{
					Icon: "lock2",
					Text: "私有组织",
				},
			},
		}
		if org.Logo != "" {
			item.PrefixImg = org.Logo
		}
		if org.IsPublic {
			item.ExtraInfos[0].Icon = "earth"
			item.ExtraInfos[0].Text = "公开组织"
		}

		item.Operations = map[string]interface{}{
			"click": ClickOperation{
				Key:    "click",
				Show:   false,
				Reload: false,
				Command: Command{
					Key:    "goto",
					Target: "https://" + org.Domain + "/dop/projects",
				},
			},
		}
		// permission, err := i.CtxBdl.Bdl.ScopeRoleAccess(i.CtxBdl.Identity.UserID, &apistructs.ScopeRoleAccessRequest{
		// 	Scope: apistructs.Scope{
		// 		Type: apistructs.OrgScope,
		// 		ID:   strconv.FormatUint(org.ID, 10),
		// 	},
		// })
		permission, err := i.CtxBdl.Bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   i.CtxBdl.Identity.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  org.ID,
			Resource: apistructs.OrgResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if permission.Access {
			item.Operations["toManage"] = ManageOperation{
				Key:    "toManage",
				Text:   "管理",
				Reload: false,
				Command: Command{
					Key:    "goto",
					Target: "https://" + org.Domain + "/orgCenter/setting/detail",
				},
			}
		}

		item.Operations["exit"] = ExitOperation{
			Key:     "exit",
			Text:    "退出",
			Reload:  true,
			Confirm: "退出当前组织后，将不在有参与组织工作的权限，如要再次加入需要组织管理员受邀加入，请确认是否退出？",
			Meta:    Meta{Id: org.ID},
		}

		if org.ID == curId {
			first = &item
			continue
		}
		data = append(data, item)
	}
	if first != nil {
		i.Data = append([]Org{*first}, data...)
	} else {
		i.Data = data
	}
	i.State.SearchRefresh = false
	return nil
}

func (i *ComponentList) ExitOrg(event apistructs.ComponentEvent) error {
	var operationData ExitOperation
	if err := getOperation(&operationData, event); err != nil {
		return err
	}
	req := apistructs.MemberRemoveRequest{
		Scope: apistructs.Scope{
			Type: apistructs.OrgScope,
			ID:   strconv.Itoa(int(operationData.Meta.Id)),
		},
		UserIDs: []string{i.CtxBdl.Identity.UserID},
	}
	req.UserID = i.CtxBdl.Identity.UserID
	return i.CtxBdl.Bdl.DeleteMember(req)
}

func getOperation(operationData *ExitOperation, event apistructs.ComponentEvent) error {
	if event.OperationData == nil {
		return nil
	}
	b, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &operationData); err != nil {
		return err
	}
	return nil
}

func (i *ComponentList) initProperty() {
	i.Operations = map[string]interface{}{
		"changePageNo": OperationBase{
			Key:    "changePageNo",
			Reload: true,
		},
		"changePageSize": OperationBase{
			Key:    "changePageSize",
			Reload: true,
		},
	}

	i.Props = Props{
		PageSizeOptions: []string{"10", "20", "50", "100"},
		Visible:         true,
	}
}

func RenderCreator() protocol.CompRender {
	return &ComponentList{}
}
