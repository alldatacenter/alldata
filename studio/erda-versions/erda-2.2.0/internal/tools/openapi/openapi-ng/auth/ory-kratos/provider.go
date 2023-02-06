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
	"net/http"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
	openapiauth "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
)

type config struct {
	Weight        int64  `file:"weight" default:"100"`
	OryKratosAddr string `file:"ory_kratos_addr"`
}

// +provider
type provider struct {
	Cfg    *config
	Log    logs.Logger
	Router openapi.Interface `autowired:"openapi-router"`
	bundle *bundle.Bundle
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.bundle = bundle.New(bundle.WithCoreServices(), bundle.WithDOP())

	router := p.Router
	router.Add(http.MethodGet, "/api/openapi/login", p.LoginURL)
	router.Add(http.MethodPost, "/api/openapi/logout", p.Logout)
	router.Add(http.MethodPost, "logout", p.Logout)
	p.addUserInfoAPI(router)
	return nil
}

var _ openapiauth.AutherLister = (*provider)(nil)

func (p *provider) Authers() []openapiauth.Auther {
	return []openapiauth.Auther{
		&loginChecker{p: p},
		&tryLoginChecker{p: p},
	}
}

func init() {
	servicehub.Register("openapi-auth-ory-kratos", &servicehub.Spec{
		Services:   []string{"openapi-auth-session"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
