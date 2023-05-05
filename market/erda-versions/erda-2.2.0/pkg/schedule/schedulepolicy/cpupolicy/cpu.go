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

package cpupolicy

import (
	"fmt"
	"math"
	"strconv"
)

// Application formula round(x+1.5**(-3.6*x)*1.6*x,1)
func AdjustCPUSize(origin float64) float64 {
	value := origin + math.Pow(1.5, -3.0*origin)*0.9*origin
	value_, _ := strconv.ParseFloat(fmt.Sprintf("%.1f", value), 64)
	return value_
}

func CalcCPUSubscribeRatio(cpuSubscribeRatio float64, extra map[string]string) float64 {
	if ratio_, ok := extra["CPU_SUBSCRIBE_RATIO"]; ok && len(ratio_) > 0 {
		if ratio, err := strconv.ParseFloat(ratio_, 64); err == nil && ratio > 1.0 {
			return ratio
		}
	}
	if cpuSubscribeRatio > 1.0 {
		return cpuSubscribeRatio
	}
	return 1.0
}
