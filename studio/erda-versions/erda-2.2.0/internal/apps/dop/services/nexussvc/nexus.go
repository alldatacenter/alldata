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

package nexussvc

import (
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/crypto/encryption"
)

// NexusSvc nexus 操作封装
type NexusSvc struct {
	db  *dao.DBClient
	bdl *bundle.Bundle

	rsaCrypt *encryption.RsaCrypt

	cms pb.CmsServiceServer
}

type Option func(*NexusSvc)

func New(options ...Option) *NexusSvc {
	svc := &NexusSvc{}
	for _, op := range options {
		op(svc)
	}

	return svc
}

func WithDBClient(db *dao.DBClient) Option {
	return func(svc *NexusSvc) {
		svc.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(svc *NexusSvc) {
		svc.bdl = bdl
	}
}

func WithRsaCrypt(rsaCrypt *encryption.RsaCrypt) Option {
	return func(svc *NexusSvc) {
		svc.rsaCrypt = rsaCrypt
	}
}

func WithPipelineCms(cms pb.CmsServiceServer) Option {
	return func(svc *NexusSvc) {
		svc.cms = cms
	}
}
