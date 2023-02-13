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

package proxy

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"reflect"
	"testing"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	discover "github.com/erda-project/erda/internal/pkg/service-discover"
)

func Test_buildPathToSegments(t *testing.T) {
	tests := []struct {
		name     string
		path     string
		wantSegs []*pathSegment
	}{
		{
			path: "/abc/def",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/abc/def",
				},
			},
		},
		{
			path: "{def}",
			wantSegs: []*pathSegment{
				{
					typ:  pathField,
					name: "def",
				},
			},
		},
		{
			path: "/abc/{def}",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/abc/",
				},
				{
					typ:  pathField,
					name: "def",
				},
			},
		},
		{
			path: "{abc}/def",
			wantSegs: []*pathSegment{
				{
					typ:  pathField,
					name: "abc",
				},
				{
					typ:  pathStatic,
					name: "/def",
				},
			},
		},
		{
			path: "/abc/{def}/g",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/abc/",
				},
				{
					typ:  pathField,
					name: "def",
				},
				{
					typ:  pathStatic,
					name: "/g",
				},
			},
		},
		{
			path: "/abc/{def.gh}/ijk",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/abc/",
				},
				{
					typ:  pathField,
					name: "def.gh",
				},
				{
					typ:  pathStatic,
					name: "/ijk",
				},
			},
		},
		{
			path: "/abc/{def=subpath/**}/g",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/abc/",
				},
				{
					typ:  pathField,
					name: "def",
				},
				{
					typ:  pathStatic,
					name: "/g",
				},
			},
		},
		{
			path: "/abc/{def=subpath/**}",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/abc/",
				},
				{
					typ:  pathField,
					name: "def",
				},
			},
		},
		{
			path: "/abc/{def}/{ghi}",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/abc/",
				},
				{
					typ:  pathField,
					name: "def",
				},
				{
					typ:  pathStatic,
					name: "/",
				},
				{
					typ:  pathField,
					name: "ghi",
				},
			},
		},
		{
			path: "/{a}/b{c}/{d}/{e}f/g",
			wantSegs: []*pathSegment{
				{
					typ:  pathStatic,
					name: "/",
				},
				{
					typ:  pathField,
					name: "a",
				},
				{
					typ:  pathStatic,
					name: "/b",
				},
				{
					typ:  pathField,
					name: "c",
				},
				{
					typ:  pathStatic,
					name: "/",
				},
				{
					typ:  pathField,
					name: "d",
				},
				{
					typ:  pathStatic,
					name: "/",
				},
				{
					typ:  pathField,
					name: "e",
				},
				{
					typ:  pathStatic,
					name: "f/g",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if gotSegs := buildPathToSegments(tt.path); !reflect.DeepEqual(gotSegs, tt.wantSegs) {
				t.Errorf("buildPathToSegments() = %v, want %v", gotSegs, tt.wantSegs)
			}
		})
	}
}

type testDiscover struct {
	addr string
	err  error
}

func (d *testDiscover) Endpoint(scheme, service string) (string, error) {
	if d.err != nil {
		return "", d.err
	}
	return d.addr, nil
}
func (d *testDiscover) ServiceURL(scheme, service string) (string, error) {
	if d.err != nil {
		return "", d.err
	}
	return fmt.Sprintf("%s://%s", scheme, d.addr), nil
}

func Test_Proxy(t *testing.T) {
	logger := logrusx.New()
	tests := []struct {
		name            string
		method          string
		path            string
		backendPath     string
		wrapers         []func(h http.HandlerFunc) http.HandlerFunc
		reqPath         string
		backendRecvPath string
		wrapError       bool
		pathError       bool
	}{
		{
			method:          "GET",
			path:            "/abc/def",
			backendPath:     "/abc/def",
			reqPath:         "/abc/def",
			backendRecvPath: "/abc/def",
		},
		{
			method:          "GET",
			path:            "/api/abc/def",
			backendPath:     "/abc/def",
			reqPath:         "/api/abc/def",
			backendRecvPath: "/abc/def",
		},
		{
			method:          "GET",
			path:            "/api/abc/{name}",
			backendPath:     "/abc/{name}",
			reqPath:         "/api/abc/def",
			backendRecvPath: "/abc/def",
		},
		{
			method:          "GET",
			path:            "/api/abc/{name}",
			backendPath:     "/abc/{name}",
			reqPath:         "/api/abc/def",
			backendRecvPath: "/abc/xxxx",
			pathError:       true,
		},
		{
			method:          "GET",
			path:            "/api/abc/{name}/{name2}",
			backendPath:     "/abc/{name}",
			reqPath:         "/api/abc/def/g",
			backendRecvPath: "/abc/def",
		},
		{
			method:      "GET",
			path:        "/api/abc/{name}",
			backendPath: "/abc/{name}/{name2}",
			wrapError:   true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			backend := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.Method != tt.method {
					t.Errorf("unexpected method %q", r.Method)
				}
				if r.URL.Path != tt.backendRecvPath && !tt.pathError {
					t.Errorf("unexpected backend path %q, want path %q", r.URL.Path, tt.backendRecvPath)
				} else if r.URL.Path == tt.backendRecvPath && tt.pathError {
					t.Errorf("want path error, but got path %q", r.URL.Path)
				}
			}))
			defer backend.Close()
			backendURL, err := url.Parse(backend.URL)
			if err != nil {
				t.Fatal(err)
			}

			p := Proxy{
				Log:      logger,
				Discover: &testDiscover{addr: backendURL.Host},
			}
			handler, err := p.Wrap(tt.method, tt.path, tt.backendPath, "", tt.wrapers...)
			if err != nil {
				if !tt.wrapError {
					t.Fatalf("Proxy.Wrap() got error: %v", err)
				}
				return
			} else if tt.wrapError && err == nil {
				t.Fatalf("Proxy.Wrap() want error, but got nil error")
				return
			}

			frontend := httptest.NewServer(handler)
			defer frontend.Close()

			getReq, _ := http.NewRequest(tt.method, frontend.URL+tt.reqPath, nil)
			getReq.Host = "some-name"
			getReq.Header.Set("Connection", "close")
			getReq.Close = true
			_, err = frontend.Client().Do(getReq)
			if err != nil {
				t.Fatalf("Get: %v", err)
			}
		})
	}
}

func Test_Discover(t *testing.T) {
	logger := logrusx.New()
	tests := []struct {
		name      string
		discover  discover.Interface
		wantError bool
	}{
		{
			wantError: false,
			discover:  &testDiscover{addr: "localhost:8080"},
		},
		{
			wantError: true,
			discover:  &testDiscover{err: fmt.Errorf("test discover error")},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := Proxy{
				Log:      logger,
				Discover: tt.discover,
			}
			_, err := p.Wrap("GET", "/", "/", "")
			if !tt.wantError && err != nil {
				t.Fatalf("Proxy.Wrap() got error: %v", err)
			} else if tt.wantError && err == nil {
				t.Fatalf("Proxy.Wrap() want error, but got nil error")
			}
		})
	}
}
