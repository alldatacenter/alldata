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

import "time"

// TimerBackoff .
type TimerBackoff interface {
	Reset()
	Wait() <-chan time.Time
}

// MultiplierBackoff .
type MultiplierBackoff struct {
	Base     time.Duration
	Max      time.Duration
	Duration time.Duration
	Factor   float64
}

var _ TimerBackoff = (*MultiplierBackoff)(nil)

// NewMultiplierBackoff
func NewMultiplierBackoff() *MultiplierBackoff {
	return &MultiplierBackoff{
		Base:     1 * time.Second,
		Duration: 1 * time.Second,
		Max:      60 * time.Second,
		Factor:   2,
	}
}

func (b *MultiplierBackoff) Reset() {
	b.Duration = b.Base
}

func (b *MultiplierBackoff) Wait() <-chan time.Time {
	ch := time.After(b.Duration)
	if b.Duration < b.Max {
		b.Duration = time.Duration(float64(b.Duration) * b.Factor)
	}
	if b.Duration > b.Max {
		b.Duration = b.Max
	}
	return ch
}
