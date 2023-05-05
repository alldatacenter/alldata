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

package qaparser

import (
	"github.com/erda-project/erda-proto-go/dop/qa/unittest/pb"
	"github.com/erda-project/erda/apistructs"
)

type Totals struct {
	*pb.TestTotal
}

type Suite struct {
	*pb.TestSuite
}

func NewStatuses(pass, skip, failed, err int64) map[string]int64 {
	return map[string]int64{
		string(apistructs.TestStatusPassed):  pass,
		string(apistructs.TestStatusSkipped): skip,
		string(apistructs.TestStatusFailed):  failed,
		string(apistructs.TestStatusError):   err,
	}
}

func (t *Totals) SetStatuses(statuses map[string]int64) *Totals {
	t.Statuses = statuses
	return t
}

func (t *Totals) Add(total *pb.TestTotal) *Totals {
	t.Tests += total.Tests
	t.Duration += total.Duration
	t.Statuses[string(apistructs.TestStatusPassed)] += total.Statuses[string(apistructs.TestStatusPassed)]
	t.Statuses[string(apistructs.TestStatusSkipped)] += total.Statuses[string(apistructs.TestStatusSkipped)]
	t.Statuses[string(apistructs.TestStatusFailed)] += total.Statuses[string(apistructs.TestStatusFailed)]
	t.Statuses[string(apistructs.TestStatusError)] += total.Statuses[string(apistructs.TestStatusError)]
	return t
}

// Aggregate calculates result sums across all tests.
func (s *Suite) Aggregate() {
	//totals := Totals{Tests: len(s.Tests)}
	totals := &pb.TestTotal{
		Tests:    int64(len(s.Tests)),
		Statuses: make(map[string]int64),
	}

	for _, test := range s.Tests {
		totals.Duration += test.Duration
		switch test.Status {
		case string(apistructs.TestStatusPassed):
			totals.Statuses[string(apistructs.TestStatusPassed)] += 1
		case string(apistructs.TestStatusSkipped):
			totals.Statuses[string(apistructs.TestStatusSkipped)] += 1
		case string(apistructs.TestStatusFailed):
			totals.Statuses[string(apistructs.TestStatusFailed)] += 1
		case string(apistructs.TestStatusError):
			totals.Statuses[string(apistructs.TestStatusError)] += 1
		}
	}

	s.Totals = totals
}
