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

package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRound(t *testing.T) {
	assert.Equal(t, 4.21, Round(4.213213, 2))
	assert.Equal(t, 4.22, Round(4.219213, 2))
}

func TestIsValidK8sSvcName(t *testing.T) {
	assert.True(t, IsValidK8sSvcName("sdf-sdfb---wqfqw-wqfe"))
	assert.True(t, IsValidK8sSvcName("12sdf-sdfb---wqfqw-wqfe213"))
	assert.False(t, IsValidK8sSvcName("sdf-sdfB---wqfqw-wqfe"))
	assert.False(t, IsValidK8sSvcName("sdf-sdfB---wqfqw-wqfe-"))
	assert.False(t, IsValidK8sSvcName(`cmdbcmdbcmdbcmdbcmdbcmdbcmdbcmdbcmdbcmdbcmdbccmdbcmdbcmdb
		cmdbcmdbcmdbcmdbcmdbcmdbcmdbmdbcmdbcmdbcmdbcmdbcmdbcmdbcmdbcmdbcmdb`))
}
