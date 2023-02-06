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
	"net/url"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestQueryPublishItemRequest_FromValues(t *testing.T) {
	var q = new(apistructs.QueryPublishItemRequest)
	var values = url.Values{
		"publisherId": {"1"},
		"name":        {"my-name"},
		"type":        {"type"},
		"public":      {"public"},
		"q":           {"my-q"},
		"ids":         {"1,2,3"},
	}
	q.FromValues(values)
	t.Log(q)
	values.Set("pageSize", "20")
	values.Set("pageNo", "1")
	q.FromValues(values)
	t.Log(q)
}

func TestQueryPublishItemRequest_ToValues(t *testing.T) {
	var (
		q = apistructs.QueryPublishItemRequest{
			PageNo:      10,
			PageSize:    1,
			PublisherId: 2,
			Name:        "my-name",
			Type:        "its-type",
			Public:      "is it public",
			Q:           "query some thing",
			Ids:         "1,2,3,4",
			OrgID:       0,
		}
		values = make(url.Values)
	)
	q.ToValues(values)
	t.Log(values)
}
