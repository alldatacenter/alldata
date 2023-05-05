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

package cassandra

import (
	"fmt"

	"github.com/scylladb/gocqlx/qb"
)

// LogMetaTableName .
const LogMetaTableName = "spot_prod.base_log_meta"

// LogMeta .
type LogMeta struct {
	Source string
	ID     string
	Tags   map[string]string
}

func (p *provider) getTableName(meta *LogMeta) string {
	table := DefaultBaseLogTable
	if meta != nil {
		if v, ok := meta.Tags["dice_org_name"]; ok {
			table = BaseLogWithOrgName(v)
		}
	}
	return table
}

func (p *provider) queryLogMetaWithFilters(filters qb.M) (*LogMeta, error) {
	var res []*LogMeta
	sql := qb.Select(LogMetaTableName).Limit(1)
	for key := range filters {
		sql = sql.Where(qb.Eq(key))
	}
	if err := p.queryFunc(sql, filters, &res); err != nil {
		return nil, fmt.Errorf("retrive %s fialed: %w", LogMetaTableName, err)
	}
	if len(res) == 0 {
		return nil, nil
	}
	return res[0], nil
}

func (p *provider) checkLogMeta(source, id, key, value string) (bool, error) {
	if source != "container" {
		// permission check only for container
		return true, nil
	}
	meta, err := p.queryLogMetaWithFilters(map[string]interface{}{
		"source": source,
		"id":     id,
	})
	if err != nil || meta == nil {
		return false, err
	}
	return meta.Tags[key] == value, nil
}
