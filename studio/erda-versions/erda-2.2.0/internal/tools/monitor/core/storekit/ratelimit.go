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
	"time"

	"golang.org/x/time/rate"
)

type RateLimiter interface {
	// pass n token to limiter, return the duration you must wait
	ReserveN(n int) time.Duration
}

type RateLimitConfig struct {
	Duration time.Duration `file:"duration"`
	Limit    int           `file:"limit"`
}

type InMemoryRateLimiter struct {
	limiter *rate.Limiter
}

func NewInMemoryRateLimiter(cfg RateLimitConfig) *InMemoryRateLimiter {
	limit := cfg.Limit / int(cfg.Duration.Seconds())
	return &InMemoryRateLimiter{limiter: rate.NewLimiter(rate.Limit(limit), limit)}
}

func (im *InMemoryRateLimiter) ReserveN(n int) time.Duration {
	now := time.Now()
	r := im.limiter.ReserveN(now, n)
	// if n > burst, then split n to multi b and get the total delay
	if !r.OK() {
		b, d := im.limiter.Burst(), time.Duration(0)
		for n > 0 {
			tmp := im.limiter.ReserveN(now, b)
			if !tmp.OK() {
				panic("sub reserve is not OK")
			}
			d += tmp.Delay()
			n -= b
		}
		return d
	}
	return r.Delay()
}
