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

package bundle

import (
	"bytes"
	"io/ioutil"
	"net/http"
	"os"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"
)

func TestBundle_GetRuntimeAddonConfig(t *testing.T) {
	os.Setenv("ADDON_PLATFORM_ADDR", "http://fake")
	defer func() {
		os.Unsetenv("ADDON_PLATFORM_ADDR")
	}()
	// fake bundle
	//bdl := New(WithAddOnPlatform())

	defer monkey.UnpatchAll()
	// path method
	var httpClient *http.Client
	monkey.PatchInstanceMethod(reflect.TypeOf(httpClient), "Do", func(_ *http.Client, req *http.Request) (*http.Response, error) {
		resp := &http.Response{
			StatusCode: http.StatusOK,
			Header:     make(http.Header, 0),
		}
		// check path
		assert.Equal(t, "/api/addon-platform/console/runtime/1377/addons/config",
			req.URL.Path)
		// check method
		assert.Equal(t, "GET", req.Method)
		// check params
		assert.Equal(t, "1", req.URL.Query().Get("project_id"))
		assert.Equal(t, "DEV", req.URL.Query().Get("env"))
		assert.Equal(t, "terminus-test", req.URL.Query().Get("az"))
		resp.Header.Set("Content-Type", "application/json")
		raw := `
{
  "success": true,
  "data": [
    {
      "name": "a1",
      "config": {
        "DEMO": "True",
        "FAKE": "true"
      }
    },
    {
      "name": "a2",
      "config": {
        "NOT_FAKE": "false"
      }
    }
  ]
}
`
		resp.Body = ioutil.NopCloser(bytes.NewReader([]byte(raw)))
		return resp, nil
	})

	// do invoke
	//configs, err := bdl.GetRuntimeAddonConfig(&apistructs.GetRuntimeAddonConfigRequest{
	//	RuntimeID:   1377,
	//	ProjectID:   1,
	//	Workspace:   "DEV",
	//	ClusterName: "terminus-test",
	//})
	//if assert.NoError(t, err) {
	//	assert.Equal(t, map[string]string{
	//		"DEMO":     "True",
	//		"FAKE":     "true",
	//		"NOT_FAKE": "false",
	//	}, configs)
	//}
}
