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

// Package endpoints API相关的数据信息
package endpoints

import (
	"context"
	"net/http"

	"github.com/gorilla/schema"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/service/adaptor"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// Endpoints 定义 endpoint 方法
type Endpoints struct {
	bdl                *bundle.Bundle
	ucAdaptor          *adaptor.Adaptor
	queryStringDecoder *schema.Decoder
}

// Option 定义Endpoints的func类型
type Option func(*Endpoints)

// New 创建 Endpoints 对象.
func New(options ...Option) *Endpoints {
	e := &Endpoints{}

	for _, op := range options {
		op(e)
	}

	return e
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(e *Endpoints) {
		e.bdl = bdl
	}
}

func WithQueryStringDecoder(decoder *schema.Decoder) Option {
	return func(e *Endpoints) {
		e.queryStringDecoder = decoder
	}
}

// WithUcAdaptor 配置数据库
func WithUcAdaptor(ucAdaptor *adaptor.Adaptor) Option {
	return func(e *Endpoints) {
		e.ucAdaptor = ucAdaptor
	}
}

// Routes 返回 endpoints 的所有 endpoint 方法，也就是 route.
func (e *Endpoints) Routes() []httpserver.Endpoint {
	return []httpserver.Endpoint{
		{Path: "/healthy", Method: http.MethodGet, Handler: e.Info},
	}
}

var queryStringDecoder *schema.Decoder

func init() {
	queryStringDecoder = schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
}

// ListSyncRecord 查看uc同步历史记录
func (e *Endpoints) ListSyncRecord(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	records, err := e.ucAdaptor.ListSyncRecord()
	if err != nil {
		return nil, errors.Errorf("list uc sync record err: %v", err)
	}

	return httpserver.OkResp(records)
}
