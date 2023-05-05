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

package apistructs

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_EnablePipelineVolume(t *testing.T) {
	var tables = []struct {
		StorageConfig StorageConfig
	}{
		{
			StorageConfig: StorageConfig{
				EnableNFS:   false,
				EnableLocal: true,
			},
		},
		{
			StorageConfig: StorageConfig{
				EnableNFS:   true,
				EnableLocal: false,
			},
		},
		{
			StorageConfig: StorageConfig{
				EnableNFS:   true,
				EnableLocal: false,
			},
		},
	}
	for _, data := range tables {
		if data.StorageConfig.EnableNFS {
			assert.True(t, data.StorageConfig.EnableNFSVolume(), "not true")
		} else {
			assert.True(t, !data.StorageConfig.EnableNFSVolume(), "not true")
		}
		if data.StorageConfig.EnableLocal {
			assert.True(t, data.StorageConfig.EnableShareVolume(), "not true")
		} else {
			assert.True(t, !data.StorageConfig.EnableShareVolume(), "not true")
		}
	}
}
