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

package loop

import (
	"math"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestLoop_CalculateInterval(t *testing.T) {
	// 0, 1, 2, 4, 8, 10, 10
	l := New(WithMaxTimes(math.MaxUint64), WithDeclineLimit(10*time.Second), WithDeclineRatio(2))

	interval := l.CalculateInterval(0)
	assert.Equal(t, time.Second*0, interval)

	interval = l.CalculateInterval(1)
	assert.Equal(t, time.Second*1, interval)

	interval = l.CalculateInterval(2)
	assert.Equal(t, time.Second*2, interval)

	interval = l.CalculateInterval(3)
	assert.Equal(t, time.Second*4, interval)

	interval = l.CalculateInterval(4)
	assert.Equal(t, time.Second*8, interval)

	interval = l.CalculateInterval(5)
	assert.Equal(t, time.Second*10, interval)

	interval = l.CalculateInterval(6)
	assert.Equal(t, time.Second*10, interval)
}
