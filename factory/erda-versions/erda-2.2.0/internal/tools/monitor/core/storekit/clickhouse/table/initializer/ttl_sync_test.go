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
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

func Test_syncTTL(t *testing.T) {

	var (
		database          = "monitor"
		defaultDuration   = time.Hour * 24 * 7
		unchangedDuration = time.Hour * 24
		tablePrefix       = "logs"
		tableMetas        = map[string]*loader.TableMeta{
			"monitor.logs":              {TTLBaseField: "toDateTime(timestamp)", TTLDays: 1},
			"monitor.logs_all":          {},
			"monitor.logs_erda_xxx":     {TTLBaseField: "toDateTime(timestamp)", TTLDays: 7},
			"monitor.logs_erda_search":  {},
			"monitor.logs_erda_xxx_all": {},
		}
	)

	var (
		changingTTLDays = map[string]int64{}
	)

	monkey.Patch((*MockRetention).DefaultTTL, func(ret *MockRetention) time.Duration {
		return defaultDuration
	})
	defer monkey.Unpatch((*MockRetention).DefaultTTL)

	monkey.Patch((*MockRetention).GetTTL, func(ret *MockRetention, key string) time.Duration {
		return unchangedDuration
	})
	defer monkey.Unpatch((*MockRetention).GetTTL)

	monkey.Patch((*MockLoader).WaitAndGetTables, func(l *MockLoader, ctx context.Context) map[string]*loader.TableMeta {
		return tableMetas
	})
	defer monkey.Unpatch((*MockLoader).WaitAndGetTables)

	monkey.Patch((*provider).AlterTableTTL, func(p *provider, tableName string, meta *loader.TableMeta, ttlDays int64) {
		changingTTLDays[tableName] = ttlDays
	})
	defer monkey.Unpatch((*provider).AlterTableTTL)

	p := &provider{
		Cfg: &config{
			Database:        database,
			TablePrefix:     tablePrefix,
			TTLSyncInterval: time.Millisecond,
		},
		Log:       logrusx.New(),
		Retention: &MockRetention{},
		Loader:    &MockLoader{},
	}

	ctx, cancel := context.WithTimeout(context.Background(), time.Millisecond*10)
	defer cancel()
	p.syncTTL(ctx)

	assert.EqualValues(t, 2, len(changingTTLDays))
	assert.EqualValues(t, 7, changingTTLDays["monitor.logs"])
	assert.EqualValues(t, 1, changingTTLDays["monitor.logs_erda_xxx"])

}
