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

package extension

import (
	"io/ioutil"
	"os"
	"path"
	"testing"

	"github.com/alecthomas/assert"
)

func Test_action_isThereSpecFile(t *testing.T) {
	dir, err := ioutil.TempDir(os.TempDir(), "*")
	defer os.RemoveAll(dir)
	assert.NoError(t, err)

	_, v := isThereSpecFile(dir)
	assert.Equal(t, v, false)

	os.Create(path.Join(dir, "spec.yaml"))
	assert.NoError(t, err)
	_, v = isThereSpecFile(dir)
	assert.Equal(t, v, true)
}

func Test_action_LoadExtensions(t *testing.T) {
	dir := path.Join(os.TempDir(), "extension")
	dir1 := path.Join(dir, "test1", "actions", "one")
	dir2 := path.Join(dir, "test2", "actions", "two")

	err := os.MkdirAll(dir1, os.ModePerm)
	assert.NoError(t, err)
	err = os.MkdirAll(dir2, os.ModePerm)
	assert.NoError(t, err)
	defer func() {
		err := os.RemoveAll(dir)
		assert.NoError(t, err)
	}()

	f, err := os.Create(path.Join(dir1, "spec.yaml"))
	assert.NoError(t, err)
	specYml := `name: api-publish
version: "1.0"
type: action`
	_, err = f.Write([]byte(specYml))
	assert.NoError(t, err)

	tests := &Repo{
		addr:     dir,
		versions: []string{dir1},
	}

	r := LoadExtensions(dir)
	assert.Equal(t, tests, r)
}
