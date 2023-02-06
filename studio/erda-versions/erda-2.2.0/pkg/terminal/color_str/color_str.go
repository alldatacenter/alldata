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

package color_str

import (
	"strings"
)

//          foreground background
// black        30         40
// red          31         41
// green        32         42
// yellow       33         43
// blue         34         44
// magenta      35         45
// cyan         36         46
// white        37         47

// reset             0  (everything back to normal)
// bold/bright       1  (often a brighter shade of the same colour)
// underline         4
// inverse           7  (swap foreground and background colours)

type Option string

func join(op []Option, i string) string {
	ops := []string{}
	for _, o := range op {
		ops = append(ops, string(o))
	}
	return strings.Join(ops, i)
}

var (
	BlackBg   Option = "40"
	RedBg     Option = "41"
	GreenBg   Option = "42"
	YellowBg  Option = "43"
	BlueBg    Option = "44"
	MagentaBg Option = "45"
	CyanBg    Option = "46"
	WhiteBg   Option = "47"

	Reset     Option = "0"
	Bold      Option = "1"
	Underline Option = "4"
	Inverse   Option = "7"
)

func Black(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("30")), ";") + "m" + s + "\033[0m"
}
func Red(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("31")), ";") + "m" + s + "\033[0m"
}
func Green(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("32")), ";") + "m" + s + "\033[0m"
}
func Yellow(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("33")), ";") + "m" + s + "\033[0m"
}
func Blue(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("34")), ";") + "m" + s + "\033[0m"
}
func Magenta(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("35")), ";") + "m" + s + "\033[0m"
}
func Cyan(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("36")), ";") + "m" + s + "\033[0m"
}
func White(s string, op ...Option) string {
	return "\033[" + join(append(op, Option("37")), ";") + "m" + s + "\033[0m"
}
