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

package sbac_test

import (
	"encoding/json"
	"testing"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/sbac"
)

// need not do unit test
func TestPolicy_CreateDefaultConfig(t *testing.T) {
	new(sbac.Policy).CreateDefaultConfig(make(map[string]interface{}))
}

func TestPolicy_UnmarshalConfig(t *testing.T) {
	var config = `{...}`
	if _, err, _ := new(sbac.Policy).UnmarshalConfig([]byte(config)); err == nil {
		t.Fatal("error should be occurred")
	}

	config = `{
		"global":false,
		"switch":true,
		"accessControlAPI":"an invalid uri",
		"methods":["post", "GET","HEAD","OPTIONS","TRACE"],
		"withHeaders":["x-control-key"],
		"withBody":true,
		"withCookie":true
	}`
	if _, err, _ := new(sbac.Policy).UnmarshalConfig([]byte(config)); err == nil {
		t.Fatal("error should be occurred with accessControlAPI")
	}

	config = `{
		"global":false,
		"switch":true,
		"accessControlAPI":"https://my-server.com/access-control",
		"methods":["post", "GET","HEAD","OPTIONS","TRACE"],
		"withHeaders":["x-control-key"],
		"withBody":true,
		"withCookie":true
	}`
	if _, err, _ := new(sbac.Policy).UnmarshalConfig([]byte(config)); err != nil {
		t.Fatal(err)
	}
}

func TestPluginConfig_ToPluginReqDto(t *testing.T) {
	var config = `{
		"global":false,
		"switch":true,
		"accessControlAPI":"https://my-server.com/access-control",
		"patterns": [".*", "/api", ""],
		"methods":["post", "GET","HEAD","OPTIONS","TRACE"],
		"withHeaders":["x-control-key"],
		"withBody":true,
		"withCookie":true
	}`
	var pc sbac.PluginConfig
	if err := json.Unmarshal([]byte(config), &pc); err != nil {
		t.Fatal(err)
	}
	pc.ToPluginReqDto()
	pc.Switch = false
	if err := pc.IsValidDto(); err != nil {
		t.Fatal(err)
	}
	pc.Switch = true
	pc.AccessControlAPI = ""
	if err := pc.IsValidDto(); err == nil {
		t.Fatal("should be err")
	}
}
