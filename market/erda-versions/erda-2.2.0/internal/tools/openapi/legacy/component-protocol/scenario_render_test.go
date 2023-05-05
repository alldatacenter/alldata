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

package component_protocol

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

type X struct {
	State XState      `json:"state"`
	Props interface{} `json:"props"`
	Name  string      `json:"-"`
	Type  string
}

type XState struct {
	S string `json:"s"`
}

func XC(x *X) CompRender {
	return x
}

func (a *X) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, _ *apistructs.GlobalStateData) error {
	return nil
}

func Test_marshal(t *testing.T) {
	init := apistructs.Component{
		Version:    "1.0",
		Type:       "well",
		Name:       "good",
		Props:      map[string]interface{}{"old": "val"},
		Data:       map[string]interface{}{"k": "v"},
		State:      map[string]interface{}{"s": "l"},
		Operations: map[string]interface{}{"v": "n"},
	}
	x := X{}
	x1 := XC(&x)
	assert.Nil(t, unmarshal(&x1, &init))
	assert.Equal(t, "l", x.State.S)
	assert.Equal(t, map[string]interface{}{"old": "val"}, x.Props)
	assert.Equal(t, "", x.Name)
	assert.Equal(t, "well", x.Type)

	x.State.S = "changed"
	x.Props = map[string]interface{}{"new": "key"}
	x.Type = "sell"

	assert.Nil(t, marshal(&x1, &init))
	assert.Equal(t, "changed", init.State["s"])
	assert.Equal(t, map[string]interface{}{"new": "key"}, init.Props)

	assert.Equal(t, "sell", init.Type)
	// no changed
	assert.Equal(t, "good", init.Name)
}
