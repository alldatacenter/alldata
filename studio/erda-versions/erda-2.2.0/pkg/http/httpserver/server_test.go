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

package httpserver_test

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/http/httpserver"
)

func TestServer_Base64EncodeRequestBody(t *testing.T) {
	s := "hello world"
	base64Str := base64.StdEncoding.EncodeToString([]byte(s))

	go func() {
		server := httpserver.New(":8080")
		server.RegisterEndpoint([]httpserver.Endpoint{
			{
				Path:   "/base64-test",
				Method: http.MethodPost,
				Handler: func(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
					b, err := ioutil.ReadAll(r.Body)
					if err != nil {
						return nil, err
					}
					return httpserver.OkResp(string(b) == s)
				},
			},
		})
		server.ListenAndServe()
	}()

	time.Sleep(time.Second)

	// without base64 header
	resp, err := http.DefaultClient.Post("http://localhost:8080/base64-test", httpserver.ContentTypeJSON, bytes.NewBufferString(base64Str))
	assert.NoError(t, err)
	var result httpserver.Resp
	err = json.NewDecoder(resp.Body).Decode(&result)
	assert.NoError(t, err)
	assert.False(t, result.Data.(bool))

	// with base64 header
	req, err := http.NewRequest(http.MethodPost, "http://localhost:8080/base64-test", bytes.NewBufferString(base64Str))
	assert.NoError(t, err)
	req.Header["Content-Type"] = []string{httpserver.ContentTypeJSON}
	req.Header[httpserver.Base64EncodedRequestBody] = []string{"true"}
	resp, err = http.DefaultClient.Do(req)
	assert.NoError(t, err)
	err = json.NewDecoder(resp.Body).Decode(&result)
	assert.NoError(t, err)
	assert.True(t, result.Data.(bool))
}
