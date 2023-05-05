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

package footnote

import (
	"container/list"
	"regexp"
	"strconv"
	"strings"
)

type seq interface {
	seq()
}

type iindent struct {
	v seq
}
type inewline struct{}
type istr struct {
	v []rune
}
type inum struct {
	v int
}
type iiappend struct {
	v1 seq
	v2 seq
}
type iempty struct{}

func (iindent) seq()  {}
func (inewline) seq() {}
func (istr) seq()     {}
func (inum) seq()     {}
func (iiappend) seq() {}
func (iempty) seq()   {}

func concat(seqs ...seq) seq {
	l := len(seqs)
	if l == 0 {
		return iempty{}
	}
	if l == 1 {
		return seqs[0]
	}
	r := iiappend{seqs[l-2], seqs[l-1]}
	for i := l - 3; i >= 0; i-- {
		r = iiappend{seqs[i], r}
	}
	return r
}
func interleave(join seq, seqs ...seq) seq {
	l := len(seqs)
	if l == 0 {
		return iempty{}
	}
	if l == 1 {
		return seqs[0]
	}
	r := concat(seqs[l-2], join, seqs[l-1])
	for i := l - 3; i >= 0; i-- {
		r = iiappend{seqs[i], iiappend{join, r}}
	}
	return r
}

type seqWithColumn struct {
	s   seq
	col int
}

// sc: seqWithColumn list
func flatten(sc *list.List, col int) []rune {
	if sc.Len() == 0 {
		return []rune{}
	}
	e := sc.Front()
	s := sc.Remove(e).(seqWithColumn)
	switch v := s.s.(type) {
	case iindent:
		sc.PushFront(seqWithColumn{v.v, col})
		return flatten(sc, col)
	case inewline:
		return append([]rune("\n"+spaces(s.col)), flatten(sc, s.col)...)
	case istr:
		return append(v.v, flatten(sc, col+len(v.v))...)
	case inum:
		n := []rune(strconv.Itoa(v.v))
		return append(n, flatten(sc, col+len(n))...)
	case iiappend:
		s1 := v.v1
		s2 := v.v2
		sc.PushFront(seqWithColumn{s2, s.col})
		sc.PushFront(seqWithColumn{s1, s.col})
		return flatten(sc, col)
	case iempty:
		return flatten(sc, col)
	}
	// can't be here
	return nil
}

func spaces(n int) string {
	return mkString(n, rune(' '))
}

func mkString(n int, char rune) string {
	r := []rune("")
	for i := 0; i < n; i++ {
		r = append(r, char)
	}
	return string(r)
}

func indent(s seq) seq {
	return iindent{s}
}
func newline() seq {
	return inewline{}
}
func str(s string) seq {
	return istr{[]rune(s)}
}
func num(s int) seq {
	return inum{s}
}
func iappend(s1, s2 seq) seq {
	return iiappend{s1, s2}
}
func empty() seq {
	return iempty{}
}

// =============================== above impl a wrapper for string building =================================

type FootNote struct {
	content string
	lines   []string
	notes   map[int]string
}

func New(content string) *FootNote {
	return &FootNote{
		content: content,
		notes:   map[int]string{},
	}
}

func (f *FootNote) NoteLine(line int, note string) *FootNote {
	if f.lines == nil {
		f.lines = strings.Split(f.content, "\n")
	}
	if line < len(f.lines) {
		f.notes[line] = note
	}
	return f
}

func (f *FootNote) NotePoint(pos int, note string) *FootNote {
	if f.lines == nil {
		f.lines = strings.Split(f.content, "\n")
	}
	curr := 0
	for l, line := range f.lines {
		curr += len(line) + 1 // \n
		if curr > pos {
			return f.NoteLine(l, note)
		}
	}
	return f
}

func (f *FootNote) NoteRegex(regex *regexp.Regexp, note string) *FootNote {
	if f.lines == nil {
		f.lines = strings.Split(f.content, "\n")
	}
	loc := regex.FindStringSubmatchIndex(f.content)
	if len(loc) != 0 {
		if len(loc) == 2 { // if no subcapture, use whole capture's position
			return f.NotePoint(loc[0], note)
		} else if len(loc) == 4 { // if exist sub capture, use the 1st subcapture's position
			return f.NotePoint(loc[2], note)
		}
	}
	return f
}

func (f *FootNote) Dump() string {
	notelines := map[int]string{}
	for l := range f.notes {
		notelines[l] = f.lines[l]
	}
	alignLines(notelines)
	for l := range f.notes {
		f.lines[l] = notelines[l] + "(" + strconv.Itoa(l+1) + ")"
	}
	// assemble seqs
	content := assemLines(f.lines)
	partingLine := assemPartingLine()
	notes := assemNotes(f.notes)
	l := list.New()
	l.PushFront(seqWithColumn{notes, 0})
	l.PushFront(seqWithColumn{partingLine, 0})
	l.PushFront(seqWithColumn{content, 0})
	return string(flatten(l, 0))
}

// update in place, default to 75 column
func alignLines(ss map[int]string) {
	maxcol := 72
	for _, s := range ss {
		if len([]rune(s)) > maxcol {
			maxcol = len([]rune(s))
		}
	}
	maxcol += 3 // add extra spaces
	for i := range ss {
		ss[i] = ss[i] + spaces(maxcol-len([]rune(ss[i])))
	}
	return
}

func assemLines(lines []string) seq {
	seqs := []seq{}
	for _, l := range lines {
		seqs = append(seqs, concat(str(l), newline()))
	}
	return concat(seqs...)
}

func assemPartingLine() seq {
	return concat(str(mkString(80, '-')), newline())
}

func assemNotes(notes map[int]string) seq {
	r := empty()
	for l, note := range notes {
		r = concat(r, newline(), num(l+1), str(") "))
		noteSplit := strings.Split(note, "\n")

		noteseq := []seq{}
		for _, n := range noteSplit {
			noteseq = append(noteseq, str(n))
		}
		r = concat(r, indent(interleave(newline(), noteseq...)))
	}
	return r
}
