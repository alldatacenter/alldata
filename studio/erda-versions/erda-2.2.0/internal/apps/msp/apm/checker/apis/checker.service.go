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

package apis

import (
	"context"

	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/pkg/common/errors"
)

type checkerService struct {
	p *provider
}

func (s *checkerService) CreateChecker(ctx context.Context, req *pb.CreateCheckerRequest) (*pb.CreateCheckerResponse, error) {
	// TODO .
	return nil, errors.NewUnimplementedError("CreateChecker")
}
func (s *checkerService) UpdateChecker(ctx context.Context, req *pb.UpdateCheckerRequest) (*pb.UpdateCheckerResponse, error) {
	// TODO .
	return nil, errors.NewUnimplementedError("UpdateChecker")
}
func (s *checkerService) DeleteChecker(ctx context.Context, req *pb.UpdateCheckerRequest) (*pb.UpdateCheckerResponse, error) {
	// TODO .
	return nil, errors.NewUnimplementedError("DeleteChecker")
}
func (s *checkerService) ListCheckers(ctx context.Context, req *pb.ListCheckersRequest) (*pb.ListCheckersResponse, error) {
	// TODO .
	return nil, errors.NewUnimplementedError("ListCheckers")
}
func (s *checkerService) DescribeCheckers(ctx context.Context, req *pb.DescribeCheckersRequest) (*pb.DescribeCheckersResponse, error) {
	// TODO .
	return nil, errors.NewUnimplementedError("DescribeCheckers")
}
func (s *checkerService) DescribeChecker(ctx context.Context, req *pb.DescribeCheckerRequest) (*pb.DescribeCheckerResponse, error) {
	// TODO .
	return nil, errors.NewUnimplementedError("DescribeChecker")
}
