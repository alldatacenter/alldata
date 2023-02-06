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
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/metadata"
)

func TestStringInterfaceMap_MarshalJSON(t *testing.T) {
	jstr := `{"1":{"name":"name1","value":"value1"},"2":2}`
	var m StringInterfaceMap
	m.Store("1", metadata.MetadataField{Name: "name1", Value: "value1"})
	m.Store("2", 2)
	b, err := json.Marshal(&m)
	assert.NoError(t, err)
	assert.Equal(t, jstr, string(b))
}

func TestStringInterfaceMap_UnmarshalJSON(t *testing.T) {
	jstr := `{"1":{"name":"name1","value":"value1"},"2":2}`
	var m StringInterfaceMap
	err := json.Unmarshal([]byte(jstr), &m)
	assert.NoError(t, err)

	var o metadata.MetadataField
	err = m.Get("1", &o)
	assert.NoError(t, err)
	assert.Equal(t, "name1", o.Name)
	assert.Equal(t, "value1", o.Value)

	var i int
	err = m.Get("2", &i)
	assert.NoError(t, err)
	assert.Equal(t, 2, i)
}

func TestStringInterfaceMap_MarshalJSON2(t *testing.T) {
	type extra struct {
		Name    string             `json:"name"`
		Volumes StringInterfaceMap `json:"volumes"`
	}
	var e extra
	e.Name = "name"
	e.Volumes = StringInterfaceMap{}
	e.Volumes.Store("k1", metadata.MetadataField{Name: "n1"})
	e.Volumes.Store("k2", metadata.MetadataField{Name: "n2"})

	b, err := json.Marshal(&e)
	assert.NoError(t, err)
	fmt.Println(string(b))
}

func TestStringInterfaceMap_GetMap(t *testing.T) {
	var m StringInterfaceMap
	m.Store("1", metadata.MetadataField{Name: "name1", Value: "value1"})
	m.Store("2", 2)
	tmpMap := m.GetMap()
	assert.Equal(t, 2, len(tmpMap))
	assert.Equal(t, tmpMap["1"], metadata.MetadataField{Name: "name1", Value: "value1"})
	assert.Equal(t, tmpMap["2"], 2)
}
