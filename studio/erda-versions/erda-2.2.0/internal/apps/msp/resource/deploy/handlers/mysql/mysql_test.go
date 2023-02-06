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

package mysql

import "testing"

func TestTryReadFile(t *testing.T) {
	p := &provider{}
	sql, err := p.tryReadFile("file://tmc/nacos.tar.gz")
	if err != nil {
		t.Errorf("with exists filepath, should not return error")
	}
	if len(sql) == 0 {
		t.Errorf("with exists filepath, should not return empty content")
	}
}
