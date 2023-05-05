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

package tabs

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-list-all/common/gshelper"
)

type ComponentFilter struct {
	sdk *cptype.SDK

	State      State                `json:"state,omitempty"`
	Data       Data                 `json:"data"`
	Operations map[string]Operation `json:"operations"`
	gsHelper   *gshelper.GSHelper
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

func init() {
	base.InitProviderWithCreator("project-list-all", "tabs",
		func() servicehub.Provider { return &ComponentFilter{} },
	)
}

func (f *ComponentFilter) InitFromProtocol(ctx context.Context, c *cptype.Component, gs *cptype.GlobalStateData) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}

	f.sdk = cputil.SDK(ctx)
	f.gsHelper = gshelper.NewGSHelper(gs)
	return nil
}

func (f *ComponentFilter) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}

const OperationKeyOnChange = "onChange"

func (f *ComponentFilter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c, gs); err != nil {
		return err
	}
	switch event.Operation {
	case cptype.InitializeOperation:
		f.Operations = map[string]Operation{
			OperationKeyOnChange: {},
		}
		f.Data = Data{
			Options: []Option{
				{
					Label: cputil.I18n(ctx, "myProject"),
					Value: "my",
				},
				{
					Label: cputil.I18n(ctx, "publicProject"),
					Value: "public",
				},
			},
		}
		f.State.Value = "my"
	case cptype.RenderingOperation:
		f.State.Value = f.gsHelper.GetOption()
	case OperationKeyOnChange:
		var op Operation
		cputil.MustObjJSONTransfer(event.OperationData, &op)
		f.State.Value = op.ClientData.Value
	}
	// public := f.State.Value
	// if f.State.Value == "all" || f.State.Value == "my" {
	// 	public = ""
	// }
	f.gsHelper.SetProjectPagingRequest(apistructs.ProjectListRequest{
		// IsPublic: true,
	})
	f.gsHelper.SetOption(f.State.Value)
	return f.SetToProtocolComponent(c)
}
