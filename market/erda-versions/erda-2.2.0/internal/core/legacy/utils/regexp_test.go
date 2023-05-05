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

package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestIsValidOrgName(t *testing.T) {
	ss := []struct {
		s    string
		want bool
	}{
		{
			"",
			false,
		},
		{
			"a",
			true,
		},
		{
			"1",
			false,
		},
		{
			"1233",
			false,
		},
		{
			"-",
			false,
		},
		{
			"-123",
			false,
		},
		{
			"3434-",
			false,
		},
		{
			"abd-3-c",
			true,
		},
		{
			"123-456",
			true,
		},
		{
			"123a-456-abc",
			true,
		},
		{
			"jfljef",
			true,
		},
		{
			"a---b",
			true,
		},
		{
			"a34jjl",
			true,
		},
		{
			"123a",
			true,
		},
		{
			"1----a",
			true,
		},
	}
	for _, v := range ss {
		assert.Equal(t, v.want, IsValidOrgName(v.s))
	}
}
