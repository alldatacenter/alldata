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

//import (
//	"net/http"
//	"net/url"
//	"testing"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/pkg/http/customhttp"
//)
//
//func TestContentTypeIsJson(t *testing.T) {
//	assert.True(t, contentTypeIsJson("application/json"))
//	assert.True(t, contentTypeIsJson("application/json; charset=UTF-8"))
//	assert.True(t, contentTypeIsJson("application/json; charset=UTF-8; qs=2"))
//	assert.True(t, contentTypeIsJson("application/json; qs=2"))
//
//	assert.False(t, contentTypeIsJson("text/plain"))
//	assert.False(t, contentTypeIsJson("text/html"))
//	assert.False(t, contentTypeIsJson("application/xml"))
//	assert.False(t, contentTypeIsJson("whatever"))
//	assert.False(t, contentTypeIsJson("another"))
//	assert.True(t, contentTypeIsJson("qs=2; application/json"))
//}
//
//func TestProto(t *testing.T) {
//	r_ := Request{
//		cli:         &http.Client{},
//		host:        "https://www.baidu.com",
//		path:        "/",
//		proto:       "http",
//		method:      http.MethodGet,
//		retryOption: RetryErrResp,
//	}
//	r := r_.Do()
//	assert.Equal(t, r.internal.URL.Scheme, "https")
//	assert.Equal(t, r.internal.URL.Host, "www.baidu.com")
//	resp, _ := r.DiscardBody()
//	assert.Equal(t, resp.StatusCode(), http.StatusOK)
//
//	r_ = Request{
//		host:        "http://dice.test.terminus.io/",
//		path:        "/",
//		proto:       "whatever",
//		method:      http.MethodGet,
//		retryOption: RetryErrResp,
//	}
//	r = r_.Do()
//	assert.Equal(t, r.internal.URL.Scheme, "http")
//	assert.Equal(t, r.internal.URL.Host, "dice.test.terminus.io")
//
//	r_ = Request{
//		cli:         &http.Client{},
//		host:        "dice.test.terminus.io",
//		path:        "/",
//		proto:       "http",
//		method:      http.MethodGet,
//		retryOption: RetryErrResp,
//	}
//	r = r_.Do()
//	assert.Equal(t, r.internal.URL.Scheme, "http")
//	assert.Equal(t, r.internal.URL.Host, "dice.test.terminus.io")
//	resp, _ = r.DiscardBody()
//	assert.Equal(t, resp.StatusCode(), http.StatusOK)
//}
//func TestProtoNetportal(t *testing.T) {
//	t.Skip()
//	customhttp.SetInetAddr("netportal.marathon.l4lb.thisdcos.directory")
//	r_ := Request{
//		cli:         &http.Client{},
//		host:        "inet://test.terminus.io:8443/ui.marathon.l4lb.thisdcos.directory",
//		path:        "/",
//		method:      http.MethodGet,
//		retryOption: RetryErrResp,
//	}
//	r := r_.Do()
//	assert.Equal(t, nil, r.err)
//	assert.Equal(t, "http", r.internal.URL.Scheme)
//	assert.Equal(t, "netportal.marathon.l4lb.thisdcos.directory", r.internal.URL.Host)
//	resp, _ := r.DiscardBody()
//	assert.Equal(t, http.StatusOK, resp.StatusCode())
//
//	resp, err := New().Get("inet://test.terminus.io:8443/ui.marathon.l4lb.thisdcos.directory").Path("/").Do().
//		DiscardBody()
//	assert.Equal(t, nil, err)
//	assert.Equal(t, http.StatusOK, resp.StatusCode())
//}
//
//func TestParams(t *testing.T) {
//	r := Request{
//		cli:         &http.Client{},
//		host:        "https://www.baidu.com",
//		path:        "/",
//		proto:       "http",
//		method:      http.MethodGet,
//		retryOption: RetryErrResp,
//	}
//	params := make(url.Values)
//	params.Add("k1", "v1")
//	params.Add("k2", "v2")
//	r.Params(params)
//	assert.Equal(t, "v2", r.params.Get("k2"))
//}
//
////func TestRetryWithBody(t *testing.T) {
////	r := Request{
////		cli:    &http.Client{},
////		host:   "https://www.baidu.com",
////		path:   "/",
////		proto:  "http",
////		method: http.MethodPost,
////		retryOption: RetryOption{3, 2, []RetryFunc{
////			func(req *http.Request, resp *http.Response, respErr error) bool {
////
////				return respErr != nil || resp.StatusCode/100 != 5
////			},
////		},
////		},
////	}
////	_, err := r.RawBody(bytes.NewReader([]byte("ssss"))).Do().DiscardBody()
////	assert.Nil(t, err)
////
////}
//
////func TestMultipartFormDataRequest(t *testing.T) {
////	client := New()
////	f, err := os.Open("../../docs/images/architecture.png")
////	assert.NoError(t, err)
////	var body bytes.Buffer
////	resp, err := client.Post("cmdb.default.svc.cluster.local:9093").
////		Path("/api/files").
////		Header("User-ID", "2").
////		MultipartFormDataBody(map[string]MultipartItem{
////			"file":  {Reader: f, Filename: "xxx.xxx"},
////			"other": {Reader: ioutil.NopCloser(strings.NewReader("hello world!"))},
////		}).Do().Body(&body)
////	assert.NoError(t, err)
////	fmt.Println(body.String())
////	_ = resp
////}
