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

// ^[a-z\u4e00-\u9fa5A-Z0-9_-]*$
package spaceFormModal

//import (
//	"regexp"
//	"testing"
//
//	"gotest.tools/assert"
//)
//
//func TestMatch(t *testing.T) {
//	// str := "1234daS撒_-&*("
//	// reg, _ := regexp.MatchString("[^a-z\u4e00-\u9fa5A-Z0-9_-]", str)
//	// //fmt.Println(reg.FindAllStringSubmatch(str, -1))
//	// assert.Equal(t, reg, true)
//
//	// a := regexp.MustCompile("[^a-z\u4e00-\u9fa5A-Z0-9_-]")
//	// b := regexp.MustCompile("^[a-z\u4e00-\u9fa5A-Z0-9_-]")
//	// assert.Equal(t, a, b)
//	// assert.NoError(t, err)
//	// ^[A-Za-z0-9\u4e00-\u9fa5｜\.｜_｜\-｜ ]+$
//
//	// a := regexp.MustCompile(`^[A-Za-z0-9|\\u4e00-\\u9fa5|\.|_|\-| ]+$`)
//	// a := regexp.MustCompile("^[A-Za-z0-9|\u4e00-\u9fa5|_|-]+$")
//	// matched, err := regexp.MatchString("^[a-zA-Z0-9_.|\\-|\\s|\\u4e00-\\u9fa5]+$", str)
//	// fmt.Println(matched, err)
//	a := regexp.MustCompile("^[a-zA-Z0-9_.|\\-|\\s|\u4e00-\u9fa5]+$")
//	b := a.MatchString("1234daS_.是%……&的-")
//	assert.Equal(t, b, true)
//}
