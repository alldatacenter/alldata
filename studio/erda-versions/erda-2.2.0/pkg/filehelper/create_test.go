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
	"bytes"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCreateFile(t *testing.T) {
	err := CreateFile("/tmp/ci/test.sh", "echo hello\necho world\necho nice", 0700)
	require.NoError(t, err)
}

func TestCreateFile2(t *testing.T) {
	err := CreateFile2("/tmp/ci/test.sh", bytes.NewBufferString("ssssxfdsfs\nfs"), 0700)
	require.NoError(t, err)
}
