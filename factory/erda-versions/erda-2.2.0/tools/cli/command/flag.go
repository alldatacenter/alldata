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
	"strconv"
	"strings"
)

type Flag interface {
	Flag()
	DefaultV() string
}

type IntFlag struct {
	Short        string
	Name         string
	Doc          string
	DefaultValue int
}
type Uint64Flag struct {
	Short        string
	Name         string
	Doc          string
	DefaultValue uint64
}
type FloatFlag struct {
	Short        string
	Name         string
	Doc          string
	DefaultValue float64
}

type BoolFlag struct {
	Short        string
	Name         string
	Doc          string
	DefaultValue bool
}
type StringFlag struct {
	Short        string
	Name         string
	Doc          string
	DefaultValue string
}

type IPFlag struct {
	Short        string
	Name         string
	Doc          string
	DefaultValue string
}

type StringListFlag struct {
	Short        string
	Name         string
	Doc          string
	DefaultValue []string
}

func (IntFlag) Flag()        {}
func (Uint64Flag) Flag()     {}
func (FloatFlag) Flag()      {}
func (BoolFlag) Flag()       {}
func (StringFlag) Flag()     {}
func (IPFlag) Flag()         {}
func (StringListFlag) Flag() {}

func (v IntFlag) DefaultV() string {
	return strconv.Itoa(v.DefaultValue)
}
func (v Uint64Flag) DefaultV() string {
	return strconv.FormatUint(v.DefaultValue, 10)
}
func (v FloatFlag) DefaultV() string {
	return fmt.Sprintf("%g", v.DefaultValue)
}
func (v BoolFlag) DefaultV() string {
	return fmt.Sprintf("%v", v.DefaultValue)
}
func (v StringFlag) DefaultV() string {
	return v.DefaultValue
}
func (v IPFlag) DefaultV() string {
	return v.DefaultValue
}

// []string{`a`, `b`}
func (v StringListFlag) DefaultV() string {
	if len(v.DefaultValue) == 0 {
		return "nil"
	}
	tmpl := "[]string{%s}"
	elems := []string{}
	for _, e := range v.DefaultValue {
		elems = append(elems, "`"+e+"`")
	}
	return fmt.Sprintf(tmpl, strings.Join(elems, ", "))
}
