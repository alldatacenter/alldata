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

package configPanel

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/common"
	"github.com/erda-project/erda/pkg/strutil"
)

type comp struct {
}

func init() {
	base.InitProviderWithCreator("addon-mysql-consumer", "configPanel",
		func() servicehub.Provider { return &comp{} })
}

func (f *comp) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	pg := common.LoadPageDataAttachment(ctx)
	if !pg.ShowConfigPanel {
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

	id, err := strutil.Atoi64(pg.AttachmentID)
	if err != nil {
		return err
	}
	att := ac.AttachmentMap[uint64(id)]

	fields := make([]map[string]interface{}, 0)
	if att != nil {
		for k, v := range att.Configs {
			fields = append(fields, map[string]interface{}{
				"label": k,
				"value": v,
			})
		}
	}

	props := make(map[string]interface{})
	props["columnNum"] = 1
	props["fields"] = fields
	props["requestIgnore"] = []string{"props", "data", "operations"}
	c.Props = props

	state := make(map[string]interface{})
	state["visible"] = true
	c.State = state
	return nil
}
