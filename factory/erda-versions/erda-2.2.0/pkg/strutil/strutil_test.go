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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestTrim(t *testing.T) {
	assert.Equal(t, "trim", Trim("trim "))
	assert.Equal(t, "this", Trim(" this  "))
	assert.Equal(t, "thi", Trim("athisb", "abs"))
}

func TestTrimLeft(t *testing.T) {
	assert.Equal(t, "trim ", TrimLeft("trim "))
	assert.Equal(t, "this", TrimLeft(" this"))
	assert.Equal(t, "thisa", TrimLeft("athisa", "a"))
}

func TestTrimRight(t *testing.T) {
	assert.Equal(t, "trim", TrimRight("trim "))
	assert.Equal(t, " this", TrimRight(" this"))
	assert.Equal(t, "athis", TrimRight("athisa", "a"))
}

func TestCollapseWhitespace(t *testing.T) {
	assert.Equal(t, "only one space", CollapseWhitespace("only    one   space"))
	assert.Equal(t, "collapse all sorts of whitespace", CollapseWhitespace("collapse \n   all \t  sorts of \r \n \r\n whitespace"))
}

func TestCenter(t *testing.T) {
	assert.Equal(t, "  a  ", Center("a", 5))
	assert.Equal(t, "  ab ", Center("ab", 5))
	assert.Equal(t, "abc", Center("abc", 1))
}

func TestTruncate(t *testing.T) {
	assert.Equal(t, "it ...", Truncate("it is too long", 6))
	assert.Equal(t, "it is too ...", Truncate("it is too long", 13))
	assert.Equal(t, "but it is not", Truncate("but it is not", 16))
}

func TestTrimSuffixes(t *testing.T) {
	assert.Equal(t, "test", TrimSuffixes("test.go", ".go"))
	assert.Equal(t, "test", TrimSuffixes("test.go", ".go", ".md", ".sh"))
	assert.Equal(t, "test.go", TrimSuffixes("test.go.tmp", ".go", ".tmp"))
}

func TestTrimPrefixes(t *testing.T) {
	assert.Equal(t, "/file", TrimPrefixes("/tmp/file", "/tmp"))
	assert.Equal(t, "/tmp/file", TrimPrefixes("/tmp/tmp/file", "/tmp", "/tmp/tmp"))
}

func TestSplit(t *testing.T) {
	assert.Equal(t, []string{"a", "bc", "12", "", "3"}, Split("a|bc|12||3", "|"))
	assert.Equal(t, []string{"a", "bc", "12", "3"}, Split("a|bc|12||3", "|", true))
	assert.Equal(t, []string{"a,b,c"}, Split("a,b,c", ":"))

}

func TestLines(t *testing.T) {
	assert.Equal(t, []string{"abc", "def", "ghi"}, Lines("abc\ndef\nghi"))
	assert.Equal(t, []string{"abc", "def", "ghi"}, Lines("abc\rdef\rghi"))
	assert.Equal(t, []string{"abc", "def", "ghi", ""}, Lines("abc\r\ndef\r\nghi\n"))
	assert.Equal(t, []string{"abc", "def", "ghi"}, Lines("abc\r\ndef\r\nghi\n", true))
}

func TestConcat(t *testing.T) {
	s1 := "hello"
	s2 := " "
	s3 := "world"

	assert.Equal(t, "hello world", Concat(s1, s2, s3))
}

func TestContains(t *testing.T) {
	assert.True(t, Contains("test contains.", "t c", "iii"))
	assert.False(t, Contains("test contains.", "t cc", "test  "))
	assert.True(t, Contains("test contains.", "iii", "uuu", "ont"))
}
func TestEqual(t *testing.T) {
	assert.False(t, Equal("aaa", "AAA"))
	assert.True(t, Equal("aaa", "AaA", true))
}

func TestHasPrefixes(t *testing.T) {
	assert.False(t, HasPrefixes("asd", "ddd", "uuu"))
	assert.True(t, HasPrefixes("asd", "sd", "as"))
	assert.True(t, HasPrefixes("asd", "asd"))
}

func TestHasSuffixes(t *testing.T) {
	assert.True(t, HasSuffixes("asd", "ddd", "d"))
	assert.True(t, HasSuffixes("asd", "sd"))
	assert.False(t, HasSuffixes("asd", "iid", "as"))
}

func TestTrimSlice(t *testing.T) {
	assert.Equal(t, []string{"trim", "trim", "trim"}, TrimSlice([]string{"trim ", " trim", " trim "}))
}

func TestTrimSliceLeft(t *testing.T) {
	assert.Equal(t, []string{"trim ", "trim", "trim "}, TrimSliceLeft([]string{"trim ", " trim", " trim "}))
}

func TestTrimSliceRight(t *testing.T) {
	assert.Equal(t, []string{"trim", " trim", " trim"}, TrimSliceRight([]string{"trim ", " trim", " trim "}))
}

func TestTrimSliceSuffixes(t *testing.T) {
	assert.Equal(t, []string{"test", "test.go"}, TrimSliceSuffixes([]string{"test.go", "test.go.tmp"}, ".go", ".tmp"))
}

func TestTrimSlicePrefixes(t *testing.T) {
	assert.Equal(t, []string{"/file", "/tmp/file"}, TrimSlicePrefixes([]string{"/tmp/file", "/tmp/tmp/file"}, "/tmp", "/tmp/tmp"))
}

func TestMap(t *testing.T) {
	assert.Equal(t, []string{"X1", "X2", "X3"}, Map([]string{"1", "2", "3"},
		func(s string) string { return Concat("X", s) }))
	assert.Equal(t, []string{"Aa", "Bb", "Cc"}, Map([]string{"Aa", "bB", "cc"}, ToLower, Title))
}

func TestDedupSlice(t *testing.T) {
	assert.Equal(t, []string{"c", "", "b", "a", "d"}, DedupSlice([]string{"c", "", "b", "a", "", "a", "b", "c", "", "d"}))
	assert.Equal(t, []string{"c", "b", "a", "d"}, DedupSlice([]string{"c", "", "b", "a", "", "a", "b", "c", "", "d"}, true))
}

func TestDedupUint64Slice(t *testing.T) {
	assert.Equal(t, []uint64{3, 1, 2, 0}, DedupUint64Slice([]uint64{3, 3, 1, 2, 1, 2, 3, 3, 2, 1, 0, 1, 2}))
	assert.Equal(t, []uint64{3, 1, 2}, DedupUint64Slice([]uint64{3, 3, 1, 2, 1, 2, 3, 3, 2, 1, 0, 1, 2}, true))
}

func TestIntersectionUin64Slice(t *testing.T) {
	assert.Equal(t, []uint64{3, 0}, IntersectionUin64Slice([]uint64{3, 1, 2, 0}, []uint64{0, 3}))
	assert.Equal(t, []uint64{1, 2, 1, 0}, IntersectionUin64Slice([]uint64{3, 1, 2, 1, 0}, []uint64{1, 2, 0}))
}

func TestRemoveSlice(t *testing.T) {
	assert.Equal(t, []string{"b", "c"}, RemoveSlice([]string{"a", "b", "c", "a"}, "a"))
	assert.Equal(t, []string{"a", "a"}, RemoveSlice([]string{"a", "b", "c", "a"}, "b", "c"))
	assert.Equal(t, []string{}, RemoveSlice([]string{"a", "b", "c", "a"}, "a", "b", "c"))
}

func TestNormalizeNewlines(t *testing.T) {
	unixStyle := []byte("version: 1.1\nstages: \n[]")
	macStyle := []byte("version: 1.1\rstages: \r[]")
	windowsStyle := []byte("version: 1.1\r\nstages: \r\n[]")
	assert.Equal(t, unixStyle, NormalizeNewlines(unixStyle))
	assert.Equal(t, unixStyle, NormalizeNewlines(macStyle))
	assert.Equal(t, unixStyle, NormalizeNewlines(windowsStyle))
}

func TestReverseSlice(t *testing.T) {
	ss := []string{"s1", "s2", "s3"}
	ReverseSlice(ss)
	assert.Equal(t, "s3", ss[0])
	assert.Equal(t, "s2", ss[1])
	assert.Equal(t, "s1", ss[2])
}

func TestContainsOrEmpty(t *testing.T) {
	type args struct {
		source string
		target string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "empty",
			args: args{
				source: "user",
				target: "",
			},
			want: true,
		},
		{
			name: "match",
			args: args{
				source: "user",
				target: "to",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := ContainsOrEmpty(tt.args.source, tt.args.target); got != tt.want {
				t.Errorf("ContainsOrEmpty() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFirstNoneEmpty(t *testing.T) {
	type args struct {
		strs []string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{"case1", args{strs: []string{"", "test", ""}}, "test"},
		{"case2", args{strs: []string{}}, ""},
		{"case3", args{strs: []string{"test", "", ""}}, "test"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := FirstNoneEmpty(tt.args.strs...); got != tt.want {
				t.Errorf("FirstNoneEmpty() = %v, want %v", got, tt.want)
			}
		})
	}
}
