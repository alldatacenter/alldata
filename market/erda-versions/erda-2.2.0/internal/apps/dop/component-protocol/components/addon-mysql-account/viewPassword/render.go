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

package viewPassword

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	addonmysqlpb "github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/accountTable/table"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/common"
)

type comp struct {
	ac *common.AccountData
}

func init() {
	base.InitProviderWithCreator("addon-mysql-account", "viewPassword",
		func() servicehub.Provider { return &comp{} })
}

func (f *comp) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	pg := common.LoadPageDataAccount(ctx)
	if !pg.ShowViewPasswordModal {
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
	f.ac = ac

	if !ac.EditPerm {
		return fmt.Errorf("no permission to view account password")
	}

	var props table.Props
	props.RequestIgnore = []string{"props", "data", "operations"}
	props.Columns = getTitles()
	props.RowKey = "label"
	props.Bordered = table.False()
	props.ShowPagination = table.False()
	props.ShowHeader = table.False()
	c.Props = cputil.MustConvertProps(props)

	c.Data = make(map[string]interface{})
	c.Data["list"] = getData(ctx, ac.AccountMap[pg.AccountID])

	state := make(map[string]interface{})
	state["visible"] = true
	c.State = state

	return nil
}

func getTitles() []*table.ColumnTitle {
	return []*table.ColumnTitle{
		{
			DataIndex: "label",
			Width:     80,
		},
		{
			DataIndex: "value",
		},
	}
}

func getData(ctx context.Context, account *addonmysqlpb.MySQLAccount) []map[string]interface{} {
	if account == nil {
		return nil
	}
	return []map[string]interface{}{
		{
			"label": cputil.I18n(ctx, "account"),
			"value": account.Username,
		},
		{
			"label": cputil.I18n(ctx, "password"),
			"value": account.Password,
		},
	}
}
