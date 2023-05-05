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

package dao

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

func Test_validateTestPlanCaseRelPagingRequest(t *testing.T) {
	type args struct {
		req apistructs.TestPlanCaseRelPagingRequest
	}
	orderBy := true
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "missing testPlanID",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{},
			},
			wantErr: true,
		},
		{
			name: "invalid priority",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{Priorities: []apistructs.TestCasePriority{"xxx"}},
			},
			wantErr: true,
		},
		{
			name: "order by both priority asc/desc",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{OrderByPriorityAsc: &orderBy, OrderByPriorityDesc: &orderBy},
			},
			wantErr: true,
		},
		{
			name: "order by both updaterID asc/desc",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{OrderByUpdaterIDAsc: &orderBy, OrderByUpdaterIDDesc: &orderBy},
			},
			wantErr: true,
		},
		{
			name: "order by both updatedAt asc/desc",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{OrderByUpdatedAtAsc: &orderBy, OrderByUpdatedAtDesc: &orderBy},
			},
			wantErr: true,
		},
		{
			name: "order by both id asc/desc",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{OrderByIDAsc: &orderBy, OrderByIDDesc: &orderBy},
			},
			wantErr: true,
		},
		{
			name: "order by both testSetID asc/desc",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{OrderByTestSetIDAsc: &orderBy, OrderByTestSetIDDesc: &orderBy},
			},
			wantErr: true,
		},
		{
			name: "order by both testSetName asc/desc",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{OrderByTestSetNameAsc: &orderBy, OrderByTestSetNameDesc: &orderBy},
			},
			wantErr: true,
		},
		{
			name: "valid request",
			args: args{
				req: apistructs.TestPlanCaseRelPagingRequest{TestPlanID: 1},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := validateTestPlanCaseRelPagingRequest(tt.args.req); (err != nil) != tt.wantErr {
				t.Errorf("validateTestPlanCaseRelPagingRequest() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_setDefaultForTestPlanCaseRelPagingRequest(t *testing.T) {
	// must order by testSet
	req1 := apistructs.TestPlanCaseRelPagingRequest{OrderByPriorityAsc: &[]bool{true}[0]}
	setDefaultForTestPlanCaseRelPagingRequest(&req1)
	assert.Equal(t, 1, len(req1.OrderFields))
	assert.Equal(t, tcFieldTestSetID, req1.OrderFields[0])
	assert.True(t, *req1.OrderByTestSetIDAsc)

	// set default order inside a testSet
	req2 := apistructs.TestPlanCaseRelPagingRequest{OrderByTestSetIDAsc: &[]bool{true}[0]}
	setDefaultForTestPlanCaseRelPagingRequest(&req2)
	assert.Equal(t, 1, len(req1.OrderFields))
	assert.Equal(t, tcFieldID, req2.OrderFields[0])
	assert.True(t, *req2.OrderByIDAsc)

	// default page
	req3 := apistructs.TestPlanCaseRelPagingRequest{}
	setDefaultForTestPlanCaseRelPagingRequest(&req3)
	assert.True(t, 1 == req3.PageNo)
	assert.True(t, 20 == req3.PageSize)
}
