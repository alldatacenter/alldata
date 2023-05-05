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

package initializer

import (
	"context"
	"fmt"
	"time"

	"github.com/ClickHouse/clickhouse-go/v2"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

func (p *provider) syncTTL(ctx context.Context) {
	p.Log.Infof("run sync ttl with interval: %v", p.Cfg.TTLSyncInterval)
	for {
		select {
		case <-ctx.Done():
			return
		case <-time.After(p.Cfg.TTLSyncInterval):
			p.Log.Infof("start check and sync ttls")
			tables := p.Loader.WaitAndGetTables(ctx)
			for t, meta := range tables {
				select {
				case <-ctx.Done():
					return
				default:
				}

				if meta.TTLDays == 0 || len(meta.TTLBaseField) == 0 {
					continue
				}

				// default table
				if t == fmt.Sprintf("%s.%s", p.Cfg.Database, p.Cfg.TablePrefix) && meta.TTLDays != table.FormatTTLToDays(p.Retention.DefaultTTL()) {
					p.AlterTableTTL(t, meta, table.FormatTTLToDays(p.Retention.DefaultTTL()))
					continue
				}

				// tenant table
				database, tenant, key, ok := p.extractTenantAndKey(t, meta, tables)
				if !ok {
					continue
				}

				ttl := table.FormatTTLToDays(p.Retention.GetTTL(key))
				if meta.TTLDays == ttl {
					continue
				}

				writeTable := fmt.Sprintf("%s.%s_%s_%s", database, p.Cfg.TablePrefix, tenant, key)
				p.AlterTableTTL(writeTable, meta, ttl)
			}
		}
	}
}

func (p *provider) AlterTableTTL(tableName string, meta *loader.TableMeta, ttlDays int64) {
	p.Log.Infof("start change ttl of table[%s]", tableName)
	sql := fmt.Sprintf("ALTER TABLE %s ON CLUSTER '{cluster}' MODIFY TTL %s + INTERVAL %v DAY;", tableName, meta.TTLBaseField, ttlDays)
	err := p.Clickhouse.Client().Exec(clickhouse.Context(context.Background(), clickhouse.WithSettings(map[string]interface{}{
		"materialize_ttl_after_modify": 0,
	})), sql)
	if err != nil {
		p.Log.Warnf("failed to change ttl of table[%s] to %v day, sql: %s", tableName, ttlDays, sql)
	} else {
		p.Log.Infof("finish change ttl of table[%s] to %v day", tableName, ttlDays)
	}
}
