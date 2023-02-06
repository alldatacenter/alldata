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
)

// Response 定义 http 应答对象.
type Response struct {
	body     []byte
	internal *http.Response
}

// StatusCode return http status code.
func (r *Response) StatusCode() int {
	return r.internal.StatusCode
}

// IsOK 返回 200 与否.
func (r *Response) IsOK() bool {
	return r.StatusCode()/100 == 2
}

// IsNotfound 返回 404 与否.
func (r *Response) IsNotfound() bool {
	return r.StatusCode() == http.StatusNotFound
}

// IsConflict 返回 409 与否.
func (r *Response) IsConflict() bool {
	return r.StatusCode() == http.StatusConflict
}

// IsBadRequest 返回 400.
func (r *Response) IsBadRequest() bool {
	return r.StatusCode() == http.StatusBadRequest
}

// ResponseHeader 返回指定应答 header 值.
func (r *Response) ResponseHeader(key string) string {
	return r.internal.Header.Get(key)
}

// Headers 返回resp的header信息
func (r *Response) Headers() http.Header {
	return r.internal.Header
}

func (r *Response) Body() []byte {
	return r.body
}
