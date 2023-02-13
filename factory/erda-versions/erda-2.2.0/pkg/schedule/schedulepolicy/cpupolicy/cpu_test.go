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
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestAdjustCPUSize(t *testing.T) {
	ks := []float64{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.5, 2.9, 3.0}
	vs := []float64{0.2, 0.3, 0.5, 0.6, 0.7, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.1, 2.2, 2.6, 3.0, 3.1}

	var v float64
	for i, k := range ks {
		v = AdjustCPUSize(k)
		assert.Equal(t, vs[i], v)
	}
}

func TestString(t *testing.T) {
	v, err := strconv.ParseFloat(fmt.Sprintf("%.1f", 2.67), 64)
	assert.Nil(t, err)
	assert.Equal(t, 2.7, v)
}

func TestCalcCPUSubscribeRatio(t *testing.T) {
	// The map contains the CPU_SUBSCRIBE_RATIO configuratio
	extra := map[string]string{
		"CPU_SUBSCRIBE_RATIO": "2.5",
	}
	v := CalcCPUSubscribeRatio(2.0, extra)
	assert.Equal(t, v, 2.5)

	// empty map
	extra2 := map[string]string{}
	v = CalcCPUSubscribeRatio(2.0, extra2)
	assert.Equal(t, v, 2.0)

	// map Does not contain CPU_SUBSCRIBE_RATIO configuration
	extra3 := map[string]string{
		"CPU_XX": "10",
	}
	v = CalcCPUSubscribeRatio(3.0, extra3)
	assert.Equal(t, v, 3.0)

	// The oversold ratio in the cluster configuration is less than 1, which is not a reasonable value
	v = CalcCPUSubscribeRatio(0.5, extra3)
	assert.Equal(t, v, 1.0)

	// The CPU_SUBSCRIBE_RATIO configuration in the map is unreasonable
	extra4 := map[string]string{
		"CPU_SUBSCRIBE_RATIO": "0.8",
	}
	v = CalcCPUSubscribeRatio(1.0, extra4)
	assert.Equal(t, v, 1.0)
}
