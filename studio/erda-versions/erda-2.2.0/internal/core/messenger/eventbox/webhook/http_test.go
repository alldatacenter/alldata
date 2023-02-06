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

package webhook

// import (
// 	"bytes"
// 	"encoding/json"
// 	"fmt"
// 	"strings"
// 	"testing"
// 	"time"

// 	"github.com/erda-project/erda/apistructs"
// 	"github.com/erda-project/erda/internal/eventbox/server"
// 	"github.com/erda-project/erda/pkg/http/httpclient"

// 	"github.com/stretchr/testify/assert"
// )

// var serverRunning = false

// func startServer() {
// 	if !serverRunning {
// 		s, _ := server.New()
// 		wh, _ := NewWebHookHTTP()
// 		s.AddEndPoints(wh.GetHTTPEndPoints())
// 		go s.Start()
// 		time.Sleep(1 * time.Second)
// 		serverRunning = true
// 	}
// }

// func TestCreateWebhook(t *testing.T) {
// 	startServer()
// 	req := CreateHookRequest{
// 		Name:   "test-hook-0",
// 		Events: []string{"e1", "e2"},
// 		URL:    "http://dede.com",
// 		Active: true,
// 		HookLocation: HookLocation{
// 			Org:         "1",
// 			Project:     "2",
// 			Application: "3",
// 		},
// 	}
// 	r, resp, err := createHTTP(req)
// 	defer func() { delHTTP(string(resp.Data)) }()

// 	assert.Nil(t, err)
// 	assert.Equal(t, 200, r.StatusCode())
// 	assert.True(t, resp.Success)
// }

// func TestCreateWebhookNoAppID(t *testing.T) {
// 	startServer()
// 	req := CreateHookRequest{
// 		Name:   "test-hook-1",
// 		Events: []string{"e1", "e2"},
// 		URL:    "http://xxx.com",
// 		Active: true,
// 		HookLocation: HookLocation{
// 			Org:     "1",
// 			Project: "2",
// 		},
// 	}
// 	r, resp, err := createHTTP(req)
// 	defer func() { delHTTP(string(resp.Data)) }()

// 	assert.Nil(t, err)
// 	assert.Equal(t, 200, r.StatusCode())
// 	assert.False(t, resp.Success)
// }

// func TestDeleteWebhook(t *testing.T) {
// 	startServer()
// 	r, resp, err := createHTTP(CreateHookRequest{
// 		Name:   "test-hook-2",
// 		Events: []string{"e1", "e2"},
// 		URL:    "http://xxx.com",
// 		Active: true,
// 		HookLocation: HookLocation{
// 			Org:         "1",
// 			Project:     "2",
// 			Application: "3",
// 		},
// 	})
// 	assert.Nil(t, err)
// 	assert.Equal(t, 200, r.StatusCode())
// 	r, err = delHTTP(string(resp.Data))
// 	assert.Nil(t, err)
// 	assert.Equal(t, 200, r.StatusCode())
// }

// // not provide org
// func TestCreateWebhookIllegal1(t *testing.T) {
// 	startServer()
// 	r, resp, err := createHTTP(CreateHookRequest{
// 		Name:   "test-hook-3",
// 		Events: []string{},
// 		URL:    "http://dede",
// 		HookLocation: HookLocation{
// 			Project: "2",
// 		},
// 	})
// 	defer func() { delHTTP(string(resp.Data)) }()
// 	assert.Nil(t, err)
// 	assert.True(t, r.IsOK())
// 	assert.False(t, resp.Success)
// }

// func testListWebhook(t *testing.T) {
// 	r, resp, err := listHTTP(HookLocation{
// 		Org:     "1",
// 		Project: "2",
// 		Env:     []string{"dev", "test"},
// 	})
// 	assert.Nil(t, err)
// 	assert.True(t, r.IsOK())
// 	assert.Equal(t, 1, len(resp.Data))

// 	r, resp, err = listHTTP(HookLocation{
// 		Org:     "1",
// 		Project: "2",
// 		Env:     []string{"dev"},
// 	})
// 	assert.Nil(t, err)
// 	assert.True(t, r.IsOK())
// 	assert.Equal(t, 0, len(resp.Data))

// 	r, resp, err = listHTTP(HookLocation{
// 		Org:     "1",
// 		Project: "2",
// 		Env:     []string{},
// 	})
// 	assert.Nil(t, err)
// 	assert.True(t, r.IsOK())
// 	assert.Equal(t, 1, len(resp.Data))

// }

// func testListWebhookWithBadEnv(t *testing.T) {
// 	r, resp, err := listHTTP(HookLocation{
// 		Org:     "1",
// 		Project: "2",
// 		Env:     []string{"dddd"},
// 	})
// 	assert.Nil(t, err)
// 	assert.True(t, r.IsOK())
// 	assert.False(t, resp.Success)

// 	r, resp, err = listHTTP(HookLocation{
// 		Org:     "1",
// 		Project: "2",
// 		Env:     []string{"Test", "DEV"},
// 	})
// 	assert.Nil(t, err)
// 	assert.True(t, r.IsOK())
// 	assert.Equal(t, 1, len(resp.Data))

// }

// func TestListWebhook(t *testing.T) {
// 	startServer()
// 	r, resp1, err := createHTTP(CreateHookRequest{
// 		Name:   "test-hook-3",
// 		Events: []string{},
// 		URL:    "http://dede",
// 		HookLocation: HookLocation{
// 			Org:         "1",
// 			Project:     "2",
// 			Application: "3",
// 			Env:         []string{"test"},
// 		},
// 	})
// 	assert.Nil(t, err)
// 	assert.True(t, r.IsOK())
// 	assert.True(t, resp1.Success)
// 	fmt.Printf("%+v\n", resp1) // debug print

// 	defer func() { delHTTP(string(resp1.Data)) }()
// 	t.Run("normal", testListWebhook)
// 	t.Run("bad env", testListWebhookWithBadEnv)
// }

// //                                   resp,              response,      err
// func createHTTP(req CreateHookRequest) (*httpclient.Response, apistructs.WebhookCreateResponse, error) {
// 	var buf bytes.Buffer
// 	r, err := httpclient.New().Post("127.0.0.1:9528").Path("/api/dice/eventbox/webhooks").JSONBody(req).Do().Body(&buf)
// 	var resp apistructs.WebhookCreateResponse
// 	json.Unmarshal(buf.Bytes(), &resp)
// 	return r, resp, err
// }

// func delHTTP(id string) (*httpclient.Response, error) {
// 	return httpclient.New().Delete("127.0.0.1:9528").Path("/api/dice/eventbox/webhooks/" + id).Do().DiscardBody()
// }

// func listHTTP(loc HookLocation) (*httpclient.Response, apistructs.WebhookListResponse, error) {
// 	var resp apistructs.WebhookListResponse
// 	r, err := httpclient.New().Get("127.0.0.1:9528").Path("/api/dice/eventbox/webhooks").
// 		Param("orgID", loc.Org).
// 		Param("projectID", loc.Project).
// 		Param("applicationID", loc.Application).
// 		Param("env", strings.Join(loc.Env, ",")).Do().JSON(&resp)
// 	return r, resp, err
// }
