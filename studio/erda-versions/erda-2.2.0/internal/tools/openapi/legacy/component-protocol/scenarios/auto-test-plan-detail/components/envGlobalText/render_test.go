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

package envGlobalText

import (
	"context"
	"testing"

	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/types"
)

func Test_render(t *testing.T) {
	a := &ComponentFileInfo{
		CommonFileInfo: CommonFileInfo{
			Props: map[string]interface{}{
				"a": "b",
				"c": "d",
			},
			State: State{
				Value: "[{ name: 'a', content: 'xxxxxxx' }]",
			},
		},
	}
	c := &apistructs.Component{}
	want := &apistructs.Component{
		State: map[string]interface{}{
			"value": "[{ name: 'a', content: 'xxxxxxx' }]",
		},
		Props: map[string]interface{}{
			"a": "b",
			"c": "d",
		},
	}
	err := a.marshal(c)
	assert.NoError(t, err)
	assert.Equal(t, c, want)
}

func Test_Render(t *testing.T) {
	ctx := context.WithValue(context.Background(), protocol.GlobalInnerKeyCtxBundle.String(), protocol.ContextBundle{})
	i := &ComponentFileInfo{}
	gs := &apistructs.GlobalStateData{
		types.AutotestGlobalKeyEnvData: apistructs.AutoTestAPIConfig{
			Domain: "domain",
			Global: map[string]apistructs.AutoTestConfigItem{
				"key": {
					Name:  "name",
					Value: "value",
				},
			},
			Header: map[string]string{
				"key": "value",
			},
		},
	}
	c := &apistructs.Component{}
	err := i.Render(ctx, c, apistructs.ComponentProtocolScenario{},
		apistructs.ComponentEvent{
			Operation:     "ooo",
			OperationData: nil,
		}, gs)
	assert.NoError(t, err)
	want := map[string]interface{}{
		"value": `[{"name":"name","content":"value"}]`,
	}
	assert.Equal(t, c.State, want)
}
