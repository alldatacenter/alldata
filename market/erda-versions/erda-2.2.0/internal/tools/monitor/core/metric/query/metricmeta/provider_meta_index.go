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

package metricmeta

import (
	"context"
	"encoding/json"
	"math"
	"sort"
	"strings"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	indexloader "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

// MetaIndexGroupProvider .
type MetaIndexGroupProvider struct {
	index indexloader.Interface
}

// NewMetaIndexGroupProvider .
func NewMetaIndexGroupProvider(index indexloader.Interface) (*MetaIndexGroupProvider, error) {
	return &MetaIndexGroupProvider{index}, nil
}

// MappingsByID .
func (p *MetaIndexGroupProvider) MappingsByID(id, scope, scopeID string, names []string, ms map[string]*pb.MetricMeta) (gmm []*GroupMetricMap, err error) {
	if id == "all" {
		for _, name := range names {
			if mm, ok := ms[name]; ok {
				gmm = append(gmm, &GroupMetricMap{
					Name: mm.Name.Key,
				})
			}
		}
	}
	return gmm, nil
}

// Groups .
func (p *MetaIndexGroupProvider) Groups(langCodes i18n.LanguageCodes, t i18n.Translator, scope, scopeID string, ms map[string]*pb.MetricMeta) (groups []*pb.Group, err error) {
	groups = append(groups, &pb.Group{
		Id:    "all",
		Name:  t.Text(langCodes, "All Metrics"),
		Order: math.MaxInt32,
	})
	groups = appendMetricToGroup(groups, "@", ms, p.getAllGroupsMetrics(ms), true)
	return groups, nil
}

func (p *MetaIndexGroupProvider) getAllGroupsMetrics(ms map[string]*pb.MetricMeta) map[string][]*GroupMetricMap {
	return p.getDynamicGroupsMetrics("all", ms, func(m *pb.MetricMeta) bool {
		return true
	})
}

func (p *MetaIndexGroupProvider) getDynamicGroupsMetrics(group string, ms map[string]*pb.MetricMeta, match func(m *pb.MetricMeta) bool) map[string][]*GroupMetricMap {
	var gm map[string][]*GroupMetricMap
	for _, m := range ms {
		if m == nil {
			continue
		}
		if match(m) {
			if gm == nil {
				gm = make(map[string][]*GroupMetricMap)
			}
			gmm := &GroupMetricMap{
				Name:   m.Name.Key,
				Fields: metric.FieldsKeys(m),
			}
			gm[group] = append(gm[group], gmm)
		}
	}
	for _, list := range gm {
		if len(list) == 0 {
			continue
		}
		sort.Slice(list, func(i, j int) bool {
			if list[i].Name == list[j].Name {
				return len(list[i].Fields) < len(list[j].Fields)
			}
			return list[i].Name < list[j].Name
		})
	}
	return gm
}

// MetaIndexMetricMetaProvider .
type MetaIndexMetricMetaProvider struct {
	index indexloader.Interface
	log   logs.Logger
}

// NewMetaIndexMetricMetaProvider .
func NewMetaIndexMetricMetaProvider(index indexloader.Interface, log logs.Logger) (*MetaIndexMetricMetaProvider, error) {
	return &MetaIndexMetricMetaProvider{
		index: index,
		log:   log,
	}, nil
}

// MetricMeta .
func (p *MetaIndexMetricMetaProvider) MetricMeta(langCodes i18n.LanguageCodes, i i18n.I18n, scope, scopeID string, names ...string) (map[string]*pb.MetricMeta, error) {
	query := elastic.NewBoolQuery().
		Filter(elastic.NewExistsQuery("fields.fields")).
		Filter(elastic.NewExistsQuery("fields.tags"))
	if scope != "org" && scope != "dice" { // For the moment hard-coded.
		query = query.Filter(elastic.NewTermQuery("tags.metric_scope", scope)).
			Filter(elastic.NewTermQuery("tags.metric_scope_id", scopeID))
	}
	if len(names) > 0 {
		var args []interface{}
		for _, item := range names {
			args = append(args, item)
		}
		query = query.Filter(elastic.NewTermsQuery("tags.metric_name", args...))
	}

	searchSource := elastic.NewSearchSource().
		Query(query).Size(0).
		Aggregation("tags.metric_name", elastic.NewTermsAggregation().Field("tags.metric_name").Size(100000). //  impossible size
															SubAggregation("topHit", elastic.NewTopHitsAggregation().Size(1).Sort("timestamp", false).
																FetchSourceContext(elastic.NewFetchSourceContext(true).Include("*"))))
	end := time.Now().UnixNano()
	start := end - 7*24*int64(time.Hour)

	indices := p.index.Indices(context.Background(), start, end, indexloader.KeyPath{
		Keys:      []string{"_metric_meta"},
		Recursive: true,
	}, indexloader.KeyPath{})
	result, err := p.searchRaw(indices, searchSource)
	if err != nil {
		return nil, err
	}
	metas := make(map[string]*pb.MetricMeta)
	if result == nil || result.Aggregations == nil {
		return metas, nil
	}
	metricNameAgg, ok := result.Aggregations.Terms("tags.metric_name")
	if !ok {
		return metas, nil
	}

	type ESMetricMeta struct {
		Name      string `json:"name"`
		Timestamp int64  `json:"timestamp"`
		Fields    *struct {
			FieldsKeys []string `json:"fields"`
			TagsKeys   []string `json:"tags"`
		} `json:"fields"`
		Tags map[string]string `json:"tags"`
	}
	for _, item := range metricNameAgg.Buckets {
		var data ESMetricMeta
		topHitAgg, _ := item.TopHits("topHit")
		if topHitAgg == nil || topHitAgg.Hits == nil || len(topHitAgg.Hits.Hits) == 0 {
			continue
		}
		hit := topHitAgg.Hits.Hits[0]
		if hit.Source == nil {
			continue
		}
		if err := json.Unmarshal(*hit.Source, &data); err != nil {
			p.log.Warnf("fail to json decode metric meta %s: %s", hit.Source, err)
			continue
		}
		if data.Tags == nil || data.Fields == nil {
			continue
		}
		metricName := data.Tags["metric_name"]
		if len(metricName) <= 0 {
			continue
		}
		labels := make(map[string]string)
		for k, v := range data.Tags {
			labels[k] = v
		}
		meta := metric.NewMeta()
		meta.Name.Key, meta.Name.Name = metricName, metricName
		if len(labels) > 0 {
			meta.Labels = labels
		}
		for _, key := range data.Fields.TagsKeys {
			meta.Tags[key] = &pb.TagDefine{
				Key:  key,
				Name: key,
			}
		}
		for _, key := range data.Fields.FieldsKeys {
			idx := strings.LastIndex(key, ":")
			if idx <= 0 {
				p.log.Warnf("invalid field format in %s: %s", key, metricName)
				continue
			}
			meta.Fields[key[:idx]] = &pb.FieldDefine{
				Key:  key[:idx],
				Type: key[idx+1:],
				Name: key[:idx],
				Unit: "",
			}
		}
		metas[metricName] = meta
	}
	return transMetricMetas(langCodes, i, metas), nil
}

func (p *MetaIndexMetricMetaProvider) searchRaw(indices []string, searchSource *elastic.SearchSource) (*elastic.SearchResult, error) {
	context, cancel := context.WithTimeout(context.Background(), p.index.RequestTimeout())
	defer cancel()
	return p.index.Client().Search(indices...).
		IgnoreUnavailable(true).AllowNoIndices(true).
		SearchSource(searchSource).Do(context)
}
