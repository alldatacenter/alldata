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

package orykratos

import (
	"fmt"
	"net/http"
	"net/url"

	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/common"
)

func (p *provider) LoginURL(rw http.ResponseWriter, r *http.Request) {
	referer := r.Header.Get("Referer")
	common.ResponseJSON(rw, &struct {
		URL string `json:"url"`
	}{
		URL: redirectUrl(referer),
	})
}

func (p *provider) Logout(rw http.ResponseWriter, r *http.Request) {
	common.ResponseJSON(rw, &struct {
		URL string `json:"url"`
	}{
		URL: "/uc/login",
	})
}

func redirectUrl(referer string) string {
	if referer == "" {
		return "/uc/login"
	}
	return fmt.Sprintf("/uc/login?redirectUrl=%s", url.QueryEscape(referer))
}
