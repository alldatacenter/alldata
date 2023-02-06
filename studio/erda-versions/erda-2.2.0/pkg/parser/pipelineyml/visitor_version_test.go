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

package pipelineyml

import (
	"testing"
)

func TestVersionVisitor_Visit(t *testing.T) {
	v := NewVersionVisitor()

	validVersionTestCases := []string{
		`version: 1.1`,
		`version: '1.1'`,
		`version: "1.1"`,
	}
	for _, tc := range validVersionTestCases {
		y, err := New([]byte(tc))
		if err != nil {
			t.Fatal(err)
		}
		y.s.Accept(v)
		if len(y.s.errs) > 0 {
			t.Fatal(y.s.mergeErrors())
		}
	}

	invalidVersionTestCases := []string{
		`version: 1.2`,
		`version: 2`,
		`version: 1.1.alpha`,
		`version: "1`,
		`version: test`,
	}
	for _, tc := range invalidVersionTestCases {
		_, err := New([]byte(tc))
		if err == nil {
			t.Fatalf("should error: `%s`", tc)
		}
	}
}

func TestGetVersion(t *testing.T) {
	validVersionTestCases := []string{
		`version: 1`,
		`version: '1'`,
		`version: "1"`,
		`version: 1.0`,
		`version: '1.0'`,
		`version: "1.0"`,
	}
	for _, tc := range validVersionTestCases {
		version, err := GetVersion([]byte(tc))
		if err != nil {
			t.Fatal(err)
		}
		if !(version == "1" || version == "1.0") {
			t.Fatalf("invalid version: %s", version)
		}
	}
}
