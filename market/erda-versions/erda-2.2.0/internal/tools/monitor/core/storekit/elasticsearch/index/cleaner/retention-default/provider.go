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

package retention

import (
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

type (
	// RetentionStrategy .
	RetentionStrategy interface {
		GetTTL(*loader.IndexEntry) time.Duration
	}
	// FixedRetentionStrategy .
	FixedRetentionStrategy struct {
		TTL time.Duration
	}
)

func (r *FixedRetentionStrategy) GetTTL(*loader.IndexEntry) time.Duration { return r.TTL }

// DefaultTTL .
const DefaultTTL = 7 * 24 * time.Hour

// DefaultRetentionStrategy .
var DefaultRetentionStrategy RetentionStrategy = &FixedRetentionStrategy{
	TTL: DefaultTTL,
}

type (
	config struct {
		DefaultTTL time.Duration `file:"default_ttl"`
	}
	provider struct {
		Cfg       *config
		Log       logs.Logger
		retention RetentionStrategy
	}
)

func (p *provider) Init(ctx servicehub.Context) error {
	if p.Cfg.DefaultTTL <= 0 {
		p.Cfg.DefaultTTL = DefaultTTL
	}
	p.retention = &FixedRetentionStrategy{p.Cfg.DefaultTTL}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p.retention
}

func init() {
	servicehub.Register("elasticsearch.index.retention-strategy-default", &servicehub.Spec{
		Services:   []string{"elasticsearch.index.retention-strategy"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
