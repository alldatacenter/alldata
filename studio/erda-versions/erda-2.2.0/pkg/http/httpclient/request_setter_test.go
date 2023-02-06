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
)

func TestSetParams(t *testing.T) {
	var (
		r Request
		p = make(url.Values)
	)
	p.Add("withQuota", "true")
	SetParams(p)(&r)
	if v := r.params.Get("withQuota"); v != "true" {
		t.Fatal("error")
	}
}

func TestSetHeaders(t *testing.T) {
	var (
		r Request
		h = make(http.Header)
	)
	h.Add("withQuota", "true")
	SetHeaders(h)(&r)
	if v := r.header["Withquota"]; v != "true" {
		t.Fatal("error", v, r.header)
	}
}

func TestSetCookie(t *testing.T) {
	var (
		r Request
		c = &http.Cookie{Name: "withQuota", Value: "true"}
	)
	SetCookie(c)(&r)
	if r.cookie[0].Value != "true" {
		t.Fatal("error")
	}
}
