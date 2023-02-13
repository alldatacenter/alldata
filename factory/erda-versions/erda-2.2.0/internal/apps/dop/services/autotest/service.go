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

package autotest

import (
	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

// Service autotest 实例对象封装
type Service struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
	cms cmspb.CmsServiceServer
}

// New 新建 autotest service
func New(options ...Option) *Service {
	var svc Service
	for _, op := range options {
		op(&svc)
	}
	return &svc
}

// Option autotest 实例对象配置选项
type Option func(*Service)

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(svc *Service) {
		svc.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(svc *Service) {
		svc.bdl = bdl
	}
}

func WithPipelineCms(cms cmspb.CmsServiceServer) Option {
	return func(svc *Service) {
		svc.cms = cms
	}
}
