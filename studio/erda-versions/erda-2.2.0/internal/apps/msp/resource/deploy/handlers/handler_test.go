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

package handlers

import (
	"encoding/json"
	"testing"

	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
)

type Obj struct {
	Name string `json:"name,omitempty"`
	Age  string `json:"age,omitempty"`
}

func TestJsonStringMarshal(t *testing.T) {
	var a = "1111"
	var b string
	utils.JsonConvertObjToType(a, &b)
	if a != b {
		t.Errorf("expect: %s, actual: %s", a, b)
	}
}

func TestJsonObjMarshal(t *testing.T) {
	obj := Obj{Name: "xiao ming", Age: "18"}
	bytes, _ := json.Marshal(obj)
	objStr := string(bytes)

	var unObj1 Obj
	utils.JsonConvertObjToType(obj, &unObj1)

	if obj.Name != unObj1.Name || obj.Age != unObj1.Age {
		t.Error("unmarshal from obj should same")
	}

	var unObj2 Obj
	utils.JsonConvertObjToType(objStr, &unObj2)

	if obj.Name != unObj2.Name || obj.Age != unObj2.Age {
		t.Error("unmarshal from str should same")
	}

	obj.Name = "xiao hong"
	utils.JsonConvertObjToType(obj, &unObj1)

	if obj.Name != unObj1.Name || obj.Age != unObj1.Age {
		t.Error("unmarshal from str should same")
	}

	var str string
	err := utils.JsonConvertObjToType(obj, &str)
	if err == nil {
		t.Error("obj should fail re-marshal to string")
	}

	var mapObj map[string]string
	utils.JsonConvertObjToType(objStr, &mapObj)

}

func TestMapJsonMarshal(t *testing.T) {
	var mapObj map[string]string
	err := utils.JsonConvertObjToType("", &mapObj)
	if err == nil {
		t.Error("convert empty string to map should fail")
	}
	if mapObj != nil {
		t.Error("when convert fail, map value should keep same")
	}

	mapIni := map[string]string{}
	err = utils.JsonConvertObjToType("", &mapIni)
	if err == nil {
		t.Error("convert empty string to map should fail")
	}
	if mapIni == nil {
		t.Error("when convert fail, map value should keep same")
	}
}

func TestMapAppend(t *testing.T) {
	map1 := map[string]string{"1": "1"}
	map2 := map[string]string{"2": "2"}
	utils.AppendMap(map1, map2)
	if len(map1) != 2 {
		t.Error("append should be in-place")
	}
}

func TestNewDefaultHandler(t *testing.T) {
	instance := NewDefaultHandler(nil, nil)
	if instance.Bdl == nil {
		t.Errorf("DefaultHandler.Bdl should not nil")
	}
}
