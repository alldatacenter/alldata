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

package loader

import (
	"context"
	"encoding/json"
	"fmt"
	"regexp"
	"strconv"
	"time"

	ck "github.com/ClickHouse/clickhouse-go/v2"
)

func (p *provider) storeTablesToCache() error {
	tables, ok := p.tables.Load().(map[string]*TableMeta)
	if !ok {
		return nil
	}
	expire := 3 * p.Cfg.ReloadInterval
	bytes, _ := json.Marshal(tables)
	err := p.Redis.Set(p.Cfg.CacheKeyPrefix+"-all", string(bytes), expire).Err()
	return err
}

func (p *provider) runClickhouseTablesLoader(ctx context.Context) error {
	p.suppressCacheLoader = true
	p.Log.Info("start clickhouse tables loader")
	defer p.Log.Info("exit clickhouse tables loader")
	timer := time.NewTimer(0)
	defer timer.Stop()
	var notifiers []chan error
	for {
		select {
		case <-ctx.Done():
			return nil
		case ch := <-p.reloadCh:
			if ch != nil {
				notifiers = append(notifiers, ch)
			}
		case <-timer.C:
		}

		p.loadLock.Lock()

		err := p.reloadTablesFromClickhouse(ctx)
		if err != nil {
			p.Log.Errorf("failed to reload indices from clickhouse: %s", err)
		}

	drain:
		for {
			select {
			case ch := <-p.reloadCh:
				if ch != nil {
					notifiers = append(notifiers, ch)
				}
			default:
				break drain
			}
		}

		for _, notifier := range notifiers {
			notifier <- err
			close(notifier)
		}
		notifiers = nil

		if p.needSyncTablesToCache {
			err = p.storeTablesToCache()
			if err != nil {
				p.Log.Errorf("failed to sync tables to cache: %s", err)
			}
		}

		timer.Reset(p.Cfg.ReloadInterval)
		p.loadLock.Unlock()
	}
}

func (p *provider) reloadTablesFromClickhouse(ctx context.Context) error {
	var tables []struct {
		Database       string `ch:"database"`
		Name           string `ch:"name"`
		Engine         string `ch:"engine"`
		CreateTableSql string `ch:"create_table_query"`
	}
	err := p.Clickhouse.Client().
		Select(ctx, &tables, "select database, name, engine, create_table_query from system.tables where database = @db and name like @name",
			ck.Named("db", p.Cfg.Database), ck.Named("name", fmt.Sprintf("%s%%", p.Cfg.TablePrefix)))
	if err != nil {
		return err
	}
	tablesMeta := map[string]*TableMeta{}
	for _, table := range tables {
		ttlBaseTime, ttl := p.extractTTLDays(table.CreateTableSql)
		tablesMeta[fmt.Sprintf("%s.%s", table.Database, table.Name)] = &TableMeta{
			Engine:       table.Engine,
			Columns:      map[string]*TableColumn{},
			TTLDays:      ttl,
			TTLBaseField: ttlBaseTime,
		}
	}

	var columns []struct {
		Database string `ch:"database"`
		Table    string `ch:"table"`
		Name     string `ch:"name"`
		Type     string `ch:"type"`
	}
	err = p.Clickhouse.Client().
		Select(ctx, &columns, "select database, table, name, type from system.columns where database= @db and table like @table",
			ck.Named("db", p.Cfg.Database), ck.Named("table", fmt.Sprintf("%s%%", p.Cfg.TablePrefix)))
	if err != nil {
		return err
	}
	for _, column := range columns {
		table := fmt.Sprintf("%s.%s", column.Database, column.Table)
		meta, ok := tablesMeta[table]
		if !ok {
			continue
		}
		meta.Columns[column.Name] = &TableColumn{Type: ColumnType(column.Type)}
	}

	ch := p.updateTables(tablesMeta)
	select {
	case <-ch:
	case <-ctx.Done():
	}
	return nil
}

func (p *provider) extractTTLDays(createTableSql string) (baseTimeField string, ttl int64) {
	regex, _ := regexp.Compile(`TTL\s(.*?)\s\+\stoIntervalDay\((\d+)\)\s+SETTINGS`)
	match := regex.FindStringSubmatch(createTableSql)
	if len(match) < 3 {
		return
	}
	baseTimeField = match[1]
	ttl, _ = strconv.ParseInt(match[2], 10, 64)
	return
}
