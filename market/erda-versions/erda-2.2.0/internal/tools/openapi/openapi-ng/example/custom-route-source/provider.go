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

package static

import (
	"net/http"

	"github.com/erda-project/erda-infra/base/servicehub"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
)

type provider struct{}

var _ openapi.RouteSource = (*provider)(nil)

func (p *provider) Name() string { return "route-source-example-custom" }
func (p *provider) RegisterTo(router transhttp.Router) (err error) {
	router.Add("GET", "/api/example/custom-route", func(rw http.ResponseWriter, r *http.Request) {
		rw.Write([]byte("custom route"))
	})
	return nil
}

func init() {
	servicehub.Register("openapi-example-custom-route", &servicehub.Spec{
		Services: []string{"openapi-route-example-custom"},
		Creator:  func() servicehub.Provider { return &provider{} },
	})
}
