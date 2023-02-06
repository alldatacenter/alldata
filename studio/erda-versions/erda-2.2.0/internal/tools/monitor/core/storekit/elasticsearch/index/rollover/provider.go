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

package rollover

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"sync"
	"time"

	cfgpkg "github.com/recallsong/go-utils/config"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

// Interface .
type Interface interface {
	RolloverIndices(ctx context.Context, filter loader.Matcher) error
}

type (
	config struct {
		RequestTimeout time.Duration `file:"request_timeout" default:"2m"`
		CheckInterval  time.Duration `file:"check_interval"`
		BodyFile       string        `file:"body_file"`
		Patterns       []struct {
			Index string `file:"index"`
			Alias string `file:"alias"`
		} `file:"patterns"`
		Verbose bool `file:"verbose"`
	}
	indexAliasPattern struct {
		index *index.Pattern
		alias *index.Pattern
	}
	provider struct {
		Cfg *config
		Log logs.Logger

		patterns     []*indexAliasPattern
		election     election.Interface
		loader       loader.Interface
		rolloverBody string
		created      map[string]bool
		createdLock  sync.Mutex
	}
)

var _ Interface = (*provider)(nil)

func (p *provider) Init(ctx servicehub.Context) error {
	if p.Cfg.CheckInterval <= 0 {
		return fmt.Errorf("invalid check_interval: %v", p.Cfg.CheckInterval)
	}
	if err := p.loadRolloverBody(); err != nil {
		return err
	}

	// build index pattern
	if len(p.Cfg.Patterns) <= 0 {
		return fmt.Errorf("patterns are required")
	}
	for i, ptn := range p.Cfg.Patterns {
		if len(ptn.Index) <= 0 || len(ptn.Alias) <= 0 {
			return fmt.Errorf("pattern(%d) index and alias is required", i)
		}
		ip, err := index.BuildPattern(ptn.Index)
		if err != nil {
			return err
		}
		ap, err := index.BuildPattern(ptn.Alias)
		if err != nil {
			return err
		}
		if ap.VarNum > 0 {
			return fmt.Errorf("pattern(%d) can't contains vars", i)
		}
		p.patterns = append(p.patterns, &indexAliasPattern{index: ip, alias: ap})
	}

	loader, err := loader.Find(ctx, p.Log, true)
	if err != nil {
		return err
	}
	p.loader = loader

	election, err := index.FindElection(ctx, true)
	if err != nil {
		return err
	}
	p.election = election
	election.OnLeader(p.runIndexRollover)

	// init manager routes
	routeRrefix := "/api/elasticsearch/index"
	if len(ctx.Label()) > 0 {
		routeRrefix = routeRrefix + "/" + ctx.Label()
	} else {
		routeRrefix = routeRrefix + "/-"
	}
	routes := ctx.Service("http-router", interceptors.CORS()).(httpserver.Router)
	err = p.intRoutes(routes, routeRrefix)
	if err != nil {
		return fmt.Errorf("failed to init routes: %s", err)
	}
	return nil
}

func (p *provider) loadRolloverBody() error {
	body, err := ioutil.ReadFile(p.Cfg.BodyFile)
	if err != nil {
		return fmt.Errorf("failed to load rollover body file: %s", err)
	}
	body = cfgpkg.EscapeEnv(body)
	var m map[string]interface{}
	err = json.Unmarshal(body, &m)
	if err != nil {
		return fmt.Errorf("rollover body %q is not is not json format", p.Cfg.BodyFile)
	}
	p.rolloverBody = string(body)
	p.Log.Info("load rollover body: \n", p.rolloverBody)
	return nil
}

func init() {
	servicehub.Register("elasticsearch.index.rollover", &servicehub.Spec{
		Services:             []string{"elasticsearch.index.rollover"},
		Dependencies:         []string{"http-router", "elasticsearch.index.loader", "etcd-election"},
		OptionalDependencies: []string{"elasticsearch.index.initializer"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
