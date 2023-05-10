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

package alert_event

import (
	"testing"
	"time"

	"gotest.tools/assert"

	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
)

func Test_calcNeedUpdateFields_Should_Success(t *testing.T) {

	p := &provider{}

	oldData := &db.AlertEvent{
		Id:              "xxx",
		Name:            "aaa",
		OrgID:           1,
		LastTriggerTime: time.Now(),
	}

	newData := &db.AlertEvent{
		Id:               "xxx",
		Name:             "bbb",
		OrgID:            2,
		LastTriggerTime:  time.Date(2022, 1, 1, 10, 0, 0, 0, time.Local),
		FirstTriggerTime: time.Now(),
	}

	want := map[string]interface{}{
		"name":              "bbb",
		"org_id":            int64(2),
		"last_trigger_time": time.Date(2022, 1, 1, 10, 0, 0, 0, time.Local),
	}

	result := p.calcNeedUpdateFields(oldData, newData)

	assert.DeepEqual(t, want, result)
}
