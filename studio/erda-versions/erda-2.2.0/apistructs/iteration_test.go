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
	"time"

	"github.com/stretchr/testify/assert"
)

func TestIterationCreateRequest_Check(t *testing.T) {
	now := time.Now()
	tt := []struct {
		Req  IterationCreateRequest
		want bool
	}{
		{IterationCreateRequest{
			StartedAt:  nil,
			FinishedAt: &now,
			ProjectID:  1,
			Title:      "foo",
			Content:    "",
		}, false},
		{IterationCreateRequest{
			StartedAt:  &now,
			FinishedAt: nil,
			ProjectID:  1,
			Title:      "foo",
			Content:    "",
		}, false},
		{IterationCreateRequest{
			StartedAt:  &now,
			FinishedAt: &now,
			ProjectID:  0,
			Title:      "foo",
			Content:    "",
		}, false},
		{IterationCreateRequest{
			StartedAt:  &now,
			FinishedAt: &now,
			ProjectID:  1,
			Title:      "",
			Content:    "",
		}, false},
		{IterationCreateRequest{
			StartedAt:  &now,
			FinishedAt: &now,
			ProjectID:  1,
			Title:      "foo",
			Content:    "",
		}, true},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, v.Req.Check() == nil)
	}

}
