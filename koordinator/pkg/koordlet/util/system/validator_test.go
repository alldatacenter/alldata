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

	"github.com/stretchr/testify/assert"
)

func Test_RangeValidate(t *testing.T) {
	type args struct {
		name      string
		validator ResourceValidator
		value     string
		expect    bool
	}

	tests := []args{
		{
			name:      "test_validate_nil",
			validator: &RangeValidator{min: 0, max: 100},
			value:     "",
			expect:    false,
		},
		{
			name:      "test_validate_invalid",
			validator: &RangeValidator{min: 0, max: 100},
			value:     "120",
			expect:    false,
		},
		{
			name:      "test_validate_valid_min",
			validator: &RangeValidator{min: 0, max: 100},
			value:     "0",
			expect:    true,
		},
		{
			name:      "test_validate_valid_max",
			validator: &RangeValidator{min: 0, max: 100},
			value:     "100",
			expect:    true,
		},
		{
			name:      "test_validate_valid",
			validator: &RangeValidator{min: 0, max: 100},
			value:     "20",
			expect:    true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, _ := tt.validator.Validate(tt.value)
			assert.Equal(t, tt.expect, got)
		})
	}
}
