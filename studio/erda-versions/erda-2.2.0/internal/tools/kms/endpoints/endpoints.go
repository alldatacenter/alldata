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

// Package endpoints defines all route handlers.
package endpoints

import (
	"net/http"

	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/kms"
)

// Endpoints 定义 endpoint 方法
type Endpoints struct {
	KmsMgr *kms.Manager
}

type Option func(*Endpoints)

// New 创建 Endpoints 对象.
func New(options ...Option) *Endpoints {
	e := &Endpoints{}

	for _, op := range options {
		op(e)
	}

	return e
}

func WithKmsManager(mgr *kms.Manager) Option {
	return func(e *Endpoints) {
		e.KmsMgr = mgr
	}
}

// Routes 返回 endpoints 的所有 endpoint 方法，也就是 route.
func (e *Endpoints) Routes() []httpserver.Endpoint {
	return []httpserver.Endpoint{
		{Path: "/health", Method: http.MethodGet, Handler: e.Health},

		// kms
		{Path: "/api/kms", Method: http.MethodPost, Handler: e.KmsCreateKey},
		{Path: "/api/kms/encrypt", Method: http.MethodPost, Handler: e.KmsEncrypt},
		{Path: "/api/kms/decrypt", Method: http.MethodPost, Handler: e.KmsDecrypt},
		{Path: "/api/kms/generate-data-key", Method: http.MethodPost, Handler: e.KmsGenerateDataKey},
		{Path: "/api/kms/rotate-key-version", Method: http.MethodPost, Handler: e.KmsRotateKeyVersion},
		{Path: "/api/kms/describe-key", Method: http.MethodGet, Handler: e.KmsDescribeKey},
	}
}
