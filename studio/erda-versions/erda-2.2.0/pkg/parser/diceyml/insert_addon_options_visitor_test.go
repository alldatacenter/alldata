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

package diceyml

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestInsertAddonOptions(t *testing.T) {
	obj := Object{
		AddOns: AddOns{"xxx": &AddOn{Plan: "mysql:small", Options: map[string]string{"op1": "op1v"}}, "yyy": &AddOn{Plan: "mysql:large", Options: map[string]string{"op2": "op2v"}}},
	}
	InsertAddonOptions(&obj, BaseEnv, "mysql", map[string]string{"op3": "op3v"})
	assert.True(t, obj.AddOns["xxx"].Options["op3"] == "op3v")
	assert.True(t, obj.AddOns["yyy"].Options["op3"] == "op3v")

}

func TestInsertAddonOptionsEnv(t *testing.T) {
	obj := Object{
		AddOns:       AddOns{"xxx": &AddOn{Plan: "mysql:small", Options: map[string]string{"op1": "op1v"}}, "yyy": &AddOn{Plan: "mysql:large", Options: map[string]string{"op2": "op2v"}}},
		Environments: EnvObjects{"test": &EnvObject{AddOns: AddOns{"xxx": &AddOn{Plan: "mysql:small", Options: map[string]string{"op1": "op1v"}}, "yyy": &AddOn{Plan: "mysql:large", Options: map[string]string{"op2": "op2v"}}}}},
	}
	InsertAddonOptions(&obj, TestEnv, "mysql", map[string]string{"op3": "op3v"})

	assert.True(t, obj.Environments["test"].AddOns["xxx"].Options["op3"] == "op3v")
	assert.True(t, obj.Environments["test"].AddOns["yyy"].Options["op3"] == "op3v")
	assert.True(t, obj.AddOns["yyy"].Options["op3"] == "")
	assert.True(t, obj.AddOns["xxx"].Options["op3"] == "")

}

func TestInsertAddonOptionsWrongEnv(t *testing.T) {
	obj := Object{
		AddOns: AddOns{"xxx": &AddOn{Plan: "mysql:small", Options: map[string]string{"op1": "op1v"}}, "yyy": &AddOn{Plan: "mysql:large", Options: map[string]string{"op2": "op2v"}}},
	}
	InsertAddonOptions(&obj, TestEnv, "mysql", map[string]string{"op3": "op3v"})
	assert.False(t, obj.AddOns["xxx"].Options["op3"] == "op3v")
	assert.False(t, obj.AddOns["yyy"].Options["op3"] == "op3v")

}
