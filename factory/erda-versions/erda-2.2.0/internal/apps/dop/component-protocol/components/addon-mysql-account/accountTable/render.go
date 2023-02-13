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

package accountTable

import (
	"context"
	"encoding/base64"
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	addonmysqlpb "github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/accountTable/table"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/strutil"
)

type comp struct {
	ac      *common.AccountData
	pg      *common.PageDataAccount
	userIDs []string
}

func init() {
	base.InitProviderWithCreator("addon-mysql-account", "accountTable",
		func() servicehub.Provider { return &comp{} })
}

func (f *comp) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	f.pg = common.LoadPageDataAccount(ctx)

	ac, err := common.LoadAccountData(ctx)
	if err != nil {
		return err
	}
	f.ac = ac

	switch event.Operation {
	case "viewPassword":
		if !f.ac.EditPerm {
			return fmt.Errorf("no permission to view password")
		}
		f.pg.ShowViewPasswordModal = true
		f.pg.AccountID = event.OperationData["meta"].(map[string]interface{})["id"].(string)
	case "delete":
		if !f.ac.EditPerm {
			return fmt.Errorf("you don't have permission to edit this account")
		}
		//f.pg.ShowDeleteModal = true
		accountID := event.OperationData["meta"].(map[string]interface{})["id"].(string)
		addonMySQLSvc := ctx.Value(types.AddonMySQLService).(addonmysqlpb.AddonMySQLServiceServer)
		_, err := addonMySQLSvc.DeleteMySQLAccount(utils.NewContextWithHeader(ctx), &addonmysqlpb.DeleteMySQLAccountRequest{
			InstanceId: f.pg.InstanceID,
			Id:         accountID,
		})
		if err != nil {
			return err
		}

		// do reload data
		f.pg, err = common.InitPageDataAccount(ctx)
		if err != nil {
			return err
		}

		f.ac, err = common.InitAccountData(ctx, f.pg.InstanceID, f.pg.ProjectID)
		if err != nil {
			return err
		}
	}

	var props table.Props
	props.Columns = getTitles(ctx)
	props.RowKey = "id"
	props.RequestIgnore = []string{"props", "data", "operations"}
	c.Props = cputil.MustConvertProps(props)

	c.Data = make(map[string]interface{})
	c.Data["list"] = f.getData(ctx)

	(*gs)[string(cptype.GlobalInnerKeyUserIDs)] = strutil.DedupSlice(f.userIDs, true)

	return nil
}

func getTitles(ctx context.Context) []*table.ColumnTitle {
	return []*table.ColumnTitle{
		{
			Title:     cputil.I18n(ctx, "account"),
			DataIndex: "username",
		},
		{
			Title:     cputil.I18n(ctx, "attachment.status"),
			DataIndex: "attachments",
		},
		{
			Title:     cputil.I18n(ctx, "creator"),
			DataIndex: "creator",
		},
		{
			Title:     cputil.I18n(ctx, "created_at"),
			DataIndex: "createdAt",
		},
		{
			Title:     cputil.I18n(ctx, "operate"),
			DataIndex: "operate",
			Width:     180,
		},
	}
}

func (f *comp) getData(ctx context.Context) []map[string]table.ColumnData {
	var columns []map[string]table.ColumnData
	for _, i := range f.ac.Accounts {
		if !f.getFilter().Match(i) {
			continue
		}
		datum := f.getDatum(ctx, i)
		if datum == nil {
			continue
		}
		columns = append(columns, datum)
	}
	return columns
}

func (f *comp) creatorFilter(v interface{}) bool {
	opts := f.pg.FilterValues.StringSlice("creator")
	if opts == nil || len(opts) == 0 {
		return true
	}
	return strutil.Exist(opts, v.(*addonmysqlpb.MySQLAccount).Creator)
}

func (f *comp) statusFilter(v interface{}) bool {
	item := v.(*addonmysqlpb.MySQLAccount)
	opts := f.pg.FilterValues.StringSlice("status")
	if opts == nil || len(opts) == 0 {
		return true
	}
	for _, status := range opts {
		cnt := f.ac.AccountRefCount[item.Id]
		if status == "YES" && cnt > 0 {
			return true
		}
		if status == "NO" && cnt == 0 {
			return true
		}
	}
	return false
}

func (f *comp) getFilter() table.Matcher {
	return table.And(
		table.ToMatcher(f.creatorFilter),
		table.ToMatcher(f.statusFilter),
	)
}

func (f *comp) getDatum(ctx context.Context, item *addonmysqlpb.MySQLAccount) map[string]table.ColumnData {
	datum := make(map[string]table.ColumnData)
	datum["username"] = table.ColumnData{RenderType: "text", Value: item.Username}

	cnt := f.ac.AccountRefCount[item.Id]
	datum["attachments"] = table.ColumnData{RenderType: "linkText", Value: cputil.I18n(ctx, "${in_use_cnt}", cnt), Operations: map[string]*table.Operation{
		"click": {
			Key:    "gotoMysqlUserManager",
			Reload: false,
			Command: &table.OperationCommand{
				Key: "goto",
				State: map[string]interface{}{
					"params": map[string]interface{}{
						"projectId":  fmt.Sprintf("%d", f.pg.ProjectID),
						"instanceId": f.pg.InstanceID,
					},
					"query": map[string]interface{}{
						"filter__urlQuery": base64.StdEncoding.EncodeToString(
							[]byte(fmt.Sprintf(`{"account":["%s"]}`, item.Id))),
					},
				},
				Target: "addonPlatformMysqlConsumer",
			},
		},
	}}
	if cnt == 0 {
		datum["attachments"] = table.ColumnData{RenderType: "text", Value: cputil.I18n(ctx, "not_used")}
	}

	datum["creator"] = table.ColumnData{RenderType: "userAvatar", Value: item.Creator}
	f.userIDs = append(f.userIDs, item.Creator)
	datum["createdAt"] = table.ColumnData{RenderType: "datePicker", Value: item.CreateAt.AsTime().Format(time.RFC3339)}
	datum["operate"] = table.ColumnData{RenderType: "tableOperation", Operations: map[string]*table.Operation{
		"viewPassword": {
			Key:    "viewPassword",
			Text:   cputil.I18n(ctx, "view_password"),
			Reload: true,
			Meta: map[string]string{
				"id": item.Id,
			},
			Disabled: !f.ac.EditPerm,
			DisabledTip: func() string {
				if !f.ac.EditPerm {
					return cputil.I18n(ctx, "view_password_no_perm_tip")
				}
				return ""
			}(),
			ShowIndex: 1,
		},
		"delete": {
			Key:    "delete",
			Text:   cputil.I18n(ctx, "delete"),
			Reload: true,
			Meta: map[string]string{
				"id": item.Id,
			},
			Disabled: !f.ac.EditPerm || cnt > 0,
			DisabledTip: func() string {
				if !f.ac.EditPerm {
					return cputil.I18n(ctx, "delete_no_perm_tip")
				}
				if cnt > 0 {
					return cputil.I18n(ctx, "deleting_tip")
				}
				return ""
			}(),
			ShowIndex:  2,
			Confirm:    cputil.I18n(ctx, "delete_confirm"),
			SuccessMsg: cputil.I18n(ctx, "delete_success_tip"),
		},
	}}
	return datum
}
