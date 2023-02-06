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

package guid

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNew(t *testing.T) {
	for i := 0; i < 1024; i++ {
		g := New()
		if !g.IsConformant() {
			t.Errorf("Guid '%v' is not RFC4122 compliant.\n", g)
		}
	}
}

func TestNewString(t *testing.T) {
	for i := 0; i < 1024; i++ {
		s := NewString()
		g, _ := ParseString(s)
		if !g.IsConformant() {
			t.Errorf("Guid '%v' is not RFC4122 compliant.\n", g)
		}
	}
}

func TestIsGuid(t *testing.T) {
	for i := 0; i < 1024; i++ {
		g := New()
		if !IsGuid(g.String()) {
			t.Errorf("Guid '%v' not contains a properly formatted Guid\n", g)
		}
	}
}

func TestIsGuid2(t *testing.T) {
	tests := []struct {
		guid     string
		expected bool
	}{
		{
			"", false,
		},
		{
			"-0e545c9c-6942-4988-fab0-645274cfaded", false,
		},
		{
			"0e545c9c-6942-4988-fab0-645274cfade", false,
		},
		{
			"0e545c9c-6942-4988-fab0-645274cfaded", true,
		},
	}
	for _, v := range tests {
		assert.Equal(t, v.expected, IsGuid(v.guid))
	}
}

func BenchmarkNewGuid(b *testing.B) {
	for i := 0; i < b.N; i++ {
		New()
	}
}

func TestParseString(t *testing.T) {
	var goodGuids = [...]string{
		"0e545c9c-6942-4988-fab0-645274cfaded",
		"22e2c08b-e2bd-449a-8fc7-6ff9558ba733",
		"3D7670ff-48CC-42D3-91E4-B09177487D0C",
		"33C69DB0-3895-4D6F-D128-1855D3995742",
	}
	var goodGuidBytes = [...][16]byte{
		{14, 84, 92, 156, 105, 66, 73, 136, 250, 176, 100, 82, 116, 207, 173, 237},
		{34, 226, 192, 139, 226, 189, 68, 154, 143, 199, 111, 249, 85, 139, 167, 51},
		{61, 118, 112, 255, 72, 204, 66, 211, 145, 228, 176, 145, 119, 72, 125, 12},
		{51, 198, 157, 176, 56, 149, 77, 111, 209, 40, 24, 85, 211, 153, 87, 66},
	}
	for i, s := range goodGuids {
		if !IsGuid(s) {
			t.Errorf("good guid '%v' failed IsGuid test\n", s)
		}
		g, err := ParseString(s)
		if err != nil {
			t.Errorf("good guid '%v' failed to parse [%v]\n", s, err)
		}
		if *g != goodGuidBytes[i] {
			t.Error("guid does not match bytes")
		}
	}
	var badGuids = [...]string{
		"0g545c9c-f942-4988-4ab0-645274cfaded",
		"2e2c08b-82bd-449a-7fc7-6ff9558ba733",
		"3D76-709898CC-42D3-41E4-B09177487D0C",
		"33C69DB0D8954D6F71281855D3995742",
	}
	for _, s := range badGuids {
		if IsGuid(s) {
			t.Error("bad guid passed IsGuid test")
		}
		if _, err := ParseString(s); err != ErrInvalid {
			t.Error("bad guid parsed")
		}
	}
}

func TestParseString2(t *testing.T) {
	for i := 0; i < 1024; i++ {
		g := New()
		g2, _ := ParseString(g.String())
		if *g != *g2 {
			t.Error("guid changed in conversion")
		}
	}
}

func TestStringUpper(t *testing.T) {
	var guids = []struct {
		guid     string
		expected string
	}{
		{"0e545c9c-6942-4988-fab0-645274cfaded", "0E545C9C-6942-4988-FAB0-645274CFADED"},
		{"22e2c08b-e2bd-449a-8fc7-6ff9558ba733", "22E2C08B-E2BD-449A-8FC7-6FF9558BA733"},
		{"3D7670ff-48CC-42D3-91E4-B09177487D0C", "3D7670FF-48CC-42D3-91E4-B09177487D0C"},
		{"33C69Db0-3895-4d6F-d128-1855d3995742", "33C69DB0-3895-4D6F-D128-1855D3995742"},
	}
	for _, v := range guids {
		g, _ := ParseString(v.guid)
		assert.Equal(t, v.expected, g.StringUpper())
	}
}

func TestString(t *testing.T) {
	var guids = []struct {
		guid     string
		expected string
	}{
		{"0E545C9C-6942-4988-FAB0-645274CFADED", "0e545c9c-6942-4988-fab0-645274cfaded"},
		{"22E2C08B-E2BD-449A-8FC7-6FF9558BA733", "22e2c08b-e2bd-449a-8fc7-6ff9558ba733"},
		{"3D7670FF-48CC-42D3-91E4-B09177487D0C", "3d7670ff-48cc-42d3-91e4-b09177487d0c"},
		{"33C69DB0-3895-4D6F-d128-1855d3995742", "33c69db0-3895-4d6f-d128-1855d3995742"},
	}
	for _, v := range guids {
		g, _ := ParseString(v.guid)
		assert.Equal(t, v.expected, g.String())
	}
}

func BenchmarkParseString(b *testing.B) {
	var guids [16]string
	for i := 0; i < 16; i++ {
		guids[i] = New().String()
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		ParseString(guids[i%16])
	}
}

func BenchmarkIsGuid(b *testing.B) {
	var guids [16]string
	for i := 0; i < 16; i++ {
		guids[i] = New().String()
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		IsGuid(guids[i%16])
	}
}
