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

package util

import "testing"

func TestIsDeepEqual(t *testing.T) {
	mA := map[string]interface{}{
		"field1": "test",
	}
	mB := map[string]interface{}{
		"field1": "test",
		"field2": "test",
	}
	isEqual, err := IsDeepEqual(mA, mA)
	if err != nil {
		t.Fatal(err)
	}
	if !isEqual {
		t.Error("test failed, expected equal, actual not")
	}

	isEqual, err = IsDeepEqual(mA, mB)
	if err != nil {
		t.Fatal(err)
	}
	if isEqual {
		t.Error("test failed, expected not equal, actual equal")
	}
}
