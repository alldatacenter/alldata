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

package jsonmap

import (
	"bytes"
	"database/sql/driver"
	"encoding/json"

	"github.com/pkg/errors"
)

type JSONMap map[string]interface{}

// Scanner 接口实现方法
func (m *JSONMap) Scan(value interface{}) error {
	if value == nil {
		*m = make(map[string]interface{})
		return nil
	}

	var bytes []byte
	convertBytes(value, &bytes)
	if err := json.Unmarshal(bytes, &m); err != nil {
		return err
	}
	return nil
}

// driver Valuer 接口实现方法
func (m JSONMap) Value() (driver.Value, error) {
	if m == nil {
		m = make(map[string]interface{})
	}

	bytes, err := json.Marshal(m)
	if err != nil {
		return nil, err
	}
	return bytes, nil
}

type NullMap struct {
	Map   map[string]interface{}
	Valid bool
}

// Scanner 接口实现方法
func (nm *NullMap) Scan(value interface{}) error {
	if value == nil {
		nm.Map, nm.Valid = nil, false
		return nil
	}

	var bytes []byte
	convertBytes(value, &bytes)
	if err := json.Unmarshal(bytes, &nm.Map); err != nil {
		return err
	}
	nm.Valid = true
	return nil
}

// driver Valuer 接口实现方法
func (nm NullMap) Value() (driver.Value, error) {
	if !nm.Valid {
		return nil, nil
	}
	return nm.Map, nil
}

// 转换为bytes
func convertBytes(src interface{}, dest *[]byte) error {
	if dest == nil {
		return errors.New("convert bytes err: dest is nil")
	}

	switch s := src.(type) {
	case string:
		*dest = []byte(s)
	case []byte:
		*dest = cloneBytes(s)
	case nil:
		*dest = nil
	default:
		*dest = nil
	}
	return nil
}

// 拷贝bytes
func cloneBytes(b []byte) []byte {
	if b == nil {
		return nil
	}
	c := make([]byte, len(b))
	copy(c, b)
	return c
}

func IsJsonArray(b []byte) bool {
	x := bytes.TrimLeft(b, " \t\r\n")
	return len(x) > 0 && x[0] == '['
}
