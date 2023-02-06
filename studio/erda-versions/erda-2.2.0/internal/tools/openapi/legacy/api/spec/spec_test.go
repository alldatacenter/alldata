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

package spec

import (
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPathMatch(t *testing.T) {
	path := NewPath("/dice/<company>/<project>/aaaa")
	assert.True(t, path.Match("/dice/ccc/bbb/aaaa"))
	assert.False(t, path.Match("/dice/ccc//aaaa"))
	assert.False(t, path.Match("/dice/ccc/bbb/aaaaa"))
	assert.True(t, path.Match("dice/ccc/bbb/aaaa"))

	path = NewPath("ddee/<i>/<*>")
	assert.True(t, path.Match("/ddee/iii/iiff/fr"))
	assert.False(t, path.Match("/ddee/iii"))
	assert.False(t, path.Match("/ddee/iii/"))
	assert.False(t, path.Match("ddee//r"))

}

func TestPathVars(t *testing.T) {
	path := NewPath("dice/<company>/<project>/aaaa")
	vars := path.Vars("/dice/ccc/bbb/aaaa")
	assert.Equal(t, "ccc", vars["company"])
	assert.Equal(t, "bbb", vars["project"])

	path = NewPath("/<a>")
	vars = path.Vars("/aa/")
	assert.Equal(t, "aa", vars["a"])

	path = NewPath("/da/<a>/<*>")
	vars = path.Vars("/da/de/fr/ty")
	assert.Equal(t, "de", vars["a"])
	assert.Equal(t, "fr/ty", vars["*"])

	path = NewPath("/de/<*>")
	vars = path.Vars("/de/fr/tt/gg")
	assert.Equal(t, "fr/tt/gg", vars["*"])

}

func TestPathFill(t *testing.T) {
	path := NewPath("/dice/<company>/<project>/aaaa")
	r := path.Fill(map[string]string{
		"company": "ccc",
		"project": "ddd",
		"notused": "666",
	})
	assert.Equal(t, "/dice/ccc/ddd/aaaa", r)

}
func TestFind(t *testing.T) {
	apis := APIs{Spec{
		Path:   NewPath("/a/v/b"),
		Method: "GET",
		Scheme: HTTP,
	},
	}
	r, _ := http.NewRequest("GET", "http://127.0.0.1/a/v/b", nil)
	spec := apis.Find(r)
	assert.NotNil(t, spec)
	r2, _ := http.NewRequest("GET", "http://127.0.0.1/a/v/bb", nil)
	spec2 := apis.Find(r2)
	assert.Nil(t, spec2)

}

func TestFind2(t *testing.T) {
	apis := APIs{Spec{
		Path:   NewPath("/a/v/b"),
		Scheme: HTTP,
	}}
	r, _ := http.NewRequest("GET", "http://127.0.0.1/a/v/b", nil)
	spec := apis.Find(r)
	assert.NotNil(t, spec)
}

func TestPathMatch2(t *testing.T) {
	path := NewPath("/a/<a>/<a>")
	assert.True(t, path.Match("/a/d/v"))

	path = NewPath("/<a>")
	assert.False(t, path.Match("/"))

	path = NewPath("/<a>/<b>")
	assert.False(t, path.Match("/a/"))
	assert.True(t, path.Match("/a/bb"))

	path = NewPath("/<a>")
	assert.True(t, path.Match("/a/"))

}
