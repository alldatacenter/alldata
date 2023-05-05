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

package sexp

import (
	"errors"
	"fmt"
	"reflect"
	"strconv"
	"strings"
	"unicode"
)

// dynamic types for i are string, qString, int, float64, list, and error.
type Sexp struct {
	I interface{}
}
type QString string
type List []Sexp

func (s Sexp) String() string {
	return fmt.Sprintf("%v", s.I)
}

// func (q qString) String() string {
// 	return strconv.Quote(string(q))
// }

func (l List) String() string {
	if len(l) == 0 {
		return "()"
	}
	b := fmt.Sprintf("(%v", l[0])
	for _, s := range l[1:] {
		b = fmt.Sprintf("%s %v", b, s)
	}
	return b + ")"
}

// Parse parses a string into a Go representation of an s-expression.
//
// Quoted strings go from one " to the next.  There is no escape character,
// all characters except " are valid.
//
// Otherwise atoms are any string of characters between any of '(', ')',
// '"', or white space characters.  If the atom parses as a Go int type
// using strconv.Atoi, it is taken as int; if it parses as a Go float64
// type using strconv.ParseFloat, it is taken as float64; otherwise it is
// taken as an unquoted string.
//
// Unmatched (, ), or " are errors.
// An empty or all whitespace input string is an error.
// Left over text after the sexp is an error.
//
// An empty list is a valid sexp, but there is no nil, no cons, no dot.
func Parse(s string) (Sexp, error) {
	s1, rem := ps2(s, -1)
	if err, isErr := s1.I.(error); isErr {
		return Sexp{}, err
	}
	if rem > "" {
		return s1, errors.New("Left over text: " + rem)
	}
	return s1, nil
}

// recursive.  n = -1 means not parsing a list.  n >= 0 means the number
// of list elements parsed so far.  string result is unparsed remainder
// of the input string s0.
func ps2(s0 string, n int) (x Sexp, rem string) {
	tok, s1 := gettok(s0)
	switch t := tok.(type) {
	case error:
		return Sexp{tok}, s1
	case nil: // this is also an error
		if n < 0 {
			return Sexp{errors.New("blank input string")}, s0
		} else {
			return Sexp{errors.New("unmatched (")}, ""
		}
	case byte:
		switch {
		case t == '(':
			x, s1 = ps2(s1, 0) // x is a list
			if _, isErr := x.I.(error); isErr {
				return x, s0
			}
		case n < 0:
			return Sexp{errors.New("unmatched )")}, ""
		default:
			// found end of list.  allocate space for it.
			return Sexp{make(List, n)}, s1
		}
	default:
		x = Sexp{tok} // x is an atom
	}
	if n < 0 {
		// not in a list, just return the s-expression x
		return x, s1
	}
	// in a list.  hold on to x while we parse the rest of the list.
	l, s1 := ps2(s1, n+1)
	// result l is either an error or the allocated list, not completely
	// filled in yet.
	if _, isErr := l.I.(error); !isErr {
		// as long as no errors, drop x into its place in the list
		l.I.(List)[n] = x
	}
	return l, s1
}

// gettok gets one token from string s.
// return values are the token and the remainder of the string.
// dynamic type of tok indicates result:
// nil:  no token.  string was empty or all white space.
// byte:  one of '(' or ')'
// otherwise string, qString, int, float64, or error.
func gettok(s string) (tok interface{}, rem string) {
	s = strings.TrimSpace(s)
	if s == "" {
		return nil, ""
	}
	switch s[0] {
	case '(', ')':
		return s[0], s[1:]
	case '"':
		if i := strings.Index(s[1:], `"`); i >= 0 {
			return QString(s[1 : i+1]), s[i+2:]
		}
		return errors.New(`unmatched "`), s
	}
	i := 1
	for i < len(s) && s[i] != '(' && s[i] != ')' && s[i] != '"' &&
		!unicode.IsSpace(rune(s[i])) {
		i++
	}
	if j, err := strconv.Atoi(s[:i]); err == nil {
		return j, s[i:]
	}
	if f, err := strconv.ParseFloat(s[:i], 64); err == nil {
		return f, s[i:]
	}
	return s[:i], s[i:]
}

func (s Sexp) Dump(i int) {
	fmt.Printf("%*s%v: ", i*3, "", reflect.TypeOf(s.I))
	if l, isList := s.I.(List); isList {
		fmt.Println(len(l), "elements")
		for _, e := range l {
			e.Dump(i + 1)
		}
	} else {
		fmt.Println(s.I)
	}
}

type Context struct {
	Funcs map[string]func([]interface{}) (Sexp, error)
	Vars  map[string]Sexp
}

func Eval(ctx *Context, s Sexp) (Sexp, error) {
	if l, islist := s.I.(List); islist {
		if len(l) == 0 {
			return Sexp{}, nil
		}
		if _, ok := l[0].I.(string); !ok {
			return Sexp{}, fmt.Errorf("'%v' is not function", l[0].I)
		}
		fun_s := l[0].I.(string)
		fun, ok := ctx.Funcs[fun_s]
		if !ok {
			return Sexp{}, fmt.Errorf("undefined function '%s'", fun_s)
		}
		params := []interface{}{}
		for i := 1; i < len(l); i++ {
			p, err := Eval(ctx, l[i])
			if err != nil {
				return Sexp{}, err
			}
			params = append(params, p.I)
		}
		r, err := fun(params)
		if err != nil {
			return Sexp{}, fmt.Errorf("call function '%s' err: %v", fun_s, err)
		}
		return r, nil
	}
	switch t := s.I.(type) {
	case string:
		sexp, ok := ctx.Vars[t]
		if !ok {
			return Sexp{}, fmt.Errorf("undefined variable '%s'", t)
		}
		return sexp, nil
	case QString, int, float64:
		return s, nil
	default:
		return Sexp{}, fmt.Errorf("unsupported type: %v", t)
	}
}

func ReferencedVars(s Sexp) []string {
	refs := []string{}
	if l, islist := s.I.(List); islist {
		if len(l) == 0 {
			return []string{}
		}
		for i := 1; i < len(l); i++ {
			r := ReferencedVars(l[i])
			refs = append(refs, r...)
		}
	}
	switch t := s.I.(type) {
	case string:
		refs = append(refs, t)
	}
	return refs
}

func format(p []interface{}) (Sexp, error) {
	if len(p) == 0 {
		return Sexp{}, fmt.Errorf("empty format parameters")
	}
	p0, ok := p[0].(QString)
	if !ok {
		return Sexp{}, fmt.Errorf("illegal format parameters[0]")
	}
	for i := 1; i < len(p); i++ {
		switch p[i].(type) {
		case QString, int, float64:
		default:
			return Sexp{}, fmt.Errorf("illegal format parameters[%d]", i)
		}
	}

	r := fmt.Sprintf(string(p0), p[1:]...)
	return Sexp{QString(r)}, nil
}

var Builtin Context = Context{
	Funcs: map[string]func([]interface{}) (Sexp, error){
		"format": format,
	},
	Vars: map[string]Sexp{
		"xxxx": {"xxx"},
	},
}
