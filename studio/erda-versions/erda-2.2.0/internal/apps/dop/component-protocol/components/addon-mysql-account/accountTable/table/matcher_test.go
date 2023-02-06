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

package table

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

type Person struct {
	Name string
	Age  int
}

func TestAndMatch(t *testing.T) {
	a := Person{
		Name: "John",
		Age:  30,
	}

	assert.True(t, And(
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Age < 35
		}),
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Name == "John"
		}),
	).Match(a))

	assert.False(t, And(
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Age < 30
		}),
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Name == "John"
		}),
	).Match(a))

	assert.False(t, And(
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Age < 30
		}),
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Name == "Bob"
		}),
	).Match(a))
}

func TestOrMatch(t *testing.T) {
	a := Person{
		Name: "John",
		Age:  30,
	}

	assert.True(t, Or(
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Age < 35
		}),
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Name == "John"
		}),
	).Match(a))

	assert.True(t, Or(
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Age < 30
		}),
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Name == "John"
		}),
	).Match(a))

	assert.False(t, Or(
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Age < 30
		}),
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Name == "Bob"
		}),
	).Match(a))

	assert.True(t, Or(
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Age < 35
		}),
		ToMatcher(func(v interface{}) bool {
			p := v.(Person)
			return p.Name == "Bob"
		}),
	).Match(a))
}
