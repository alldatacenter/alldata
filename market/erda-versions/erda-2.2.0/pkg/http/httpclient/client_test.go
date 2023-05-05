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

package httpclient

import (
	"net/http"
	"net/url"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestGetRequest(t *testing.T) {
	request := New().Get("127.0.0.1:8080").Path("/a").Param("aaa", "111").Do()
	assert.Nil(t, request.err)

	assert.Equal(t, "http://127.0.0.1:8080/a?aaa=111", request.internal.URL.String())
	assert.Equal(t, http.MethodGet, request.method)
	assert.Nil(t, request.body)
}

func TestPostRequest(t *testing.T) {
	request := New().Post("127.0.0.1:8080").Path("/a").Param("aaa", "111").Do()
	assert.Nil(t, request.err)

	assert.Equal(t, "http://127.0.0.1:8080/a?aaa=111", request.internal.URL.String())
	assert.Equal(t, http.MethodPost, request.method)
	assert.Nil(t, request.body)
}

func TestPutRequest(t *testing.T) {
	request := New().Put("127.0.0.1:8080").Path("/a").Param("aaa", "111").Do()
	assert.Nil(t, request.err)

	assert.Equal(t, "http://127.0.0.1:8080/a?aaa=111", request.internal.URL.String())
	assert.Equal(t, http.MethodPut, request.method)
	assert.Nil(t, request.body)
}

func TestResponseStatusCode(t *testing.T) {
	var httpResp http.Response
	var resp Response

	// False
	httpResp.StatusCode = http.StatusInternalServerError
	resp.internal = &httpResp
	res := resp.IsOK()
	assert.Equal(t, false, res)

	// True
	httpResp.StatusCode = http.StatusNoContent
	res = resp.IsOK()
	assert.Equal(t, true, res)
}

// func TestRedirect(t *testing.T) {
// 	r := New(WithCompleteRedirect())
// 	assert.Nil(t, err)
// 	ts := httptest.NewServer(http.HandlerFunc(func(w ResponseWriter, r *Request) {
// 		http.Redirect(w, r, "127.0.0.1:8081/b", 301)
// 	}))
// 	ts2 := httptest.NewServer(http.HandlerFunc(func(w ResponseWriter, r *Request) {
// 		http.Redirect(w, r, "127.0.0.1:8081/b", 301)
// 	}))
// 	defer ts.Close()
// 	defer ts2.Close()
// 	req, err := http.NewRequest("POST", "127.0.0.1:8080")
// 	assert.Nil(t, err)
// 	resp, err := r.cli.Do(req)
// 	assert.Nil(t, err)
// 	resp.
// }
//func TestDnsCache(t *testing.T) {
//	r, err := New(WithDnsCache()).Get("www.baidu.com:80").Path("/").Do().DiscardBody()
//	assert.Nil(t, err)
//	fmt.Printf("%+v\n", r.StatusCode()) // debug print
//}

func TestTimeout(t *testing.T) {
	_, err := New(WithTimeout(5*time.Millisecond, 5*time.Millisecond)).Get("www.baidu.com").Path("/").Do().DiscardBody()
	assert.NotNil(t, err, err.(*url.Error).Err.Error())
}
