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

package apitestsv2

import (
	"fmt"
	"net/url"
	"strings"

	"github.com/erda-project/erda/pkg/http/httpclientutil"
)

// domain: schema://${prefix} + u
func polishURL(rawurl string, domain string) (string, error) {
	if rawurl == "" {
		return "", fmt.Errorf("empty url")
	}

	// 将 URL 后面的参数去掉
	rawurl = strings.Split(rawurl, "?")[0]

	// 若 url 非 http://, https:// 开头，则补充 domain
	if !httpclientutil.HasSchema(rawurl) {
		rawurl = domain + rawurl
	}

	// 保证协议头存在
	rawurl = httpclientutil.WrapProto(rawurl)

	if _, err := url.Parse(rawurl); err != nil {
		return "", fmt.Errorf("invalid url: %s, err: %v", rawurl, err)
	}

	return rawurl, nil
}
