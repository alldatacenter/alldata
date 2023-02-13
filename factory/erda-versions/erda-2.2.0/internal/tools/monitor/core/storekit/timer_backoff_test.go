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
	"testing"
	"time"
)

func TestMultiplierBackoff(t *testing.T) {
	tests := []struct {
		name    string
		backoff *MultiplierBackoff
		count   int
		want    time.Duration
	}{
		{
			backoff: &MultiplierBackoff{
				Base:     1 * time.Second,
				Duration: 1 * time.Second,
				Max:      60 * time.Second,
				Factor:   2,
			},
			count: 1,
			want:  2 * time.Second,
		},
		{
			backoff: &MultiplierBackoff{
				Base:     1 * time.Second,
				Duration: 1 * time.Second,
				Max:      60 * time.Second,
				Factor:   2,
			},
			count: 4,
			want:  16 * time.Second,
		},
		{
			backoff: &MultiplierBackoff{
				Base:     1 * time.Second,
				Duration: 1 * time.Second,
				Max:      60 * time.Second,
				Factor:   2,
			},
			count: 100,
			want:  60 * time.Second,
		},
		{
			backoff: &MultiplierBackoff{
				Base:     1 * time.Second,
				Duration: 1 * time.Second,
				Max:      60 * time.Second,
				Factor:   1,
			},
			count: 100,
			want:  1 * time.Second,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			backoff := *tt.backoff
			backoff.Reset()
			for i := 0; i < tt.count; i++ {
				backoff.Wait()
			}
			if backoff.Duration != tt.want {
				t.Errorf("MultiplierBackoff.Wait() * %d, got duration = %v, want %v", tt.count, backoff.Duration, tt.want)
			}
			backoff.Reset()
			if backoff.Duration != tt.backoff.Duration {
				t.Errorf("MultiplierBackoff.Reset(), got duration = %v, want %v", backoff.Duration, tt.backoff.Duration)
			}
		})
	}
}
