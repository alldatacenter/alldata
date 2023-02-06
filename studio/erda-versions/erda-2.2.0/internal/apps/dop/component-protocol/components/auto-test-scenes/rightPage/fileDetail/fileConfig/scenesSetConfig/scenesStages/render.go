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

package scenesStages

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "scenesStages", func() servicehub.Provider {
		return &SceneStage{}
	})
}

func (s *SceneStage) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := s.initFromProtocol(ctx, c, event, gs); err != nil {
		return err
	}
	if operationFunc, ok := OperationRender[event.Operation]; ok {
		for _, Handle := range operationFunc {
			if err := Handle(s); err != nil {
				return err
			}
		}
	}

	return s.setToComponent(c)
}
