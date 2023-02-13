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

package createProject

import (
	"context"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

func (i *ComponentProjectCreation) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	sdk := cputil.SDK(ctx)
	bdl := sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	i.Props = Props{
		Visible: false,
		Text:    cputil.I18n(ctx, "createProject"),
		Type:    "primary",
	}

	i.Operations = map[string]interface{}{
		"click": Operation{
			Key:    "createProject",
			Reload: false,
			Command: Command{
				Key:     "goto",
				Target:  "createProject",
				JumpOut: true,
			},
		},
	}

	orgID, err := strconv.Atoi(sdk.Identity.OrgID)
	if err != nil {
		return err
	}

	gh := gshelper.NewGSHelper(gs)
	visible := gh.GetIsEmpty()
	selectedOption := gh.GetOption()
	if visible && selectedOption == "my" {
		permission, err := bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   sdk.Identity.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(orgID),
			Resource: apistructs.ProjectResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return err
		}
		if permission.Access {
			i.Props.Visible = true
		}
	}

	c.Operations = i.Operations
	c.Props = cputil.MustConvertProps(i.Props)
	return nil
}

func init() {
	base.InitProviderWithCreator("project-list-all", "createProject",
		func() servicehub.Provider { return &ComponentProjectCreation{} },
	)
}
