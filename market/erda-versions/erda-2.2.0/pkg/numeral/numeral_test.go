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

package numeral

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestFormalizeUnitToByte(t *testing.T) {
	ret, err := FormalizeUnitToByte("128974848")
	assert.NoError(t, err)
	assert.Equal(t, int64(128974848), ret)

	ret, err = FormalizeUnitToByte("129K")
	assert.NoError(t, err)
	assert.Equal(t, int64(129000), ret)

	ret, err = FormalizeUnitToByte("123Ki")
	assert.NoError(t, err)
	assert.Equal(t, int64(125952), ret)

	ret, err = FormalizeUnitToByte("129M")
	assert.NoError(t, err)
	assert.Equal(t, int64(129000000), ret)

	ret, err = FormalizeUnitToByte("123Mi")
	assert.NoError(t, err)
	assert.Equal(t, int64(128974848), ret)

	ret, err = FormalizeUnitToByte("129G")
	assert.NoError(t, err)
	assert.Equal(t, int64(129000000000), ret)

	ret, err = FormalizeUnitToByte("123Gi")
	assert.NoError(t, err)
	assert.Equal(t, int64(132070244352), ret)

	ret, err = FormalizeUnitToByte("129T")
	assert.NoError(t, err)
	assert.Equal(t, int64(129000000000000), ret)

	ret, err = FormalizeUnitToByte("123Ti")
	assert.NoError(t, err)
	assert.Equal(t, int64(135239930216448), ret)

	ret, err = FormalizeUnitToByte("129P")
	assert.NoError(t, err)
	assert.Equal(t, int64(129000000000000000), ret)

	ret, err = FormalizeUnitToByte("123Pi")
	assert.NoError(t, err)
	assert.Equal(t, int64(138485688541642752), ret)

	ret, err = FormalizeUnitToByte("1E")
	assert.NoError(t, err)
	assert.Equal(t, int64(1000000000000000000), ret)

	ret, err = FormalizeUnitToByte("1Ei")
	assert.NoError(t, err)
	assert.Equal(t, int64(1152921504606846976), ret)
}
