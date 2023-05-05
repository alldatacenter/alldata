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

// Package strutil 字符串工具包
package strutil

import (
	"bytes"
	"fmt"
	"math/rand"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"time"
	"unicode"
)

// Trim 两边裁剪 `s`, 如果不指定 `cutset`, 默认cutset=space
//
// Trim("trim ") => "trim"
//
// Trim(" this  ") => "this"
//
// Trim("athisb", "abs") => "this"
func Trim(s string, cutset ...string) string {
	if len(cutset) == 0 {
		return strings.TrimSpace(s)
	}
	return strings.Trim(s, cutset[0])
}

// TrimLeft 裁剪 `s` 左边, 如果不指定 `cutset`, 默认cutset=space
//
// TrimLeft("trim ") => "trim "
//
// TrimLeft(" this") => "this"
//
// TrimLeft("athisa", "a") => "thisa"
func TrimLeft(s string, cutset ...string) string {
	if len(cutset) == 0 {
		return strings.TrimLeftFunc(s, unicode.IsSpace)
	}
	return strings.TrimLeft(s, cutset[0])
}

// TrimRight 裁剪 `s` 右边，如果不指定 `cutset`, 默认cutset=space
//
// TrimRight("trim ") => "trim"
//
// TrimRight(" this") => " this"
//
// TrimRight("athisa", "a") => "athis"
func TrimRight(s string, cutset ...string) string {
	if len(cutset) == 0 {
		return strings.TrimRightFunc(s, unicode.IsSpace)
	}
	return strings.TrimRight(s, cutset[0])
}

// TrimSuffixes 裁剪 `s` 的后缀
//
// TrimSuffixes("test.go", ".go") => "test"
//
// TrimSuffixes("test.go", ".md", ".go", ".sh") => "test"
//
// TrimSuffixes("test.go.tmp", ".go", ".tmp") => "test.go"
func TrimSuffixes(s string, suffixes ...string) string {
	originLen := len(s)
	for i := range suffixes {
		trimmed := strings.TrimSuffix(s, suffixes[i])
		if len(trimmed) != originLen {
			return trimmed
		}
	}
	return s
}

// TrimPrefixes 裁剪 `s` 的前缀
//
// TrimPrefixes("/tmp/file", "/tmp") => "/file"
//
// TrimPrefixes("/tmp/tmp/file", "/tmp", "/tmp/tmp") => "/tmp/file"
func TrimPrefixes(s string, prefixes ...string) string {
	originLen := len(s)
	for i := range prefixes {
		trimmed := strings.TrimPrefix(s, prefixes[i])
		if len(trimmed) != originLen {
			return trimmed
		}
	}
	return s
}

// TrimSlice Trim 的 Slice 版本
//
// TrimSlice([]string{"trim ", " trim", " trim "}) => []string{"trim", "trim", "trim"}
func TrimSlice(ss []string, cutset ...string) []string {
	r := make([]string, len(ss))
	for i := range ss {
		r[i] = Trim(ss[i], cutset...)
	}
	return r
}

// TrimSliceLeft TrimLeft 的 Slice 版本
//
// TrimSliceLeft([]string{"trim ", " trim", " trim "}) => []string{"trim ", "trim", "trim "}
func TrimSliceLeft(ss []string, cutset ...string) []string {
	r := make([]string, len(ss))
	for i := range ss {
		r[i] = TrimLeft(ss[i], cutset...)
	}
	return r
}

// TrimSliceRight TrimRight 的 Slice 版本
//
// TrimSliceRight([]string{"trim ", " trim", " trim "}) => []string{"trim", " trim", " trim"}
func TrimSliceRight(ss []string, cutset ...string) []string {
	r := make([]string, len(ss))
	for i := range ss {
		r[i] = TrimRight(ss[i], cutset...)
	}
	return r
}

// TrimSliceSuffixes TrimSuffixes 的 Slice 版本
//
// TrimSliceSuffixes([]string{"test.go", "test.go.tmp"}, ".go", ".tmp") => []string{"test", "test.go"}
func TrimSliceSuffixes(ss []string, suffixes ...string) []string {
	r := make([]string, len(ss))
	for i := range ss {
		r[i] = TrimSuffixes(ss[i], suffixes...)
	}
	return r
}

// TrimSlicePrefixes TrimPrefixes 的 Slice 版本
//
// TrimSlicePrefixes([]string{"/tmp/file", "/tmp/tmp/file"}, "/tmp", "/tmp/tmp") => []string{"/file", "/tmp/file"}
func TrimSlicePrefixes(ss []string, prefixes ...string) []string {
	r := make([]string, len(ss))
	for i := range ss {
		r[i] = TrimPrefixes(ss[i], prefixes...)
	}
	return r
}

// HasPrefixes `prefixes` 中是否存在 `s` 的前缀
//
// HasPrefixes("asd", "ddd", "uuu") => false
//
// HasPrefixes("asd", "sd", "as") => true
//
// HasPrefixes("asd", "asd") => true
func HasPrefixes(s string, prefixes ...string) bool {
	for i := range prefixes {
		if strings.HasPrefix(s, prefixes[i]) {
			return true
		}
	}
	return false
}

// HasSuffixes `suffixes` 中是否存在 `s` 的后缀
//
// HasSuffixes("asd", "ddd", "d") => true
//
// HasSuffixes("asd", "sd") => true
//
// HasSuffixes("asd", "iid", "as") => false
func HasSuffixes(s string, suffixes ...string) bool {
	for i := range suffixes {
		if strings.HasSuffix(s, suffixes[i]) {
			return true
		}
	}
	return false
}

var (
	collapseWhitespaceRegex = regexp.MustCompile("[ \t\n\r]+")
)

// CollapseWhitespace 转化连续的 space 为 _一个_ 空格
//
// CollapseWhitespace("only    one   space") => "only one space"
//
// CollapseWhitespace("collapse \n   all \t  sorts of \r \n \r\n whitespace") => "collapse all sorts of whitespace"
func CollapseWhitespace(s string) string {
	return collapseWhitespaceRegex.ReplaceAllString(s, " ")
}

// Center 居中 `s`
//
// Center("a", 5) => "  a  "
//
// Center("ab", 5) => "  ab "
//
// Center("abc", 1) => "abc"
func Center(s string, length int) string {
	minus := length - len(s)
	if minus <= 0 {
		return s
	}
	right := minus / 2
	mod := minus % 2
	return strings.Join([]string{Repeat(" ", right+mod), s, Repeat(" ", right)}, "")
}

// Truncate 截断 `s` 到 `length`-3 的长度，末尾增加 "..."
//
// Truncate("it is too long", 6) => "it ..."
//
// Truncate("it is too long", 13) => "it is too ..."
//
// Truncate("but it is not", 16) => "but it is not"
func Truncate(s string, length int) string {
	if len(s) > length {
		return s[:length-3] + "..."
	}
	return s
}

// Split 根据 `sep` 来切分 `s`, `omitEmptyOpt`=true 时，忽略结果中的空字符串
//
// Split("a|bc|12||3", "|") => []string{"a", "bc", "12", "", "3"}
//
// Split("a|bc|12||3", "|", true) => []string{"a", "bc", "12", "3"}
//
// Split("a,b,c", ":") => []string{"a,b,c"}
func Split(s string, sep string, omitEmptyOpt ...bool) []string {
	var omitEmpty bool
	if len(omitEmptyOpt) > 0 && omitEmptyOpt[0] {
		omitEmpty = true
	}
	parts := strings.Split(s, sep)
	if !omitEmpty {
		return parts
	}
	result := []string{}
	for _, v := range parts {
		if v != "" {
			result = append(result, v)
		}
	}
	return result
}

var (
	linesRegex = regexp.MustCompile("\r\n|\n|\r")
)

// Lines 将 `s` 按 newline 切分成 string slice, omitEmptyOpt=true 时，忽略结果中的空字符串
//
// Lines("abc\ndef\nghi") => []string{"abc", "def", "ghi"}
//
// Lines("abc\rdef\rghi") => []string{"abc", "def", "ghi"}
//
// Lines("abc\r\ndef\r\nghi\n") => []string{"abc", "def", "ghi", ""}
//
// Lines("abc\r\ndef\r\nghi\n", true) => []string{"abc", "def", "ghi"}
func Lines(s string, omitEmptyOpt ...bool) []string {
	lines := linesRegex.Split(s, -1)
	if len(omitEmptyOpt) == 0 || !omitEmptyOpt[0] {
		return lines
	}
	r := []string{}
	for i := range lines {
		if lines[i] != "" {
			r = append(r, lines[i])
		}
	}
	return r
}

// Repeat see also strings.Repeat
func Repeat(s string, count int) string {
	return strings.Repeat(s, count)
}

// Concat 合并字符串
func Concat(s ...string) string {
	return strings.Join(s, "")
}

// Join see also strings.Join,
// omitEmptyOpt = true 时，不拼接 `ss` 中空字符串
func Join(ss []string, sep string, omitEmptyOpt ...bool) string {
	if len(omitEmptyOpt) == 0 || !omitEmptyOpt[0] {
		return strings.Join(ss, sep)
	}
	r := []string{}
	for i := range ss {
		if ss[i] != "" {
			r = append(r, ss[i])
		}
	}
	return strings.Join(r, sep)
}

// JoinPath see also filepath.Join
func JoinPath(ss ...string) string {
	return filepath.Join(ss...)
}

// ToLower see also strings.ToLower
func ToLower(s string) string {
	return strings.ToLower(s)
}

// ToUpper see also strings.ToUpper
func ToUpper(s string) string {
	return strings.ToUpper(s)
}

// ToTitle see also strings.ToTitle
func ToTitle(s string) string {
	return strings.ToTitle(s)
}

// Title see also strings.Title
func Title(s string) string {
	return strings.Title(s)
}

// Contains 检查 `s` 中是否存在 `substrs` 中的某个字符串
//
// Contains("test contains.", "t c", "iii")  => true
//
// Contains("test contains.", "t cc", "test  ") => false
//
// Contains("test contains.", "iii", "uuu", "ont") => true
func Contains(s string, substrs ...string) bool {
	for i := range substrs {
		if strings.Contains(s, substrs[i]) {
			return true
		}
	}
	return false
}

// Equal 判断 `s` 和 `other` 是否相同，如果 ignorecase = true, 忽略大小写
//
// Equal("aaa", "AAA") => false
//
// Equal("aaa", "AaA", true) => true
func Equal(s, other string, ignorecase ...bool) bool {
	if len(ignorecase) == 0 || !ignorecase[0] {
		return strings.Compare(s, other) == 0
	}
	return strings.EqualFold(s, other)
}

// Atoi64 parse string to int64
//
// Atoi64("6") => (6, nil)
func Atoi64(s string) (int64, error) {
	return strconv.ParseInt(s, 10, 64)
}

// Map 对 `ss` 中的每个元素执行 `f`, 返回f返回的结果列表
//
// Map([]string{"1", "2", "3"}, func(s string) string {return Concat("X", s)}) => []string{"X1", "X2", "X3"}
//
// Map([]string{"Aa", "bB", "cc"}, ToLower, Title) => []string{"Aa", "Bb", "Cc"}
func Map(ss []string, fs ...func(s string) string) []string {
	r := []string{}
	for i := range ss {
		r = append(r, ss[i])
	}
	r2 := []string{}
	for _, f := range fs {
		for i := range r {
			r2 = append(r2, f(r[i]))
		}
		r = r2[:]
		r2 = []string{}
	}
	return r
}

// DedupSlice 返回不含重复元素的 slice，各元素按第一次出现顺序排序。如果 omitEmptyOpt = true, 忽略空字符串
//
// DedupSlice([]string{"c", "", "b", "a", "", "a", "b", "c", "", "d"}) => []string{"c", "", "b", "a", "d"}
//
// DedupSlice([]string{"c", "", "b", "a", "", "a", "b", "c", "", "d"}, true) => []string{"c", "b", "a", "d"}
func DedupSlice(ss []string, omitEmptyOpt ...bool) []string {
	var omitEmpty bool
	if len(omitEmptyOpt) > 0 && omitEmptyOpt[0] {
		omitEmpty = true
	}
	result := make([]string, 0, len(ss))
	m := make(map[string]struct{}, len(ss))
	for _, s := range ss {
		if s == "" && omitEmpty {
			continue
		}
		if _, ok := m[s]; ok {
			continue
		}
		result = append(result, s)
		m[s] = struct{}{}
	}
	return result
}

// DedupUint64Slice 返回不含重复元素的 slice，各元素按第一次出现顺序排序。
//
// DedupUint64Slice([]uint64{3, 3, 1, 2, 1, 2, 3, 3, 2, 1, 0, 1, 2}) => []uint64{3, 1, 2, 0}
//
// DedupUint64Slice([]uint64{3, 3, 1, 2, 1, 2, 3, 3, 2, 1, 0, 1, 2}, true) => []uint64{3, 1, 2}
func DedupUint64Slice(ii []uint64, omitZeroOpt ...bool) []uint64 {
	var omitZero bool
	if len(omitZeroOpt) > 0 && omitZeroOpt[0] {
		omitZero = true
	}
	result := make([]uint64, 0, len(ii))
	m := make(map[uint64]struct{}, len(ii))
	for _, i := range ii {
		if i == 0 && omitZero {
			continue
		}
		if _, ok := m[i]; ok {
			continue
		}
		result = append(result, i)
		m[i] = struct{}{}
	}
	return result
}

// DedupInt64Slice ([]int64{3, 3, 1, 2, 1, 2, 3, 3, 2, 1, 0, 1, 2}, true) => []int64{3, 1, 2}
func DedupInt64Slice(ii []int64, omitZeroOpt ...bool) []int64 {
	var omitZero bool
	if len(omitZeroOpt) > 0 && omitZeroOpt[0] {
		omitZero = true
	}
	result := make([]int64, 0, len(ii))
	m := make(map[int64]struct{}, len(ii))
	for _, i := range ii {
		if i == 0 && omitZero {
			continue
		}
		if _, ok := m[i]; ok {
			continue
		}
		result = append(result, i)
		m[i] = struct{}{}
	}
	return result
}

// IntersectionUin64Slice 返回两个 uint64 slice 的交集，复杂度 O(m * n)，待优化
//
// IntersectionUin64Slice([]uint64{3, 1, 2, 0}, []uint64{0, 3}) => []uint64{3, 0}
//
// IntersectionUin64Slice([]uint64{3, 1, 2, 1, 0}, []uint64{1, 2, 0}) => []uint64{1, 2, 1, 0}
func IntersectionUin64Slice(s1, s2 []uint64) []uint64 {
	if len(s1) == 0 {
		return nil
	}
	if len(s2) == 0 {
		return s1
	}
	var result []uint64
	for _, i := range s1 {
		for _, j := range s2 {
			if i == j {
				result = append(result, i)
				break
			}
		}
	}
	return result
}

// IntersectionIn64Slice 返回两个 int64 slice 的交集，复杂度 O(m * log(m))
//
// IntersectionIn64Slice([]int64{3, 1, 2, 0}, []int64{0, 3}) => []int64{3, 0}
//
// IntersectionIn64Slice([]int64{3, 1, 2, 1, 0}, []int64{1, 2, 0}) => []int64{1, 2, 1, 0}
func IntersectionInt64Slice(s1, s2 []int64) []int64 {
	m := make(map[int64]bool)
	nn := make([]int64, 0)
	for _, v := range s1 {
		m[v] = true
	}
	for _, v := range s2 {
		if _, ok := m[v]; ok {
			nn = append(nn, v)
		}
	}
	return nn
}

// Remove 删除 slice 在 removes 中存在的元素。
//
// RemoveSlice([]string{"a", "b", "c", "a"}, "a") => []string{"b", "c"})
//
// RemoveSlice([]string{"a", "b", "c", "a"}, "b", "c") => []string{"a", "a"})
func RemoveSlice(ss []string, removes ...string) []string {
	m := make(map[string]struct{})
	for _, rm := range removes {
		m[rm] = struct{}{}
	}
	result := make([]string, 0, len(ss))
	for _, s := range ss {
		if _, ok := m[s]; ok {
			continue
		}
		result = append(result, s)
	}
	return result
}

func Exist(slice []string, val string) bool {
	for _, v := range slice {
		if v == val {
			return true
		}
	}
	return false
}

// NormalizeNewlines normalizes \r\n (windows) and \r (mac)
// into \n (unix).
//
// There are 3 ways to represent a newline.
//   Unix: using single character LF, which is byte 10 (0x0a), represented as “” in Go string literal.
//   Windows: using 2 characters: CR LF, which is bytes 13 10 (0x0d, 0x0a), represented as “” in Go string literal.
//   Mac OS: using 1 character CR (byte 13 (0x0d)), represented as “” in Go string literal. This is the least popular.
func NormalizeNewlines(d []byte) []byte {
	// replace CR LF \r\n (windows) with LF \n (unix)
	d = bytes.Replace(d, []byte{13, 10}, []byte{10}, -1)
	// replace CF \r (mac) with LF \n (unix)
	d = bytes.Replace(d, []byte{13}, []byte{10}, -1)
	return d
}

func SplitIfEmptyString(s, sep string) []string {
	if s == "" {
		return []string{}
	}

	return strings.SplitN(s, sep, -1)
}

var fontKinds = [][]int{{10, 48}, {26, 97}, {26, 65}}

// RandStr 获取随机字符串
func RandStr(size int) string {
	result := make([]byte, size)
	rand.Seed(time.Now().UnixNano())
	for i := 0; i < size; i++ {
		ikind := rand.Intn(3)
		scope, base := fontKinds[ikind][0], fontKinds[ikind][1]
		result[i] = uint8(base + rand.Intn(scope))
	}
	return string(result)
}

// ParseVersion 序列化版本 "1.05.1" --> "1.5.1",
func ParseVersion(version string) string {
	// ISO/IEC 14651:2011
	const maxByte = 1<<8 - 1
	vo := make([]byte, 0, len(version)+8)
	j := -1
	for i := 0; i < len(version); i++ {
		b := version[i]
		if '0' > b || b > '9' {
			vo = append(vo, b)
			j = -1
			continue
		}
		if j == -1 {
			vo = append(vo, 0x00)
			j = len(vo) - 1
		}
		if vo[j] == 1 && vo[j+1] == '0' {
			vo[j+1] = b
			continue
		}
		if vo[j]+1 > maxByte {
			panic("VersionOrdinal: invalid version")
		}
		vo = append(vo, b)
		vo[j]++
	}
	return string(vo)
}

// FlatErrors 将 errors 打平为一个 error
func FlatErrors(errs []error, sep string) error {
	var errMsgs []string
	for _, err := range errs {
		errMsgs = append(errMsgs, err.Error())
	}
	return fmt.Errorf("%s", Join(errMsgs, sep, true))
}

func ContainsOrEmpty(source, target string) bool {
	return target == "" || strings.Contains(source, target)
}

func FirstNoneEmpty(strs ...string) string {
	for _, str := range strs {
		if len(str) > 0 {
			return str
		}
	}
	return ""
}
