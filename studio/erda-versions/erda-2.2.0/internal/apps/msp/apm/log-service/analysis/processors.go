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

package analysis

import (
	"github.com/recallsong/go-utils/encoding"
	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis/processors"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis/processors/regex" //
)

type processorConfig struct {
	Type   string            `json:"type"`
	Config encoding.RawBytes `json:"config"`
}

type tag struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

func (p *provider) loadProcessors() error {
	list, err := p.db.LogMetricConfig.QueryEnabledByScope(p.C.Processors.Scope, p.C.Processors.ScopeID)
	if err != nil {
		return err
	}
	ps := processors.New()
	for _, item := range list {
		if len(item.Filters) <= 0 {
			continue
		}
		var taglist []*tag
		err := json.Unmarshal(reflectx.StringToBytes(item.Filters), &taglist)
		if err != nil {
			p.L.Debugf("fail to parse log filters: %s", err)
			continue
		}
		tags := make(map[string]string, len(taglist)+4)
		for _, item := range taglist {
			tags[item.Key] = item.Value
		}
		var configs []*processorConfig
		err = json.Unmarshal(reflectx.StringToBytes(item.Processors), &configs)
		if err != nil {
			p.L.Debugf("fail to parse log processors: %s", err)
			continue
		}
		for _, cfg := range configs {
			err := ps.Add(item.ScopeID, tags, item.Metric, cfg.Type, cfg.Config)
			if err != nil {
				p.L.Debugf("fail to add log processor: %s", err)
				continue
			}
		}
	}
	p.processors.Store(ps)
	return nil
}
