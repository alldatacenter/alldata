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

//import (
//	"io/ioutil"
//	"os"
//	"testing"
//
//	"gopkg.in/stretchr/testify.v1/assert"
//)
//
//func TestAppend(t *testing.T) {
//	tmpDir := os.TempDir()
//	f, err := ioutil.TempFile(tmpDir, "")
//	assert.NoError(t, err)
//	defer func() {
//		f.Close()
//		os.RemoveAll(tmpDir)
//	}()
//	assert.NoError(t, Append(f.Name(), "action meta: hello=world\n"))
//	assert.NoError(t, Append(f.Name(), "action meta: key=value\n"))
//	b, err := ioutil.ReadAll(f)
//	assert.NoError(t, err)
//	t.Log("write: ", string(b))
//}
