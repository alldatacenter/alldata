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

package example

import (
	"context"
	"fmt"

	"github.com/recallsong/go-utils/encoding/jsonx"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	indexloader "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

type provider struct {
	Log    logs.Logger
	Index  indexloader.Interface      `autowired:"elasticsearch.index.loader@metric"`
	Meta   pb.MetricMetaServiceServer `autowired:"erda.core.monitor.metric.MetricMetaService"`
	Metric pb.MetricServiceServer     `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) Init(ctx servicehub.Context) error { return nil }

func (p *provider) Run(ctx context.Context) error {
	p.Index.WaitAndGetIndices(ctx) // example query can be carried out after the index is loaded
	return p.queryExample(ctx)
}

func (p *provider) queryExample(ctx context.Context) error {
	defer func() {
		recover()
	}()
	req := &pb.QueryWithInfluxFormatRequest{
		Start:     "1645158894018", // or timestamp
		End:       "1645162494018", // or timestamp
		Statement: "SELECT timestamp(), avg(cpu_usage_percent) FROM docker_container_summary WHERE container_id::tag = $container_id  GROUP BY time(1m0s) ",
		Params: map[string]*structpb.Value{
			"cluster_name": structpb.NewStringValue("terminus-dev"),
			"terminus_key": structpb.NewStringValue("54055597b1cc15b56e59c35e7b231e0c"),
			"container_id": structpb.NewStringValue("6078d040a8b01f92f380c76e877301cd1a0a1842bcc1790ab86fe7db798eb8ce"),
		},
		//Options: map[string]string{
		//	"debug": "true",
		//},
	}
	resp, err := p.Metric.QueryWithInfluxFormat(ctx, req)
	if err != nil {
		return err
	}
	fmt.Println(jsonx.MarshalAndIndent(resp))
	return nil
}

func init() {
	servicehub.Register("metric-query-example", &servicehub.Spec{
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
