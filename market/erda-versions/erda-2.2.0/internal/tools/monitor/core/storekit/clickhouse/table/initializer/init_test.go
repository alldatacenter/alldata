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
	"strings"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

func Test_initDefaultDDLs(t *testing.T) {

	var (
		database        = "monitor"
		defaultDuration = time.Hour * 24 * 7
		tablePrefix     = "logs"
		replaceResult   string
	)

	monkey.Patch((*MockRetention).DefaultTTL, func(ret *MockRetention) time.Duration {
		return defaultDuration
	})
	defer monkey.Unpatch((*MockRetention).DefaultTTL)

	monkey.Patch((*provider).executeDDLs, func(p *provider, ddlFiles []ddlFile, replacer *strings.Replacer) error {
		replaceResult = replacer.Replace(fmt.Sprintf("%s|%s|%s|%s",
			table.DatabaseNameKey,
			table.TableNameKey,
			table.AliasTableNameKey,
			table.TtlDaysNameKey))
		return nil
	})
	defer monkey.Unpatch((*provider).executeDDLs)

	p := &provider{
		Cfg: &config{
			Database:    database,
			TablePrefix: tablePrefix,
		},
		Log:       logrusx.New(),
		Retention: &MockRetention{},
	}

	p.initDefaultDDLs()

	assert.Equal(t,
		fmt.Sprintf("%v|%v|%v|%v", database, table.TableNameKey, table.AliasTableNameKey, int64(defaultDuration/time.Hour/24)),
		replaceResult)
}

func Test_initTenantDDLs(t *testing.T) {

	var (
		database        = "monitor"
		defaultDuration = time.Hour * 24 * 7
		tablePrefix     = "logs"
		tableMetas      = map[string]*loader.TableMeta{
			"monitor.logs_erda_xxx":     nil,
			"monitor.logs_erda_search":  nil,
			"monitor.logs_erda_xxx_all": nil,
		}
		replaceResult string
	)

	monkey.Patch((*MockRetention).DefaultTTL, func(ret *MockRetention) time.Duration {
		return defaultDuration
	})
	defer monkey.Unpatch((*MockRetention).DefaultTTL)

	monkey.Patch((*MockLoader).WaitAndGetTables, func(l *MockLoader, ctx context.Context) map[string]*loader.TableMeta {
		return tableMetas
	})
	defer monkey.Unpatch((*MockLoader).WaitAndGetTables)

	monkey.Patch((*provider).executeDDLs, func(p *provider, ddlFiles []ddlFile, replacer *strings.Replacer) error {
		replaceResult = replacer.Replace(fmt.Sprintf("%s|%s|%s|%s",
			table.DatabaseNameKey,
			table.TableNameKey,
			table.AliasTableNameKey,
			table.TtlDaysNameKey))
		return nil
	})
	defer monkey.Unpatch((*provider).executeDDLs)

	p := &provider{
		Cfg: &config{
			Database:    database,
			TablePrefix: tablePrefix,
		},
		Log:       logrusx.New(),
		Retention: &MockRetention{},
		Loader:    &MockLoader{},
	}

	p.initTenantDDLs()

	assert.Greater(t, len(replaceResult), 0)
	assert.Equal(t,
		fmt.Sprintf("%v|%v|%v|%v", database, "logs_erda_xxx", "logs_erda", int64(defaultDuration/time.Hour/24)),
		replaceResult)
}

func Test_extractTenantAndKey(t *testing.T) {
	tests := []struct {
		tableName string
		want      struct {
			database string
			tenant   string
			key      string
			ok       bool
		}
	}{
		{
			tableName: "monitor.logs",
			want: struct {
				database string
				tenant   string
				key      string
				ok       bool
			}{},
		},
		{
			tableName: "monitor.logs_all",
			want: struct {
				database string
				tenant   string
				key      string
				ok       bool
			}{},
		},
		{
			tableName: "monitor.logs_erda_xxx",
			want: struct {
				database string
				tenant   string
				key      string
				ok       bool
			}{database: "monitor", tenant: "erda", key: "xxx", ok: true},
		},
		{
			tableName: "monitor.logs_erda_search",
			want: struct {
				database string
				tenant   string
				key      string
				ok       bool
			}{},
		},
		{
			tableName: "monitor.logs_erda_all",
			want: struct {
				database string
				tenant   string
				key      string
				ok       bool
			}{},
		},
	}

	tableMetas := map[string]*loader.TableMeta{
		"monitor.logs":              nil,
		"monitor.logs_all":          nil,
		"monitor.logs_erda_xxx":     nil,
		"monitor.logs_erda_search":  nil,
		"monitor.logs_erda_xxx_all": nil,
	}
	p := &provider{
		Cfg: &config{
			Database:    "monitor",
			TablePrefix: "logs",
		},
	}
	for _, test := range tests {
		database, tenant, key, ok := p.extractTenantAndKey(test.tableName, tableMetas[test.tableName], tableMetas)
		assert.Equal(t, test.want.ok, ok)
		assert.Equal(t, test.want.database, database)
		assert.Equal(t, test.want.tenant, tenant)
		assert.Equal(t, test.want.key, key)
	}
}
