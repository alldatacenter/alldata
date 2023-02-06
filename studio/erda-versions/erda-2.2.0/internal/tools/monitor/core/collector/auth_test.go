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
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/labstack/echo"
	"github.com/stretchr/testify/assert"
)

////go:generate mockgen -destination=./collector_validator_test.go -package collector github.com/erda-project/erda/internal/tools/monitor/oap/collector/authentication Validator
func Test_Auth(t *testing.T) {
	// Init collector validator
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	validator := NewMockValidator(ctrl)
	validator.EXPECT().Validate(gomock.Any(), gomock.Any(), gomock.Any()).AnyTimes().Return(true)

	validatorFailed := NewMockValidator(ctrl)
	validatorFailed.EXPECT().Validate(gomock.Any(), gomock.Any(), gomock.Any()).AnyTimes().Return(false)

	// Init httptest
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	res := httptest.NewRecorder()
	c := e.NewContext(req, res)

	cfg := &config{
		Auth: struct {
			Username string `file:"username"`
			Password string `file:"password"`
			Force    bool   `file:"force"`
			Skip     bool   `file:"skip"`
		}{
			Username: "test",
			Password: "test",
		},
	}

	// Init auth
	auth := NewAuthenticator(
		WithLogger(nil),
		WithConfig(cfg),
		WithValidator(validator),
	)

	// test skip auth
	err := auth.basicAuth().(echo.MiddlewareFunc)(func(c echo.Context) error {
		return c.String(http.StatusOK, "test")
	})(c)

	assert.NoError(t, err)

	// 401 auth failed
	req.Header.Set("Authorization", "Basic dGVzdDp0ZXN0Cg==")
	err = auth.basicAuth().(echo.MiddlewareFunc)(func(c echo.Context) error {
		return c.String(http.StatusOK, "test")
	})(c)

	assert.Equal(t, err, echo.ErrUnauthorized)

	// basic auth success
	req.Header.Set("Authorization", "Basic dGVzdDp0ZXN0")
	err = auth.basicAuth().(echo.MiddlewareFunc)(func(c echo.Context) error {
		return c.String(http.StatusOK, "test")
	})(c)

	assert.Equal(t, err, nil)

	// 401 auth failed, doesn't have erda cluster key
	req.Header.Set("Authorization", "Bearer dGVzdDp0ZXN0")
	err = auth.keyAuth().(echo.MiddlewareFunc)(func(c echo.Context) error {
		return c.String(http.StatusOK, "test")
	})(c)

	assert.Equal(t, err, echo.ErrUnauthorized)

	// key auth success
	req.Header.Set("X-Erda-Cluster-Key", "test")
	err = auth.keyAuth().(echo.MiddlewareFunc)(func(c echo.Context) error {
		return c.String(http.StatusOK, "test")
	})(c)

	assert.Equal(t, err, nil)

	// auth object with skip
	cfg.Auth.Skip = true
	authSkip := NewAuthenticator(
		WithLogger(nil),
		WithConfig(cfg),
		WithValidator(validatorFailed),
	)

	// skip auth testing
	req.Header.Set("Authorization", "Bearer errorToken")
	err = authSkip.keyAuth().(echo.MiddlewareFunc)(func(c echo.Context) error {
		return c.String(http.StatusOK, "test")
	})(c)

	assert.Equal(t, err, nil)
}
