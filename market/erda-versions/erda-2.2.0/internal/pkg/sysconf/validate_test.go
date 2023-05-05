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

package sysconf

import (
	"testing"
)

func TestIsClusterName(t *testing.T) {
	m := map[string]bool{
		"":              false,
		"terminus":      false,
		"terminus-test": true,
		"terminus-prod": true,
	}
	for k, v := range m {
		if v != isClusterName(k) {
			t.Fatal(k)
		}
	}
}

func TestIsPort(t *testing.T) {
	m := map[int]bool{
		0:     false,
		-1:    false,
		22:    true,
		65535: true,
		65536: false,
	}
	for k, v := range m {
		if v != IsPort(k) {
			t.Fatal(k)
		}
	}
}

func TestIsDNSName(t *testing.T) {
	m := map[string]bool{
		"":            false,
		"*.aa":        false,
		"a.b":         true,
		"192.168.0.1": true,
		"test.com":    true,
	}
	for k, v := range m {
		if v != IsDNSName(k) {
			t.Fatal(k)
		}
	}
}
