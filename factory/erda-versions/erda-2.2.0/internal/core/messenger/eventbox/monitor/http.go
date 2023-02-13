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

package monitor

import (
	"bytes"
	"context"
	"strconv"

	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda/pkg/terminal/table"
)

type MonitorHTTP struct {
}

func NewMonitorHTTP() (*MonitorHTTP, error) {
	return &MonitorHTTP{}, nil
}

func (w *MonitorHTTP) Stat(ctx context.Context, req *pb.StatRequest, vars map[string]string) (*pb.StatResponse, error) {
	s1, err := std.pstat.Last5Min()
	if err != nil {
		return &pb.StatResponse{
			Data: "",
		}, err
	}
	s2, err := std.pstat.Last20Min()
	if err != nil {
		return &pb.StatResponse{
			Data: "",
		}, err
	}
	s3, err := std.pstat.Last1Hour()
	if err != nil {
		return &pb.StatResponse{
			Data: "",
		}, err
	}
	s4, err := std.pstat.Last6Hour()
	if err != nil {
		return &pb.StatResponse{
			Data: "",
		}, err
	}
	s5, err := std.pstat.Last1Day()
	if err != nil {
		return &pb.StatResponse{
			Data: "",
		}, err
	}
	infotpList := infoTypeList()
	infotpStrList := []string{}
	for _, it := range infotpList {
		infotpStrList = append(infotpStrList, it.String())
	}
	s1data := []string{"Last5Min"}
	s2data := []string{"Last20Min"}
	s3data := []string{"Last1Hour"}
	s4data := []string{"Last6Hour"}
	s5data := []string{"Last1Day"}

	for _, it := range infotpStrList {
		s1data = append(s1data, strconv.FormatInt(s1[it], 10))
		s2data = append(s2data, strconv.FormatInt(s2[it], 10))
		s3data = append(s3data, strconv.FormatInt(s3[it], 10))
		s4data = append(s4data, strconv.FormatInt(s4[it], 10))
		s5data = append(s5data, strconv.FormatInt(s5[it], 10))
	}

	var buf bytes.Buffer
	if err := table.NewTable(table.WithVertical(), table.WithWriter(&buf)).Header(append([]string{" "}, infotpStrList...)).
		Data([][]string{s1data, s2data, s3data, s4data, s5data}).Flush(); err != nil {
		return &pb.StatResponse{
			Data: "",
		}, err
	}
	return &pb.StatResponse{
		Data: buf.String(),
	}, nil

}
