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

package common

import (
	"fmt"
	"net/http"
	"net/http/httputil"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors"
)

type config struct {
	Order    int
	DumpAll  bool   `file:"dump_all" default:"false"`
	CheckKey string `file:"check_key" default:"__dump__"`
}

// +provider
type provider struct {
	Cfg *config
}

var _ interceptors.Interface = (*provider)(nil)

func (p *provider) List() []*interceptors.Interceptor {
	return []*interceptors.Interceptor{{Order: p.Cfg.Order, Wrapper: p.Interceptor}}
}

func (p *provider) Interceptor(h http.HandlerFunc) http.HandlerFunc {
	return func(rw http.ResponseWriter, r *http.Request) {
		dump := p.Cfg.DumpAll
		if !dump {
			_, dump = r.URL.Query()[p.Cfg.CheckKey]
		}
		if dump {
			byts, err := httputil.DumpRequest(r, false)
			if err == nil {
				fmt.Println(string(byts))
			}
		}
		h(rw, r)
	}
}

func init() {
	servicehub.Register("openapi-interceptor-dump", &servicehub.Spec{
		Services:   []string{"openapi-interceptor-dump"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
