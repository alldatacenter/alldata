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
	"net/http"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors"
	"github.com/erda-project/erda/pkg/goroutine_context"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/i18n"
)

// +provider
type provider struct {
	Cfg *interceptors.Config
}

var _ interceptors.Interface = (*provider)(nil)

func (p *provider) List() []*interceptors.Interceptor {
	return []*interceptors.Interceptor{{Order: p.Cfg.Order, Wrapper: p.Interceptor}}
}

func (p *provider) Interceptor(h http.HandlerFunc) http.HandlerFunc {
	return func(rw http.ResponseWriter, r *http.Request) {
		r.Header.Del(httputil.InternalHeader)
		r.Header.Del(httputil.ClientIDHeader)
		r.Header.Del(httputil.ClientNameHeader)
		localeName := i18n.GetLocaleNameByRequest(r)
		// set global context bind goroutine id
		i18n.SetGoroutineBindLang(localeName)
		// clear all global context
		defer goroutine_context.ClearContext()
		h(rw, r)
	}
}

func init() {
	servicehub.Register("openapi-interceptor-filter-client-header", &servicehub.Spec{
		Services:   []string{"openapi-interceptor-filter-client-header"},
		ConfigFunc: func() interface{} { return &interceptors.Config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
