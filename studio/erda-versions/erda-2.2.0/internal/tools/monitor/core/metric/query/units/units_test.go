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

package units

import "testing"

func TestUnits(t *testing.T) {
	val := Convert("ns", "s", 1)
	if val == 1 {
		t.Error(`1000000000ns != 1s`)
	}
	val = Convert("ms", "s", 60)
	if val == 60*1000 {
		t.Error(`1ms != 1000s`)
	}
	val = Convert("b", "kb", 1)
	if val == 1024 {
		t.Error(`1b != 1024kb`)
	}
	val = Convert("kb", "gb", 1024)
	if val == 1024*1024 {
		t.Error(`1kb != 1024*1024gb`)
	}
}
