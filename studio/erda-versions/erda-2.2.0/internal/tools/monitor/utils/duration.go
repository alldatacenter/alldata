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

package utils

import (
	"fmt"
	"time"
)

type Duration time.Duration

func (d *Duration) UnmarshalYAML(unmarshal func(interface{}) error) error {
	var text string
	err := unmarshal(&text)
	if err != nil {
		return err
	}
	duration, err := time.ParseDuration(text)
	if err != nil {
		return err
	}
	*d = Duration(duration)
	return nil
}

func (d Duration) MarshalYAML() (interface{}, error) {
	return fmt.Sprint(time.Duration(d)), nil
}

func (d Duration) Duration() time.Duration {
	return time.Duration(d)
}

// Keep the specified decimal places.
func (d Duration) FormatDuration(precision int) (string, error) {
	if precision < 0 {
		return "", fmt.Errorf("invalid precision: %v", precision)
	}
	val, base := int64(d), int64(time.Nanosecond)
	switch {
	case val <= int64(time.Microsecond):
		return d.Duration().String(), nil
	case val <= int64(time.Millisecond):
		base = int64(time.Microsecond)
	case val <= int64(time.Second):
		base = int64(time.Millisecond)
	default:
		base = int64(time.Second)
	}
	for i := 0; i < precision; i++ {
		base /= 10
	}
	if base > 1 {
		if (val % base) >= (base / 2) {
			return time.Duration((val/base + 1) * base).String(), nil
		}
		return time.Duration(val / base * base).String(), nil
	}
	return d.Duration().String(), nil
}
