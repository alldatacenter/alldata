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

package storekit

import (
	"math"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestInMemoryRateLimiter_ReserveN(t *testing.T) {
	limit := 10
	im := NewInMemoryRateLimiter(RateLimitConfig{
		Duration: time.Second,
		Limit:    limit,
	})

	ass := assert.New(t)

	d := roundDelay(im.ReserveN(2 * limit))
	ass.Equal(time.Second, d)
	time.Sleep(d)           // wait delay
	time.Sleep(time.Second) // wait token full

	d = roundDelay(im.ReserveN(3 * limit))
	ass.Equal(time.Second*3, d)
	time.Sleep(d)
	time.Sleep(time.Second) // wait token full

	d = roundDelay(im.ReserveN(limit))
	ass.Equal(time.Duration(0), d)
}

// ignore small timing issue
func roundDelay(d time.Duration) time.Duration {
	return time.Second * time.Duration(math.Round(float64(d)/float64(time.Second)))
}
