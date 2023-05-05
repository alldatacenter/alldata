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

package editFormModal

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	addonmysqlpb "github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-attachment/editFormModal/form"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/strutil"
)

type comp struct {
}

func init() {
	base.InitProviderWithCreator("addon-mysql-consumer", "editFormModal",
		func() servicehub.Provider { return &comp{} })
}

func (f *comp) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	pg := common.LoadPageDataAttachment(ctx)

	switch event.Operation {
	case "submit":
		addonMySQLSvc := ctx.Value(types.AddonMySQLService).(addonmysqlpb.AddonMySQLServiceServer)
		attID, err := strutil.Atoi64(c.State["attachmentId"].(string))
		if err != nil {
			return err
		}
		accID := c.State["formData"].(map[string]interface{})["account"].(string)
		if accID == "" {
			return errors.Errorf("account not found, attachemnt: %v", c.State["attachmentId"])
		}
		_, err = addonMySQLSvc.UpdateAttachmentAccount(utils.NewContextWithHeader(ctx), &addonmysqlpb.UpdateAttachmentAccountRequest{
			InstanceId: pg.InstanceID,
			Id:         uint64(attID),
			AccountId:  accID,
		})
		if err != nil {
			return err
		}
	}

	if !pg.ShowEditFormModal {
		state := make(map[string]interface{})
		state["visible"] = false
		c.State = state
		c.Props = nil
		c.Data = nil
		return nil
	}

	ac, err := common.LoadAccountData(ctx)
	if err != nil {
		return err
	}

	accountList := make([]map[string]interface{}, 0)
	for _, a := range ac.Accounts {
		accountList = append(accountList, map[string]interface{}{
			"name":  a.Username,
			"value": a.Id,
		})
	}

	props := make(map[string]interface{})
	props["requestIgnore"] = []string{"props", "data"}
	props["title"] = cputil.I18n(ctx, "edit")
	props["fields"] = []form.Field{
		{
			Component: "input",
			Key:       "app",
			Label:     cputil.I18n(ctx, "app"),
			Required:  true,
			Disabled:  true,
		},
		{
			Component: "select",
			Key:       "runtime",
			Label:     cputil.I18n(ctx, "runtime"),
			Required:  true,
			Disabled:  true,
		},
		{
			Component: "select",
			Key:       "account",
			Label:     cputil.I18n(ctx, "db_account"),
			ComponentProps: map[string]interface{}{
				"options": accountList,
			},
			Required: true,
		},
	}
	c.Props = props

	attID, err := strutil.Atoi64(pg.AttachmentID)
	if err != nil {
		return err
	}
	att := ac.AttachmentMap[uint64(attID)]
	if att == nil {
		return nil
	}

	state := make(map[string]interface{})
	state["formData"] = map[string]interface{}{
		"app":     ac.GetAppName(att.AppId),
		"runtime": att.RuntimeName,
		"account": att.AccountId,
	}
	state["attachmentId"] = pg.AttachmentID
	state["visible"] = true
	c.State = state

	c.Operations = map[string]interface{}{
		"submit": cptype.LegacyOperation{
			Key:    "submit",
			Reload: true,
		},
	}
	return nil
}
