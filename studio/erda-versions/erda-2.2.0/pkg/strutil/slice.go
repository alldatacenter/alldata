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

package strutil

import (
	"reflect"
)

// ReverseSlice reverses the slice s in place with any type
func ReverseSlice(s interface{}) {
	if reflect.TypeOf(s).Kind() != reflect.Slice {
		return
	}
	n := reflect.ValueOf(s).Len()
	swap := reflect.Swapper(s)
	for i, j := 0, n-1; i < j; i, j = i+1, j-1 {
		swap(i, j)
	}
}

func ReverseString(s string) string {
	var n = len(s)
	if n <= 1 {
		return s
	}
	var b = make([]byte, n)
	for i, j := 0, n-1; i < j; i, j = i+1, j-1 {
		b[i], b[j] = s[j], s[i]
	}
	return string(b)
}

func DedupAnySlice(s interface{}, uniq func(i int) interface{}) interface{} {
	in := reflect.ValueOf(s)
	if in.Kind() != reflect.Slice && in.Kind() != reflect.Array {
		return s
	}
	var (
		dup = make(map[interface{}]struct{})
		out = reflect.MakeSlice(in.Type(), 0, in.Len())
	)
	for i := 0; i < in.Len(); i++ {
		v := uniq(i)
		if _, ok := dup[v]; !ok {
			out = reflect.Append(out, in.Index(i))
			dup[v] = struct{}{}
		}
	}
	return out.Interface()
}
