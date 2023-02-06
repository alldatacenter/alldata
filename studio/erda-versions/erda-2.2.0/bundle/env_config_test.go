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

//import (
//	"bytes"
//	"io/ioutil"
//	"net/http"
//	"os"
//	"reflect"
//	"testing"
//
//	"bou.ke/monkey"
//	"github.com/stretchr/testify/assert"
//)
//
//func TestBundle_FetchDeploymentConfig(t *testing.T) {
//	os.Setenv("ADDON_PLATFORM_ADDR", "http://fake")
//	defer func() {
//		os.Unsetenv("ADDON_PLATFORM_ADDR")
//	}()
//	// fake bundle
//	bundle := New(WithAddOnPlatform())
//
//	defer monkey.UnpatchAll()
//	// path method
//	var httpClient *http.Client
//	monkey.PatchInstanceMethod(reflect.TypeOf(httpClient), "Do", func(_ *http.Client, req *http.Request) (*http.Response, error) {
//		resp := &http.Response{
//			StatusCode: http.StatusOK,
//			Header:     make(http.Header, 0),
//		}
//		// check path
//		assert.Equal(t, "/api/config/deployment",
//			req.URL.Path)
//		// check method
//		assert.Equal(t, "GET", req.Method)
//		resp.Header.Set("Content-Type", "application/json")
//		raw := `
//{
//  "success": true,
//  "data": [
//    {
//      "key": "BASE",
//      "value": "True"
//    },
//    {
//      "key": "FAKE",
//      "value": "true(override)"
//    },
//    {
//      "key": "NOT_FAKE",
//      "value": "False"
//    },
//    {
//      "key": "NEW",
//      "value": "YES"
//    }
//  ]
//}
//`
//		resp.Body = ioutil.NopCloser(bytes.NewReader([]byte(raw)))
//		return resp, nil
//	})
//
//	// do invoke
//	configs, _, err := bundle.FetchDeploymentConfig("app-1-DEV")
//	if assert.NoError(t, err) {
//		assert.Equal(t, map[string]string{
//			"BASE":     "True",
//			"FAKE":     "true(override)",
//			"NOT_FAKE": "False",
//			"NEW":      "YES",
//		}, configs)
//	}
//}
