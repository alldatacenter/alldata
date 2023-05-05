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

package cassandra

import (
	"encoding/hex"
	"fmt"
	"strings"
	"unicode"
)

// SavedLog Cassandra查询的结构
type SavedLog struct {
	Source     string
	ID         string
	Stream     string
	TimeBucket int64 `db:"time_bucket"`
	Timestamp  int64
	Offset     int64
	Content    []byte
	Level      string
	RequestID  string `db:"request_id"`
}

// String .
func (l *SavedLog) String() string {
	return fmt.Sprintf("%d %d [%s] %s", l.Offset, l.Timestamp, l.Level, hex.EncodeToString(l.Content))
}

// DefaultBaseLogTable .
var DefaultBaseLogTable = "spot_prod.base_log"

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
