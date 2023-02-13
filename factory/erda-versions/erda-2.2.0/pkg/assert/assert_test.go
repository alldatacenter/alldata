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

package assert

import (
	"encoding/json"
	"testing"

	ast "github.com/stretchr/testify/assert"
)

func TestAssert(t *testing.T) {
	var (
		v   interface{} // value
		op  string      // operator
		e   string      // expect
		ret bool
		err error
	)

	// 测试 =
	v = map[string]string{
		"aa": "bb",
		"cc": "dd",
	}
	op = "="
	e = `{"aa":"bb","cc":"dd"}`
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = []int{1, 2, 3, 4}
	op = "="
	e = `[1,2,3,4]`
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = []string{"q1", "q2", "test"}
	op = "="
	e = `["q1","q2","test"]`
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 200.00
	op = "="
	e = "200.0000"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 !=
	v = "200.00"
	op = "!="
	e = "200.01"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 >=
	v = "200.001"
	op = ">="
	e = "100"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 200
	op = ">="
	e = "200"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 <=
	v = "200"
	op = "<="
	e = "300"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = "200"
	op = "<="
	e = "200"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 >
	v = "200"
	op = ">"
	e = "100"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 <
	v = "200"
	op = "<"
	e = "300"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 contains
	ss := `{"result":{"tenantId":null,"extra":null,"userId":11339,"token":"xxx","expireTime":2592000},"success":true}`
	err = json.Unmarshal([]byte(ss), &v)
	op = "contains"
	e = "\"userId\":11339"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 123456788888
	op = "contains"
	e = "123"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = "hello123 test dice!!"
	op = "contains"
	e = "[a-z]+[0-9]+"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 not_contains
	v = "1e2bc0278349bc5a4ca7ab2ac5daf6fc34d99563ec89edcc87f64a219622c364"
	op = "not_contains"
	e = "2bcdsf"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 belong
	v = 25
	op = "belong"
	e = "[-30,25]"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = "2592000"
	op = "belong"
	e = "{2592000, 23,34}"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 25
	op = "belong"
	e = "[2,235]"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 20
	op = "belong"
	e = "(10,30)"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 10
	op = "belong"
	e = "[10,30)"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 2592000
	op = "belong"
	e = "{[10,2600000),300,20}"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = 300
	op = "belong"
	e = "{300,20,10,30}"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 not_belong
	v = 259
	op = "not_belong"
	e = "[2,235]"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 empty
	v = make(map[string]string)
	op = "empty"
	e = ""
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	v = []int{}
	op = "empty"
	e = ""
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 not_empty
	v = "test"
	op = "not_empty"
	e = "test"
	ret, err = DoAssert(v, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 exist
	var val interface{}
	err = json.Unmarshal([]byte(`{"data":{"key1":{"key2": "test"},"key":{"key2": "test"}}}`), &val)
	ast.Equal(t, err, nil)
	op = "exist"
	e = `data.key.key2`
	ret, err = DoAssert(val, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)

	// 测试 not_exist
	var val1 interface{}
	err = json.Unmarshal([]byte(`{"data":{"key1":{"key2": "test"},"key":{"key2": "test"}}}`), &val1)
	ast.Equal(t, err, nil)
	op = "not_exist"
	e = `data.key11`
	ret, err = DoAssert(val1, op, e)
	ast.Equal(t, err, nil)
	ast.Equal(t, ret, true)
}
