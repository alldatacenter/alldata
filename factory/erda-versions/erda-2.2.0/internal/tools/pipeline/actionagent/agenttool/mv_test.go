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

package agenttool

import (
	"io/ioutil"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMv(t *testing.T) {
	tmpDir := "/tmp"
	tmpPrefix := "tmp"
	sourceDir, err := ioutil.TempDir(tmpDir, tmpPrefix)
	assert.NoError(t, err)
	defer os.RemoveAll(sourceDir)
	destDir, err := ioutil.TempDir(tmpDir, tmpPrefix)
	assert.NoError(t, err)
	defer os.RemoveAll(destDir)
	err = Mv(sourceDir, destDir)
	assert.NoError(t, err)
}
