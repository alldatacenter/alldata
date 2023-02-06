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

package nodetopo

import (
	"fmt"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	routes.GET("/api/node/topology", p.getNodeTopology)
	return nil
}

const maxSize = 5000

// TopoMetrics .
type TopoMetrics struct {
	Bytes      int64 `json:"bytes"`
	Packets    int64 `json:"packets"`
	TCPBytes   int64 `json:"tcp_bytes"`
	UDPBytes   int64 `json:"udp_bytes"`
	TCPPackets int64 `json:"tcp_packets"`
	UDPPackets int64 `json:"udp_packets"`
}

// Node .
type Node struct {
	Name    string      `json:"name"`
	Metrics TopoMetrics `json:"metrics"`
}

// Line .
type Line struct {
	Source  string      `json:"source"`
	Target  string      `json:"target"`
	Metrics TopoMetrics `json:"metrics"`
}

// TopoResponse .
type TopoResponse struct {
	Nodes []*Node `json:"nodes"`
	Links []*Line `json:"links"`
}

func (p *provider) getNodeTopology(param struct {
	Start       int64  `query:"start"`
	End         int64  `query:"end"`
	ClusterName string `query:"clusterName"`
}) interface{} {
	if len(param.ClusterName) <= 0 {
		return api.Errors.InvalidParameter("clusterName must not be empty")
	}
	if param.End <= 0 {
		param.End = time.Now().UnixNano() / int64(time.Millisecond)
	}
	// filter
	query := elastic.NewBoolQuery().
		Filter(elastic.NewTermQuery("tags.cluster_name", param.ClusterName)).
		Filter(elastic.NewRangeQuery("timestamp").Gte(param.Start * int64(time.Millisecond)).Lte(param.End * int64(time.Millisecond)))
	// agg
	searchSource := elastic.NewSearchSource().Query(query).Size(0)
	src := elastic.NewTermsAggregation().Field("tags.src").Size(maxSize)
	dst := elastic.NewTermsAggregation().Field("tags.dst").Size(maxSize)
	src.SubAggregation("dst", dst)
	searchSource.Aggregation("src", src)
	// fields agg
	dst.SubAggregation("bytes", elastic.NewSumAggregation().Field("fields.bytes"))
	dst.SubAggregation("packets", elastic.NewSumAggregation().Field("fields.packets"))
	dst.SubAggregation("tcp_bytes", elastic.NewSumAggregation().Field("fields.tcp_bytes"))
	dst.SubAggregation("tcp_packets", elastic.NewSumAggregation().Field("fields.tcp_packets"))
	dst.SubAggregation("udp_bytes", elastic.NewSumAggregation().Field("fields.udp_bytes"))
	dst.SubAggregation("udp_packets", elastic.NewSumAggregation().Field("fields.udp_packets"))
	src.SubAggregation("bytes", elastic.NewSumAggregation().Field("fields.bytes"))
	src.SubAggregation("packets", elastic.NewSumAggregation().Field("fields.packets"))
	src.SubAggregation("tcp_bytes", elastic.NewSumAggregation().Field("fields.tcp_bytes"))
	src.SubAggregation("tcp_packets", elastic.NewSumAggregation().Field("fields.tcp_packets"))
	src.SubAggregation("udp_bytes", elastic.NewSumAggregation().Field("fields.udp_bytes"))
	src.SubAggregation("udp_packets", elastic.NewSumAggregation().Field("fields.udp_packets"))
	// request
	indices := p.metricq.Indices([]string{"net_packets"}, []string{param.ClusterName}, param.Start, param.End)
	resp, err := p.esRequest(indices, searchSource)
	if err != nil {
		return api.Errors.Internal(err)
	}
	result := &TopoResponse{}
	if resp == nil || resp.Aggregations == nil {
		return api.Success(result)
	}
	srcTerms, ok := resp.Aggregations.Terms("src")
	if !ok {
		return api.Success(result)
	}
	nodes := make(map[string]*Node)
	for _, srcb := range srcTerms.Buckets {
		if srcb.Key == nil {
			continue
		}
		src := fmt.Sprint(srcb.Key)
		snode, ok := nodes[src]
		if !ok {
			snode = &Node{
				Name: src,
			}
			nodes[src] = snode
		}
		p.parseTopoMetrics(srcb.Aggregations, &snode.Metrics)
		dstTerms, ok := srcb.Aggregations.Terms("dst")
		if !ok {
			continue
		}
		for _, dstb := range dstTerms.Buckets {
			if dstb.Key == nil {
				continue
			}
			dst := fmt.Sprint(dstb.Key)
			dnode, ok := nodes[dst]
			if !ok {
				dnode = &Node{
					Name: dst,
				}
				nodes[dst] = dnode
			}
			line := &Line{
				Source: src,
				Target: dst,
			}
			result.Links = append(result.Links, line)
			p.parseTopoMetrics(dstb.Aggregations, &line.Metrics)
			p.parseTopoMetrics(dstb.Aggregations, &dnode.Metrics)
		}
	}
	for _, n := range nodes {
		result.Nodes = append(result.Nodes, n)
	}
	return api.Success(result)
}

func (p *provider) parseTopoMetrics(aggs elastic.Aggregations, m *TopoMetrics) {
	m.Bytes += getSumAggMetricValue(aggs, "bytes")
	m.Packets += getSumAggMetricValue(aggs, "packets")
	m.TCPBytes += getSumAggMetricValue(aggs, "tcp_bytes")
	m.TCPPackets += getSumAggMetricValue(aggs, "tcp_packets")
	m.UDPBytes += getSumAggMetricValue(aggs, "udp_bytes")
	m.UDPPackets += getSumAggMetricValue(aggs, "udp_packets")
}

func getSumAggMetricValue(aggs elastic.Aggregations, key string) int64 {
	sum, ok := aggs.Sum(key)
	if !ok || sum.Value == nil {
		return int64(0)
	}
	return int64(*sum.Value)
}

func (p *provider) esRequest(indices []string, searchSource *elastic.SearchSource) (*elastic.SearchResult, error) {
	resp, err := p.metricq.SearchRaw(indices, searchSource)
	if err != nil || (resp != nil && resp.Error != nil) {
		if resp != nil {
			if resp.Status == 404 {
				return nil, nil
			}
			if resp.Error != nil {
				return nil, fmt.Errorf("fail to request storage: %s", jsonx.MarshalAndIndent(resp.Error))
			}
		}
		return nil, fmt.Errorf("fail to request storage: %s", err)
	}
	return resp, nil
}
