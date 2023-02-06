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

package pbutil

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestGetBool(t *testing.T) {
	// nil
	vv, set := GetBool(nil)
	assert.False(t, vv)
	assert.False(t, set)

	// *true
	vv, set = GetBool(&[]bool{true}[0])
	assert.True(t, vv)
	assert.True(t, set)

	// *false
	vv, set = GetBool(&[]bool{false}[0])
	assert.False(t, vv)
	assert.True(t, set)
}

func TestMustGetBool(t *testing.T) {
	// nil
	vv := MustGetBool(nil)
	assert.False(t, vv)

	// *true
	vv = MustGetBool(&[]bool{true}[0])
	assert.True(t, vv)

	// *false
	vv = MustGetBool(&[]bool{false}[0])
	assert.False(t, vv)
}

func TestGetUint64(t *testing.T) {
	// nil
	vv, set := GetUint64(nil)
	assert.Equal(t, uint64(0), vv)
	assert.False(t, set)

	// 1
	vv, set = GetUint64(&[]uint64{1}[0])
	assert.Equal(t, uint64(1), vv)
	assert.True(t, set)
}

func TestMustGetUint64(t *testing.T) {
	// nil
	vv := MustGetUint64(nil)
	assert.Equal(t, uint64(0), vv)

	// 1
	vv = MustGetUint64(&[]uint64{1}[0])
	assert.Equal(t, uint64(1), vv)
}

func TestGetTimestamp(t *testing.T) {
	// nil *time
	pbt := GetTimestamp(nil)
	assert.Nil(t, pbt)

	// now
	now := time.Now()
	pbt = GetTimestamp(&now)
	assert.NotNil(t, pbt)
	assert.True(t, pbt.AsTime().Equal(now))
}
