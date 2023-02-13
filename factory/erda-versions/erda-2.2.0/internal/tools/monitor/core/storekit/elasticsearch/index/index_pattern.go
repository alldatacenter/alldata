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

package index

import (
	"errors"
	"fmt"
	"strings"
)

type (
	// Pattern
	Pattern struct {
		Pattern  string
		Segments []*PatternSegment
		Keys     []string
		KeyNum   int
		Vars     []string
		VarNum   int
	}
	// PatternSegment .
	PatternSegment struct {
		Type PatternSegmentType
		Name string
	}
	// PatternSegmentType .
	PatternSegmentType uint8
	// MatchResult .
	MatchResult struct {
		Pattern *Pattern
		Keys    []string
		Vars    []string
	}
)

// PatternSegmentType values
const (
	PatternSegmentStatic PatternSegmentType = iota
	PatternSegmentKey
	PatternSegmentVar
)

const (
	// IndexVarNone .
	IndexVarNone = ""
	// IndexVarNumber .
	IndexVarNumber = "number"
	// IndexVarTimestamp .
	IndexVarTimestamp = "timestamp"
)

// InvalidPatternValueChars
const InvalidPatternValueChars = "-."

var patternSegmentMatcher = map[rune]struct {
	typ     PatternSegmentType
	endChar rune
}{
	'<': {PatternSegmentKey, '>'},
	'{': {PatternSegmentVar, '}'},
}

// String .
func (s *PatternSegment) String() string { return fmt.Sprint(*s) }

// String .
func (p *Pattern) String() string { return p.Pattern }

// Match .
func (p *Pattern) Match(text, invalidChars string) (*MatchResult, bool) {
	var lastTyp PatternSegmentType
	result := &MatchResult{
		Pattern: p,
		Keys:    make([]string, 0, len(p.Keys)),
		Vars:    make([]string, 0, len(p.Vars)),
	}
	validate := func(text string) bool {
		if len(invalidChars) > 0 {
			if strings.ContainsAny(text, invalidChars) {
				return false
			}
		}
		return true
	}
	for _, s := range p.Segments {
		switch s.Type {
		case PatternSegmentStatic:
			if lastTyp > 0 {
				idx := strings.Index(text, s.Name)
				if idx < 0 {
					return nil, false
				}
				switch lastTyp {
				case PatternSegmentKey:
					if !validate(text[:idx]) {
						return nil, false
					}
					result.Keys = append(result.Keys, text[:idx])
				case PatternSegmentVar:
					if !validate(text[:idx]) {
						return nil, false
					}
					result.Vars = append(result.Vars, text[:idx])
				}
				text = text[idx+len(s.Name):]
				lastTyp = PatternSegmentStatic
			} else if !strings.HasPrefix(text, s.Name) {
				return nil, false
			} else {
				text = text[len(s.Name):]
			}
		case PatternSegmentKey, PatternSegmentVar:
			lastTyp = s.Type
		}
	}
	switch lastTyp {
	case PatternSegmentKey:
		if !validate(text) {
			return nil, false
		}
		result.Keys = append(result.Keys, text)
	case PatternSegmentVar:
		if !validate(text) {
			return nil, false
		}
		result.Vars = append(result.Vars, text)
	default:
		if len(text) > 0 {
			return nil, false
		}
	}
	return result, true
}

// BuildPattern .
func BuildPattern(ptn string) (p *Pattern, err error) {
	chars := []rune(ptn)
	start, i, n := 0, 0, len(chars)
	p = &Pattern{Pattern: ptn}
	for ; i < n; i++ {
		if m, ok := patternSegmentMatcher[chars[i]]; ok {
			if start < i {
				p.Segments = append(p.Segments, &PatternSegment{
					Type: PatternSegmentStatic,
					Name: string(chars[start:i]),
				})
			}
			i++
		loop:
			for begin := i; i < n; i++ {
				switch chars[i] {
				case m.endChar:
					p.Segments = append(p.Segments, &PatternSegment{
						Type: m.typ,
						Name: string(chars[begin:i]),
					})
					start = i + 1
					break loop
				}
			}
			if i >= n {
				return nil, fmt.Errorf("invalid pattern %q", ptn)
			}
		}
	}
	if start < n {
		p.Segments = append(p.Segments, &PatternSegment{
			Type: PatternSegmentStatic,
			Name: string(chars[start:]),
		})
	}

	// setup keys and vars
	for _, s := range p.Segments {
		switch s.Type {
		case PatternSegmentKey:
			p.Keys = append(p.Keys, s.Name)
		case PatternSegmentVar:
			p.Vars = append(p.Vars, s.Name)
		}
	}
	p.KeyNum, p.VarNum = len(p.Keys), len(p.Vars)

	// check
	lastTyp := PatternSegmentStatic
	for _, s := range p.Segments {
		if lastTyp != PatternSegmentStatic && s.Type != PatternSegmentStatic {
			return nil, fmt.Errorf("invalid pattern %q", ptn)
		}
		lastTyp = s.Type
	}
	return p, nil
}

// CheckVars .
func (p *Pattern) CheckVars() error {
	for _, s := range p.Segments {
		if s.Type == PatternSegmentVar {
			switch s.Name {
			case IndexVarNumber:
			case IndexVarTimestamp:
			case IndexVarNone:
			default:
				return fmt.Errorf("invalid pattern %q, unknown var %q", p.Pattern, s.Name)
			}
		}
	}
	return nil
}

// ErrKeyLength .
var ErrKeyLength = errors.New("invalid keys length")

// ErrVarLength
var ErrVarLength = errors.New("invalid vars length")

// Fill .
func (p *Pattern) Fill(keys ...string) (string, error) {
	if p.VarNum > 0 {
		return "", ErrVarLength
	}
	if p.KeyNum != len(keys) {
		return "", ErrKeyLength
	}
	sb := strings.Builder{}
	k := 0
	for _, s := range p.Segments {
		if s.Type == PatternSegmentStatic {
			sb.WriteString(s.Name)
		} else {
			sb.WriteString(keys[k])
			k++
		}
	}
	return sb.String(), nil
}
