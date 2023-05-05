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

package notifyConfigModal

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

func (m *ComponentModel) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, _ *apistructs.GlobalStateData) error {
	if err := m.Import(c); err != nil {
		logrus.Errorf("import modal component is failed err is %v", err)
		return err
	}
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	m.CtxBdl = bdl
	switch event.Operation.String() {
	case apistructs.NotifySubmit.String():
		if err := m.handlerSubmitOperation(); err != nil {
			return err
		}
		//展示用户选择的数据
	case apistructs.RenderingOperation.String(), apistructs.InitializeOperation.String():
		if err := m.handlerFieldData(m.State); err != nil {
			return err
		}
	}
	if err := m.Export(c); err != nil {
		logrus.Errorf("export component is failed err is %v", err)
		return err
	}
	return nil
}

func (m *ComponentModel) Export(c *apistructs.Component) error {
	b, err := json.Marshal(m)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, c); err != nil {
		return err
	}
	return nil
}

func (m *ComponentModel) Import(c *apistructs.Component) error {
	com, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(com, m); err != nil {
		return err
	}
	return nil
}

func (m *ComponentModel) getInParams() (*apistructs.InParams, error) {
	inParamsBytes, err := json.Marshal(m.CtxBdl.InParams)
	if err != nil {
		return nil, err
	}
	inParams := apistructs.InParams{}
	err = json.Unmarshal(inParamsBytes, &inParams)
	if err != nil {
		return nil, err
	}
	return &inParams, nil
}

func (m *ComponentModel) handlerSubmitOperation() error {
	data := m.State.FormData
	inParams, err := m.getInParams()
	if err != nil {
		return err
	}
	name, ok := data["name"].(string)
	if !ok {
		return fmt.Errorf("name assert is failed name is %v", data["name"])
	}
	var id float64
	if idData, ok := data["id"]; ok {
		id, ok = idData.(float64)
		if !ok {
			return fmt.Errorf("id assert is failed id is %v", idData)
		}
	}
	target, ok := data["target"].(string)
	if !ok {
		return fmt.Errorf("target assert is faild target is %v", data["target"])
	}
	targetNumber, err := strconv.Atoi(target)
	if err != nil {
		return fmt.Errorf("target parset int is failed err is %v", err)
	}
	channelStr := fmt.Sprintf("channels-%d", targetNumber)
	channels, ok := data[channelStr].([]interface{})
	if !ok {
		return fmt.Errorf("channels assert is failed channels is %v", data[channelStr])
	}
	channelArr := make([]string, 0)
	for _, v := range channels {
		c, ok := v.(string)
		if !ok {
			return fmt.Errorf("channel assert is failed channel is %v", c)
		}
		channelArr = append(channelArr, c)
	}
	items, ok := data["items"].([]interface{})
	if !ok {
		return fmt.Errorf("items assert is failed items is %v", data["itmes"])
	}
	itemsArr := make([]string, 0)
	for _, v := range items {
		i, ok := v.(string)
		if !ok {
			return fmt.Errorf("item assert is failed item is %v", i)
		}
		itemsArr = append(itemsArr, i)
	}
	submitData := apistructs.EditOrCreateModalData{
		Name: name,
		//更新时id为确切的值，创建时id为0
		Id:       int(id),
		Target:   targetNumber,
		Items:    itemsArr,
		Channels: channelArr,
	}
	err = m.CtxBdl.Bdl.CreateOrEditNotify(&submitData, inParams, m.CtxBdl.Identity.UserID)
	if err != nil {
		return err
	}
	m.Props.Name = "通知"
	m.State.Visible = false
	return nil
}

func (m *ComponentModel) getDetailAndField(state State) (*apistructs.DetailResponse, *[]Field, error) {
	var detail *apistructs.DetailResponse
	inParams, err := m.getInParams()
	if err != nil {
		return nil, nil, err
	}
	fieldDataResp := make([]Field, 0)
	fieldData := Field{
		Key:       "name",
		Label:     "通知名称",
		Component: "input",
		Required:  true,
		ComponentProps: ComponentProps{
			MaxLength: 50,
		},
	}
	fieldData.Disabled = false
	//如果是编辑操作则获取原先用户设置的值
	if state.EditId != 0 {
		fieldData.Disabled = true
		detail, err = m.CtxBdl.Bdl.GetNotifyDetail(state.EditId)
	}
	fieldDataResp = append(fieldDataResp, fieldData)
	//获取所有可用模版
	allTemplates, err := m.CtxBdl.Bdl.GetAllTemplates(inParams.ScopeType, inParams.ScopeId,
		m.CtxBdl.Identity.UserID)
	if err != nil {
		return nil, nil, err
	}
	options := make([]Option, 0)
	for k, v := range allTemplates {
		option := Option{
			Name:  v,
			Value: k,
		}
		options = append(options, option)
	}
	fieldData = Field{
		Key:       "items",
		Label:     "触发时机",
		Component: "select",
		Required:  true,
		ComponentProps: ComponentProps{
			Mode:        "multiple",
			PlaceHolder: "请选择触发时机",
			Options:     options,
		},
	}
	fieldDataResp = append(fieldDataResp, fieldData)
	fieldData = Field{
		Key:       "target",
		Label:     "选择群组",
		Component: "select",
		Required:  true,
		ComponentProps: ComponentProps{
			PlaceHolder: "请选择群组",
		},
	}
	//获取所有通知组信息
	allGroups, err := m.CtxBdl.Bdl.GetAllGroups(inParams.ScopeType, inParams.ScopeId, m.CtxBdl.Identity.OrgID, m.CtxBdl.Identity.UserID)
	if err != nil {
		logrus.Errorf("get all groups is failed err is %v", err)
		return nil, nil, err
	}
	groupOptions := make([]Option, 0)
	for _, v := range allGroups {
		groupOption := Option{
			Name:  v.Name,
			Value: strconv.Itoa(int(v.Value)),
		}
		groupOptions = append(groupOptions, groupOption)
	}
	fieldData.ComponentProps.Options = groupOptions
	fieldDataResp = append(fieldDataResp, fieldData)
	//判断是否配置了通知组
	var flag bool
	if allGroups != nil && len(allGroups) > 0 {
		flag = true
	}
	for _, v := range allGroups {
		fieldData = Field{
			Key:       "channels-" + strconv.Itoa(int(v.Value)),
			Label:     "通知方式",
			Component: "select",
			Required:  true,
			ComponentProps: ComponentProps{
				Mode:        "multiple",
				PlaceHolder: "请选择通知方式",
				Options:     TypeOperation[v.Type],
			},
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field:    "target",
						Operator: "!=",
						Value:    strconv.Itoa(int(v.Value)),
					},
				},
			},
		}
		//判断是否需要添加电话和短信
		enableMS, err := m.CtxBdl.Bdl.GetNotifyConfigMS(m.CtxBdl.Identity.UserID, m.CtxBdl.Identity.OrgID)
		if err != nil {
			return nil, nil, err
		}
		if enableMS {
			msOption := []Option{
				{
					Name:  "SMS",
					Value: "sms",
				},
				{
					Name:  "phone",
					Value: "vms",
				},
			}
			fieldData.ComponentProps.Options = append(fieldData.ComponentProps.Options, msOption...)
		}
		fieldDataResp = append(fieldDataResp, fieldData)
	}
	if !flag {
		nullOption := make([]Option, 0)
		fieldData = Field{
			Key:       "channels",
			Label:     "通知方式",
			Component: "select",
			Required:  true,
			ComponentProps: ComponentProps{
				Mode:        "multiple",
				PlaceHolder: "请选择通知方式",
				Options:     nullOption,
			},
			RemoveWhen: [][]RemoveWhen{
				{
					{
						Field:    "target",
						Operator: "!=",
					},
				},
			},
		}
		fieldDataResp = append(fieldDataResp, fieldData)
	}
	return detail, &fieldDataResp, nil
}

func (m *ComponentModel) handlerFieldData(state State) error {
	detail, fieldData, err := m.getDetailAndField(state)
	if state.EditId != 0 {
		target := TargetInfo{}
		err = json.Unmarshal([]byte(detail.Target), &target)
		if err != nil {
			return err
		}
		items := make([]string, 0)
		err = json.Unmarshal([]byte(detail.NotifyID), &items)
		if err != nil {
			return err
		}
		groupStr := strconv.Itoa(int(target.GroupId))
		respMap := map[string]interface{}{
			"id":                   detail.Id,
			"name":                 detail.NotifyName,
			"target":               strconv.Itoa(int(target.GroupId)),
			"items":                items,
			"channels-" + groupStr: target.Channels,
		}
		m.State.FormData = respMap
	} else {
		m.State.FormData = nil
	}
	m.Props.Name = "通知"
	m.Props.Fields = *fieldData
	m.Operations = ModalOperation{
		Submit: Submit{
			Reload: true,
			Key:    "submit",
		},
	}
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentModel{}
}
