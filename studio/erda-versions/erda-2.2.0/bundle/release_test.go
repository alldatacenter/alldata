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
//	"encoding/json"
//	"io/ioutil"
//	"net/http"
//	"os"
//	"reflect"
//	"testing"
//
//	"bou.ke/monkey"
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//	"github.com/erda-project/erda/pkg/parser/diceyml"
//)
//
//func TestBundle_PullDiceYAML(t *testing.T) {
//	os.Setenv("DICEHUB_ADDR", "http://fake")
//	defer func() {
//		os.Unsetenv("DICEHUB_ADDR")
//	}()
//	// fake bundle
//	bdl := New(WithDiceHub())
//
//	raw := `version: 2.0
//services:
//  none:
//    image: nginx:latest
//    resources:
//      cpu: 0.01
//      mem: 64
//      disk: 33
//    deployments:
//      replicas: 1`
//
//	rawYAML, err := diceyml.New([]byte(raw), true)
//	if !assert.NoError(t, err) {
//		return
//	}
//
//	// path method
//	var httpClient *http.Client
//	monkey.PatchInstanceMethod(reflect.TypeOf(httpClient), "Do", func(_ *http.Client, req *http.Request) (*http.Response, error) {
//		resp := &http.Response{
//			StatusCode: http.StatusOK,
//			Header:     make(http.Header, 0),
//		}
//		resp.Header.Set("Content-Type", "application/x-yaml; charset=utf-8")
//		resp.Body = ioutil.NopCloser(bytes.NewReader([]byte(raw)))
//		return resp, nil
//	})
//
//	// do invoke
//	diceYAML, err := bdl.GetDiceYAML("11739eb415e14abb874458a977fd04c3")
//
//	if assert.NoError(t, err) {
//		assert.Equal(t, rawYAML, diceYAML)
//	}
//}
//
//func TestBundle_UpdateReference(t *testing.T) {
//	os.Setenv("DICEHUB_ADDR", "http://fake")
//	defer func() {
//		os.Unsetenv("DICEHUB_ADDR")
//	}()
//	// fake bundle
//	bdl := New(WithDiceHub())
//
//	// record
//	var steps []bool
//
//	// path method
//	var httpClient *http.Client
//	monkey.PatchInstanceMethod(reflect.TypeOf(httpClient), "Do", func(_ *http.Client, req *http.Request) (*http.Response, error) {
//		resp := &http.Response{
//			StatusCode: http.StatusOK,
//			Header:     make(http.Header, 0),
//		}
//
//		// check path
//		assert.Equal(t, "/api/releases/02783b176ed84284b60a4506daa5c166/reference/actions/change", req.URL.Path)
//		// check method
//		assert.Equal(t, "PUT", req.Method)
//		// check body
//		body, _ := ioutil.ReadAll(req.Body)
//		var reqBody apistructs.ReleaseReferenceUpdateRequest
//		json.Unmarshal(body, &reqBody)
//		steps = append(steps, reqBody.Increase)
//
//		resp.Header.Set("Content-Type", "application/json; charset=utf-8")
//		resp.Body = ioutil.NopCloser(bytes.NewReader([]byte(`{"success":true}`)))
//		return resp, nil
//	})
//
//	err := bdl.UpdateReference("02783b176ed84284b60a4506daa5c166", true)
//	if !assert.NoError(t, err) {
//		return
//	}
//	err = bdl.UpdateReference("02783b176ed84284b60a4506daa5c166", false)
//	if !assert.NoError(t, err) {
//		return
//	}
//
//	assert.Equal(t, []bool{true, false}, steps)
//}
