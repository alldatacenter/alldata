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

package persist

import (
	"context"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/storage"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

// MetadataProcessor .
type MetadataProcessor interface {
	Process(m *metric.Metric) error
}

type nopMetadataProcessor struct{}

func (*nopMetadataProcessor) Process(m *metric.Metric) error { return nil }

// NopMetadataProcessor .
var NopMetadataProcessor MetadataProcessor = &nopMetadataProcessor{}

func newMetadataProcessor(cfg *config, p *provider) MetadataProcessor {
	return &metadataProcessor{
		metrics:      make(chan *metric.Metric, 128),
		metadataName: "_metric_meta",
		cache:        make(map[string]*metadataCacheItem),
		maxWait:      10 * time.Second,
		cacheExpire:  15 * time.Minute,
		stats:        p.stats,
		log:          p.Log,
		storage:      p.StorageWriter,
	}
}

type (
	metadataCacheItem struct {
		lastTime time.Time
		data     *metric.Metric
	}
	metadataProcessor struct {
		metrics      chan *metric.Metric
		cache        map[string]*metadataCacheItem
		maxWait      time.Duration
		cacheExpire  time.Duration
		stats        Statistics
		log          logs.Logger
		storage      storage.Storage
		metadataName string
	}
)

func (p *metadataProcessor) Process(m *metric.Metric) error {
	if m.Name == MetricMeta {
		return nil
	}
	if metaVal, ok := m.Tags["_meta"]; ok {
		if b, err := strconv.ParseBool(metaVal); err == nil && b {
			p.metrics <- convertToMetadata(p.metadataName, m)
			// select {
			// case p.metrics <- convertToMetadata(p.metadataName, m):
			// default:
			// 	return nil
			// }
		}
	}
	return nil
}

func (p *metadataProcessor) Run(ctx context.Context) error {
	w, err := p.storage.NewWriter(ctx)
	if err != nil {
		return err
	}
	defer w.Close()
	const bufSize = 50
	buf := make([]interface{}, 0, bufSize+1)
	timer := time.NewTimer(p.maxWait)
	defer timer.Stop()

	for {

	readMany:
		for {
			select {
			case <-ctx.Done():
				return nil
			case m := <-p.metrics:
				if p.update(m) {
					buf = append(buf, m)
					if len(buf) >= bufSize {
						break readMany
					}
				}
			case <-timer.C:
				break readMany
			}
		}

		if len(buf) > 0 {
			p.stats.MetadataUpdates(len(buf))
			_, err := w.WriteN(buf...)
			if err != nil {
				for _, data := range buf {
					p.stats.MetadataError(data.(*metric.Metric), err)
					p.log.Errorf("failed to process metric metadata: %v", err)
				}
			}
			buf = buf[0:0]
		}
		timer.Reset(p.maxWait)
	}
}

func (p *metadataProcessor) update(m *metric.Metric) bool {
	id := m.Tags["_id"]
	cacheItem, ok := p.cache[id]
	if !ok {
		cacheItem = &metadataCacheItem{
			data:     m,
			lastTime: time.Now().Add(p.cacheExpire),
		}
		p.cache[id] = cacheItem
		return true
	} else {
		m.Fields["tags"] = distinct(m.Fields["tags"].([]string), cacheItem.data.Fields["tags"].([]string))
		m.Fields["fields"] = distinct(m.Fields["fields"].([]string), cacheItem.data.Fields["fields"].([]string))
		cacheItem.data = m
		now := time.Now()
		if cacheItem.lastTime.Before(now) {
			cacheItem.lastTime = now
			return true
		}
	}
	return false
}

func convertToMetadata(name string, m *metric.Metric) *metric.Metric {
	// {
	// 	"name":"xxx",
	// 	"tags":{
	// 		"_meta": "true"
	// 	}
	// }

	// after convert
	// {
	// 	"name":"_metric_meta",
	// 	"tags":{
	// 		"metric_name": "application_http"
	// 	},
	// 	"fields": {
	// 		"tags": [
	// 			"service_name",
	// 			"terminus_key"
	// 		],
	// 		"fields":[
	// 			"count:number"
	// 		]
	// 	}
	// }

	fields := make([]string, 0)
	for field, val := range m.Fields {
		fields = append(fields, field+":"+utils.TypeOf(val))
	}
	tags := make([]string, 0)
	for tag := range m.Tags {
		// filter tags with built-in _ prefixes, such as _id _meta.
		if !strings.HasPrefix(tag, "_") {
			tags = append(tags, tag)
		}
	}
	metaTags := map[string]string{}
	for tag, val := range m.Tags {
		// add tags with built-in _ prefixes, such as _id _meta.
		if strings.HasPrefix(tag, "_") {
			metaTags[tag[1:]] = val
		}
	}
	metaTags["metric_name"] = m.Name
	metaTags["_id"] = m.Name + "-" + m.Tags["_metric_scope"] + "-" + m.Tags["_metric_scope_id"]
	metaTags["org_name"] = m.Tags["org_name"]
	metaTags["cluster_name"] = m.Tags["cluster_name"]
	return &metric.Metric{
		Name:      name,
		Timestamp: m.Timestamp,
		Tags:      metaTags,
		Fields: map[string]interface{}{
			"tags":   tags,
			"fields": fields,
		},
	}
}

func distinct(list ...[]string) (result []string) {
	set := make(map[string]bool)
	for _, items := range list {
		for _, item := range items {
			if !set[item] {
				result = append(result, item)
				set[item] = true
			}
		}
	}
	return result
}
