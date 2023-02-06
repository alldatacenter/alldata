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

package page

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/org-list-all/i18n"
)

func (i *ComponentPage) marshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(i.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	c.State = state
	c.Props = i.Props
	return nil
}

func (i *ComponentPage) SetCtxBundle(ctx context.Context) error {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	if bdl.Bdl == nil || bdl.I18nPrinter == nil {
		return fmt.Errorf("invalid context bundle")
	}
	logrus.Infof("inParams:%+v, identity:%+v", bdl.InParams, bdl.Identity)
	i.CtxBdl = bdl
	return nil
}

func (i *ComponentPage) Render(ctx context.Context, c *apistructs.Component, s apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	if err := i.SetCtxBundle(ctx); err != nil {
		return err
	}

	defer func() {
		fail := i.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	i.initProperty(s)

	return nil
}

func (i *ComponentPage) initProperty(s apistructs.ComponentProtocolScenario) {
	i18nLocale := i.CtxBdl.Bdl.GetLocale(i.CtxBdl.Locale)
	publicOrgs := Option{
		Key:  "public",
		Name: i18nLocale.Get(i18n.I18nPublicOrg),
		Operations: map[string]interface{}{
			"click": ClickOperation{
				Reload: false,
				Key:    "publicOrg",
				Command: Command{
					Key:          "changeScenario",
					ScenarioKey:  s.ScenarioKey,
					ScenarioType: s.ScenarioType,
				},
			},
		},
	}

	i.Props = Props{
		TabMenu: []Option{publicOrgs},
	}
	i.State.ActiveKey = publicOrgs.Key
}

func RenderCreator() protocol.CompRender {
	return &ComponentPage{}
}
