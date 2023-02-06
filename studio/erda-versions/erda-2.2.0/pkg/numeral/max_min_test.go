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

func TestMinFloat64(t *testing.T) {
	min := MinFloat64([]float64{1, 2, 3, 4, 5, 0, 0})
	assert.Equal(t, min, float64(0))

	min = MinFloat64([]float64{1, 2, 3, 4, 5, 0, 0}, true)
	assert.Equal(t, min, float64(1))

	min = MinFloat64([]float64{-1.1, -2.3, 0, 1.2, 100})
	assert.Equal(t, min, -2.3)

	min = MinFloat64([]float64{0, 1, 2, 3, 4}, true)
	assert.Equal(t, min, float64(1))
}

func TestMaxFloat64(t *testing.T) {
	max := MaxFloat64([]float64{1, 2, 3, 4, 5})
	assert.Equal(t, max, float64(5))

	max = MaxFloat64([]float64{-2, -3, -100, -0.5, 0})
	assert.Equal(t, max, float64(0))
}
