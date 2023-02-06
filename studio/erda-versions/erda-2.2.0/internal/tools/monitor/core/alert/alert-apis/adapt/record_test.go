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

package adapt

import (
	"fmt"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
)

func Test_QueryAlertHistory_BothFail_Should_Return_Error(t *testing.T) {
	a := &Adapt{}
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromES)
	monkey.Patch((*Adapt).queryAlertHistoryFromES, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return nil, fmt.Errorf("booooo!")
	})
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromCassandra)
	monkey.Patch((*Adapt).queryAlertHistoryFromCassandra, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return nil, fmt.Errorf("booooo!")
	})

	_, err := a.QueryAlertHistory(nil, "group-1", 1, 2, 10)
	if err == nil {
		t.Error("assert failed, expect err, but got nil")
	}
}

func Test_QueryAlertHistory_WithBothSuccess_Should_Return_NoneEmpty(t *testing.T) {
	a := &Adapt{}
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromES)
	monkey.Patch((*Adapt).queryAlertHistoryFromES, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return []*pb.AlertHistory{
			{
				Timestamp: 4,
			},
			{
				Timestamp: 2,
			},
		}, nil
	})
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromCassandra)
	monkey.Patch((*Adapt).queryAlertHistoryFromCassandra, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return []*pb.AlertHistory{
			{
				Timestamp: 3,
			},
			{
				Timestamp: 1,
			},
		}, nil
	})
	expect := []*pb.AlertHistory{
		{
			Timestamp: 4,
		},
		{
			Timestamp: 3,
		},
		{
			Timestamp: 2,
		},
		{
			Timestamp: 1,
		},
	}

	list, err := a.QueryAlertHistory(nil, "group-1", 1, 2, 10)
	if err != nil {
		t.Error("assert failed, expect err, but got nil")
	}
	if len(list) != len(expect) {
		t.Errorf("assert result failed, expect: %+v, but got: %+v", expect, list)
	}
	for i := 0; i < len(expect); i++ {
		if list[i].Timestamp != expect[i].Timestamp {
			t.Errorf("assert list order failed, index: %d, expect: %+v, but got: %+v", i, expect[i], list[i])
		}
	}
}

func Test_QueryAlertHistory_WithEnoughFromES_Should_Skip_Call_Cassandra(t *testing.T) {
	a := &Adapt{}
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromES)
	monkey.Patch((*Adapt).queryAlertHistoryFromES, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return []*pb.AlertHistory{
			{
				Timestamp: 4,
			},
			{
				Timestamp: 2,
			},
		}, nil
	})
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromCassandra)
	monkey.Patch((*Adapt).queryAlertHistoryFromCassandra, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return []*pb.AlertHistory{
			{
				Timestamp: 3,
			},
			{
				Timestamp: 1,
			},
		}, nil
	})
	expect := []*pb.AlertHistory{
		{
			Timestamp: 4,
		},
		{
			Timestamp: 2,
		},
	}

	list, err := a.QueryAlertHistory(nil, "group-1", 1, 2, 2)
	if err != nil {
		t.Error("assert failed, expect err, but got nil")
	}
	if len(list) != len(expect) {
		t.Errorf("assert result failed, expect: %+v, but got: %+v", expect, list)
	}
	for i := 0; i < len(expect); i++ {
		if list[i].Timestamp != expect[i].Timestamp {
			t.Errorf("assert list order failed, index: %d, expect: %+v, but got: %+v", i, expect[i], list[i])
		}
	}
}

func Test_QueryAlertHistory_WithFailedFromES_Should_Skip_Call_Merge(t *testing.T) {
	a := &Adapt{}
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromES)
	monkey.Patch((*Adapt).queryAlertHistoryFromES, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return nil, fmt.Errorf("boooo!")
	})
	defer monkey.Unpatch((*Adapt).queryAlertHistoryFromCassandra)
	monkey.Patch((*Adapt).queryAlertHistoryFromCassandra, func(a *Adapt, groupID string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
		return []*pb.AlertHistory{
			{
				Timestamp: 3,
			},
			{
				Timestamp: 1,
			},
		}, nil
	})
	expect := []*pb.AlertHistory{
		{
			Timestamp: 3,
		},
		{
			Timestamp: 1,
		},
	}

	list, err := a.QueryAlertHistory(nil, "group-1", 1, 2, 2)
	if err != nil {
		t.Error("assert failed, expect err, but got nil")
	}
	if len(list) != len(expect) {
		t.Errorf("assert result failed, expect: %+v, but got: %+v", expect, list)
	}
	for i := 0; i < len(expect); i++ {
		if list[i].Timestamp != expect[i].Timestamp {
			t.Errorf("assert list order failed, index: %d, expect: %+v, but got: %+v", i, expect[i], list[i])
		}
	}
}

func Test_mergeAlertHistories_WithEmptyResults_Should_Return_Empty(t *testing.T) {
	a := &Adapt{}
	result := a.mergeAlertHistories(1, [][]*pb.AlertHistory{}...)
	if len(result) != 0 {
		t.Errorf("merge with empty results should return empty list")
	}
}

func Test_mergeAlertHistories_WithOneResult_Should_Return_The_One(t *testing.T) {
	a := &Adapt{}
	var list1 = []*pb.AlertHistory{{Timestamp: 1}}
	result := a.mergeAlertHistories(1, list1)
	if len(result) != len(list1) {
		t.Errorf("merge results with one item should return exactly the one")
	}
}

func Test_mergeAlertHistories_WithNonEmptyResults_Should_Return_SortedList(t *testing.T) {
	var list1 = []*pb.AlertHistory{
		{
			Timestamp: 4,
		},
		{
			Timestamp: 2,
		},
	}
	var list2 = []*pb.AlertHistory{
		{
			Timestamp: 3,
		},
		{
			Timestamp: 1,
		},
	}
	expect := []*pb.AlertHistory{
		{
			Timestamp: 4,
		},
		{
			Timestamp: 3,
		},
		{
			Timestamp: 2,
		},
		{
			Timestamp: 1,
		},
	}

	var list = (&Adapt{}).mergeAlertHistories(10, list1, list2)
	if len(list) != len(expect) {
		t.Errorf("assert result failed, expect: %+v, but got: %+v", expect, list)
	}
	for i := 0; i < len(expect); i++ {
		if list[i].Timestamp != expect[i].Timestamp {
			t.Errorf("assert list order failed, index: %d, expect: %+v, but got: %+v", i, expect[i], list[i])
		}
	}
}
