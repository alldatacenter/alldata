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

package unrecover_alerts

import (
	"context"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
)

type config struct {
	Interval              time.Duration `file:"interval" default:"5m"`
	MetricReportBatchSize int           `file:"metric_report_batch_size" default:"200"`
}

type provider struct {
	Cfg      *config
	Log      logs.Logger
	DB       *gorm.DB            `autowired:"mysql-client"`
	Election election.Interface  `autowired:"etcd-election@alert-event-metrics"`
	Report   report.MetricReport `autowired:"metric-report-client" `

	alertEventDB *db.AlertEventDB
	alertDB      *db.AlertDB
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.alertEventDB = &db.AlertEventDB{DB: p.DB}
	p.alertDB = &db.AlertDB{DB: p.DB}
	p.Election.OnLeader(func(ctx context.Context) {
		timer := time.NewTicker(p.Cfg.Interval)
		defer timer.Stop()
		for {
			select {
			case <-timer.C:
				p.statisticAlertEvents(ctx)
			case <-ctx.Done():
				return
			}
		}
	})
	return nil
}

func (p *provider) statisticAlertEvents(ctx context.Context) {
	result, err := p.alertEventDB.CountUnRecoverEventsGroupByScope()
	if err != nil {
		p.Log.Warnf("failed to do unRecover alert events statistics: %s", err)
		return
	}

	availableAlertIds, err := p.alertDB.GetAllAvailableAlertIds()
	if err != nil {
		p.Log.Warnf("failed to get all disabled alert ids: %s", err)
		return
	}
	result = p.kickOutDisabledAlertsAndRollupByScopeId(result, availableAlertIds)

	metrics := p.convertToMetrics(p.Cfg.MetricReportBatchSize, result...)
	for _, metric := range metrics {
		err = p.Report.Send(metric)
		if err != nil {
			p.Log.Warnf("failed to report metrics for alert event statistics")
		}
	}
}

func (p *provider) kickOutDisabledAlertsAndRollupByScopeId(stats []*db.AlertEventScopeCountResult, availableAlertIds []uint64) []*db.AlertEventScopeCountResult {
	idMap := map[uint64]bool{}
	for _, id := range availableAlertIds {
		idMap[id] = true
	}

	scopeLevelStats := map[string]*db.AlertEventScopeCountResult{}
	for _, stat := range stats {
		if _, ok := idMap[stat.AlertId]; !ok {
			continue
		}

		scopeStat, ok := scopeLevelStats[stat.ScopeId]
		if !ok {
			scopeLevelStats[stat.ScopeId] = stat
			continue
		}

		scopeStat.Count += stat.Count
	}

	var list []*db.AlertEventScopeCountResult
	for _, result := range scopeLevelStats {
		list = append(list, result)
	}
	return list
}

func (p *provider) convertToMetrics(batchSize int, stats ...*db.AlertEventScopeCountResult) [][]*report.Metric {
	var groups [][]*report.Metric
	var metrics []*report.Metric
	for _, item := range stats {
		metrics = append(metrics, &report.Metric{
			Name:      "alert-event-unrecover",
			Timestamp: time.Now().UnixNano(),
			Tags: map[string]string{
				"alert_scope":    item.Scope,
				"alert_scope_id": item.ScopeId,
				"org_id":         strconv.FormatInt(item.OrgID, 10),
			},
			Fields: map[string]interface{}{
				"unrecover_count": item.Count,
			},
		})
		if len(metrics)%batchSize == 0 {
			groups = append(groups, metrics)
			metrics = []*report.Metric{}
		}
	}
	if len(metrics) > 0 {
		groups = append(groups, metrics)
	}
	return groups
}

func init() {
	servicehub.Register("erda.core.monitor.alert.jobs.unrecover-alerts", &servicehub.Spec{
		Services:     []string{"erda.core.monitor.alert.jobs.unrecover-alerts"},
		Dependencies: []string{"etcd-election"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
