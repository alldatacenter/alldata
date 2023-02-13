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

package attachmentTable

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/strutil"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	addonmysqlpb "github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/accountTable/table"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/common"
)

type comp struct {
	pg *common.PageDataAttachment
	ac *common.AccountData
}

func init() {
	base.InitProviderWithCreator("addon-mysql-consumer", "attachmentTable",
		func() servicehub.Provider { return &comp{} })
}

func (f *comp) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	f.pg = common.LoadPageDataAttachment(ctx)

	ac, err := common.LoadAccountData(ctx)
	if err != nil {
		return err
	}
	f.ac = ac

	switch event.Operation {
	case "showConfig":
		f.pg.ShowConfigPanel = true
		f.pg.AttachmentID = event.OperationData["meta"].(map[string]interface{})["id"].(string)
	case "editAttachment":
		f.pg.ShowEditFormModal = true
		f.pg.AttachmentID = event.OperationData["meta"].(map[string]interface{})["id"].(string)
	}

	var props table.Props
	props.Columns = getTitles(ctx)
	props.RowKey = "id"
	props.RequestIgnore = []string{"props", "data", "operations"}
	c.Props = cputil.MustConvertProps(props)

	c.Data = make(map[string]interface{})
	c.Data["list"] = f.getData(ctx)

	return nil
}

func getTitles(ctx context.Context) []*table.ColumnTitle {
	return []*table.ColumnTitle{
		{
			Title:     cputil.I18n(ctx, "app"),
			DataIndex: "appName",
			Width:     180,
		},
		{
			Title:     cputil.I18n(ctx, "runtime"),
			DataIndex: "runtime",
		},
		{
			Title:     cputil.I18n(ctx, "account"),
			DataIndex: "account",
		},
		{
			Title:     cputil.I18n(ctx, "operate"),
			DataIndex: "operate",
			Width:     260,
		},
	}
}

func (f *comp) getData(ctx context.Context) []map[string]table.ColumnData {
	var columns []map[string]table.ColumnData
	for _, i := range f.ac.Attachments {
		if !f.getFilter().Match(i) {
			continue
		}
		columns = append(columns, f.getDatum(ctx, i))
	}
	return columns
}

func (f *comp) getFilter() table.Matcher {
	return table.And(
		table.ToMatcher(f.accountFilter),
		table.ToMatcher(f.stateFilter),
		table.ToMatcher(f.appFilter),
	)
}

func (f *comp) accountFilter(v interface{}) bool {
	opts := f.pg.FilterValues.StringSlice("account")
	if opts == nil || len(opts) == 0 {
		return true
	}
	item := v.(*addonmysqlpb.Attachment)
	return strutil.Exist(opts, item.AccountId) || (item.AccountState == "PRE" && strutil.Exist(opts, item.PreAccountId))
}

func (f *comp) stateFilter(v interface{}) bool {
	opts := f.pg.FilterValues.StringSlice("state")
	if opts == nil || len(opts) == 0 {
		return true
	}
	item := v.(*addonmysqlpb.Attachment)
	for _, opt := range opts {
		if opt == "PRE" && item.AccountState == "PRE" {
			return true
		}
		if opt == "CUR" && (item.AccountState == "CUR" || item.AccountState == "") {
			return true
		}
	}
	return false
}

func (f *comp) appFilter(v interface{}) bool {
	opts := f.pg.FilterValues.StringSlice("app")
	if opts == nil || len(opts) == 0 {
		return true
	}
	item := v.(*addonmysqlpb.Attachment)
	return strutil.Exist(opts, item.AppId)
}

func (f *comp) getDatum(ctx context.Context, item *addonmysqlpb.Attachment) map[string]table.ColumnData {
	switching := item.AccountState == "PRE"

	datum := make(map[string]table.ColumnData)
	datum["appName"] = table.ColumnData{RenderType: "text", Value: f.ac.GetAppName(item.AppId)}

	var accountText string
	if switching {
		accountText = fmt.Sprintf("%s -> %s",
			f.ac.GetAccountName(item.PreAccountId), f.ac.GetAccountName(item.AccountId))
	} else {
		accountText = f.ac.GetAccountName(item.AccountId)
	}
	datum["account"] = table.ColumnData{RenderType: "text", Value: accountText}

	if switching {
		datum["runtime"] = table.ColumnData{RenderType: "textWithTags", Value: item.RuntimeName, Tags: []table.ColumnDataTag{
			{
				Tag:   cputil.I18n(ctx, "account_switching"),
				Color: "orange",
			},
		}}
	} else {
		datum["runtime"] = table.ColumnData{RenderType: "text", Value: item.RuntimeName}
	}

	datum["operate"] = table.ColumnData{RenderType: "tableOperation", Operations: map[string]*table.Operation{
		"showConfig": {
			Key:    "showConfig",
			Text:   cputil.I18n(ctx, "show_config"),
			Reload: true,
			Meta: map[string]string{
				"id": fmt.Sprintf("%d", item.Id),
			},
			ShowIndex: 1,
		},
		"click": {
			Key:    "gotoRuntimeDetail",
			Text:   cputil.I18n(ctx, "goto_runtime_detail"),
			Reload: false,
			Command: &table.OperationCommand{
				Key: "goto",
				State: map[string]interface{}{
					"params": func() map[string]interface{} {
						app := f.ac.GetApp(item.AppId)
						projectID := "0"
						if app != nil {
							projectID = fmt.Sprintf("%d", app.ProjectID)
						}
						return map[string]interface{}{
							"projectId": projectID,
							"appId":     item.AppId,
							"runtimeId": item.RuntimeId,
							"workspace": item.Workspace,
						}
					}(),
				},
				Target: "projectDeployRuntime",
			},
			ShowIndex: 2,
		},
		"edit": {
			Key:    "editAttachment",
			Text:   cputil.I18n(ctx, "edit"),
			Reload: true,
			Meta: map[string]string{
				"id": fmt.Sprintf("%d", item.Id),
			},
			Disabled: !f.ac.EditPerm,
			DisabledTip: func() string {
				if !f.ac.EditPerm {
					return cputil.I18n(ctx, "edit_no_perm_tip")
				}
				return ""
			}(),
			ShowIndex: 3,
		},
	}}
	return datum
}
