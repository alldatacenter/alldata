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

package metric

import (
	"strconv"
	"testing"
)

func Test_getMetricFromSQL(t *testing.T) {
	tests := []struct {
		sql  string
		want string
	}{
		{
			sql:  "SELECT * FROM metric1",
			want: "metric1",
		},
		{
			sql:  "select * FrOM metric1",
			want: "metric1",
		},
		{
			sql:  "sElEct * from metric1",
			want: "metric1",
		},
		{
			sql:  "SELECT field from metric1",
			want: "metric1",
		},
		{
			sql:  "SELECT *from metric1",
			want: "",
		},
		{
			sql:  "SELECT * from metric1;",
			want: "metric1",
		},
		{
			sql:  "SELECT field from metric1 ;",
			want: "metric1",
		},
		{
			sql:  "SELECT * from metric1 WHERE id::tag='1';",
			want: "metric1",
		},
		{
			sql:  "SELECT * from metric1WHERE id::tag='1';",
			want: "metric1WHERE",
		},
		{
			sql:  "SELECT field from metric1,metric2",
			want: "metric1",
		},
	}
	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			if got := getMetricFromSQL(tt.sql); got != tt.want {
				t.Errorf("getMetricFromSQL(%q) = %v, want %v", tt.sql, got, tt.want)
			}
		})
	}
}
