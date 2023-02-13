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

package reportapisv1

import (
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/mysql"
	"github.com/stretchr/testify/assert"
)

func Test_reportTaskQuery_Supplements(t *testing.T) {
	db, _, err := sqlmock.New()
	if err != nil {
		t.Errorf("Failed to open mock sql db, got error: %v", err)
	}
	if db == nil {
		t.Error("mock db is null")
	}

	gdb, err := gorm.Open("mysql", db)

	defer db.Close()

	hid, taskid, starttime, endtime := uint64(1), uint64(1), int64(1), int64(1)

	rh := &reportHistoryQuery{
		PreLoadDashboardBlock: true,
		ID:                    &hid,
		Scope:                 "org",
		ScopeID:               "erda",
		StartTime:             &starttime,
		EndTime:               &endtime,
		CreatedAtDesc:         true,
		PreLoadTask:           true,
		TaskId:                &taskid,
	}
	rh.Supplements(gdb)
}

func Test_reportTaskQuery_Supplements1(t *testing.T) {
	db, _, err := sqlmock.New()
	if err != nil {
		t.Errorf("Failed to open mock sql db, got error: %v", err)
	}
	if db == nil {
		t.Error("mock db is null")
	}

	gdb, err := gorm.Open("mysql", db)

	defer db.Close()

	hid := uint64(1)

	rt := &reportTaskQuery{
		PreLoadDashboardBlock: true,
		ID:                    &hid,
		Scope:                 "org",
		ScopeID:               "erda",
		CreatedAtDesc:         true,
		Type:                  "aaa",
	}
	rt.Supplements(gdb)
}

func Test_reportHistory_TableName(t *testing.T) {
	rh := &reportHistory{}
	assert.Equal(t, tableReportHistory, rh.TableName())

	rt := &reportTask{}
	assert.Equal(t, tableReportTask, rt.TableName())
}
