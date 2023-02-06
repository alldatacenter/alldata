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
	"fmt"
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table"
)

type Interface interface {
	ExistsWriteTable(tenant, key string) (ok bool, writeTableName string)
	GetSearchTable(tenant string) (string, *TableMeta)
	ReloadTables() chan error
	WatchLoadEvent(listener func(map[string]*TableMeta))
	WaitAndGetTables(ctx context.Context) map[string]*TableMeta
	Database() string
}

func (p *provider) ExistsWriteTable(tenant, key string) (ok bool, writeTableName string) {
	writeTableName = fmt.Sprintf("%s.%s_%s_%s", p.Cfg.Database, p.Cfg.TablePrefix, table.NormalizeKey(tenant), table.NormalizeKey(key))
	tables, ok := p.tables.Load().(map[string]*TableMeta)
	if !ok {
		return false, writeTableName
	}
	_, ok = tables[writeTableName]
	return ok, writeTableName
}

func (p *provider) GetSearchTable(tenant string) (string, *TableMeta) {
	tables, ok := p.tables.Load().(map[string]*TableMeta)
	if !ok {
		return fmt.Sprintf("%s.%s", p.Cfg.Database, p.Cfg.DefaultSearchTable), nil
	}
	searchTableName := fmt.Sprintf("%s.%s_%s_search", p.Cfg.Database, p.Cfg.TablePrefix, table.NormalizeKey(tenant))
	meta, ok := tables[searchTableName]
	if ok {
		return searchTableName, meta
	}
	// fallback to default table
	searchTableName = fmt.Sprintf("%s.%s", p.Cfg.Database, p.Cfg.DefaultSearchTable)
	meta = tables[searchTableName]
	return searchTableName, meta
}

func (p *provider) ReloadTables() chan error {
	ch := make(chan error, 1)
	p.reloadCh <- ch
	return ch
}

func (p *provider) WatchLoadEvent(listener func(map[string]*TableMeta)) {
	p.listeners = append(p.listeners, listener)
}

func (p *provider) WaitAndGetTables(ctx context.Context) map[string]*TableMeta {
	for {
		tables, ok := p.tables.Load().(map[string]*TableMeta)

		if ok && len(tables) > 0 {
			return tables
		}

		// wait for the index to complete loading
		select {
		case <-time.After(1 * time.Second):
		case <-ctx.Done():
			return nil
		}
	}
}

func (p *provider) Database() string {
	return p.Cfg.Database
}
