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

package podTitle

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

func (podTitle *PodTitle) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := podTitle.GenComponentState(c); err != nil {
		return err
	}

	splits := strings.Split(podTitle.State.PodID, "_")
	if len(splits) != 2 {
		return fmt.Errorf("invalid pod name: %s", podTitle.State.PodID)
	}
	name := splits[1]
	podTitle.Props.Title = fmt.Sprintf("Pod: %s", name)
	podTitle.Transfer(c)
	return nil
}

func (podTitle *PodTitle) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}

	data, err := json.Marshal(component.State)
	if err != nil {
		logrus.Errorf("failed to marshal for eventTable state, %v", err)
		return err
	}
	var state State
	err = json.Unmarshal(data, &state)
	if err != nil {
		logrus.Errorf("failed to unmarshal for eventTable state, %v", err)
		return err
	}
	podTitle.State = state
	return nil
}

func (podTitle *PodTitle) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(podTitle.Props)
	component.State = map[string]interface{}{
		"podId": podTitle.State.PodID,
	}
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-podDetail", "podTitle", func() servicehub.Provider {
		return &PodTitle{Type: "Title"}
	})
}
