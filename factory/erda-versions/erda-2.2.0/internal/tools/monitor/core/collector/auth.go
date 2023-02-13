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

package collector

import (
	"strings"
	"sync"

	"github.com/labstack/echo"
	"github.com/labstack/echo/middleware"

	"github.com/erda-project/erda-infra/base/logs"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/authentication"
)

type Option func(authenticator *Authenticator)

type Authenticator struct {
	mu        sync.RWMutex
	logger    logs.Logger
	config    *config
	validator authentication.Validator
}

func NewAuthenticator(opts ...Option) *Authenticator {
	a := &Authenticator{}

	for _, opt := range opts {
		opt(a)
	}

	return a
}

// WithLogger with Logger
func WithLogger(l logs.Logger) Option {
	return func(a *Authenticator) {
		a.logger = l
	}
}

// WithValidator with validator
func WithValidator(v authentication.Validator) Option {
	return func(a *Authenticator) {
		a.validator = v
	}
}

// WithConfig with config
func WithConfig(cfg *config) Option {
	return func(a *Authenticator) {
		a.config = cfg
	}
}

// basicAuth basic auth
func (a *Authenticator) basicAuth() interface{} {
	return middleware.BasicAuthWithConfig(middleware.BasicAuthConfig{
		Validator: func(username string, password string, context echo.Context) (bool, error) {
			if username == a.config.Auth.Username && password == a.config.Auth.Password {
				return true, nil
			}
			return false, nil
		},
		Skipper: func(context echo.Context) bool {
			if a.config.Auth.Skip {
				return true
			}

			if a.config.Auth.Force {
				return false
			}
			// 兼容旧版本，没有添加认证的客户端，这个版本先跳过
			authorizationHeader := context.Request().Header.Get("Authorization")
			return authorizationHeader == ""
		},
	})
}

// keyAuth key auth
func (a *Authenticator) keyAuth() interface{} {
	return middleware.KeyAuthWithConfig(middleware.KeyAuthConfig{
		Validator: func(s string, context echo.Context) (bool, error) {
			clusterName := context.Request().Header.Get(apistructs.AuthClusterKeyHeader)
			if clusterName == "" {
				return false, nil
			}

			if a.validator.Validate(strings.ToLower(tokenpb.ScopeEnum_CMP_CLUSTER.String()), clusterName, s) {
				return true, nil
			}

			return false, nil
		},
		Skipper: func(context echo.Context) bool {
			if a.config.Auth.Skip {
				return true
			}
			return false
		},
	})

}
