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
	"fmt"
	"math"
	"strconv"

	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

// ResourceValidator validates the resource value
type ResourceValidator interface {
	Validate(value string) (isValid bool, msg string)
}

type RangeValidator struct {
	max int64
	min int64
}

func (r *RangeValidator) Validate(value string) (bool, string) {
	if value == "" {
		return false, fmt.Sprintf("value is nil")
	}
	var v int64
	var err error
	if value == CgroupMaxSymbolStr { // compatible to cgroup-v2 file valued "max"
		v = math.MaxInt64
	} else {
		v, err = strconv.ParseInt(value, 10, 64)
		if err != nil {
			return false, fmt.Sprintf("value %v is not an integer, err: %v", value, err)
		}
	}
	if v < r.min || v > r.max {
		return false, fmt.Sprintf("value %v is not in [min:%d, max:%d]", value, r.min, r.max)
	}
	return true, ""
}

type CPUSetStrValidator struct{}

func (c *CPUSetStrValidator) Validate(value string) (bool, string) {
	_, err := cpuset.Parse(value)
	if err != nil {
		return false, fmt.Sprintf("value %v is not valid cpuset string", value)
	}
	return true, ""
}
