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
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

type MockRetention struct {
}

func (r MockRetention) GetTTL(key string) time.Duration {
	panic("implement me")
}

func (r MockRetention) DefaultTTL() time.Duration {
	panic("implement me")
}

func (r MockRetention) GetConfigKey(name string, tags map[string]string) string {
	panic("implement me")
}

func (r MockRetention) GetTTLByTags(name string, tags map[string]string) time.Duration {
	panic("implement me")
}

func (r MockRetention) Loading(ctx context.Context) {
	panic("implement me")
}

type MockLoader struct {
}

func (m MockLoader) ExistsWriteTable(tenant, key string) (ok bool, writeTableName string) {
	panic("implement me")
}

func (m MockLoader) GetSearchTable(tenant string) (string, *loader.TableMeta) {
	panic("implement me")
}

func (m MockLoader) ReloadTables() chan error {
	panic("implement me")
}

func (m MockLoader) WatchLoadEvent(listener func(map[string]*loader.TableMeta)) {
	panic("implement me")
}

func (m MockLoader) WaitAndGetTables(ctx context.Context) map[string]*loader.TableMeta {
	panic("implement me")
}

func (m MockLoader) Database() string {
	panic("implement me")
}
