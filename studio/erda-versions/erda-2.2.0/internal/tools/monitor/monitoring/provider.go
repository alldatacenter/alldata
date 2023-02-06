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

package monitoring

import (
	"context"
	"time"

	"github.com/prometheus/client_golang/prometheus"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
)

type config struct {
	UsageSyncInterval syncInterval `file:"usage_sync_interval"`
}

type syncInterval struct {
	Metric time.Duration `file:"metric"`
	Log    time.Duration `file:"log"`
}

// +provider
type provider struct {
	Cfg     *config
	Log     logs.Logger
	metricq metricq.Queryer
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	p.metricq = ctx.Service("metrics-query").(metricq.Queryer)
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	if p.Cfg.UsageSyncInterval.Metric > 0 {
		p.Log.Info("metric storage stats sync start")
		go p.syncStorage(ctx, newEsStorageMetric(p.metricq), metricStorageUsage, p.Cfg.UsageSyncInterval.Metric)
	}

	if p.Cfg.UsageSyncInterval.Log > 0 {
		// go p.syncStorage(newCassandraStorageLog(p.metricq), logStorageUsage, p.Cfg.UsageSyncInterval.Log)
	}
	return nil
}

// disk usage summary of different storage interface
type storageMetric interface {
	UsageSummaryOrg() (map[string]uint64, error)
}

func (p *provider) syncStorage(ctx context.Context, sm storageMetric, gauge *prometheus.GaugeVec, interval time.Duration) {
	prometheus.MustRegister(gauge)
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	for {
		data, err := sm.UsageSummaryOrg()
		if err != nil {
			p.Log.Errorf("sync %T storage usage failed: %s", sm, err)
		}
		for k, v := range data {
			if v > 0 {
				gauge.WithLabelValues(k).Set(float64(v))
			}
		}
		p.Log.Debugf("data of %T: %+v", sm, data)
		select {
		case <-ticker.C:
		case <-ctx.Done():
			return
		}
	}
}

func init() {
	servicehub.Register("monitor-monitoring", &servicehub.Spec{
		Services: []string{
			"monitor-monitoring-service",
		},
		Description:  "centralize some metrics of component monitor",
		Dependencies: []string{"metrics-query"},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
