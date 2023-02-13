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

package filehelper

import (
	"testing"

	"github.com/magiconair/properties/assert"
)

func TestAbs2Rel(t *testing.T) {
	path1 := "/"
	path1 = Abs2Rel(path1)
	assert.Equal(t, ".", path1)

	path2 := "//testdata/"
	path2 = Abs2Rel(path2)
	assert.Equal(t, "testdata", path2)
}

func TestAPIFileUrlRetriever(t *testing.T) {
	path1 := "https://test.io/api/files/xed"
	path1 = APIFileUrlRetriever(path1)
	assert.Equal(t, "/api/files/xed", path1)
	path2 := "api/files/xxx"
	path2 = APIFileUrlRetriever(path2)
	assert.Equal(t, "api/files/xxx", path2)
	path3 := "https://test.io/erda/api/files/xxx"
	path3 = APIFileUrlRetriever(path3)
	assert.Equal(t, "https://test.io/erda/api/files/xxx", path3)
}

func TestFilterAPIFileUrl(t *testing.T) {
	path1 := "(dasd)![a.png(94.56 KB)](https://test.io/api/files/xed)![b.png(94.56 KB)](https://test.io/api/files/xe2)"
	path1 = FilterAPIFileUrl(path1)
	assert.Equal(t, "(dasd)![a.png(94.56 KB)](/api/files/xed)![b.png(94.56 KB)](/api/files/xe2)", path1)
	path2 := "(/api/files/xev)"
	path2 = FilterAPIFileUrl(path2)
	assert.Equal(t, "(/api/files/xev)", path2)
}
