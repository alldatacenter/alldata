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

package cleaner

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"strings"
	"time"

	cfgpkg "github.com/recallsong/go-utils/config"
	"github.com/recallsong/go-utils/lang/size"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

type (
	// RetentionStrategy .
	RetentionStrategy interface {
		GetTTL(*loader.IndexEntry) time.Duration
	}
	// RetentionStrategyLoader .
	RetentionStrategyLoader interface {
		Loading(ctx context.Context)
	}
	// Interface .
	Interface interface {
		CleanIndices(ctx context.Context, filter loader.Matcher) error
	}
)

type (
	config struct {
		RequestTimeout time.Duration `file:"request_timeout" default:"1m"`
		CheckInterval  time.Duration `file:"check_interval" default:"1h"`
		PrintOnly      bool          `file:"print_only"`
		DiskClean      diskClean     `file:"disk_clean"`
	}
	diskClean struct {
		Enable bool `file:"enable"`
		TTL    struct {
			Enable            bool   `json:"enable" default:"true"`
			MaxStoreTime      int    `file:"max_store_time" default:"7"`
			TriggerSpecCron   string `file:"trigger_spec_cron" default:"0 0 3 * * *"`
			TaskCheckInterval int64  `json:"task_check_interval" default:"5"`
		}
		CheckInterval          time.Duration `file:"check_interval" default:"5m"`
		MinIndicesStore        string        `file:"min_indices_store" default:"10GB"`
		MinIndicesStorePercent float64       `file:"min_indices_store_percent" default:"10"`
		HighDiskUsagePercent   float64       `file:"high_disk_usage_percent" default:"85"`
		LowDiskUsagePercent    float64       `file:"low_disk_usage_percent" default:"70"`
		RolloverBodyFile       string        `file:"rollover_body_file"`
		RolloverAliasPatterns  []struct {
			Index string `file:"index"`
			Alias string `file:"alias"`
		} `file:"rollover_alias_patterns"`
	}
	indexAliasPattern struct {
		index *index.Pattern
		alias *index.Pattern
	}
	TtlTask struct {
		TaskId  string
		Indices []string
	}
	provider struct {
		Cfg        *config
		Log        logs.Logger
		election   election.Interface
		loader     loader.Interface
		retentions RetentionStrategy

		clearCh chan *clearRequest

		// for disk clean
		minIndicesStoreInDisk    int64
		rolloverBodyForDiskClean string
		rolloverAliasPatterns    []*indexAliasPattern

		ttlTaskCh chan *TtlTask
	}
)

var _ Interface = (*provider)(nil)

func (p *provider) Init(ctx servicehub.Context) error {
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

	if err := p.initRetentionStrategy(ctx); err != nil {
		return err
	}
	if loader, ok := p.retentions.(RetentionStrategyLoader); ok {
		p.election.OnLeader(loader.Loading)
	}

	if int64(p.Cfg.CheckInterval) <= 0 {
		return fmt.Errorf("invalid check_interval: %v", p.Cfg.CheckInterval)
	}
	if !p.loader.QueryIndexTimeRange() {
		p.Log.Warnf("index clean is enable, but QueryIndexTimeRange of elasticsearch.index.loader is disable")
	}
	p.election.OnLeader(p.runCleanIndices)

	if int64(p.Cfg.DiskClean.CheckInterval) <= 0 {
		return fmt.Errorf("invalid disk_clean.check_interval: %v", p.Cfg.DiskClean.CheckInterval)
	}

	// rollover body for disk clean
	if len(p.Cfg.DiskClean.RolloverBodyFile) > 0 {
		body, err := ioutil.ReadFile(p.Cfg.DiskClean.RolloverBodyFile)
		if err != nil {
			return fmt.Errorf("failed to load rollover body file for disk clean: %s", err)
		}
		body = cfgpkg.EscapeEnv(body)
		p.rolloverBodyForDiskClean = string(body)
		if len(p.rolloverBodyForDiskClean) <= 0 {
			return fmt.Errorf("RolloverBody is empty for disk clean")
		}
		var m map[string]interface{}
		err = json.NewDecoder(strings.NewReader(p.rolloverBodyForDiskClean)).Decode(&m)
		if err != nil {
			return fmt.Errorf("invalid RolloverBody for disk clean: %v", string(body))
		}
		p.Log.Info("load rollover body for disk clean: \n", p.rolloverBodyForDiskClean)
	}

	minIndicesStore, err := size.ParseBytes(p.Cfg.DiskClean.MinIndicesStore)
	if err != nil {
		return fmt.Errorf("invalid min_indices_store: %s", err)
	}
	p.minIndicesStoreInDisk = minIndicesStore

	if len(p.Cfg.DiskClean.RolloverAliasPatterns) <= 0 {
		return fmt.Errorf("rollover_alias_patterns are required")
	}
	for i, ptn := range p.Cfg.DiskClean.RolloverAliasPatterns {
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
		p.rolloverAliasPatterns = append(p.rolloverAliasPatterns, &indexAliasPattern{index: ip, alias: ap})
	}

	if p.Cfg.DiskClean.Enable {
		p.election.OnLeader(p.runDiskCheckAndClean)
	}
	p.ttlTaskCh = make(chan *TtlTask, 1)
	p.election.OnLeader(p.runDocsCheckAndClean)
	p.election.OnLeader(p.runTaskCheck)

	// init manager routes
	routePrefix := "/api/elasticsearch/index"
	if len(ctx.Label()) > 0 {
		routePrefix = routePrefix + "/" + ctx.Label()
	} else {
		routePrefix = routePrefix + "/-"
	}
	routes := ctx.Service("http-router", interceptors.CORS()).(httpserver.Router)
	err = p.intRoutes(routes, routePrefix)
	if err != nil {
		return fmt.Errorf("failed to init routes: %s", err)
	}
	return nil
}

func (p *provider) initRetentionStrategy(ctx servicehub.Context) error {
	obj, name := index.FindService(ctx, "elasticsearch.index.retention-strategy")
	if obj == nil {
		return fmt.Errorf("%q is required", name)
	}
	rs, ok := obj.(RetentionStrategy)
	if !ok {
		return fmt.Errorf("%q is not RetentionStrategy", name)
	}
	p.retentions = rs
	p.Log.Debugf("use RetentionStrategy(%q) for index clean", name)
	return nil
}

func init() {
	servicehub.Register("elasticsearch.index.cleaner", &servicehub.Spec{
		Services:     []string{"elasticsearch.index.cleaner"},
		Dependencies: []string{"http-router", "elasticsearch.index.loader", "elasticsearch.index.retention-strategy", "etcd-election"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{
				clearCh: make(chan *clearRequest),
			}
		},
	})
}
