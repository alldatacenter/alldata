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
	"io/ioutil"
	"regexp"
	"strconv"
	"strings"
	"time"

	cfgpkg "github.com/recallsong/go-utils/config"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

func (p *provider) initDefaultDDLs() error {
	p.Log.Infof("start init default ddls")
	defer p.Log.Infof("finish init default ddls")
	replacer := strings.NewReplacer(
		table.DatabaseNameKey, p.Cfg.Database,
		table.TtlDaysNameKey, strconv.FormatInt(int64(p.Retention.DefaultTTL()/time.Hour/24), 10),
	)
	return p.executeDDLs(p.Cfg.DefaultDDLs, replacer)
}

func (p *provider) initTenantDDLs() {
	p.Log.Infof("start init tenant ddls")
	defer p.Log.Infof("finish init tenant ddls")
	tables := p.Loader.WaitAndGetTables(context.Background())
	for t, tableMeta := range tables {
		database, tenant, key, ok := p.extractTenantAndKey(t, tableMeta, tables)
		if !ok {
			continue
		}

		writeTable := fmt.Sprintf("%s_%s_%s", p.Cfg.TablePrefix, table.NormalizeKey(tenant), table.NormalizeKey(key))
		searchTable := fmt.Sprintf("%s_%s", p.Cfg.TablePrefix, table.NormalizeKey(tenant))

		replacer := strings.NewReplacer(
			table.DatabaseNameKey, database,
			table.TableNameKey, writeTable,
			table.AliasTableNameKey, searchTable,
			table.TtlDaysNameKey, strconv.FormatInt(int64(p.Retention.DefaultTTL()/time.Hour/24), 10))

		_ = p.executeDDLs(p.Cfg.TenantDDLs, replacer)
	}
}

func (p *provider) extractTenantAndKey(table string, meta *loader.TableMeta, tables map[string]*loader.TableMeta) (database, tenant, key string, ok bool) {
	distTableName := fmt.Sprintf("%s_all", table)
	if _, o := tables[distTableName]; !o {
		return
	}

	searchWindow := table
	for {
		index := strings.LastIndex(searchWindow, "_")
		if index < 0 {
			return
		}
		tenant = table[:index]
		searchTable := fmt.Sprintf("%s_search", tenant)
		if _, o := tables[searchTable]; !o {
			searchWindow = searchWindow[:index]
			continue
		}

		arr := strings.SplitN(tenant, fmt.Sprintf(".%s_", p.Cfg.TablePrefix), 2)
		if len(arr) != 2 {
			return
		}

		database = arr[0]
		tenant = arr[1]
		key = table[index+1:]
		ok = true

		return
	}
}

func (p *provider) executeDDLs(ddlFiles []ddlFile, replacer *strings.Replacer) error {
	for _, file := range ddlFiles {
		data, err := ioutil.ReadFile(file.Path)
		if err != nil {
			return fmt.Errorf("failed to read file: %s", file.Path)
		}
		data = cfgpkg.EscapeEnv(data)
		regex, _ := regexp.Compile("[^;]+[;$]")
		ddls := regex.FindAllString(string(data), -1)
		for _, ddl := range ddls {
			ddl = replacer.Replace(ddl)
			err := p.Clickhouse.Client().Exec(context.Background(), ddl)
			if err == nil {
				continue
			}
			p.Log.Warnf("failed to execute ddl of file[%s], ddl: %s, err: %s", file.Path, ddl, err)
			if file.IgnoreErr {
				continue
			}
			return err
		}
	}
	return nil
}
