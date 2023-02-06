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

package command

import (
	"fmt"
	"net"
	"strconv"
	"strings"
)

const (
	validateErrStr = "validate arg(%d) fail: %v, should be %s type, e.g %v"
)

type Arg interface {
	Validate(idx int, src string) error
	// return (converted_value, type_string)
	Convert(src string) interface{}
	// return converted_value_type_string
	ConvertType() string
	GetName() string
	// set name
	Name(name string) Arg
	// is an optional arg?
	Option() Arg

	IsOption() bool
}

type CommonArg struct {
	name string
	opt  bool
}

type StringArg struct {
	CommonArg
}
type IntArg struct {
	CommonArg
}
type FloatArg struct {
	CommonArg
}
type IPArg struct {
	CommonArg
}
type Level2Arg struct {
	CommonArg
}
type Level3Arg struct {
	CommonArg
}
type Level4Arg struct {
	CommonArg
}
type Level5Arg struct {
	CommonArg
}
type Level6Arg struct {
	CommonArg
}

func (a CommonArg) GetName() string {
	return a.name
}

func (a StringArg) Name(name string) Arg {
	a.name = name
	return a
}
func (a IntArg) Name(name string) Arg {
	a.name = name
	return a
}
func (a FloatArg) Name(name string) Arg {
	a.name = name
	return a
}
func (a IPArg) Name(name string) Arg {
	a.name = name
	return a
}
func (a Level2Arg) Name(name string) Arg {
	a.name = name
	return a
}
func (a Level3Arg) Name(name string) Arg {
	a.name = name
	return a
}
func (a Level4Arg) Name(name string) Arg {
	a.name = name
	return a
}
func (a Level5Arg) Name(name string) Arg {
	a.name = name
	return a
}
func (a Level6Arg) Name(name string) Arg {
	a.name = name
	return a
}

func (a StringArg) Option() Arg {
	a.opt = true
	return a
}
func (a IntArg) Option() Arg {
	a.opt = true
	return a
}
func (a FloatArg) Option() Arg {
	a.opt = true
	return a
}
func (a IPArg) Option() Arg {
	a.opt = true
	return a
}
func (a Level2Arg) Option() Arg {
	a.opt = true
	return a
}
func (a Level3Arg) Option() Arg {
	a.opt = true
	return a
}
func (a Level4Arg) Option() Arg {
	a.opt = true
	return a
}
func (a Level5Arg) Option() Arg {
	a.opt = true
	return a
}
func (a Level6Arg) Option() Arg {
	a.opt = true
	return a
}

func (a CommonArg) IsOption() bool {
	return a.opt
}

func (StringArg) Validate(idx int, src string) error {
	return nil
}
func (IntArg) Validate(idx int, src string) error {
	_, err := strconv.Atoi(src)
	if err != nil {
		return fmt.Errorf(validateErrStr, idx, err, "int", 1)
	}
	return nil
}
func (FloatArg) Validate(idx int, src string) error {
	_, err := strconv.ParseFloat(src, 64)
	if err != nil {
		return fmt.Errorf(validateErrStr, idx, err, "float", 1.2)
	}
	return nil
}
func (IPArg) Validate(idx int, src string) error {
	ip := net.ParseIP(src)
	if ip.String() == "<nil>" {
		return fmt.Errorf(validateErrStr, idx, "parseIP fail", "ip", "1.2.3.4")
	}
	return nil
}

func validateLevelArg(idx int, src string, level int, example string) error {
	src = strings.Trim(src, "/")
	parts := strings.Split(src, "/")
	if len(parts) == 1 && parts[0] == "" { // just "/", it's fine
		return nil
	}
	for _, p := range parts {
		if p == "" {
			return fmt.Errorf(validateErrStr, idx, "empty part", "LEVEL"+strconv.Itoa(level), example)
		}
	}
	if len(parts) > level {
		return fmt.Errorf(validateErrStr, idx, "too many parts", "LEVEL"+strconv.Itoa(level), example)
	}
	return nil
}
func (Level2Arg) Validate(idx int, src string) error {
	return validateLevelArg(idx, src, 2, "/a/b")
}
func (Level3Arg) Validate(idx int, src string) error {
	return validateLevelArg(idx, src, 3, "/a/b/c")
}
func (Level4Arg) Validate(idx int, src string) error {
	return validateLevelArg(idx, src, 4, "/a/b/c/d")
}
func (Level5Arg) Validate(idx int, src string) error {
	return validateLevelArg(idx, src, 5, "/a/b/c/d/e")
}
func (Level6Arg) Validate(idx int, src string) error {
	return validateLevelArg(idx, src, 6, "/a/b/c/d/e/f")
}

func (StringArg) Convert(src string) interface{} {
	return src
}

func (IntArg) Convert(src string) interface{} {
	v, _ := strconv.Atoi(src)
	return v
}
func (FloatArg) Convert(src string) interface{} {
	v, _ := strconv.ParseFloat(src, 64)
	return v
}

func (IPArg) Convert(src string) interface{} {
	return net.IP([]byte(src))
}

type Level2 struct {
	V [2]string
}
type Level3 struct {
	V [3]string
}
type Level4 struct {
	V [4]string
}
type Level5 struct {
	V [5]string
}
type Level6 struct {
	V [6]string
}

func (Level2) DicePathAble() {}
func (Level3) DicePathAble() {}
func (Level4) DicePathAble() {}
func (Level5) DicePathAble() {}

func (Level2Arg) Convert(src string) interface{} {
	src = strings.Trim(src, "/")
	parts := strings.SplitN(src, "/", 2)
	if len(parts) == 2 {
		return Level2{[2]string{parts[0], parts[1]}}
	}
	if len(parts) == 1 {
		return Level2{[2]string{parts[0], ""}}
	}
	return Level2{[2]string{"", ""}}
}

func (Level3Arg) Convert(src string) interface{} {
	src = strings.Trim(src, "/")
	parts := strings.SplitN(src, "/", 3)
	if len(parts) == 3 {
		return Level3{[3]string{parts[0], parts[1], parts[2]}}
	}
	if len(parts) == 2 {
		return Level3{[3]string{parts[0], parts[1], ""}}
	}
	if len(parts) == 1 {
		return Level3{[3]string{parts[0], "", ""}}
	}
	return Level3{[3]string{"", "", ""}}
}

func (Level4Arg) Convert(src string) interface{} {
	src = strings.Trim(src, "/")
	parts := strings.SplitN(src, "/", 4)
	if len(parts) == 4 {
		return Level4{[4]string{parts[0], parts[1], parts[2], parts[3]}}
	}
	if len(parts) == 3 {
		return Level4{[4]string{parts[0], parts[1], parts[2], ""}}
	}
	if len(parts) == 2 {
		return Level4{[4]string{parts[0], parts[1], "", ""}}
	}
	if len(parts) == 1 {
		return Level4{[4]string{parts[0], "", "", ""}}
	}
	return Level4{[4]string{"", "", "", ""}}
}
func (Level5Arg) Convert(src string) interface{} {
	src = strings.Trim(src, "/")
	parts := strings.SplitN(src, "/", 5)
	if len(parts) == 5 {
		return Level5{[5]string{parts[0], parts[1], parts[2], parts[3], parts[4]}}
	}
	if len(parts) == 4 {
		return Level5{[5]string{parts[0], parts[1], parts[2], parts[3], ""}}
	}
	if len(parts) == 3 {
		return Level5{[5]string{parts[0], parts[1], parts[2], "", ""}}
	}
	if len(parts) == 2 {
		return Level5{[5]string{parts[0], parts[1], "", "", ""}}
	}
	if len(parts) == 1 {
		return Level5{[5]string{parts[0], "", "", "", ""}}
	}
	return Level5{[5]string{"", "", "", "", ""}}
}
func (Level6Arg) Convert(src string) interface{} {
	src = strings.Trim(src, "/")
	parts := strings.SplitN(src, "/", 6)
	if len(parts) == 6 {
		return Level6{[6]string{parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]}}
	}
	if len(parts) == 5 {
		return Level6{[6]string{parts[0], parts[1], parts[2], parts[3], parts[4], ""}}
	}
	if len(parts) == 4 {
		return Level6{[6]string{parts[0], parts[1], parts[2], parts[3], "", ""}}
	}
	if len(parts) == 3 {
		return Level6{[6]string{parts[0], parts[1], parts[2], "", "", ""}}
	}
	if len(parts) == 2 {
		return Level6{[6]string{parts[0], parts[1], "", "", "", ""}}
	}
	if len(parts) == 1 {
		return Level6{[6]string{parts[0], "", "", "", "", ""}}
	}
	return Level6{[6]string{"", "", "", "", "", ""}}
}

func (StringArg) ConvertType() string {
	return "string"
}
func (IntArg) ConvertType() string {
	return "int"
}

func (FloatArg) ConvertType() string {
	return "float64"
}
func (IPArg) ConvertType() string {
	return "net.IP"
}
func (Level2Arg) ConvertType() string {
	return "command.Level2"
}
func (Level3Arg) ConvertType() string {
	return "command.Level3"
}
func (Level4Arg) ConvertType() string {
	return "command.Level4"
}
func (Level5Arg) ConvertType() string {
	return "command.Level5"
}
func (Level6Arg) ConvertType() string {
	return "command.Level6"
}
