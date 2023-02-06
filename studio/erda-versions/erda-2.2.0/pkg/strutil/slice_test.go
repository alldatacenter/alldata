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

package strutil_test

import (
	"testing"

	"github.com/erda-project/erda/pkg/strutil"
)

func TestReverseSlice(t *testing.T) {
	var cases = [][]int{
		nil,
		{},
		{1},
		{2, 3},
		{4, 9, 2},
	}
	var results = [][]int{
		nil,
		{},
		{1},
		{3, 2},
		{2, 9, 4},
	}

	equal := func(a, b []int) bool {
		if len(a) != len(b) {
			return false
		}
		for i, v := range a {
			if v != b[i] {
				return false
			}
		}
		return true
	}

	for i, case_ := range cases {
		strutil.ReverseSlice(case_)
		if !equal(case_, results[i]) {
			t.Fatal(i, case_, "fails")
		}
	}

	strutil.ReverseSlice([2]int{0, 2})
	strutil.ReverseSlice("a")
}

func TestDedupAnySlice(t *testing.T) {
	var (
		a = []int{2, 3, 4, 6, 7, 8, 9, 7, 3, 6}
		b = []string{"json", "yaml", "txt", "yaml"}
		c = []struct {
			Name string
			Age  int
		}{{Name: "dspo", Age: 13}, {Name: "cmc", Age: 14}, {Name: "yuxiaoer", Age: 15}, {Name: "cmc", Age: 14}}
	)
	a = strutil.DedupAnySlice(a, func(i int) interface{} {
		return a[i]
	}).([]int)
	b = strutil.DedupAnySlice(b, func(i int) interface{} {
		return b[i]
	}).([]string)
	c = strutil.DedupAnySlice(c, func(i int) interface{} {
		return c[i].Name
	}).([]struct {
		Name string
		Age  int
	})
	t.Log(a)
	t.Log(b)
	t.Log(c)
}

func TestReverseString(t *testing.T) {
	var (
		s1 = "desrever si siht"
		s2 = "a"
	)
	if reversed := strutil.ReverseString(s1); reversed != "this is reversed" {
		t.Error("error")
	}
	if reversed := strutil.ReverseString(s2); reversed != s2 {
		t.Error("error")
	}
}
