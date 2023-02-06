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

package oas3_test

// timeout
//import (
//	"bytes"
//	"io/ioutil"
//	"testing"
//
//	"github.com/erda-project/erda/pkg/swagger/oas3"
//)
//
//const petstore = "./testdata/petstore-oas3.json"
//
//// 测试 MarshalYaml 序列化结果的一致性
//// 重复执行序列化 100 次, 如果发生两次结果值不一致, 则测试失败
//func TestMarshalYamlConsistency(t *testing.T) {
//	data, err := ioutil.ReadFile(petstore)
//	if err != nil {
//		t.Fatalf("failed to ReadFile: %v", err)
//	}
//
//	v3, err := oas3.LoadFromData(data)
//	if err != nil {
//		t.Fatalf("failed to LoadFromData: %v", err)
//	}
//
//	y, err := oas3.MarshalYaml(v3)
//	if err != nil {
//		t.Fatalf("failed to MarshalYaml: %v", err)
//	}
//
//	for i := 0; i < 100; i++ {
//		y2, err := oas3.MarshalYaml(v3)
//		if err != nil {
//			t.Fatalf("failed to MarshalYaml: %v", err)
//		}
//		if !bytes.Equal(y, y2) {
//			t.Fatalf("y is not equal with y2, index: %v", i)
//		}
//	}
//}
