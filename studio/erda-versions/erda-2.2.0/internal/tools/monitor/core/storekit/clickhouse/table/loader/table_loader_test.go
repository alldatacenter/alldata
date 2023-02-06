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
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_extractTTLDays(t *testing.T) {
	tests := []struct {
		sql  string
		want struct {
			baseTimeField string
			ttl           int64
		}
	}{
		{
			sql: "CREATE TABLE monitor.logs (`_id` String, `timestamp` DateTime64(9, 'Asia/Shanghai'), `source` LowCardinality(String), `id` String, `org_name` LowCardinality(String), `tenant_id` LowCardinality(String), `group_id` String, `stream` Enum8('' = 0, 'stdout' = 1, 'stderr' = 2), `offset` Int64, `content` String, `tags` Map(String, String), `tags.trace_id` String MATERIALIZED tags['trace_id'], `tags.level` LowCardinality(String) MATERIALIZED tags['level'], `tags.application_name` LowCardinality(String) MATERIALIZED tags['application_name'], `tags.service_name` String MATERIALIZED tags['service_name'], `tags.pod_name` String MATERIALIZED tags['pod_name'], `tags.pod_ip` String MATERIALIZED tags['pod_ip'], `tags.container_name` String MATERIALIZED tags['container_name'], `tags.container_id` String MATERIALIZED tags['container_id'], `tags.monitor_log_key` LowCardinality(String) MATERIALIZED tags['monitor_log_key'], `tags.msp_env_id` LowCardinality(String) MATERIALIZED tags['msp_env_id'], `tags.dice_application_id` LowCardinality(String) MATERIALIZED tags['dice_application_id'], INDEX idx__id _id TYPE minmax GRANULARITY 1, INDEX idx_trace_id tags.trace_id TYPE bloom_filter GRANULARITY 1, INDEX idx_id id TYPE bloom_filter GRANULARITY 1, INDEX idx_monitor_log_key tags.monitor_log_key TYPE bloom_filter GRANULARITY 1, INDEX idx_msp_env_id tags.msp_env_id TYPE bloom_filter GRANULARITY 1, INDEX idx_dice_application_id tags.dice_application_id TYPE bloom_filter GRANULARITY 1) ENGINE = ReplicatedMergeTree('/clickhouse/tables/{cluster}-{shard}/logs', '{replica}') PARTITION BY toYYYYMMDD(timestamp) ORDER BY (org_name, tenant_id, group_id, timestamp) TTL toDateTime(timestamp) + toIntervalDay(7) SETTINGS index_granularity = 8192",
			want: struct {
				baseTimeField string
				ttl           int64
			}{baseTimeField: "toDateTime(timestamp)", ttl: 7},
		},
		{
			sql: "CREATE TABLE monitor.logs_all (`_id` String, `timestamp` DateTime64(9, 'Asia/Shanghai'), `source` LowCardinality(String), `id` String, `org_name` LowCardinality(String), `tenant_id` LowCardinality(String), `group_id` String, `stream` Enum8('' = 0, 'stdout' = 1, 'stderr' = 2), `offset` Int64, `content` String, `tags` Map(String, String), `tags.trace_id` String MATERIALIZED tags['trace_id'], `tags.level` LowCardinality(String) MATERIALIZED tags['level'], `tags.application_name` LowCardinality(String) MATERIALIZED tags['application_name'], `tags.service_name` String MATERIALIZED tags['service_name'], `tags.pod_name` String MATERIALIZED tags['pod_name'], `tags.pod_ip` String MATERIALIZED tags['pod_ip'], `tags.container_name` String MATERIALIZED tags['container_name'], `tags.container_id` String MATERIALIZED tags['container_id'], `tags.monitor_log_key` LowCardinality(String) MATERIALIZED tags['monitor_log_key'], `tags.msp_env_id` LowCardinality(String) MATERIALIZED tags['msp_env_id'], `tags.dice_application_id` LowCardinality(String) MATERIALIZED tags['dice_application_id']) ENGINE = Distributed('{cluster}', 'monitor', 'logs', rand())",
			want: struct {
				baseTimeField string
				ttl           int64
			}{baseTimeField: "", ttl: 0},
		},
		{
			sql: "CREATE TABLE monitor.logs_erda_search (`_id` String, `timestamp` DateTime64(9, 'Asia/Shanghai'), `source` LowCardinality(String), `id` String, `org_name` LowCardinality(String), `tenant_id` LowCardinality(String), `group_id` String, `stream` Enum8('' = 0, 'stdout' = 1, 'stderr' = 2), `offset` Int64, `content` String, `tags` Map(String, String), `tags.trace_id` String MATERIALIZED tags['trace_id'], `tags.level` LowCardinality(String) MATERIALIZED tags['level'], `tags.application_name` LowCardinality(String) MATERIALIZED tags['application_name'], `tags.service_name` String MATERIALIZED tags['service_name'], `tags.pod_name` String MATERIALIZED tags['pod_name'], `tags.pod_ip` String MATERIALIZED tags['pod_ip'], `tags.container_name` String MATERIALIZED tags['container_name'], `tags.container_id` String MATERIALIZED tags['container_id'], `tags.monitor_log_key` LowCardinality(String) MATERIALIZED tags['monitor_log_key'], `tags.msp_env_id` LowCardinality(String) MATERIALIZED tags['msp_env_id'], `tags.dice_application_id` LowCardinality(String) MATERIALIZED tags['dice_application_id']) ENGINE = Merge('monitor', 'logs_all|logs_erda.*_all$') ",
			want: struct {
				baseTimeField string
				ttl           int64
			}{baseTimeField: "", ttl: 0},
		},
	}

	p := &provider{}

	for _, test := range tests {
		baseTimeField, ttl := p.extractTTLDays(test.sql)
		assert.Equal(t, test.want.baseTimeField, baseTimeField)
		assert.Equal(t, test.want.ttl, ttl)
	}
}
