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
	"reflect"
	"testing"
	"time"
)

type testItem struct {
	a, b    interface{}
	op      Operator
	reverse bool
	r       interface{}
	err     error
}

var now = time.Now()

var testList = []*testItem{
	{
		a:       1,
		op:      ADD,
		b:       2,
		reverse: true,
		r:       int64(3),
	},
	{
		a:       float32(1),
		op:      ADD,
		b:       2,
		reverse: true,
		r:       float64(3),
	},
	{
		a:       uint32(1),
		op:      ADD,
		b:       2,
		reverse: true,
		r:       uint64(3),
	},
	{
		a:       true,
		op:      ADD,
		b:       uint64(2),
		reverse: true,
		r:       uint64(3),
	},
	{
		a:       time.Second,
		op:      ADD,
		b:       int64(time.Second),
		reverse: true,
		r:       time.Duration(2 * time.Second),
	},
	{
		a:       now,
		op:      ADD,
		b:       time.Hour,
		reverse: true,
		r:       now.Add(time.Hour),
	},
	{
		a:       now,
		op:      ADD,
		b:       int64(time.Hour),
		reverse: true,
		r:       now.Add(time.Hour),
	},
	{
		a:       int64(time.Hour),
		op:      ADD,
		b:       now,
		reverse: true,
		r:       now.Add(time.Hour),
	},
	{
		a:  now.Add(time.Hour),
		op: SUB,
		b:  now,
		r:  time.Hour,
	},
	{
		a:       2,
		op:      MUL,
		b:       time.Hour,
		reverse: true,
		r:       2 * time.Hour,
	},
	{
		a:       "2",
		op:      MUL,
		b:       time.Hour,
		reverse: true,
		r:       2 * time.Hour,
	},
	{
		a:       "2",
		op:      MUL,
		b:       2,
		reverse: true,
		r:       float64(4),
	},
}

func TestParseTree_operateValues(t *testing.T) {
	for _, item := range testList {
		if !checkTestItem(t, item.a, item.op, item.b, item.r, item.err) {
			return
		}
		if item.reverse {
			if !checkTestItem(t, item.b, item.op, item.a, item.r, item.err) {
				return
			}
		}
	}
}

func checkTestItem(t *testing.T, a interface{}, op Operator, b interface{}, ret interface{}, e error) bool {
	r, err := OperateValues(a, op, b)
	if err != nil {
		if e == nil || err.Error() != e.Error() {
			t.Fatalf("%v %s %v -> unexpected error: %s", a, op, b, err)
		}
		return false
	}
	if reflect.TypeOf(r) != reflect.TypeOf(ret) {
		t.Fatalf("%v %s %v -> unexpected type: %s", a, op, b, reflect.TypeOf(r))
		return false
	}
	if r != ret {
		t.Fatalf("%v %s %v -> unexpected value: %s", a, op, b, r)
		return false
	}
	return true
}
