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

package syncmap

import (
	"encoding/json"
	"sync"

	"github.com/pkg/errors"
)

type StringInterfaceMap struct {
	sync.Map
}

func (m *StringInterfaceMap) MarshalJSON() ([]byte, error) {
	tmpMap := make(map[string]interface{})
	m.Range(func(k, v interface{}) bool {
		tmpMap[k.(string)] = MarkInterfaceType(v)
		return true
	})
	return json.Marshal(tmpMap)
}

func (m *StringInterfaceMap) UnmarshalJSON(data []byte) error {
	var tmpMap map[string]interface{}
	if err := json.Unmarshal(data, &tmpMap); err != nil {
		return err
	}
	for k, v := range tmpMap {
		m.Store(k, v)
	}
	return nil
}

func (m *StringInterfaceMap) Get(key string, o interface{}) error {
	v, ok := m.Load(key)
	if !ok {
		return errors.Errorf("not found")
	}

	b, err := json.Marshal(v)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, o); err != nil {
		return err
	}

	return nil
}

func (m *StringInterfaceMap) GetMap() map[string]interface{} {
	tmpMap := make(map[string]interface{})
	m.Range(func(k, v interface{}) bool {
		tmpMap[k.(string)] = v
		return true
	})
	return tmpMap
}

func (m *StringInterfaceMap) Store(k, v interface{}) {
	m.Map.Store(k, v)
}
