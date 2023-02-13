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

package core

import (
	"reflect"
	"testing"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
)

func TestConvertRelations(t *testing.T) {
	type args struct {
		issueID   int64
		relations []pb.IssuePropertyInstance
	}
	v1 := structpb.NewNumberValue(1)
	v2 := structpb.NewStringValue("")
	v3 := structpb.NewStringValue("t1")
	r1 := []pb.IssuePropertyInstance{
		{
			ArbitraryValue: v2,
			PropertyType:   pb.PropertyTypeEnum_Number,
		},
		{
			ArbitraryValue: v1,
			PropertyType:   pb.PropertyTypeEnum_Number,
		},
		{
			ArbitraryValue: v3,
			PropertyType:   pb.PropertyTypeEnum_Text,
		},
	}
	p1 := []*pb.IssuePropertyExtraProperty{
		{
			ArbitraryValue: v2,
			PropertyType:   pb.PropertyTypeEnum_Number,
		},
		{
			ArbitraryValue: v1,
			PropertyType:   pb.PropertyTypeEnum_Number,
		},
		{
			ArbitraryValue: v3,
			PropertyType:   pb.PropertyTypeEnum_Text,
		},
	}
	tests := []struct {
		name    string
		args    args
		want    *pb.IssueAndPropertyAndValue
		wantErr bool
	}{
		{
			args: args{1, r1},
			want: &pb.IssueAndPropertyAndValue{
				IssueID:  1,
				Property: p1,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := ConvertRelations(tt.args.issueID, tt.args.relations)
			if (err != nil) != tt.wantErr {
				t.Errorf("ConvertRelations() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ConvertRelations() = %v, want %v", got, tt.want)
			}
		})
	}
}
