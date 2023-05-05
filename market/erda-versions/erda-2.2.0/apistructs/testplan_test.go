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

package apistructs

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestTestPlanCreateRequest_Check(t *testing.T) {
	tt := []struct {
		req  TestPlanCreateRequest
		want bool
	}{
		{
			TestPlanCreateRequest{
				Name:        "",
				OwnerID:     "",
				PartnerIDs:  nil,
				ProjectID:   0,
				IterationID: 0,
			}, false,
		}, {
			TestPlanCreateRequest{
				Name:        "foo",
				OwnerID:     "",
				PartnerIDs:  nil,
				ProjectID:   0,
				IterationID: 0,
			}, false,
		}, {
			TestPlanCreateRequest{
				Name:        "foo",
				OwnerID:     "1",
				PartnerIDs:  []string{"1"},
				ProjectID:   1,
				IterationID: 0,
			}, false,
		}, {
			TestPlanCreateRequest{
				Name:        "foo",
				OwnerID:     "1",
				PartnerIDs:  []string{"1"},
				ProjectID:   1,
				IterationID: 1,
			}, true,
		},
	}

	for _, v := range tt {
		assert.Equal(t, v.want, v.req.Check() == nil)
	}
}
