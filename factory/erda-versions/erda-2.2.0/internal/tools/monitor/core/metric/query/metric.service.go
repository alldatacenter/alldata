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

package query

import (
	"context"
	"encoding/json"
	"fmt"
	"reflect"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats/influxdb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	"github.com/erda-project/erda/pkg/common/errors"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

type metricService struct {
	p     *provider
	query query.Queryer
}

func (s *metricService) QueryWithInfluxFormat(ctx context.Context, req *pb.QueryWithInfluxFormatRequest) (*pb.QueryWithInfluxFormatResponse, error) {
	if len(req.Statement) <= 0 {
		return &pb.QueryWithInfluxFormatResponse{}, nil
	}
	rs, data, err := s.query.QueryWithFormat("influxql", req.Statement, "influxdb", nil, convertParams(req.Params), convertFilters(req.Filters), convertOptions(req.Start, req.End, req.Options))
	if err != nil {
		return nil, errors.NewServiceInvokingError("metric.query", err)
	}
	if rs.Details != nil {
		fmt.Println(rs.Details)
	}
	if data == nil {
		return &pb.QueryWithInfluxFormatResponse{}, nil
	}
	rawResp, ok := data.(*api.RawResponse)
	if !ok {
		return nil, errors.NewInternalServerError(fmt.Errorf("%s not *api.RawResponse", reflect.TypeOf(data)))
	}
	resp, ok := rawResp.Body().(*influxdb.Response)
	if !ok {
		return nil, errors.NewInternalServerError(fmt.Errorf("%s is not *influxdb.Response", reflect.TypeOf(rawResp.Body())))
	}
	if resp.Error != nil {
		return nil, errors.NewInternalServerError(resp.Error)
	}
	return &pb.QueryWithInfluxFormatResponse{Results: resp.Results}, nil
}

func (s *metricService) SearchWithInfluxFormat(ctx context.Context, req *pb.QueryWithInfluxFormatRequest) (*pb.QueryWithInfluxFormatResponse, error) {
	return s.QueryWithInfluxFormat(ctx, req)
}

func (s *metricService) QueryWithTableFormat(ctx context.Context, req *pb.QueryWithTableFormatRequest) (*pb.QueryWithTableFormatResponse, error) {
	if len(req.Statement) <= 0 {
		return &pb.QueryWithTableFormatResponse{Data: &pb.TableResult{}}, nil
	}
	opts := convertOptions(req.Start, req.End, req.Options)
	opts.Set("type", "_")
	opts.Set("protobuf", "true")
	rs, data, err := s.query.QueryWithFormat("influxql", req.Statement, "chartv2", nil, convertParams(req.Params), convertFilters(req.Filters), opts)
	if err != nil {
		return nil, errors.NewServiceInvokingError("metric.query", err)
	}
	if rs.Details != nil {
		fmt.Println(rs.Details)
	}
	if data == nil {
		return &pb.QueryWithTableFormatResponse{Data: &pb.TableResult{}}, nil
	}
	result, ok := data.(*pb.TableResult)
	if !ok {
		return nil, errors.NewInternalServerError(fmt.Errorf("%s is not *pb.TableResult", reflect.TypeOf(data)))
	}
	return &pb.QueryWithTableFormatResponse{Data: result}, nil
}

func (s *metricService) SearchWithTableFormat(ctx context.Context, req *pb.QueryWithTableFormatRequest) (*pb.QueryWithTableFormatResponse, error) {
	return s.QueryWithTableFormat(ctx, req)
}

func (s *metricService) GeneralQuery(ctx context.Context, req *pb.GeneralQueryRequest) (*pb.GeneralQueryResponse, error) {
	if len(req.Statement) <= 0 {
		return &pb.GeneralQueryResponse{Data: nil}, nil
	}
	if len(req.Format) == 0 {
		req.Format = "influxdb"
	}
	if len(req.Ql) == 0 {
		req.Ql = "influxql"
	}
	rs, data, err := s.query.QueryWithFormat(req.Ql, req.Statement, req.Format, nil, nil, nil, convertParamsToValues(req.Params))
	if err != nil {
		return nil, errors.NewServiceInvokingError("metric.query", err)
	}
	if rs.Details != nil {
		fmt.Println(rs.Details)
		return &pb.GeneralQueryResponse{Data: nil}, nil
	}
	if data == nil {
		return &pb.GeneralQueryResponse{Data: nil}, nil
	}
	byts, err := json.Marshal(data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := make(map[string]interface{})
	json.Unmarshal(byts, &result)
	val, err := structpb.NewValue(result)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.GeneralQueryResponse{Data: val}, nil
}

func (s *metricService) GeneralSearch(ctx context.Context, req *pb.GeneralQueryRequest) (*pb.GeneralQueryResponse, error) {
	return s.GeneralQuery(ctx, req)
}
