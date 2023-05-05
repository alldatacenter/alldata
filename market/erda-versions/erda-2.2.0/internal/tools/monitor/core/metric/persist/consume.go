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
	"encoding/json"
	"errors"
	"strings"
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

// Skip .
var Skip = errors.New("skip")

func (p *provider) decodeData(key, value []byte, topic *string, timestamp time.Time) (interface{}, error) {
	data := &metric.Metric{}
	if err := json.Unmarshal(value, data); err != nil {
		p.stats.DecodeError(value, err)
		if p.Cfg.PrintInvalidMetric {
			p.Log.Warnf("unknown format metric data: %s", string(value))
		} else {
			p.Log.Warnf("failed to decode metric: %v", err)
		}
		return nil, err
	}

	// filter
	if data.Tags == nil || data.Tags[MetricTagLifetime] == MetricTagTransient {
		return nil, Skip
	}
	if len(p.Cfg.Features.FilterPrefix) > 0 {
		if strings.HasPrefix(data.Name, p.Cfg.Features.FilterPrefix) {
			return nil, Skip
		}
	}

	if p.Cfg.Features.MachineSummary {
		if ok := p.handleMachineSummary(data); ok {
			return data, nil
		}
	}

	if err := p.validator.Validate(data); err != nil {
		p.stats.ValidateError(data)
		if p.Cfg.PrintInvalidMetric {
			p.Log.Warnf("invalid metric data: %s, %s", string(value), err)
		} else {
			p.Log.Warnf("invalid metric: %v", err)
		}
		return nil, err
	}
	if p.Cfg.Features.GenerateMeta {
		if err := p.metadata.Process(data); err != nil {
			p.stats.MetadataError(data, err)
			p.Log.Errorf("failed to process metric metadata: %v", err)
		}
	}
	return data, nil
}

func (p *provider) handleReadError(err error) error {
	p.Log.Errorf("failed to read metrics from kafka: %s", err)
	return nil // return nil to continue read
}

func (p *provider) handleWriteError(list []interface{}, err error) error {
	p.Log.Errorf("failed to write into storage: %s", err)
	return nil // return nil to continue consume
}

func (p *provider) confirmErrorHandler(err error) error {
	p.Log.Errorf("failed to confirm metrics from kafka: %s", err)
	return err // return error to exit
}

const (
	MetricMeta = "_metric_meta"

	MetricTagLifetime  = "_lt"       // lifetime
	MetricTagTransient = "transient" // transient, not stored to elasticsearch.

	Minute int64 = Second * 60
	Second int64 = 1000 * 1000 * 1000
)

// handleMachineSummary for compatibility
func (p *provider) handleMachineSummary(m *metric.Metric) bool {
	if m.Name != "machine_summary" {
		return false
	}
	if labels, ok := m.Tags["labels"]; ok {
		m.Fields["labels"] = strings.Split(labels, ",")
	}

	if id, ok := m.Tags["terminus_index_id"]; ok {
		m.Tags["_id"] = id
		delete(m.Tags, "terminus_index_id")
	} else {
		m.Tags["_id"] = m.Tags["cluster_name"] + "/" + m.Tags["host_ip"]
	}
	return true
}
