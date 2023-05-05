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
)

type RequestSetter func(request *Request)

func SetParams(params url.Values) RequestSetter {
	return func(r *Request) {
		if r.params == nil {
			r.params = make(url.Values)
		}
		for k, v := range params {
			r.params[k] = v
		}
	}
}

func SetHeaders(headers http.Header) RequestSetter {
	return func(r *Request) {
		if r.header == nil {
			r.header = make(map[string]string)
		}
		for k, v := range headers {
			if len(v) > 0 {
				r.header[k] = v[0]
			}
		}
	}
}

func SetCookie(cookie *http.Cookie) RequestSetter {
	return func(r *Request) {
		r.cookie = append(r.cookie, cookie)
	}
}
