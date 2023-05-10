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

package token

import (
	"github.com/go-redis/redis"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
	openapiauth "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
	"github.com/erda-project/erda/pkg/oauth2"
)

type config struct {
	Weight int64 `file:"weight" default:"10"`
}

// +provider
type provider struct {
	Cfg          *config
	Log          logs.Logger
	Router       openapi.Interface `autowired:"openapi-router"`
	Redis        *redis.Client     `autowired:"redis-client"`
	oauth2server *oauth2.OAuth2Server
	TokenService tokenpb.TokenServiceServer `autowired:"erda.core.token.TokenService"`
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.oauth2server = oauth2.NewOAuth2Server()

	router := p.Router
	router.Add("", "/oauth2/token", ForwardAuthToken)
	router.Add("", "/oauth2/invalidate_token", ForwardInvalidateToken)
	router.Add("", "/oauth2/validate_token", ForwardValidateToken)
	return nil
}

var _ openapiauth.AutherLister = (*provider)(nil)

func (p *provider) Authers() []openapiauth.Auther {
	return []openapiauth.Auther{p}
}

func init() {
	servicehub.Register("openapi-auth-token", &servicehub.Spec{
		Services:   []string{"openapi-auth-token"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
