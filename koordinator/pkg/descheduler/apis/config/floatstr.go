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

package config

import (
	"encoding/json"
	"fmt"
	"strconv"
)

type Float64OrString struct {
	Type     Type
	FloatVal float64
	StrVal   string
}

// Type represents the stored type of Float64OrString.
type Type int64

const (
	Float  Type = iota // The Float64OrString holds a Float.
	String             // The Float64OrString holds a string.
)

// UnmarshalJSON implements the json.Unmarshaller interface.
func (floatstr *Float64OrString) UnmarshalJSON(value []byte) error {
	if value[0] == '"' {
		floatstr.Type = String
		return json.Unmarshal(value, &floatstr.StrVal)
	}
	floatstr.Type = Float
	return json.Unmarshal(value, &floatstr.FloatVal)
}

// String returns the string value, or the Itoa of the float value.
func (floatstr *Float64OrString) String() string {
	if floatstr == nil {
		return "<nil>"
	}
	if floatstr.Type == String {
		return floatstr.StrVal
	}
	return strconv.FormatFloat(floatstr.FloatVal, 'f', 3, 64)
}

// FloatValue returns the FloatVal if type Float, or if
// it is a String, will attempt a conversion to float64,
// returning 0 if a parsing error occurs.
func (floatstr *Float64OrString) FloatValue() float64 {
	if floatstr.Type == String {
		f, _ := strconv.ParseFloat(floatstr.StrVal, 64)
		return f
	}
	return floatstr.FloatVal
}

// MarshalJSON implements the json.Marshaller interface.
func (floatstr *Float64OrString) MarshalJSON() ([]byte, error) {
	switch floatstr.Type {
	case Float:
		return json.Marshal(floatstr.FloatVal)
	case String:
		return json.Marshal(floatstr.StrVal)
	default:
		return []byte{}, fmt.Errorf("impossible FloatValue.Type")
	}
}
