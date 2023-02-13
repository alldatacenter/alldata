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

package strutil

import (
	"regexp"
	"testing"

	"gotest.tools/assert"
)

func TestReplaceAllStringSubmatchFunc(t *testing.T) {
	s := "${java:OUTPUT:image} ${js:OUTPUT:image}"
	m := map[string]map[string]string{
		"java": {
			"image": "openjdk:8",
		},
		"js": {
			"image": "herd:1.3",
		},
	}
	re := regexp.MustCompile(`\${([^:]+):OUTPUT:([^:]+)}`)
	replaced := ReplaceAllStringSubmatchFunc(re, s, func(sub []string) string {
		return m[sub[1]][sub[2]]
	})
	assert.Equal(t, "openjdk:8 herd:1.3", replaced)
}

func TestPrefixWithSemVer(t *testing.T) {
	var versions = map[string]bool{
		`v1.0.0`:                true,
		`v1.0.0-20210101000000`: true,
		`1.0.0`:                 true,
		`0.1.2`:                 true,
		`1.2.3-alpha.10.beta.0+build.unicorn.rainbow`: true,
		`1.0`:       true,
		`1.0-alpha`: true,
		`1.2-alpha.10.beta.0+build.unicorn.rainbow`: true,
		`1.0.0.alpha`:  false,
		`01.02.03`:     false,
		`1.0.b`:        false,
		`a1.0.0`:       false,
		`some-feature`: false,
		`1.0.alpha`:    false,
	}
	for version, expected := range versions {
		if ok := PrefixWithSemVer(version); ok != expected {
			t.Fatalf("assert error, version: %s, expected: %v, actual: %v", version, expected, ok)
		}
	}
}
