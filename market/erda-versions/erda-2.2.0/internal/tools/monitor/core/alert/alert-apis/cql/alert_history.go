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

package cql

import (
	"github.com/gocql/gocql"
	"github.com/scylladb/gocqlx"
	"github.com/scylladb/gocqlx/qb"
)

type AlertHistoryCql struct {
	*gocql.Session
}

func (cql *AlertHistoryCql) TableName() string {
	return "alert_history"
}

func (cql *AlertHistoryCql) QueryAlertHistory(groupID string, start, end int64, limit uint) ([]*AlertHistory, error) {
	stmt, names := qb.Select(cql.TableName()).
		Where(
			qb.Eq("group_id"),
			qb.GtNamed("timestamp", "start"),
			qb.LtNamed("timestamp", "end")).
		OrderBy("timestamp", qb.DESC).
		Limit(limit).ToCql()
	var histories []*AlertHistory
	if err := gocqlx.Query(cql.Query(stmt), names).BindMap(qb.M{
		"group_id": groupID,
		"start":    start,
		"end":      end,
	}).SelectRelease(&histories); err != nil {
		return nil, err
	}
	return histories, nil
}
