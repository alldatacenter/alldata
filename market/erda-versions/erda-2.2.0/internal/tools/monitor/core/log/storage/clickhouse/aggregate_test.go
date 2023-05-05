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

package clickhouse

import (
	"context"
	"fmt"
	"testing"

	ckdriver "github.com/ClickHouse/clickhouse-go/v2/lib/driver"
	"gotest.tools/assert"

	"github.com/erda-project/erda-infra/providers/clickhouse"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

func Test_Aggregate(t *testing.T) {
	p := &provider{
		Loader:     MockLoader{},
		Creator:    MockCreator{},
		clickhouse: MockClickhouse{},
		Cfg:        &config{},
	}
	_, err := p.Aggregate(context.Background(), &storage.Aggregation{
		Selector: &storage.Selector{
			Start: 1,
			End:   10,
			Filters: []*storage.Filter{
				{
					Key:   "tags.trace_id",
					Op:    storage.EQ,
					Value: "trace_id_1",
				},
			},
			Meta: storage.QueryMeta{
				OrgNames: []string{"", "erda"},
			},
		},
		Aggs: []*storage.AggregationDescriptor{
			{
				Typ:   storage.AggregationTerms,
				Field: "tags.pod_name",
				Options: storage.TermsAggOptions{
					Size:    20,
					Missing: "null",
				},
			},
		},
	})

	assert.NilError(t, err)
}

type MockLoader struct {
}

func (m MockLoader) ExistsWriteTable(tenant, key string) (ok bool, writeTableName string) {
	panic("implement me")
}

func (m MockLoader) GetSearchTable(tenant string) (string, *loader.TableMeta) {
	return "monitor.logs_all", nil
}

func (m MockLoader) ReloadTables() chan error {
	panic("implement me")
}

func (m MockLoader) WaitAndGetTables(ctx context.Context) map[string]*loader.TableMeta {
	panic("implement me")
}

func (m MockLoader) WatchLoadEvent(func(map[string]*loader.TableMeta)) {

}

func (m MockLoader) Database() string {
	panic("implement me")
}

type MockCreator struct {
}

func (m MockCreator) Ensure(ctx context.Context, tenant, key string, ttlDay int64) (<-chan error, string) {
	return nil, fmt.Sprintf("monitor.logs_%s_%s", tenant, key)
}

type MockClickhouse struct {
}

func (m MockClickhouse) NewWriter(opts *clickhouse.WriterOptions) *clickhouse.Writer {
	panic("implement me")
}

func (m MockClickhouse) Client() ckdriver.Conn {
	return MockCkDriver{}
}

type MockCkDriver struct {
}

func (m MockCkDriver) Contributors() []string {
	panic("implement me")
}

func (m MockCkDriver) ServerVersion() (*ckdriver.ServerVersion, error) {
	panic("implement me")
}

func (m MockCkDriver) Select(ctx context.Context, dest interface{}, query string, args ...interface{}) error {
	return nil
}

func (m MockCkDriver) Query(ctx context.Context, query string, args ...interface{}) (ckdriver.Rows, error) {
	panic("implement me")
}

func (m MockCkDriver) QueryRow(ctx context.Context, query string, args ...interface{}) ckdriver.Row {
	panic("implement me")
}

func (m MockCkDriver) PrepareBatch(ctx context.Context, query string) (ckdriver.Batch, error) {
	panic("implement me")
}

func (m MockCkDriver) Exec(ctx context.Context, query string, args ...interface{}) error {
	panic("implement me")
}

func (m MockCkDriver) AsyncInsert(ctx context.Context, query string, wait bool) error {
	panic("implement me")
}

func (m MockCkDriver) Ping(ctx context.Context) error {
	panic("implement me")
}

func (m MockCkDriver) Stats() ckdriver.Stats {
	panic("implement me")
}

func (m MockCkDriver) Close() error {
	panic("implement me")
}
