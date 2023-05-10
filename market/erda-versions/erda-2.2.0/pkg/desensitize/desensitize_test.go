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

package desensitize

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestKeep(t *testing.T) {
	require.Equal(t, "", desensitize("", 1, 1))
	require.Equal(t, "*", desensitize("1", 1, 1))
	require.Equal(t, "*", desensitize("1", 5, 1))
	require.Equal(t, "1*", desensitize("12", 5, 1))
	require.Equal(t, "1*", desensitize("12", 0, 1))
	require.Equal(t, "**3", desensitize("123", 0, 1))
	require.Equal(t, "1*3", desensitize("123", 1, 2))
	require.Equal(t, "1*34", desensitize("1234", 1, 2))
	require.Equal(t, "12*4", desensitize("1234", 2, 2))
	require.Equal(t, "12*4", desensitize("1234", 3, 2))
	require.Equal(t, "12*4", desensitize("1234", 3, 5))
	require.Equal(t, "1*345", desensitize("12345", 1, 5))
	require.Equal(t, "1**456", desensitize("123456", 1, 5))
	require.Equal(t, "1**4567", desensitize("1234567", 1, 5))
}

func TestMobile(t *testing.T) {
	require.Equal(t, "188****8888", Mobile("18888888888"))
}

func TestName(t *testing.T) {
	require.Equal(t, "e**********t", Name("erda-project"))
}

func TestEmail(t *testing.T) {
	require.Equal(t, "pla**b@plan.com", Email("plan-b@plan.com"))
	require.Equal(t, "*@plan.com", Email("t@plan.com"))
	require.Equal(t, "t*@plan.com", Email("tb@plan.com"))
	require.Equal(t, "t*a@plan.com", Email("tba@plan.com"))
	require.Equal(t, "oj*k@plan.com", Email("ojbk@plan.com"))
	require.Equal(t, "dic*******s@plan.com", Email("dice.tjjtds@plan.com"))

	// illegal emails
	require.Equal(t, "@plan.com", Email("@plan.com"))
	require.Equal(t, "pla****m", Email("plan.com"))
	require.Equal(t, "*", Email("a"))
	require.Equal(t, "a*", Email("ab"))
	require.Equal(t, "a*c", Email("abc"))
	require.Equal(t, "ab*d", Email("abcd"))
	require.Equal(t, "abc*e", Email("abcde"))
	require.Equal(t, "abc**f", Email("abcdef"))
}
