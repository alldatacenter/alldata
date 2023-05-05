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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/org-list-all/i18n"
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
		if err := i.RenderPublicOrgs(); err != nil {
			return err
		}
	}
	return nil
}

func (i *ComponentList) RenderPublicOrgs() error {
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
		Q:        i.State.SearchEntry,
	}
	req.UserID = i.CtxBdl.Identity.UserID
	orgs, err := i.CtxBdl.Bdl.ListDopPublicOrgs(&req)
	if err != nil {
		return err
	}

	req = apistructs.OrgSearchRequest{}
	req.UserID = i.CtxBdl.Identity.UserID
	myOrgs, err := i.CtxBdl.Bdl.ListDopOrgs(&req)
	if err != nil {
		return err
	}
	orgMap := map[uint64]bool{}
	for _, o := range myOrgs.List {
		orgMap[o.ID] = true
	}

	i18nLocale := i.CtxBdl.Bdl.GetLocale(i.CtxBdl.Locale)
	i.State.Total = orgs.Total
	data := []Org{}
	for _, org := range orgs.List {
		item := Org{
			Id:          org.ID,
			Title:       org.DisplayName,
			Description: org.Desc,
			PrefixImg:   defaultOrgImage,
			ExtraInfos: []ExtraInfo{
				{
					Icon: "unlock",
					Text: i18nLocale.Get(i18n.I18nPublicOrg),
				},
			},
		}
		if org.Logo != "" {
			item.PrefixImg = org.Logo
		}

		command := Command{
			Key:     "goto",
			Target:  "dopPublicProjects",
			JumpOut: false,
			State: CommandState{
				Params{
					OrgName: org.Name,
				},
			},
		}

		if _, ok := orgMap[org.ID]; ok {
			item.ExtraInfos = append(item.ExtraInfos, ExtraInfo{
				Icon: "user",
				Text: i18nLocale.Get(i18n.I18nOrgJoined),
			})
			command.Target = "dopRoot"
		}

		item.Operations = map[string]interface{}{
			"click": ClickOperation{
				Key:     "click",
				Show:    false,
				Reload:  false,
				Command: command,
			},
		}

		data = append(data, item)
	}
	i.Data = data
	i.State.SearchRefresh = false
	return nil
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

	i.Props.PageSizeOptions = []string{"10", "20", "50", "100"}
}

func RenderCreator() protocol.CompRender {
	return &ComponentList{}
}
