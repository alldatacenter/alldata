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
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestLockAndUnlockPath(t *testing.T) {
	pwd, err := os.Getwd()
	require.NoError(t, err)

	err = LockPath(pwd)
	require.NoError(t, err)

	isLocked := IsPathBeLocked(pwd)
	assert.Equal(t, true, isLocked)

	err = WaitingPathUnlock(pwd, 3)
	assert.Equal(t, false, err == nil)

	err = UnLockPath(pwd)
	require.NoError(t, err)

	isLocked = IsPathBeLocked(pwd)
	assert.Equal(t, false, isLocked)

	err = WaitingPathUnlock(pwd, 3)
	assert.NoError(t, err)
}

func Test_calculateNextCheckTimeDuration(t *testing.T) {
	tt := []struct {
		loopedTimes uint64
		want        string
	}{
		{
			loopedTimes: 0,
			want:        "100ms",
		},
		{
			loopedTimes: 1,
			want:        "150ms",
		},
		{
			loopedTimes: 2,
			want:        "225ms",
		},
		{
			loopedTimes: 3,
			want:        "337.5ms",
		},
		{
			loopedTimes: 4,
			want:        "506.25ms",
		},
		{
			loopedTimes: 5,
			want:        "759.375ms",
		},
		{
			loopedTimes: 6,
			want:        "1s",
		},
		{
			loopedTimes: 7,
			want:        "1s",
		},
		{
			loopedTimes: 8,
			want:        "1s",
		},
		{
			loopedTimes: 9,
			want:        "1s",
		},
	}
	for i := range tt {
		assert.Equal(t, tt[i].want, calculateNextCheckTimeDuration(tt[i].loopedTimes).String())
	}
}
