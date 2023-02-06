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

package actionagent

import (
	"io/ioutil"
	"os"
	"testing"

	"bou.ke/monkey"
	"github.com/c2h5oh/datasize"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/pipeline/actionagent/agenttool"
)

func TestStoreAndRestore(t *testing.T) {
	tmpCacheDir := "/tmp"
	tmpCachePrefix := "tmp"
	tmpDir, err := ioutil.TempDir(tmpCacheDir, tmpCachePrefix)
	assert.NoError(t, err)
	defer os.RemoveAll(tmpDir)
	agent := Agent{MaxCacheFileSizeMB: 1 * datasize.MB}
	tarFile := "/tmp/abc.tar"
	err = agent.storeCache(tarFile, tmpDir)
	assert.NoError(t, err)
	err = agent.restoreCache(tarFile, tmpDir)
	assert.NoError(t, err)
	defer os.Remove(tarFile)
}

func Test_isCachePathExceedLimit(t *testing.T) {
	pm1 := monkey.Patch(agenttool.GetDiskSize, func(path string) (datasize.ByteSize, error) {
		return 1048577 * datasize.B, nil
	})
	defer pm1.Unpatch()

	a := Agent{MaxCacheFileSizeMB: 1 * datasize.MB}
	size, isExceed := a.isCachePathExceedLimit("./erda")
	assert.Equal(t, true, isExceed)
	assert.Equal(t, uint64(1048577), size.Bytes())
}
