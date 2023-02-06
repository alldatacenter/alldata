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

package tsql

import (
	"testing"
	"time"
)

var (
	ctx      Context
	testTime time.Time
)

const (
	timeFormat  = "2006-01-02 15:04:05"
	strTestTime = "1970-01-01 00:00:01"
)

func prepare() {
	testTime, _ = time.Parse(timeFormat, strTestTime)
}

func Test_BuildInFunctions(t *testing.T) {
	prepare()
	tests := []struct {
		name    string
		args    []interface{}
		want    interface{}
		wantErr bool
	}{
		{"default_value", []interface{}{"1", "2", "3"}, "error", true},
		{"default_value", []interface{}{nil, "1"}, "1", false},
		{"default_value", []interface{}{"1", nil}, "1", false},
		{"format", []interface{}{}, "error", true},
		{"format", []interface{}{"1"}, "1", false},
		{"format", []interface{}{nil}, "error", true},
		{"format_time", []interface{}{}, "error", true},
		{"format_time", []interface{}{"s", "s"}, "error", true},
		{"format_time", []interface{}{time.Now(), 7.6}, "error", true},
		{"format_time", []interface{}{"2000-01-01 15:04:05", "2006-01-02"}, "2000-01-01", false},
		{"format_date", []interface{}{}, "error", true},
		{"format_date", []interface{}{"string"}, "error", true},
		{"format_date", []interface{}{"2000-01-01 15:04:05"}, "2000-01-01", false},
		{"format_bytes", []interface{}{}, "error", true},
		{"format_bytes", []interface{}{1024}, "1KB", false},
		{"format_duration", []interface{}{}, "error", true},
		{"format_duration", []interface{}{1000100, "ms", 0}, "16m40s", false},
		{"format_duration", []interface{}{1000100, "s"}, "277h48m20s", false},
		{"format_duration", []interface{}{1000100, "m"}, "16668h20m0s", false},
		{"format_duration", []interface{}{1000100, "h"}, "1000100h0m0s", false},
		{"format_duration", []interface{}{101, "d"}, "2424h0m0s", false},
		{"format_duration", []interface{}{101, 1}, "error", true},
		{"map", []interface{}{}, "error", true},
		{"map", []interface{}{0, 0, "Gen0", 1}, "error", true},
		{"map", []interface{}{int64(1), int(0), "Gen0", int(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{int64(1), uint(0), "Gen0", uint(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{int64(1), uint64(0), "Gen0", uint64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{int64(1), float32(0), "Gen0", float32(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{int64(1), float64(0), "Gen0", float64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{int64(1), int64(0), "Gen0", int64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{uint64(1), int(0), "Gen0", int(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{uint64(1), uint(0), "Gen0", uint(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{uint64(1), uint64(0), "Gen0", uint64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{uint64(1), float32(0), "Gen0", float32(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{uint64(1), float64(0), "Gen0", float64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{uint64(1), int64(0), "Gen0", int64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{float64(1), int(0), "Gen0", int(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{float64(1), uint(0), "Gen0", uint(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{float64(1), uint64(0), "Gen0", uint64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{float64(1), float32(0), "Gen0", float32(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{float64(1), float64(0), "Gen0", float64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{float64(1), int64(0), "Gen0", int64(1), "Gen1"}, "Gen1", false},
		{"map", []interface{}{"1", int64(0), "Gen0", int64(1), "Gen1"}, "1", false},
		{"map", []interface{}{3, int64(0), "Gen0", int64(1), "Gen1"}, 3, false},
		{"round_float", []interface{}{}, "error", true},
		{"round_float", []interface{}{int64(7), 1}, int64(7), false},
		{"round_float", []interface{}{float32(7.85), 1}, 7.8, false},
		{"round_float", []interface{}{float64(7.85), 1}, 7.8, false},
		{"trim", []interface{}{"    trim    ", " "}, "trim", false},
		{"trim_left", []interface{}{"    trim", " "}, "trim", false},
		{"trim_right", []interface{}{"trim    ", " "}, "trim", false},
		{"trim_space", []interface{}{}, "error", true},
		{"trim_space", []interface{}{"        trim    "}, "trim", false},
		{"trim_space", []interface{}{1.1}, "trim", true},
		{"trim_prefix", []interface{}{"ttrim", "t"}, "trim", false},
		{"trim_suffix", []interface{}{"trimm", "m"}, "trim", false},
		{"max_value", []interface{}{}, "error", true},
		{"max_value", []interface{}{1, 2}, 2, false},
		{"max_value", []interface{}{3, 2}, 3, false},
		{"min_value", []interface{}{}, "error", true},
		{"min_value", []interface{}{1, 2}, 1, false},
		{"min_value", []interface{}{3, 2}, 2, false},
		{"min_value", []interface{}{}, "error", true},
		{"min_value", []interface{}{}, "error", true},
		{"int", []interface{}{}, "error", true},
		{"int", []interface{}{nil}, int64(0), false},
		{"int", []interface{}{false}, int64(0), false},
		{"int", []interface{}{int(1)}, int64(1), false},
		{"int", []interface{}{int8(1)}, int64(1), false},
		{"int", []interface{}{int16(1)}, int64(1), false},
		{"int", []interface{}{int32(1)}, int64(1), false},
		{"int", []interface{}{int64(1)}, int64(1), false},
		{"int", []interface{}{uint(1)}, int64(1), false},
		{"int", []interface{}{uint8(1)}, int64(1), false},
		{"int", []interface{}{uint16(1)}, int64(1), false},
		{"int", []interface{}{uint32(1)}, int64(1), false},
		{"int", []interface{}{uint64(1)}, int64(1), false},
		{"int", []interface{}{float32(1)}, int64(1), false},
		{"int", []interface{}{float64(1)}, int64(1), false},
		{"int", []interface{}{"1"}, int64(1), false},
		{"int", []interface{}{1 * time.Nanosecond}, int64(1), false},
		{"int", []interface{}{testTime}, int64(1000000000), false},
		{"int", []interface{}{map[string]string{}}, int64(0), true},
		{"bool", []interface{}{}, "error", true},
		{"bool", []interface{}{nil}, false, false},
		{"bool", []interface{}{false}, false, false},
		{"bool", []interface{}{int(1)}, true, false},
		{"bool", []interface{}{int8(1)}, true, false},
		{"bool", []interface{}{int16(1)}, true, false},
		{"bool", []interface{}{int32(1)}, true, false},
		{"bool", []interface{}{int64(1)}, true, false},
		{"bool", []interface{}{uint(1)}, true, false},
		{"bool", []interface{}{uint8(1)}, true, false},
		{"bool", []interface{}{uint16(1)}, true, false},
		{"bool", []interface{}{uint32(1)}, true, false},
		{"bool", []interface{}{uint64(1)}, true, false},
		{"bool", []interface{}{float32(1)}, true, false},
		{"bool", []interface{}{float64(1)}, true, false},
		{"bool", []interface{}{"test"}, true, false},
		{"bool", []interface{}{1 * time.Nanosecond}, true, false},
		{"bool", []interface{}{testTime}, true, false},
		{"bool", []interface{}{map[string]string{}}, true, false},
		{"float", []interface{}{}, "error", true},
		{"float", []interface{}{nil}, float64(0), false},
		{"float", []interface{}{false}, float64(0), false},
		{"float", []interface{}{int(1)}, float64(1), false},
		{"float", []interface{}{int8(1)}, float64(1), false},
		{"float", []interface{}{int16(1)}, float64(1), false},
		{"float", []interface{}{int32(1)}, float64(1), false},
		{"float", []interface{}{int64(1)}, float64(1), false},
		{"float", []interface{}{uint(1)}, float64(1), false},
		{"float", []interface{}{uint8(1)}, float64(1), false},
		{"float", []interface{}{uint16(1)}, float64(1), false},
		{"float", []interface{}{uint32(1)}, float64(1), false},
		{"float", []interface{}{uint64(1)}, float64(1), false},
		{"float", []interface{}{float32(1)}, float64(1), false},
		{"float", []interface{}{float64(1)}, float64(1), false},
		{"float", []interface{}{"1.0"}, float64(1), false},
		{"float", []interface{}{1 * time.Nanosecond}, float64(1), false},
		{"float", []interface{}{testTime}, float64(1000000000), false},
		{"float", []interface{}{map[string]string{}}, float64(0), true},
		{"string", []interface{}{}, "error", true},
		{"string", []interface{}{1}, "1", false},
		{"duration", []interface{}{}, "error", true},
		{"duration", []interface{}{nil}, time.Duration(0), false},
		{"duration", []interface{}{false}, time.Duration(0), false},
		{"duration", []interface{}{int(1)}, time.Duration(1), false},
		{"duration", []interface{}{int8(1)}, time.Duration(1), false},
		{"duration", []interface{}{int16(1)}, time.Duration(1), false},
		{"duration", []interface{}{int32(1)}, time.Duration(1), false},
		{"duration", []interface{}{int64(1)}, time.Duration(1), false},
		{"duration", []interface{}{uint(1)}, time.Duration(1), false},
		{"duration", []interface{}{uint8(1)}, time.Duration(1), false},
		{"duration", []interface{}{uint16(1)}, time.Duration(1), false},
		{"duration", []interface{}{uint32(1)}, time.Duration(1), false},
		{"duration", []interface{}{uint64(1)}, time.Duration(1), false},
		{"duration", []interface{}{float32(1)}, time.Duration(1), false},
		{"duration", []interface{}{float64(1)}, time.Duration(1), false},
		{"duration", []interface{}{"1ns"}, time.Duration(1), false},
		{"duration", []interface{}{"z"}, time.Duration(0), true},
		{"duration", []interface{}{1 * time.Nanosecond}, time.Duration(1), false},
		{"duration", []interface{}{map[string]string{}}, time.Duration(0), true},
		{"parse_time", []interface{}{}, "error", true},
		{"parse_time", []interface{}{"2021-01-02 20:07:08", "2006-01"}, "error", true},
		{"parse_time", []interface{}{"2021-01-02 20:07:08", map[string]string{}}, "error", true},
		{"substring", []interface{}{}, "error", true},
		{"substring", []interface{}{1, 2}, "error", true},
		{"substring", []interface{}{"substring", 3, 9}, "string", false},
		{"substring", []interface{}{"substring", 10, 10}, "", false},
		{"substring", []interface{}{"substring", 2, 1}, "", false},
		{"tostring", []interface{}{}, "error", true},
		{"tostring", []interface{}{nil}, "", false},
		{"tostring", []interface{}{1}, "1", false},
		{"if", []interface{}{}, "error", true},
		{"if", []interface{}{true, 1, 2}, 1, false},
		{"if", []interface{}{false, 1, 2}, 2, false},
		{"if", []interface{}{"if", 1, 2}, "error", true},
		{"eq", []interface{}{}, "error", true},
		{"eq", []interface{}{"eq", "eq"}, true, false},
		{"eq", []interface{}{"eq", "neq"}, false, false},
		{"neq", []interface{}{}, "error", true},
		{"neq", []interface{}{"eq", "eq"}, false, false},
		{"neq", []interface{}{"eq", "neq"}, true, false},
		{"include", []interface{}{}, "error", true},
		{"include", []interface{}{"include", "in", "clude"}, false, false},
		{"include", []interface{}{"include", "in", "clude", "include"}, true, false},
		{"gt", []interface{}{}, "error", true},
		{"gt", []interface{}{"t", 1}, "error", true},
		{"gt", []interface{}{2, 1}, true, false},
		{"gte", []interface{}{}, "error", true},
		{"gte", []interface{}{"t", 1}, "error", true},
		{"gte", []interface{}{1, 1}, true, false},
		{"lt", []interface{}{}, "error", true},
		{"lt", []interface{}{2, 1}, false, false},
		{"lt", []interface{}{"t", 1}, "error", true},
		{"lte", []interface{}{}, "error", true},
		{"lte", []interface{}{1, 1}, true, false},
		{"lte", []interface{}{"t", 1}, "error", true},
		{"andf", []interface{}{}, "error", true},
		{"andf", []interface{}{true, true}, true, false},
		{"andf", []interface{}{true, false, true}, false, false},
		{"andf", []interface{}{"t", true}, "error", true},
		{"orf", []interface{}{}, "error", true},
		{"orf", []interface{}{true, false}, true, false},
		{"orf", []interface{}{false, false}, false, false},
		{"orf", []interface{}{"t", true}, "error", true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			buildInFunction := BuildInFunctions[tt.name]
			got, err := buildInFunction(ctx, tt.args...)
			if err != nil && tt.wantErr == true {
				return
			}
			if err != nil && tt.wantErr == false {
				t.Errorf("buildInFunction() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("buildInFunction() = %v, want %v", got, tt.want)
			}
		})
	}
}
