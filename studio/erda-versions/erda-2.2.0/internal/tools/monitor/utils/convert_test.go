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

package utils

import (
	"testing"
)

func TestTypeOf(t *testing.T) {
	var (
		bv      bool
		iv      int
		sv      string
		bytes   []byte
		slist   []string
		ilist   []int
		empty   interface{}
		unknown struct{}
	)
	if TypeOf(bv) != BoolType {
		t.Error("bool type error", TypeOf(bv))
	}
	if TypeOf(iv) != NumberType {
		t.Error("number type error", TypeOf(iv))
	}
	if TypeOf(sv) != StringType {
		t.Error("string type error", TypeOf(sv))
	}
	if TypeOf(bytes) != StringType {
		t.Error("[]byte type error", TypeOf(bytes))
	}
	if TypeOf(slist) != StringType {
		t.Error("[]string type error", TypeOf(slist))
	}
	if TypeOf(ilist) != NumberType {
		t.Error("[]int type error", TypeOf(ilist))
	}
	if TypeOf(empty) != "" {
		t.Error("nil type error", TypeOf(empty))
	}
	if TypeOf(unknown) != Unknown {
		t.Error("unknown type error", TypeOf(unknown))
	}
}
