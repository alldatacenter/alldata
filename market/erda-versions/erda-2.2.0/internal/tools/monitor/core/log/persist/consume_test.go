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

package persist

import (
	"testing"

	"gotest.tools/assert"

	"github.com/erda-project/erda/internal/tools/monitor/core/log"
)

func Test_normalize_request_id_compatible(t *testing.T) {

	tests := []struct {
		Log      *log.Log
		wantTags map[string]string
		wantErr  bool
	}{
		{
			Log:      &log.Log{Tags: map[string]string{"request-id": "1"}},
			wantTags: map[string]string{"level": "INFO", "request_id": "1", "trace_id": "1"},
		},
		{
			Log:      &log.Log{Tags: map[string]string{"request_id": "1"}},
			wantTags: map[string]string{"level": "INFO", "request_id": "1", "trace_id": "1"},
		},
		{
			Log:      &log.Log{Tags: map[string]string{"trace_id": "1"}},
			wantTags: map[string]string{"level": "INFO", "request_id": "1", "trace_id": "1"},
		},
	}

	p := &provider{Cfg: &config{}}

	for _, test := range tests {
		p.normalize(test.Log)
		assert.DeepEqual(t, test.wantTags, test.Log.Tags)
	}
}
