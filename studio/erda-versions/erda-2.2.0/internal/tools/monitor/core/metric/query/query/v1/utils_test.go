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

package queryv1

import (
	"fmt"
	"log"
	"strconv"
	"testing"
	"time"
)

func TestDynamicPoints(t *testing.T) {
	now := time.Now()
	t.Log(dynamicPoints(&Request{
		Start: now.Add(-time.Second*20).Unix() * 1000,
		End:   now.Unix() * 1000,
	}))
	t.Log(dynamicPoints(&Request{
		Start: now.Add(-time.Second*40).Unix() * 1000,
		End:   now.Unix() * 1000,
	}))
	t.Log(dynamicPoints(&Request{
		Start: now.Add(-time.Minute*20).Unix() * 1000,
		End:   now.Unix() * 1000,
	}))
	t.Log(dynamicPoints(&Request{
		Start: now.Add(-time.Hour*1).Unix() * 1000,
		End:   now.Unix() * 1000,
	}))
	t.Log(dynamicPoints(&Request{
		Start: now.AddDate(0, 0, -2).Unix() * 1000,
		End:   now.Unix() * 1000,
	}))
	t.Log(dynamicPoints(&Request{
		Start: now.AddDate(0, 0, -5).Unix() * 1000,
		End:   now.Unix() * 1000,
	}))
}

func TestMapToRawQuery(t *testing.T) {
	metricParams := make(map[string]string)
	metricParams["start"] = strconv.FormatInt(123, 10)
	metricParams["end"] = strconv.FormatInt(1234, 10)
	metricParams["filter_terminus_key"] = "asdfasdf"
	metricParams["group"] = "trace_id"
	metricParams["limit"] = strconv.FormatInt(11, 10)
	metricParams["sort"] = "max_start_time_min"
	metricParams["sum"] = "errors_sum"
	metricParams["min"] = "start_time_min"
	metricParams["max"] = "end_time_max"
	metricParams["last"] = "labels_distinct"
	metricParams["align"] = "false"

	statement, err := MapToRawQuery("test_metric", "agge", metricParams)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println(statement)
}
