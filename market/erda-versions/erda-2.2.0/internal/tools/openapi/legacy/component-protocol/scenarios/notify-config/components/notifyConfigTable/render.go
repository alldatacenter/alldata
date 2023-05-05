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

package notifyConfigTable

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/table"
)

func (n *Notify) Import(c *apistructs.Component) error {
	com, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(com, n); err != nil {
		return err
	}
	return nil
}

func (n *Notify) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario,
	event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	if err := n.Import(c); err != nil {
		logrus.Errorf("import component is failed err is %v", err)
		return err
	}
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	n.CtxBdl = bdl
	n.State.EditId = 0
	n.State.Visible = false
	switch event.Operation.String() {
	case apistructs.InitializeOperation.String(), apistructs.RenderingOperation.String():
		err := n.RenderOnFilter()
		if err != nil {
			logrus.Errorf("render on filter is failed err:%v", err)
			return err
		}
	case apistructs.NotifyDelete.String():
		err := n.deleteNotify(event)
		if err != nil {
			logrus.Errorf("delete notify is failed err is %v", err)
			return err
		}
	case apistructs.NotifySwitch.String():
		err := n.switchNotify(event)
		if err != nil {
			logrus.Errorf("switch notify is failed err is %v", err)
			return err
		}
	case apistructs.NotifyEdit.String():
		err := n.editNotify(event)
		if err != nil {
			logrus.Errorf("edit notify is failed err is %v", err)
			return err
		}
	default:
		logrus.Warnf("operation [%s] not support, scenario:%v, event:%v", event.Operation, scenario, event)
	}
	err := n.RenderOnFilter()
	if err != nil {
		logrus.Errorf("render on filter is failed err:%v", err)
		return err
	}
	if err := n.Export(c, gs); err != nil {
		logrus.Errorf("export component is failed err:%v", err)
		return err
	}
	return nil
}

func (n *Notify) RenderOnFilter() error {
	inParams, err := n.getInParams()
	if err != nil {
		return err
	}
	userId := n.CtxBdl.Identity.UserID
	orgId := n.CtxBdl.Identity.OrgID
	req := apistructs.NotifyPageRequest{
		ScopeId: inParams.ScopeId,
		Scope:   inParams.ScopeType,
		UserId:  userId,
		OrgId:   orgId,
	}
	logrus.Errorf("scopeId is %v,scope is %v,userID is %v,orgID is %v", req.ScopeId, req.Scope, req.UserId, req.OrgId)
	data, err := n.CtxBdl.Bdl.NotifyList(req)
	if err != nil {
		logrus.Errorf("NotifyList is failed, request: %v,err: %v", req, err)
		return err
	}
	var list []table.NotifyTableList
	for _, v := range *data {
		value := apistructs.Value{
			Type:   v.NotifyTarget[0].Type,
			Values: v.NotifyTarget[0].Values,
		}

		targets := table.Target{
			RoleMap:    roleMap,
			RenderType: "listTargets",
			Value:      []apistructs.Value{value},
		}
		switchText := "开启"
		if v.Enable {
			switchText = "关闭"
		}
		operate := table.Operate{
			RenderType: "tableOperation",
			Operations: map[string]table.Operations{
				"edit": {
					Key:    "edit",
					Text:   "编辑",
					Reload: true,
					Meta: table.Meta{
						Id: v.Id,
					},
				},
				"delete": {
					Key:     "delete",
					Text:    "删除",
					Confirm: "确认删除该条通知？",
					Meta: table.Meta{
						Id: v.Id,
					},
					Reload: true,
				},
				"switch": {
					Key:  "switch",
					Text: switchText,
					Meta: table.Meta{
						Id: v.Id,
					},
					Reload: true,
				},
			},
		}
		creatTime := v.CreatedAt.Format("2006-01-02 15:04:05")
		listMember := table.NotifyTableList{
			Id:        v.Id,
			Name:      v.NotifyName,
			Targets:   targets,
			CreatedAt: creatTime,
			Operate:   operate,
		}
		list = append(list, listMember)
	}
	n.genProps()
	n.Data.List = list
	return nil
}

func (n *Notify) Export(c *apistructs.Component, gs *apistructs.GlobalStateData) error {
	b, err := json.Marshal(n)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, c); err != nil {
		return err
	}
	return nil
}

func (n *Notify) getInParams() (*InParams, error) {
	if n.CtxBdl.InParams == nil {
		return nil, fmt.Errorf("params is empty")
	}
	inParamsBytes, err := json.Marshal(n.CtxBdl.InParams)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal inParams:%+v, err:%v", n.CtxBdl.InParams, err)
	}
	var inParams InParams
	if err = json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return nil, err
	}
	logrus.Errorf("scope is %v,scopeID is %v", inParams.ScopeType, inParams.ScopeId)
	return &inParams, nil
}

func (n *Notify) deleteNotify(event apistructs.ComponentEvent) error {
	inParams, err := n.getInParams()
	if err != nil {
		return err
	}
	operationData := event.OperationData
	data, err := json.Marshal(operationData)
	if err != nil {
		return err
	}
	var deleteOperation DeleteNotifyOperation
	err = json.Unmarshal(data, &deleteOperation)
	if err != nil {
		return err
	}
	if deleteOperation.Meta.Id == 0 {
		return fmt.Errorf("delete notify id not be empty")
	}
	err = n.CtxBdl.Bdl.DeleteNotifyRecord(inParams.ScopeType, inParams.ScopeId, deleteOperation.Meta.Id,
		n.CtxBdl.Identity.UserID)
	if err != nil {
		logrus.Errorf("delete notify is failed err is failed id:%v, err:%v", deleteOperation.Meta.Id, err)
		return err
	}
	return nil
}

func (n *Notify) switchNotify(event apistructs.ComponentEvent) error {
	inParams, err := n.getInParams()
	if err != nil {
		return err
	}
	operationData := event.OperationData
	data, err := json.Marshal(operationData)
	if err != nil {
		return err
	}
	var switchOperation apistructs.SwitchOperation
	err = json.Unmarshal(data, &switchOperation)
	if err != nil {
		return err
	}
	if switchOperation.Meta.Id == 0 {
		return fmt.Errorf("switch notify id not be empty")
	}
	scope := inParams.ScopeType
	scopeId := inParams.ScopeId
	userId := n.CtxBdl.Identity.UserID
	err = n.CtxBdl.Bdl.SwitchNotifyRecord(scope, scopeId, userId, &switchOperation.Meta)
	if err != nil {
		logrus.Errorf("switch notify is failed id:%v, err:%v", switchOperation.Meta.Id, err)
		return err
	}
	return nil
}

func (n *Notify) editNotify(event apistructs.ComponentEvent) error {
	operationData := event.OperationData
	data, err := json.Marshal(operationData)
	if err != nil {
		return err
	}
	var editOperation EditOperation
	err = json.Unmarshal(data, &editOperation)
	n.State.EditId = editOperation.Meta.Id
	n.State.Operation = "edit"
	n.State.Visible = true
	return nil
}

func RenderCreator() protocol.CompRender {
	return &Notify{}
}
