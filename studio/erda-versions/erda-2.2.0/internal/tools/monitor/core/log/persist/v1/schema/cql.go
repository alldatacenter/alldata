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

package schema

import (
	"strings"
	"unicode"
)

const (
	DefaultKeySpace     = "spot_prod"
	DefaultBaseLogTable = "spot_prod.base_log"

	BaseLogCreateTable = `
     CREATE TABLE IF NOT EXISTS %s.base_log (
         source text,
         id text,
         stream text,
         time_bucket bigint,
         timestamp bigint,
         offset bigint,
         content blob,
         level text,
         request_id text,
         PRIMARY KEY ((source, id, stream, time_bucket), timestamp, offset)
     ) WITH CLUSTERING ORDER BY (timestamp DESC, offset DESC)
         AND bloom_filter_fp_chance = 0.01
         AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
         AND comment = 'base log'
         AND compaction = {'class': 'org.apache.cassandra.db.compaction.TimeWindowCompactionStrategy', 'compaction_window_size': '4', 'compaction_window_unit': 'HOURS'}
         AND compression = {'chunk_length_in_kb': '64', 'class': 'LZ4Compressor'}
         AND gc_grace_seconds = %d;`
	BaseLogAlterTableGCGraceSeconds = `ALTER TABLE %s.base_log WITH gc_grace_seconds = %d;`
	BaseLogCreateIndex              = `CREATE INDEX IF NOT EXISTS idx_request_id ON %s.base_log (request_id);`

	LogMetaCreateTable = `
          CREATE TABLE IF NOT EXISTS %s.base_log_meta (
             source text,
             id text,
             tags map<text, text>,
             PRIMARY KEY ((source, id))
        ) WITH bloom_filter_fp_chance = 0.01
             AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
             AND comment = 'base log meta'
             AND compaction = {'class': 'org.apache.cassandra.db.compaction.TimeWindowCompactionStrategy', 'compaction_window_size': '4', 'compaction_window_unit': 'HOURS', 'max_threshold': '32', 'min_threshold': '4'}
             AND compression = {'chunk_length_in_kb': '64', 'class': 'LZ4Compressor'}
             AND gc_grace_seconds = %d;
	`
	LogMetaCreateIndex = `CREATE INDEX IF NOT EXISTS idx_tags_entry ON %s.base_log_meta (ENTRIES(tags));`
)

// BaseLogWithOrgName .
func BaseLogWithOrgName(orgName string) string {
	return KeyspaceWithOrgName(orgName) + ".base_log"
}

// KeyspaceWithOrgName orgName must match ^([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9]$
// refer https://stackoverflow.com/questions/29569443/cassandra-keyspace-name-with-hyphen
func KeyspaceWithOrgName(orgName string) string {
	orgName = strings.ReplaceAll(strings.ToLower(orgName), "-", "_")
	list := []rune(orgName)
	if len(list) > 0 && !unicode.IsLetter(list[0]) {
		list[0] = 'a'
	}
	return "spot_" + string(list)
}
