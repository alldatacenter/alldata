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
	"net/http/httputil"
	"net/url"
	"strings"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/pkg/transport/http/runtime"
	discover "github.com/erda-project/erda/internal/pkg/service-discover"
)

// Proxy .
type Proxy struct {
	Log      logs.Logger
	Discover discover.Interface
}

func (p *Proxy) Wrap(method, path, backendPath, service string, wrapers ...func(h http.HandlerFunc) http.HandlerFunc) (http.HandlerFunc, error) {
	srvURL, err := p.Discover.ServiceURL("http", service)
	if err != nil {
		return nil, fmt.Errorf("fail to discover url (%s %s) of service %q: %s", method, backendPath, service, err)
	}
	return p.WrapWithServiceURL(method, path, backendPath, srvURL, wrapers...)
}

func (p *Proxy) WrapWithServiceURL(method, path, backendPath, srvURL string, wrapers ...func(h http.HandlerFunc) http.HandlerFunc) (http.HandlerFunc, error) {
	srvURL = strings.TrimRight(srvURL, "/")
	pubMatcher, err := runtime.Compile(path)
	if err != nil {
		return nil, fmt.Errorf("invalid path %q: %s", path, err)
	}
	backendURL := srvURL + "/" + strings.TrimLeft(backendPath, "/")
	backMatcher, err := runtime.Compile(backendPath)
	if err != nil {
		return nil, fmt.Errorf("invalid backend-path %q: %s", backendURL, err)
	}
	var director func(req *http.Request)
	if backMatcher.IsStatic() {
		director, err = p.staticDirector(srvURL, backendPath)
	} else {
		director, err = p.paramsDirector(srvURL, pubMatcher, backMatcher)
	}
	if err != nil {
		return nil, err
	}
	p.Log.Infof("[proxy] %s -> %s", path, backendURL)
	rp := &httputil.ReverseProxy{Director: func(req *http.Request) {
		director(req)
		if _, ok := req.Header["User-Agent"]; !ok {
			// explicitly disable User-Agent so it's not set to default value
			req.Header.Set("User-Agent", "")
		}
	}}
	hander := rp.ServeHTTP
	for i := len(wrapers) - 1; i > 0; i-- {
		hander = wrapers[i](hander)
	}
	return hander, nil
}

func (p *Proxy) staticDirector(srvURL, backendPath string) (func(req *http.Request), error) {
	srvURL = srvURL + "/" + strings.TrimLeft(backendPath, "/")
	target, err := url.Parse(srvURL)
	if err != nil {
		return nil, fmt.Errorf("fail to parse service url: %s", err)
	}
	path, rawpath := target.Path, target.EscapedPath()
	return func(req *http.Request) {
		req.URL.Scheme = target.Scheme
		req.URL.Host = target.Host
		originpath := req.URL.Path
		req.URL.Path, req.URL.RawPath = path, rawpath
		p.Log.Debugf("proxy %s %s -> %s", req.Method, originpath, req.URL)
	}, nil
}

func (p *Proxy) paramsDirector(srvURL string, pmatcher, bmatcher runtime.Matcher) (func(req *http.Request), error) {
	if pmatcher.IsStatic() {
		return nil, fmt.Errorf("backend-path:%s has parameters, but publish-path:%s is static", bmatcher.Pattern(), pmatcher.Pattern())
	}
	fields := bmatcher.Fields()
	for _, field := range fields {
		var find bool
		for _, key := range pmatcher.Fields() {
			if field == key {
				find = true
				break
			}
		}
		if !find {
			return nil, fmt.Errorf("backend-path:%s has parameter %q, but not present in publish-path:%s", bmatcher.Pattern(), field, pmatcher.Pattern())
		}
	}
	target, err := url.Parse(srvURL)
	if err != nil {
		return nil, fmt.Errorf("fail to parse service url: %s", err)
	}
	segments := buildPathToSegments(bmatcher.Pattern())
	return func(req *http.Request) {
		req.URL.Scheme = target.Scheme
		req.URL.Host = target.Host
		params, _ := pmatcher.Match(req.URL.Path)
		sb := strings.Builder{}
		for _, seg := range segments {
			if seg.typ == pathStatic {
				sb.WriteString(seg.name)
			} else {
				sb.WriteString(params[seg.name])
			}
		}
		rawpath := req.URL.Path
		req.URL.Path, req.URL.RawPath = sb.String(), ""
		req.URL.RawPath = req.URL.EscapedPath()
		p.Log.Debugf("proxy %s %s -> %s", req.Method, rawpath, req.URL)
	}, nil
}

const pathStatic, pathField int8 = 0, 1

type pathSegment struct {
	typ  int8
	name string
}

func (s *pathSegment) String() string { return fmt.Sprint(*s) }

func buildPathToSegments(path string) (segs []*pathSegment) {
	chars := []rune(path)
	start, i, n := 0, 0, len(chars)
	for ; i < n; i++ {
		if chars[i] == '{' {
			if start < i {
				segs = append(segs, &pathSegment{
					typ:  pathStatic,
					name: string(chars[start:i]),
				})
			}
			i++
		loop:
			for begin := i; i < n; i++ {
				switch chars[i] {
				case '}':
					segs = append(segs, &pathSegment{
						typ:  pathField,
						name: string(chars[begin:i]),
					})
					start = i + 1
					break loop
				case '=':
					segs = append(segs, &pathSegment{
						typ:  pathField,
						name: string(chars[begin:i]),
					})
					for ; i < n && chars[i] != '}'; i++ {
					}
					start = i + 1
					break loop
				}
			}
		}
	}
	if start < n {
		segs = append(segs, &pathSegment{
			typ:  pathStatic,
			name: string(chars[start:]),
		})
	}
	return
}
