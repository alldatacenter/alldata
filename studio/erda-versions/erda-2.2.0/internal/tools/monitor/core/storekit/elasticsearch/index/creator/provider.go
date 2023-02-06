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

package creator

import (
	"fmt"
	"sync"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

// Interface .
type Interface interface {
	Ensure(keys ...string) (<-chan error, string)
	FixedIndex(keys ...string) (string, error)
}

type (
	config struct {
		RequestTimeout time.Duration `file:"request_timeout" default:"2m"`
		Patterns       []struct {
			FirstIndex string `file:"first_index"`
			Alias      string `file:"alias"`
		} `file:"patterns"`
		FixedPatterns            []string `file:"fixed_patterns"`
		RemoveConflictingIndices bool     `file:"remove_conflicting_indices" default:"false"`
	}
	indexAliasPattern struct {
		index *index.Pattern
		alias *index.Pattern
	}
	request struct {
		Index string
		Alias string
		Wait  chan error
	}
	provider struct {
		Cfg           *config
		Log           logs.Logger
		createCh      chan request
		patterns      []*indexAliasPattern
		fixedPatterns []*index.Pattern

		loader      loader.Interface
		created     map[string]bool
		createdLock sync.Mutex
	}
)

var _ Interface = (*provider)(nil)

func (p *provider) Init(ctx servicehub.Context) error {

	if len(p.Cfg.Patterns) <= 0 {
		return fmt.Errorf("patterns are required")
	}
	lengths := make(map[int]bool)
	for i, ptn := range p.Cfg.Patterns {
		if len(ptn.FirstIndex) <= 0 || len(ptn.Alias) <= 0 {
			return fmt.Errorf("pattern(%d) first_index and alias is required", i)
		}
		ip, err := index.BuildPattern(ptn.FirstIndex)
		if err != nil {
			return err
		}
		ap, err := index.BuildPattern(ptn.Alias)
		if err != nil {
			return err
		}
		if ip.VarNum > 0 {
			return fmt.Errorf("pattern(%q) can't contains vars", ip.Pattern)
		}
		if ap.VarNum > 0 {
			return fmt.Errorf("pattern(%q) can't contains vars", ap.Pattern)
		}
		if lengths[ap.KeyNum] {
			return fmt.Errorf("pattern(%q) keys length conflict", ap.Pattern)
		}
		lengths[ap.KeyNum] = true
		p.patterns = append(p.patterns, &indexAliasPattern{index: ip, alias: ap})
	}

	lengths = make(map[int]bool)
	for i, item := range p.Cfg.FixedPatterns {
		if len(item) <= 0 {
			return fmt.Errorf("pattern(%d) not allowed empty", i)
		}
		ptn, err := index.BuildPattern(item)
		if err != nil {
			return err
		}
		if ptn.VarNum > 0 {
			return fmt.Errorf("pattern(%q) can't contains vars", ptn.Pattern)
		}
		if lengths[ptn.KeyNum] {
			return fmt.Errorf("pattern(%q) keys length conflict", ptn.Pattern)
		}
		lengths[ptn.KeyNum] = true
		p.fixedPatterns = append(p.fixedPatterns, ptn)
	}

	loader, err := loader.Find(ctx, p.Log, true)
	if err != nil {
		return err
	}
	p.loader = loader
	p.loader.WatchLoadEvent(p.onIndicesReloaded)
	if p.Cfg.RemoveConflictingIndices {
		if err := p.removeConflictingIndices(ctx); err != nil {
			return err
		}
	}
	return nil
}

func (p *provider) onIndicesReloaded(indices *loader.IndexGroup) {
	p.createdLock.Lock()
	if len(p.created) > 0 {
		p.created = make(map[string]bool)
	}
	p.createdLock.Unlock()
}

func init() {
	servicehub.Register("elasticsearch.index.creator", &servicehub.Spec{
		Services:             []string{"elasticsearch.index.creator"},
		Dependencies:         []string{"http-router", "elasticsearch.index.loader"},
		OptionalDependencies: []string{"elasticsearch.index.initializer"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{
				createCh: make(chan request),
				created:  make(map[string]bool),
			}
		},
	})
}
