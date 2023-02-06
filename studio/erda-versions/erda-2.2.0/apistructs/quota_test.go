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

package apistructs_test

import (
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestTrendRequest_Validate(t *testing.T) {
	var tr apistructs.TrendRequest
	if err := tr.Validate(); err == nil {
		t.Fatal("orgID should not be valid")
	}
	tr.OrgID = "0"
	if err := tr.Validate(); err == nil {
		t.Fatal("userID should not be valid")
	}
	tr.UserID = "0"
	if err := tr.Validate(); err == nil {
		t.Fatal("nil query should not be valid")
	}
	tr.Query = new(apistructs.TrendRequestQuery)
	if err := tr.Validate(); err == nil {
		t.Fatal("query.Start should not be valid")
	}
	tr.Query.Start = "0"
	if err := tr.Validate(); err == nil {
		t.Fatal("query.Start should not be valid")
	}
	tr.Query.Start = "1234567891011"
	if err := tr.Validate(); err == nil {
		t.Fatal("query.End should not be valid")
	}
	tr.Query.End = tr.Query.Start
	if err := tr.Validate(); err == nil {
		t.Fatal("query.ScopeID should not be valid")
	}
	tr.Query.ScopeID = "0"
	if err := tr.Validate(); err != nil {
		t.Fatal(err)
	}
}

func TestTrendRequestQuery(t *testing.T) {
	var trq apistructs.TrendRequestQuery
	trq.GetInterval()
	trq.GetClustersNames()
	trq.GetScope()
	trq.GetResourceType()
}
