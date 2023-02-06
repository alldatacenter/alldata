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

package strutil_test

import (
	"strings"
	"testing"

	"github.com/erda-project/erda/pkg/strutil"
)

func TestFirstCustomPlaceholder(t *testing.T) {
	type testCase struct {
		s           string
		left, right string
		key         string
	}
	var cases = []testCase{
		{
			s:     "do ${something}",
			left:  "${",
			right: "}",
			key:   "something",
		}, {
			s:     "do ((something))",
			left:  "((",
			right: "))",
			key:   "something",
		}, {
			s:     "do ${{ configs.something }}",
			left:  "${{",
			right: "}}",
			key:   "something",
		}, {
			s:     "do ${env.something:homework}",
			left:  "${",
			right: "}",
			key:   "something",
		},
	}
	for i, testCase := range cases {
		placeholder, start, end, err := strutil.FirstCustomPlaceholder(testCase.s, testCase.left, testCase.right)
		if err != nil {
			t.Fatal(i, err)
		}
		t.Logf("[%v] placeholder: %s, indexStart: %v, indexEnd: %v, after interpolation: %s",
			i, placeholder, start, end, strutil.Replace(testCase.s, "homework", start, end))
	}
}

func TestInterpolate(t *testing.T) {
	var (
		s      = "do ${k1} or ${k2}"
		values = map[string]string{
			"k1": "homework",
			"k2": "${k1} and something",
			"k3": "do ${k2}",
		}
	)
	s, err := strutil.Interpolate(s, values, false, "${", "}")
	if err != nil {
		t.Fatal(err)
	}
	t.Log(s)

	s = "do ${k2} and ${k4:clean up}"
	s, err = strutil.Interpolate(s, values, false, "${", "}")
	if err != nil {
		t.Fatal(err)
	}
	t.Log(s)
}

func TestInterpolationDereference(t *testing.T) {
	var values = map[string]string{
		"k1": "homework",
		"k2": "${k1} and something",
		"k3": "do ${k2}",
	}
	if err := strutil.InterpolationDereference(values, "${", "}"); err != nil {
		t.Fatal(err)
	}
	t.Log(values)

	values = map[string]string{
		"${k1}": "a",
		"k2":    "b",
	}
	err := strutil.InterpolationDereference(values, "${", "}")
	if err == nil {
		t.Fatal("errors", values)
	}
	t.Log(err)

	values = map[string]string{
		"k1": "${k3}/${k4}",
		"k2": "${k1} and something",
		"k3": "do${k2}",
		"k4": "d",
	}
	err = strutil.InterpolationDereference(values, "${", "}")
	if err == nil {
		t.Fatal("errors", values)
	}
	t.Log(err)
}

func TestFirstCustomExpression(t *testing.T) {
	type testCase struct {
		s           string
		left, right string
		key         string
		f           func(string) bool
	}
	var cases = []testCase{
		{
			s:     "do ${something}",
			left:  "${",
			right: "}",
			key:   "something",
			f: func(_ string) bool {
				return true
			},
		}, {
			s:     "do ((something))",
			left:  "((",
			right: "))",
			key:   "something",
			f: func(_ string) bool {
				return true
			},
		}, {
			s:     "do ${{ configs.something }}",
			left:  "${{",
			right: "}}",
			key:   "something",
			f: func(s string) bool {
				return strings.HasPrefix(s, "configs.")
			},
		}, {
			s:     "do ${env.something:homework}",
			left:  "${",
			right: "}",
			key:   "something",
			f: func(s string) bool {
				return strings.HasPrefix(s, "env.")
			},
		},
	}
	for i, testCase := range cases {
		placeholder, start, end, err := strutil.FirstCustomExpression(testCase.s, testCase.left, testCase.right, testCase.f)
		if err != nil {
			t.Fatal(i, err)
		}
		t.Logf("[%v] placeholder: %s, indexStart: %v, indexEnd: %v, after interpolation: %s",
			i, placeholder, start, end, strutil.Replace(testCase.s, "homework", start, end))
		if strutil.Replace(testCase.s, "homework", start, end) != "do homework" {
			t.Fatal("replace error")
		}
	}
}
