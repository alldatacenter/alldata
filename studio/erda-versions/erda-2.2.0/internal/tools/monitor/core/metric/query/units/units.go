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

package units

import (
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

// Converter .
type Converter interface {
	Valid(unit string) bool
	Convert(from, to string, value float64) (float64, bool)
}

var converts = []Converter{
	&TimeConverter{
		BaseConverter{
			Default: "ns",
			Units: map[string]float64{
				"ns":  float64(time.Nanosecond),
				"ms":  float64(time.Millisecond),
				"s":   float64(time.Second),
				"min": float64(time.Minute),
				"h":   float64(time.Hour),
				"d":   24 * float64(time.Hour),
			},
		},
	},
	&ByteConverter{
		BaseConverter{
			Default: "b",
			Units: map[string]float64{
				"b":  1,
				"B":  1,
				"kb": 1 * 1024,
				"KB": 1 * 1024,
				"mb": 1 * 1024 * 1024,
				"MB": 1 * 1024 * 1024,
				"gb": 1 * 1024 * 1024 * 1024,
				"GB": 1 * 1024 * 1024 * 1024,
				"tb": 1 * 1024 * 1024 * 1024 * 1024,
				"TB": 1 * 1024 * 1024 * 1024 * 1024,
			},
		},
	},
}

// BaseConverter .
type BaseConverter struct {
	Default string
	Units   map[string]float64
}

// Convert .
func (c *BaseConverter) Convert(from, to string, value float64) (float64, bool) {
	if from == "" {
		from = c.Default
	}
	base, ok := c.Units[from]
	if !ok {
		return value, false
	}
	val := value * base
	base, ok = c.Units[to]
	if !ok {
		return value, false
	}
	return val / base, true
}

// Valid .
func (c *BaseConverter) Valid(unit string) bool {
	_, ok := c.Units[unit]
	return ok
}

// TimeConverter .
type TimeConverter struct {
	BaseConverter
}

// ByteConverter .
type ByteConverter struct {
	BaseConverter
}

// Convert .
func Convert(from, to string, data interface{}) interface{} {
	val, ok := utils.ConvertFloat64(data)
	if !ok {
		return data
	}
	for _, c := range converts {
		if c.Valid(to) {
			result, ok := c.Convert(from, to, val)
			if !ok {
				return data
			}
			return result
		}
	}
	return data
}
