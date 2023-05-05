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

package httpclientutil

import (
	"net/url"
	"strconv"
	"strings"
)

const (
	httpProto  = "http://"
	httpsProto = "https://"
)

func WrapHttp(addr string) string {
	if strings.HasPrefix(addr, httpProto) {
		return addr
	}
	if strings.HasPrefix(addr, httpsProto) {
		return httpProto + strings.TrimLeft(addr, httpsProto)
	}
	return httpProto + addr
}

func WrapHttps(addr string) string {
	if strings.HasPrefix(addr, httpsProto) {
		return addr
	}
	if strings.HasPrefix(addr, httpProto) {
		return httpsProto + strings.TrimLeft(addr, httpProto)
	}
	return httpsProto + addr
}

func WrapProto(addr string) string {
	if strings.HasPrefix(addr, httpsProto) || strings.HasPrefix(addr, httpProto) {
		return addr
	}

	// default return http address
	return httpProto + addr
}

func RmProto(url string) string {
	if strings.HasPrefix(url, httpProto) {
		return strings.TrimPrefix(url, httpProto)
	}
	if strings.HasPrefix(url, httpsProto) {
		return strings.TrimPrefix(url, httpsProto)
	}
	return url
}

func GetProto(url string) string {
	if strings.HasPrefix(url, httpProto) {
		return "http"
	}
	if strings.HasPrefix(url, httpsProto) {
		return "https"
	}
	return "http"
}

func GetHost(rawurl string) string {
	URL, _ := url.Parse(WrapProto(rawurl))
	return URL.Host
}

func GetPath(rawurl string) string {
	URL, _ := url.Parse(WrapProto(rawurl))
	return URL.Path
}

func GetPort(rawurl string) int {
	URL, _ := url.Parse(WrapProto(rawurl))
	s := URL.Port()
	if s == "" {
		return 80
	}
	i, _ := strconv.Atoi(s)
	return i
}

func CombineHostAndPath(host string, path string) string {
	host = WrapProto(host)
	if strings.HasSuffix(host, "/") {
		host = host[:len(host)-1]
	}
	return host + path
}

func HasSchema(rawurl string) bool {
	if strings.HasPrefix(rawurl, httpProto) || strings.HasPrefix(rawurl, httpsProto) {
		return true
	}
	return false
}
