/*
Copyright 2022 The Koordinator Authors.
Copyright 2017 The Kubernetes Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cpuset

import (
	"bytes"
	"fmt"
	"sort"
	"strconv"
	"strings"

	"k8s.io/klog/v2"
)

const (
	maxAvailableCPUCount = 4096
)

// CPUSetBuilder is a mutable builder for CPUSet.
// Functions that mutate instances of this type are not thread-safe.
type CPUSetBuilder struct {
	result CPUSet
	done   bool
}

// NewCPUSetBuilder returns a mutable CPUSet builder.
func NewCPUSetBuilder() *CPUSetBuilder {
	return &CPUSetBuilder{
		result: CPUSet{
			elems: map[int]struct{}{},
		},
	}
}

// Add adds the supplied elements to the result.
// Calling Add after calling Result has no effect.
func (b *CPUSetBuilder) Add(elems ...int) {
	if b.done {
		return
	}
	for _, elem := range elems {
		b.result.elems[elem] = struct{}{}
	}
}

// Result returns the result CPUSet containing all elements that were
// previously added to this builder. Subsequent calls to Add have no effect.
func (b *CPUSetBuilder) Result() CPUSet {
	b.done = true
	return b.result
}

// CPUSet is a thread-safe, immutable set-like data structure for CPU IDs.
type CPUSet struct {
	elems map[int]struct{}
}

// NewCPUSet returns a new CPUSet containing the supplied elements.
func NewCPUSet(cpus ...int) CPUSet {
	b := NewCPUSetBuilder()
	for _, c := range cpus {
		b.Add(c)
	}
	return b.Result()
}

// Clone returns a copy of this CPUSet.
func (s CPUSet) Clone() CPUSet {
	b := NewCPUSetBuilder()
	for elem := range s.elems {
		b.Add(elem)
	}
	return b.Result()
}

// Size returns the number of elements in this CPUSet.
func (s CPUSet) Size() int {
	return len(s.elems)
}

// IsEmpty returns true if there are zero elements in this CPUSet.
func (s CPUSet) IsEmpty() bool {
	return s.Size() == 0
}

// Contains returns true if the supplied element is present in this CPUSet.
func (s CPUSet) Contains(cpu int) bool {
	_, found := s.elems[cpu]
	return found
}

// Equals returns true if the supplied CPUSet contains exactly the same elements
// as this CPUSet (s IsSubsetOf s2 and s2 IsSubsetOf s).
func (s CPUSet) Equals(s2 CPUSet) bool {
	if len(s.elems) != len(s2.elems) {
		return false
	}
	for k := range s.elems {
		if _, ok := s2.elems[k]; !ok {
			return false
		}
	}
	return true
}

// Filter returns a new CPUSet that contains the elements from this
// CPUSet that match the supplied predicate, without mutating the source CPUSet.
func (s CPUSet) Filter(predicate func(int) bool) CPUSet {
	b := NewCPUSetBuilder()
	for cpu := range s.elems {
		if predicate(cpu) {
			b.Add(cpu)
		}
	}
	return b.Result()
}

// FilterNot returns a new CPUSet that contains the elements from this
// CPUSet that do not match the supplied predicate, without mutating the source CPUSet.
func (s CPUSet) FilterNot(predicate func(int) bool) CPUSet {
	b := NewCPUSetBuilder()
	for cpu := range s.elems {
		if !predicate(cpu) {
			b.Add(cpu)
		}
	}
	return b.Result()
}

// IsSubsetOf returns true if the supplied CPUSet contains all the elements
func (s CPUSet) IsSubsetOf(s2 CPUSet) bool {
	result := true
	for cpu := range s.elems {
		if !s2.Contains(cpu) {
			result = false
			break
		}
	}
	return result
}

// Union returns a new CPUSet that contains the elements from this CPUSet
// and the elements from the supplied CPUSet, without mutating either source CPUSet.
func (s CPUSet) Union(s2 CPUSet) CPUSet {
	b := NewCPUSetBuilder()
	for cpu := range s.elems {
		b.Add(cpu)
	}
	for cpu := range s2.elems {
		b.Add(cpu)
	}
	return b.Result()
}

// UnionSlice returns a new CPUSet that contains the elements from this CPUSet
// and the elements from the supplied CPUSet, without mutating either source CPUSet.
func (s CPUSet) UnionSlice(s2 ...int) CPUSet {
	b := NewCPUSetBuilder()
	for cpu := range s.elems {
		b.Add(cpu)
	}
	for _, cpu := range s2 {
		b.Add(cpu)
	}
	return b.Result()
}

// UnionAll returns a new CPUSet that contains the elements from this
// CPUSet and the elements from the supplied sets, without mutating either source CPUSet.
func (s CPUSet) UnionAll(s2 []CPUSet) CPUSet {
	b := NewCPUSetBuilder()
	for cpu := range s.elems {
		b.Add(cpu)
	}
	for _, cs := range s2 {
		for cpu := range cs.elems {
			b.Add(cpu)
		}
	}
	return b.Result()
}

// Intersection returns a new CPUSet that contains the elements
// that are present in both this CPUSet and the supplied CPUSet, without mutating either source CPUSet.
func (s CPUSet) Intersection(s2 CPUSet) CPUSet {
	return s.Filter(func(cpu int) bool { return s2.Contains(cpu) })
}

// Difference returns a new CPUSet that contains the elements that
// are present in this CPUSet and not the supplied CPUSet, without mutating either source CPUSet.
func (s CPUSet) Difference(s2 CPUSet) CPUSet {
	return s.FilterNot(func(cpu int) bool { return s2.Contains(cpu) })
}

// ToSlice returns a slice of integers that contains all elements from this CPUSet.
func (s CPUSet) ToSlice() []int {
	if len(s.elems) == 0 {
		return nil
	}
	result := make([]int, 0, len(s.elems))
	for cpu := range s.elems {
		result = append(result, cpu)
	}
	sort.Ints(result)
	return result
}

// ToSliceNoSort returns a slice of integers that contains all elements from this CPUSet.
func (s CPUSet) ToSliceNoSort() []int {
	if len(s.elems) == 0 {
		return nil
	}
	result := make([]int, 0, len(s.elems))
	for cpu := range s.elems {
		result = append(result, cpu)
	}
	return result
}

// ToInt32Slice returns a slice of int32 values that contains all elements from this CPUSet.
func (s CPUSet) ToInt32Slice() []int32 {
	if len(s.elems) == 0 {
		return nil
	}
	result := make([]int32, 0, len(s.elems))
	for cpu := range s.elems {
		result = append(result, int32(cpu)) // assert cpu id is in int32 range
	}
	sort.Slice(result, func(i, j int) bool {
		return result[i] < result[j]
	})
	return result
}

func (s CPUSet) MarshalText() ([]byte, error) {
	return []byte(s.String()), nil
}

func (s *CPUSet) UnmarshalText(data []byte) error {
	r, err := Parse(string(data))
	if err != nil {
		return err
	}
	s.elems = r.elems
	return nil
}

// String returns a new string representation of the elements in this CPUSet
// in canonical linux CPU list format.
//
// See: http://man7.org/linux/man-pages/man7/cpuset.7.html#FORMATS
func (s CPUSet) String() string {
	if s.IsEmpty() {
		return ""
	}

	elems := s.ToSlice()

	type rng struct {
		start int
		end   int
	}

	ranges := []rng{{elems[0], elems[0]}}

	for i := 1; i < len(elems); i++ {
		lastRange := &ranges[len(ranges)-1]
		// if this element is adjacent to the high end of the last range
		if elems[i] == lastRange.end+1 {
			// then extend the last range to include this element
			lastRange.end = elems[i]
			continue
		}
		// otherwise, start a new range beginning with this element
		ranges = append(ranges, rng{elems[i], elems[i]})
	}

	// construct string from ranges
	var result bytes.Buffer
	for i, r := range ranges {
		if r.start == r.end {
			result.WriteString(strconv.Itoa(r.start))
		} else {
			result.WriteString(fmt.Sprintf("%d-%d", r.start, r.end))
		}
		if i != len(ranges)-1 {
			result.WriteString(",")
		}
	}
	return result.String()
}

// MustParse CPUSet constructs a new CPUSet from a Linux CPU list formatted
// string. Unlike Parse, it does not return an error but rather panics if the
// input cannot be used to construct a CPUSet.
func MustParse(s string) CPUSet {
	res, err := Parse(s)
	if err != nil {
		klog.Fatalf("unable to parse [%s] as CPUSet: %v", s, err)
	}
	return res
}

// Parse CPUSet constructs a new CPUSet from a Linux CPU list formatted string.
//
// See: http://man7.org/linux/man-pages/man7/cpuset.7.html#FORMATS
func Parse(s string) (CPUSet, error) {
	b := NewCPUSetBuilder()

	// Handle empty string.
	if s == "" {
		return b.Result(), nil
	}

	// Split CPU list string:
	// "0-5,34,46-48 => ["0-5", "34", "46-48"]
	ranges := strings.Split(s, ",")

	for _, r := range ranges {
		boundaries := strings.Split(r, "-")
		if len(boundaries) == 1 {
			// Handle ranges that consist of only one element like "34".
			elem, err := strconv.ParseInt(boundaries[0], 10, 32) // assert cpu id is in range of int32
			if err != nil {
				return NewCPUSet(), err
			}
			b.Add(int(elem))
		} else if len(boundaries) == 2 {
			// Handle multi-element ranges like "0-5".
			start, err := strconv.ParseInt(boundaries[0], 10, 32) // assert cpu id is in range of int32
			if err != nil {
				return NewCPUSet(), err
			}
			end, err := strconv.ParseInt(boundaries[1], 10, 32)
			if err != nil {
				return NewCPUSet(), err
			}
			if end > maxAvailableCPUCount {
				return NewCPUSet(), fmt.Errorf("end %d exceed the maximum available CPU count: %d", end, maxAvailableCPUCount)
			}
			// Add all elements to the result.
			// e.g. "0-5", "46-48" => [0, 1, 2, 3, 4, 5, 46, 47, 48].
			for e := start; e <= end; e++ {
				b.Add(int(e))
			}
		} else {
			return NewCPUSet(), fmt.Errorf("invalid format: %s", r)
		}
	}
	return b.Result(), nil
}
