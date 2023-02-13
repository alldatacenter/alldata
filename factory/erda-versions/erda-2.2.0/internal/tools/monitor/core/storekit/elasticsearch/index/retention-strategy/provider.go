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
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/cleaner"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

// Interface .
type Interface interface {
	GetTTL(key string) time.Duration
	DefaultTTL() time.Duration
	GetConfigKey(name string, tags map[string]string) string
	Loading(ctx context.Context)
}

type (
	config struct {
		KeyPatterns []string `file:"key_patterns"`
	}
	keyPattern struct {
		pattern *index.Pattern
		idx     int
	}
	provider struct {
		Cfg      *config
		Log      logs.Logger
		strategy Interface `autowired:"storage-retention-strategy"`
		patterns []*keyPattern
	}
)

var _ cleaner.RetentionStrategy = (*provider)(nil)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	for _, item := range p.Cfg.KeyPatterns {
		ptn, err := index.BuildPattern(item)
		if err != nil {
			return err
		}
		kp := &keyPattern{
			pattern: ptn,
			idx:     -1,
		}
		for i, key := range ptn.Keys {
			if key == "key" {
				kp.idx = i
				break
			}
		}
		if kp.idx < 0 {
			return fmt.Errorf("not fount <key> in pattern %q", item)
		}
		p.patterns = append(p.patterns, kp)
	}
	if len(p.patterns) <= 0 {
		return fmt.Errorf("patterns is required")
	}
	return p.initStrategy(ctx)
}

func (p *provider) GetTTL(entry *loader.IndexEntry) time.Duration {
	for _, ptn := range p.patterns {
		result, ok := ptn.pattern.Match(entry.Index, index.InvalidPatternValueChars)
		if ok {
			return p.strategy.GetTTL(result.Keys[ptn.idx])
		}
	}
	return p.strategy.DefaultTTL()
}

func (p *provider) Loading(ctx context.Context) {
	p.strategy.Loading(ctx)
}

func (p *provider) initStrategy(ctx servicehub.Context) error {
	obj, name := index.FindService(ctx, "storage-retention-strategy")
	if obj == nil {
		return fmt.Errorf("%q is required", name)
	}
	strategy, ok := obj.(Interface)
	if !ok {
		return fmt.Errorf("%q is not StorageRetentionStrategy", name)
	}
	p.strategy = strategy
	p.Log.Debugf("use StorageRetentionStrategy (%q) for index clean", name)
	return nil
}

func init() {
	servicehub.Register("elasticsearch.index.retention-strategy", &servicehub.Spec{
		Services:     []string{"elasticsearch.index.retention-strategy"},
		Dependencies: []string{"storage-retention-strategy"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
