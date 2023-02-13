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

package table

type Matcher interface {
	Match(interface{}) bool
}

type AndMatcher struct {
	matchers []Matcher
}

func (m *AndMatcher) Match(v interface{}) bool {
	for _, matcher := range m.matchers {
		if !matcher.Match(v) {
			return false
		}
	}
	return true
}

type OrMatcher struct {
	matchers []Matcher
}

func (m *OrMatcher) Match(v interface{}) bool {
	for _, matcher := range m.matchers {
		if matcher.Match(v) {
			return true
		}
	}
	return false
}

type NotMatcher struct {
	matcher Matcher
}

func (m *NotMatcher) Match(v interface{}) bool {
	return !m.matcher.Match(v)
}

type Match func(v interface{}) bool

type FuncMatcher struct {
	match Match
}

func (m *FuncMatcher) Match(v interface{}) bool {
	return m.match(v)
}

func ToMatcher(m Match) *FuncMatcher {
	return &FuncMatcher{match: m}
}

func And(matchers ...Matcher) *AndMatcher {
	return &AndMatcher{matchers: matchers}
}

func Or(matchers ...Matcher) *OrMatcher {
	return &OrMatcher{matchers: matchers}
}

func Not(matcher Matcher) Matcher {
	return &NotMatcher{matcher: matcher}
}
