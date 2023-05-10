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

package org

import (
	"github.com/go-redis/redis"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/nexussvc"
	"github.com/erda-project/erda/internal/apps/dop/services/publisher"
	"github.com/erda-project/erda/pkg/ucauth"
)

type Org struct {
	db        *dao.DBClient
	uc        *ucauth.UCClient
	bdl       *bundle.Bundle
	publisher *publisher.Publisher
	nexusSvc  *nexussvc.NexusSvc
	redisCli  *redis.Client
	cmp       dashboardPb.ClusterResourceServer
	trans     i18n.Translator
}

// Option 定义 Org 对象的配置选项
type Option func(*Org)

// New 新建 Org 实例，通过 Org 实例操作企业资源
func New(options ...Option) *Org {
	o := &Org{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(dbClient *dao.DBClient) Option {
	return func(o *Org) {
		o.db = dbClient
	}
}

// WithUCClient 配置 uc client
func WithUCClient(ucClient *ucauth.UCClient) Option {
	return func(o *Org) {
		o.uc = ucClient
	}
}

// WithBundle 配置 bundle
func WithBundle(bundle *bundle.Bundle) Option {
	return func(o *Org) {
		o.bdl = bundle
	}
}

// WithPublisher 配置 publisher
func WithPublisher(pub *publisher.Publisher) Option {
	return func(o *Org) {
		o.publisher = pub
	}
}

// WithNexusSvc 配置 nexus service
func WithNexusSvc(nexusSvc *nexussvc.NexusSvc) Option {
	return func(o *Org) {
		o.nexusSvc = nexusSvc
	}
}

// WithRedisClient 配置 redis client
func WithRedisClient(redisClient *redis.Client) Option {
	return func(o *Org) {
		o.redisCli = redisClient
	}
}

// WithCMP sets the gRPC client to invoke CMP service
// Todo: the dependency on CMP will be moved to a service which is more suitable
func WithCMP(clusterResourceServer dashboardPb.ClusterResourceServer) Option {
	return func(org *Org) {
		org.cmp = clusterResourceServer
	}
}

// WithTrans sets the i18n.Translator
func WithTrans(translator i18n.Translator) Option {
	return func(org *Org) {
		org.trans = translator
	}
}

func (o *Org) GetPublisherID(orgID int64) int64 {
	pub, err := o.db.GetPublisherByOrgID(orgID)
	if err != nil && err != dao.ErrNotFoundPublisher {
		logrus.Warning(err)
		return 0
	}
	if pub == nil {
		return 0
	}
	return int64(pub.ID)
}
