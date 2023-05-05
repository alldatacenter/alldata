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

package metricq

import (
	"context"
	"net/http"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/chartmeta"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricmeta"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	queryv1 "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query/v1"
	indexloader "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

// InfluxQL tsql
const InfluxQL = "influxql"

// QueryParams .
type QueryParams struct {
	Scope     string `param:"scope"`
	Aggregate string `param:"aggregate"`
	Format    string `query:"format"`
	Query     string `query:"q"`
	QL        string `query:"ql"`
}

// Queryer .
type Queryer interface {
	query.Queryer

	Client() *elastic.Client
	Indices(metrics []string, clusters []string, start, end int64) []string
	Handle(r *http.Request) interface{}
	MetricNames(langCodes i18n.LanguageCodes, scope, scopeID string) ([]*pb.NameDefine, error)
	MetricMeta(langCodes i18n.LanguageCodes, scope, scopeID string, pb ...string) ([]*pb.MetricMeta, error)
	GetSingleMetricsMeta(langCodes i18n.LanguageCodes, scope, scopeID, metric string) (*pb.MetricMeta, error)
	GetSingleAggregationMeta(langCodes i18n.LanguageCodes, mode, name string) (*pb.Aggregation, error)
	RegeistMetricMeta(scope, scopeID, group string, metrics ...*pb.MetricMeta) error
	UnregeistMetricMeta(scope, scopeID, group string, metrics ...string) error
	MetricGroups(langCodes i18n.LanguageCodes, scope, scopeID, mode string) ([]*pb.Group, error)
	MetricGroup(langCodes i18n.LanguageCodes, scope, scopeID, group, mode, format string, appendTags bool) (*pb.MetricGroup, error)

	// v1
	queryv1.Queryer
	HandleV1(r *http.Request, params *QueryParams) interface{}
	Charts(langCodes i18n.LanguageCodes, typ string) []*chartmeta.ChartMeta
}

// Q Queryer .
var Q Queryer

type Metricq struct {
	query.Queryer
	index   indexloader.Interface
	meta    *metricmeta.Manager
	handler func(r *http.Request) interface{}

	queryv1   queryv1.Queryer
	charts    *chartmeta.Manager
	handlerV1 func(r *http.Request, params *QueryParams) interface{}
}

// Client .
func (q *Metricq) Client() *elastic.Client {
	return q.index.Client()
}

// Indices .
func (q *Metricq) Indices(metrics []string, clusters []string, start, end int64) []string {
	keys := make([]indexloader.KeyPath, len(metrics)+1)
	for i, item := range metrics {
		keys[i] = indexloader.KeyPath{
			Keys:      []string{item},
			Recursive: true,
		}
	}
	keys[len(metrics)] = indexloader.KeyPath{}
	start = start * int64(time.Millisecond)
	end = end*int64(time.Millisecond) + (int64(time.Millisecond) - 1)
	return q.index.Indices(context.Background(), start, end, keys...)
}

// Handle .
func (q *Metricq) Handle(r *http.Request) interface{} {
	return q.handler(r)
}

// MetricMeta .
func (q *Metricq) MetricNames(langCodes i18n.LanguageCodes, scope, scopeID string) ([]*pb.NameDefine, error) {
	return q.meta.MetricNames(langCodes, scope, scopeID)
}

// MetricMeta .
func (q *Metricq) MetricMeta(langCodes i18n.LanguageCodes, scope, scopeID string, metrics ...string) ([]*pb.MetricMeta, error) {
	return q.meta.MetricMeta(langCodes, scope, scopeID, metrics...)
}

func (q *Metricq) GetSingleMetricsMeta(langCodes i18n.LanguageCodes, scope, scopeID, metric string) (*pb.MetricMeta, error) {
	return q.meta.GetSingleMetricsMeta(langCodes, scope, scopeID, metric)
}

func (q *Metricq) GetSingleAggregationMeta(langCodes i18n.LanguageCodes, mode, name string) (*pb.Aggregation, error) {
	return q.meta.GetSingleAggregationMeta(langCodes, mode, name)
}

// RegeistMetricMeta .
func (q *Metricq) RegeistMetricMeta(scope, scopeID, group string, metrics ...*pb.MetricMeta) error {
	return q.meta.RegeistMetricMeta(scope, scopeID, group, metrics...)
}

// UnregeistMetricMeta .
func (q *Metricq) UnregeistMetricMeta(scope, scopeID, group string, metrics ...string) error {
	return q.meta.UnregeistMetricMeta(scope, scopeID, group, metrics...)
}

// MetricGroups .
func (q *Metricq) MetricGroups(langCodes i18n.LanguageCodes, scope, scopeID, mode string) ([]*pb.Group, error) {
	return q.meta.MetricGroups(langCodes, scope, scopeID, mode)
}

// MetricGroup .
func (q *Metricq) MetricGroup(langCodes i18n.LanguageCodes, scope, scopeID, group, mode, format string, appendTags bool) (*pb.MetricGroup, error) {
	return q.meta.MetricGroup(langCodes, scope, scopeID, group, mode, format, appendTags)
}

func (q *Metricq) QueryWithFormatV1(qlang, statement, format string, langCodes i18n.LanguageCodes) (*queryv1.Response, error) {
	return q.queryv1.QueryWithFormatV1(qlang, statement, format, langCodes)
}

// Charts .
func (q *Metricq) Charts(langCodes i18n.LanguageCodes, typ string) []*chartmeta.ChartMeta {
	return q.charts.ChartMetaList(langCodes, typ)
}

// HandleV1 .
func (q *Metricq) HandleV1(r *http.Request, params *QueryParams) interface{} {
	return q.handlerV1(r, params)
}
