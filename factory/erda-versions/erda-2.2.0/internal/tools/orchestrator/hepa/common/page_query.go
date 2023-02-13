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

package common

import (
	"encoding/json"
	"reflect"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/hepa/pb"
)

type PageQuery struct {
	Result interface{}
	Page   *pb.Page
}

type NewPageQuery struct {
	List  interface{} `json:"list"`
	Total int64       `json:"total"`
}

func GetPageQuery(page *Page, list interface{}) PageQuery {
	return PageQuery{
		Result: list,
		Page:   (*pb.Page)(page),
	}
}

func NewPages(list interface{}, total int64) NewPageQuery {
	if list == nil || reflect.ValueOf(list).IsNil() {
		return NewPageQuery{
			List:  []string{},
			Total: 0,
		}
	}
	return NewPageQuery{
		List:  list,
		Total: total,
	}
}

func (query PageQuery) Convert() NewPageQuery {
	return NewPageQuery{
		List:  query.Result,
		Total: query.Page.TotalNum,
	}
}

func (query PageQuery) ToPbPage() *pb.PageResult {
	var listv interface{}
	bytes, _ := json.Marshal(query.Result)
	json.Unmarshal(bytes, &listv)
	value, _ := structpb.NewValue(listv)
	return &pb.PageResult{
		Result: value,
		Page:   query.Page,
	}
}

func (query NewPageQuery) ToPbPage() *pb.NewPageResult {
	var listv interface{}
	bytes, _ := json.Marshal(query.List)
	json.Unmarshal(bytes, &listv)
	value, _ := structpb.NewValue(listv)
	return &pb.NewPageResult{
		List:  value,
		Total: query.Total,
	}
}
