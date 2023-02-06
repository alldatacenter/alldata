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

package model

import (
	"fmt"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/filter"
)

// semantic same as https://github.com/influxdata/telegraf/blob/master/docs/CONFIGURATION.md#metric-filtering
// key* <=> tag*
// TODO. infra's config parser don't supported embed config
type FilterConfig struct {
	// Selectors
	Keypass    map[string][]string `file:"keypass"`
	Keydrop    map[string][]string `file:"keydrop"`
	Keyinclude []string            `file:"keyinclude"`
	Keyexclude []string            `file:"keyexclude"`
}

type DataFilter struct {
	Keypass    map[string]filter.Filter
	Keydrop    map[string]filter.Filter
	Keyinclude []string
	Keyexclude []string
}

// https://github.com/influxdata/telegraf/blob/master/docs/CONFIGURATION.md#selectors
func (df *DataFilter) Selected(od odata.ObservableData) bool {
	if df.Keypass != nil {
		for k, subf := range df.Keypass {
			val, ok := odata.GetKeyValue(od, k)
			if !ok {
				continue
			}
			if !subf.Match(val.(string)) {
				return false
			}
		}
	}

	if df.Keydrop != nil {
		for k, subf := range df.Keydrop {
			val, ok := odata.GetKeyValue(od, k)
			if !ok {
				continue
			}
			if subf.Match(val.(string)) {
				return false
			}
		}
	}

	for _, key := range df.Keyinclude {
		_, ok := odata.GetKeyValue(od, key)
		if !ok {
			return false
		}
	}
	for _, key := range df.Keyexclude {
		_, ok := odata.GetKeyValue(od, key)
		if ok {
			return false
		}
	}
	return true
}

func NewDataFilter(cfg FilterConfig) (*DataFilter, error) {
	keypass := make(map[string]filter.Filter)
	for k, v := range cfg.Keypass {
		tmp, err := filter.Compile(v)
		if err != nil {
			return nil, fmt.Errorf("keypass<%s>: %w", k, err)
		}
		keypass[k] = tmp
	}
	keydrop := make(map[string]filter.Filter)
	for k, v := range cfg.Keydrop {
		tmp, err := filter.Compile(v)
		if err != nil {
			return nil, fmt.Errorf("keydrop<%s>: %w", k, err)
		}
		keydrop[k] = tmp
	}

	return &DataFilter{
		Keypass:    keypass,
		Keydrop:    keydrop,
		Keyinclude: cfg.Keyinclude,
		Keyexclude: cfg.Keyexclude,
	}, nil
}
