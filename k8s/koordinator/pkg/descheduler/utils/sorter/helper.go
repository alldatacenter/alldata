/*
Copyright 2022 The Koordinator Authors.

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

package sorter

import (
	"sort"

	corev1 "k8s.io/api/core/v1"
)

// CompareFn compares p1 and p2 and returns:
//
//   -1 if p1 <  p2
//    0 if p1 == p2
//   +1 if p1 >  p2
//
type CompareFn func(p1, p2 *corev1.Pod) int

// MultiSorter implements the Sort interface
type MultiSorter struct {
	ascending bool
	pods      []*corev1.Pod
	cmp       []CompareFn
}

// Sort sorts the pods according to the cmp functions passed to OrderedBy.
func (ms *MultiSorter) Sort(pods []*corev1.Pod) {
	ms.pods = pods
	sort.Sort(ms)
}

// OrderedBy returns a Sorter sorted using the cmp functions, sorts in ascending order by default
func OrderedBy(cmp ...CompareFn) *MultiSorter {
	return &MultiSorter{
		ascending: true,
		cmp:       cmp,
	}
}

func (ms *MultiSorter) Ascending() *MultiSorter {
	ms.ascending = true
	return ms
}

func (ms *MultiSorter) Descending() *MultiSorter {
	ms.ascending = false
	return ms
}

// Len is part of sort.Interface.
func (ms *MultiSorter) Len() int {
	return len(ms.pods)
}

// Swap is part of sort.Interface.
func (ms *MultiSorter) Swap(i, j int) {
	ms.pods[i], ms.pods[j] = ms.pods[j], ms.pods[i]
}

// Less is part of sort.Interface.
func (ms *MultiSorter) Less(i, j int) bool {
	p1, p2 := ms.pods[i], ms.pods[j]
	var k int
	for k = 0; k < len(ms.cmp)-1; k++ {
		cmpResult := ms.cmp[k](p1, p2)
		// p1 is less than p2
		if cmpResult < 0 {
			return ms.ascending
		}
		// p1 is greater than p2
		if cmpResult > 0 {
			return !ms.ascending
		}
	}
	cmpResult := ms.cmp[k](p1, p2)
	if cmpResult < 0 {
		return ms.ascending
	}
	return !ms.ascending
}

// cmpBool compares booleans, placing true before false
func cmpBool(a, b bool) int {
	if a == b {
		return 0
	}
	if !b {
		return -1
	}
	return 1
}

func Reverse(cmp CompareFn) CompareFn {
	return func(p1, p2 *corev1.Pod) int {
		result := cmp(p1, p2)
		if result > 0 {
			return -1
		}
		if result < 0 {
			return 1
		}
		return 0
	}
}
