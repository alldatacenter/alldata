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

package span

import (
	"testing"

	"github.com/ClickHouse/clickhouse-go/v2/lib/driver"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
)

func TestMain(m *testing.M) {

	m.Run()
}

func TestWriteSpan_enrichBatch(t *testing.T) {
	type fields struct {
		highCardinalityKeys map[string]struct{}
	}
	type args struct {
		metaBatch   driver.Batch
		seriesBatch driver.Batch
		items       []*trace.Span
	}
	tests := []struct {
		name            string
		fields          fields
		args            args
		wantMetaBatch   driver.Batch
		wantSeriesBatch driver.Batch
		wantErr         bool
	}{
		{
			fields: fields{highCardinalityKeys: map[string]struct{}{}},
			args: args{
				metaBatch:   &mockMetaBatch{},
				seriesBatch: &mockSeriesBatch{},
				items: []*trace.Span{
					{
						StartTime:    1652421595810000000,
						EndTime:      1652421595810843400,
						OrgName:      "erda",
						TraceId:      "bac6e329-4be5-4ed5-b364-c0fc305b4f8e",
						SpanId:       "4e4a3048-7ac0-4233-a133-c63cb3293e39",
						ParentSpanId: "411d3049-0053-44c4-a96b-997e277fd08e",
						Tags: map[string]string{
							"application_id":   "8931",
							"application_name": "trantor-datastore",
							"cluster_name":     "terminus-captain",
							"component":        "Mysql",
							"db_host":          "mysql-master.addon-mysql--wb8019b0757474e1c88b5dcaa64a5ab1f.svc.cluster.local:3306",
							"db_instance":      "trantor_console_staging",
							"db_statement":     "update `meta_store_management__versioned_model_field` set `FromModule` = ? where `id` in (?)",
							"db_system":        "Mysql",
							"env_id":           "c75de7278874f37d9f1bc818b473fc23",
							"host":             "node-010167000123",
							"host_ip":          "10.167.0.123",
							"operation_name":   "Mysql/PreparedStatement/executeUpdate",
							"org_id":           "2",
							"org_name":         "terminus",
							"terminus_key":     "c75de7278874f37d9f1bc818b473fc23",
							"workspace":        "STAGING",
						},
					},
				},
			},
			wantMetaBatch: &mockMetaBatch{arr: []trace.Meta{
				trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "application_id", Value: "8931"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "application_name", Value: "trantor-datastore"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "cluster_name", Value: "terminus-captain"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "component", Value: "Mysql"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_host", Value: "mysql-master.addon-mysql--wb8019b0757474e1c88b5dcaa64a5ab1f.svc.cluster.local:3306"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_instance", Value: "trantor_console_staging"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_statement", Value: "update `meta_store_management__versioned_model_field` set `FromModule` = ? where `id` in (?)"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_system", Value: "Mysql"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "env_id", Value: "c75de7278874f37d9f1bc818b473fc23"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "host", Value: "node-010167000123"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "host_ip", Value: "10.167.0.123"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "operation_name", Value: "Mysql/PreparedStatement/executeUpdate"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "org_id", Value: "2"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "org_name", Value: "terminus"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "terminus_key", Value: "c75de7278874f37d9f1bc818b473fc23"}, trace.Meta{SeriesID: 0x69c6485485d35717, CreateAt: 1652421595810843400, OrgName: "erda", Key: "workspace", Value: "STAGING"},
			}},
			wantSeriesBatch: &mockSeriesBatch{arr: []trace.Series{
				trace.Series{
					StartTime:    1652421595810000000,
					EndTime:      1652421595810843400,
					SeriesID:     0x69c6485485d35717,
					OrgName:      "erda",
					TraceId:      "bac6e329-4be5-4ed5-b364-c0fc305b4f8e",
					SpanId:       "4e4a3048-7ac0-4233-a133-c63cb3293e39",
					ParentSpanId: "411d3049-0053-44c4-a96b-997e277fd08e",
					Tags:         nil,
				},
			}},
			wantErr: false,
		},
		{
			name: "with highCardinalityKeys set",
			fields: fields{highCardinalityKeys: map[string]struct{}{
				"terminus_key": {},
			}},
			args: args{
				metaBatch:   &mockMetaBatch{},
				seriesBatch: &mockSeriesBatch{},
				items: []*trace.Span{
					{
						StartTime:    1652421595810000000,
						EndTime:      1652421595810843400,
						OrgName:      "erda",
						TraceId:      "bac6e329-4be5-4ed5-b364-c0fc305b4f8e",
						SpanId:       "4e4a3048-7ac0-4233-a133-c63cb3293e39",
						ParentSpanId: "411d3049-0053-44c4-a96b-997e277fd08e",
						Tags: map[string]string{
							"application_id":   "8931",
							"application_name": "trantor-datastore",
							"cluster_name":     "terminus-captain",
							"component":        "Mysql",
							"db_host":          "mysql-master.addon-mysql--wb8019b0757474e1c88b5dcaa64a5ab1f.svc.cluster.local:3306",
							"db_instance":      "trantor_console_staging",
							"db_statement":     "update `meta_store_management__versioned_model_field` set `FromModule` = ? where `id` in (?)",
							"db_system":        "Mysql",
							"env_id":           "c75de7278874f37d9f1bc818b473fc23",
							"host":             "node-010167000123",
							"host_ip":          "10.167.0.123",
							"operation_name":   "Mysql/PreparedStatement/executeUpdate",
							"org_id":           "2",
							"org_name":         "terminus",
							"terminus_key":     "c75de7278874f37d9f1bc818b473fc23",
							"workspace":        "STAGING",
						},
					},
				},
			},
			wantMetaBatch: &mockMetaBatch{arr: []trace.Meta{
				trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "application_id", Value: "8931"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "application_name", Value: "trantor-datastore"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "cluster_name", Value: "terminus-captain"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "component", Value: "Mysql"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_host", Value: "mysql-master.addon-mysql--wb8019b0757474e1c88b5dcaa64a5ab1f.svc.cluster.local:3306"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_instance", Value: "trantor_console_staging"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_statement", Value: "update `meta_store_management__versioned_model_field` set `FromModule` = ? where `id` in (?)"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "db_system", Value: "Mysql"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "env_id", Value: "c75de7278874f37d9f1bc818b473fc23"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "host", Value: "node-010167000123"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "host_ip", Value: "10.167.0.123"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "operation_name", Value: "Mysql/PreparedStatement/executeUpdate"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "org_id", Value: "2"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "org_name", Value: "terminus"}, trace.Meta{SeriesID: 0xfd271a2137feb4a1, CreateAt: 1652421595810843400, OrgName: "erda", Key: "workspace", Value: "STAGING"},
			}},
			wantSeriesBatch: &mockSeriesBatch{arr: []trace.Series{
				trace.Series{
					StartTime:    1652421595810000000,
					EndTime:      1652421595810843400,
					SeriesID:     0xfd271a2137feb4a1,
					OrgName:      "erda",
					TraceId:      "bac6e329-4be5-4ed5-b364-c0fc305b4f8e",
					SpanId:       "4e4a3048-7ac0-4233-a133-c63cb3293e39",
					ParentSpanId: "411d3049-0053-44c4-a96b-997e277fd08e",
					Tags: map[string]string{
						"terminus_key": "c75de7278874f37d9f1bc818b473fc23",
					},
				},
			}},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ws := &Storage{
				highCardinalityKeys: tt.fields.highCardinalityKeys,
				sidSet:              newSeriesIDSet(0),
			}
			if err := ws.enrichBatch(tt.args.metaBatch, tt.args.seriesBatch, tt.args.items); (err != nil) != tt.wantErr {
				t.Errorf("enrichBatch() error = %v, wantErr %v", err, tt.wantErr)
			}
			assert.Equal(t, tt.wantMetaBatch, tt.args.metaBatch)
			assert.Equal(t, tt.wantSeriesBatch, tt.args.seriesBatch)
		})
	}
}

type mockMetaBatch struct {
	arr []trace.Meta
}

func (m *mockMetaBatch) Abort() error { return nil }

func (m *mockMetaBatch) Append(v ...interface{}) error { return nil }

func (m *mockMetaBatch) AppendStruct(v interface{}) error {
	m.arr = append(m.arr, *v.(*trace.Meta))
	return nil
}

func (m *mockMetaBatch) Column(i int) driver.BatchColumn { return nil }

func (m *mockMetaBatch) Send() error { return nil }

type mockSeriesBatch struct {
	arr []trace.Series
}

func (m *mockSeriesBatch) Abort() error { return nil }

func (m *mockSeriesBatch) Append(v ...interface{}) error { return nil }

func (m *mockSeriesBatch) AppendStruct(v interface{}) error {
	m.arr = append(m.arr, *v.(*trace.Series))
	return nil
}

func (m *mockSeriesBatch) Column(i int) driver.BatchColumn { return nil }

func (m *mockSeriesBatch) Send() error { return nil }
