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

package messageTabs

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common/gshelper"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/i18n"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/types"
)

const (
	CompNameMessagesTabs = "messageTabs"
)

type MessageTabs struct {
	sdk      *cptype.SDK
	bdl      *bundle.Bundle
	gsHelper *gshelper.GSHelper
	identity apistructs.Identity

	Data       Data                 `json:"data"`
	Operations map[string]Operation `json:"operations"`
	State      State                `json:"state"`
}

type Option struct {
	Value string `json:"value"`
	Label string `json:"label"`
}

type Data struct {
	Options []Option `json:"options"`
}

type Operation struct {
	ClientData ClientData `json:"clientData"`
}

type ClientData struct {
	Value string `json:"value"`
}

type State struct {
	Value string `json:"value"`
}

func (f *MessageTabs) InitFromProtocol(ctx context.Context, c *cptype.Component, gs *cptype.GlobalStateData) error {
	// component 序列化
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}

	// sdk
	f.sdk = cputil.SDK(ctx)
	f.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	f.identity = apistructs.Identity{
		UserID: f.sdk.Identity.UserID,
		OrgID:  f.sdk.Identity.OrgID,
	}
	f.gsHelper = gshelper.NewGSHelper(gs)
	return nil
}

func (f *MessageTabs) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}

func (f *MessageTabs) InitComp() error {
	f.Operations = map[string]Operation{
		"onChange": {},
	}

	// list unread message
	req := apistructs.QueryMBoxRequest{PageNo: 1,
		PageSize: 0,
		Status:   apistructs.MBoxUnReadStatus,
		Type:     apistructs.MBoxTypeIssue,
	}
	res, err := f.bdl.ListMbox(f.identity, req)
	if err != nil {
		logrus.Errorf("get mbxo stats failed, identity: %v, error: %v", f.identity, err)
		return err
	}

	orgID, err := strconv.ParseUint(f.identity.OrgID, 10, 64)
	if err != nil {
		return err
	}
	approveRsp, err := f.bdl.ListApprove(orgID, f.identity.UserID, map[string][]string{
		"pageNo":   {"1"},
		"pageSize": {"0"},
		"status":   {"pending"},
	})
	if err != nil {
		return err
	}
	reviewRsp, err := f.bdl.ListManualApproval(f.identity.OrgID, f.identity.UserID, map[string][]string{
		"pageNo":         {"1"},
		"pageSize":       {"0"},
		"approvalStatus": {"pending"},
	})
	if err != nil {
		return err
	}

	f.Data = Data{
		Options: []Option{
			// unread message
			{Value: apistructs.WorkbenchItemUnreadMes.String(), Label: fmt.Sprintf("%s(%v)", f.sdk.I18n(i18n.I18nKeyUnreadMes), res.UnRead)},
			{Value: apistructs.WorkbenchItemApproveRequest.String(), Label: fmt.Sprintf("%s(%v)", f.sdk.I18n(i18n.I18nKeyApproveRequest), approveRsp.Data.Total+reviewRsp.Data.Total)},
		},
	}
	return nil
}

func (f *MessageTabs) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c, gs); err != nil {
		logrus.Errorf("init from protocol failed, error: %v", err)
		return err
	}

	if err := f.InitComp(); err != nil {
		logrus.Errorf("init comp failed, error: %v", err)
		return err
	}

	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		tp, ok := f.gsHelper.GetMsgTabName()
		if !ok {
			f.State.Value = apistructs.WorkbenchItemUnreadMes.String()
		} else {
			f.State.Value = tp.String()
		}
	case common.EventChangeEventTab:
		var op Operation
		cputil.MustObjJSONTransfer(event.OperationData, &op)
		f.State.Value = op.ClientData.Value
	default:
		logrus.Errorf("scenario %v component [%s] does not support event %v", scenario, CompNameMessagesTabs, event)
		return nil
	}
	// set message table value to global state
	f.gsHelper.SetMsgTabName(f.State.Value)
	return f.SetToProtocolComponent(c)
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, CompNameMessagesTabs, func() servicehub.Provider {
		return &MessageTabs{}
	})
}
