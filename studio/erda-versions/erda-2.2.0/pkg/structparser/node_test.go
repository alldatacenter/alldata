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

package structparser

import (
	"fmt"
	"reflect"
	"testing"
	"time"
)

type testType struct {
	a *int `tagtagtag`
	b map[string]*bool
	c *struct {
		d **int
	}
}

func TestNewNode(t *testing.T) {
	tp := reflect.TypeOf(testType{})
	n := newNode(constructCtx{name: tp.Name()}, tp)
	fmt.Printf("%+v\n", n) // debug print
}

func TestCompress(t *testing.T) {
	tp := reflect.TypeOf(testType{})
	n := newNode(constructCtx{name: tp.Name()}, tp)
	fmt.Printf("%+v\n", n) // debug print
	nn := n.Compress()
	fmt.Printf("%+v\n", nn) // debug print
}

func TestTest(t *testing.T) {
	tp := reflect.TypeOf(time.Time{})
	n := newNode(constructCtx{}, tp)
	fmt.Printf("%+v\n", n.String()) // debug print
}
