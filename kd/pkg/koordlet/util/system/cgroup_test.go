/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package system

import (
	"testing"
)

func TestCalcCPUThrottledRatio(t *testing.T) {
	type args struct {
		curPoint *CPUStatRaw
		prePoint *CPUStatRaw
	}
	tests := []struct {
		name string
		args args
		want float64
	}{
		{
			name: "calculate-throttled-ratio",
			args: args{
				curPoint: &CPUStatRaw{
					NrPeriods:            200,
					NrThrottled:          40,
					ThrottledNanoSeconds: 40000,
				},
				prePoint: &CPUStatRaw{
					NrPeriods:            100,
					NrThrottled:          20,
					ThrottledNanoSeconds: 20000,
				},
			},
			want: 0.2,
		},
		{
			name: "calculate-throttled-ratio-zero-period",
			args: args{
				curPoint: &CPUStatRaw{
					NrPeriods:            100,
					NrThrottled:          20,
					ThrottledNanoSeconds: 20000,
				},
				prePoint: &CPUStatRaw{
					NrPeriods:            100,
					NrThrottled:          20,
					ThrottledNanoSeconds: 20000,
				},
			},
			want: 0,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := CalcCPUThrottledRatio(tt.args.curPoint, tt.args.prePoint); got != tt.want {
				t.Errorf("CalcCPUThrottledRatio() = %v, want %v", got, tt.want)
			}
		})
	}
}
