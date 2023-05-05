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

package details_apis

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

// PodInfoSummary .
type PodInfoSummary struct {
	ClusterName      interface{} `json:"clusterName"`
	NodeName         interface{} `json:"nodeName"`
	HostIP           interface{} `json:"hostIP"`
	Namespace        interface{} `json:"namespace"`
	PodName          interface{} `json:"podName"`
	RestartTotal     interface{} `json:"restartTotal"`
	StateCode        interface{} `json:"stateCode"`
	TerminatedReason interface{} `json:"terminatedReason"`
}

// PodInfoInstanse .
type PodInfoInstanse struct {
	ContainerID interface{} `json:"containerId"`
	HostIP      interface{} `json:"hostIP"`
	StartedAt   interface{} `json:"startedAt"`
	FinishedAt  interface{} `json:"finishedAt"`
	ExitCode    interface{} `json:"exitCode"`
	OomKilled   interface{} `json:"oomKilled"`
}

// PodInfo .
type PodInfo struct {
	Summary   PodInfoSummary     `json:"summary"`
	Instances []*PodInfoInstanse `json:"instances"`
}

func (p *provider) getPodInfo(clusterName, podName string, start, end int64) (*PodInfo, error) {
	boolQuery := elastic.NewBoolQuery().
		Filter(elastic.NewRangeQuery(query.TimestampKey).Gte(start * 1000000).Lte(end * 1000000)).
		Filter(elastic.NewTermQuery(query.ClusterNameKey, clusterName)).
		Filter(elastic.NewTermQuery(query.TagKey+".pod_name", podName))
	searchSource := elastic.NewSearchSource().Query(boolQuery).Size(0)
	searchSource = searchSource.Aggregation("pod", elastic.NewTopHitsAggregation().Size(1).Sort(query.TimestampKey, false))

	resp, err := p.metricq.QueryRaw([]string{"kubernetes_pod_container"},
		[]string{clusterName}, start, end, searchSource)
	if err != nil {
		if esErr, ok := err.(*elastic.Error); ok {
			if esErr.Status < http.StatusInternalServerError {
				return &PodInfo{}, nil
			}
		}
		return nil, fmt.Errorf("fail to query pod and containers: %s", err)
	}
	var info PodInfo
	if resp.Aggregations == nil {
		return &info, nil
	}
	topHis, ok := resp.Aggregations.TopHits("pod")
	if ok && topHis != nil && topHis.Hits != nil && len(topHis.Hits.Hits) > 0 {
		var source map[string]interface{}
		err := json.Unmarshal([]byte(*topHis.Hits.Hits[0].Source), &source)
		if err == nil {
			info.Summary.ClusterName = utils.GetMapValue(query.TagKey+".cluster_name", source)
			info.Summary.NodeName = utils.GetMapValue(query.TagKey+".node_name", source)
			info.Summary.HostIP = utils.GetMapValue(query.TagKey+".host_ip", source)
			info.Summary.Namespace = utils.GetMapValue(query.TagKey+".namespace", source)
			info.Summary.PodName = utils.GetMapValue(query.TagKey+".pod_name", source)
			info.Summary.StateCode = utils.GetMapValue(query.FieldKey+".state_code", source)
			info.Summary.RestartTotal = utils.GetMapValue(query.FieldKey+".restarts_total", source)
			info.Summary.TerminatedReason = utils.GetMapValue(query.FieldKey+".terminated_reason", source)
		}
	}
	err = p.getContainers(clusterName, podName, start, end, &info)
	if err != nil {
		return nil, err
	}
	return &info, nil
}

func (p *provider) getContainers(clusterName, podName string, start, end int64, info *PodInfo) error {
	boolQuery := elastic.NewBoolQuery().
		Filter(elastic.NewRangeQuery(query.TimestampKey).Gte(start * 1000000).Lte(end * 1000000)).
		Filter(elastic.NewTermQuery(query.ClusterNameKey, clusterName)).
		Filter(elastic.NewTermQuery(query.TagKey+".pod_name", podName)).
		MustNot(elastic.NewTermQuery(query.TagKey+".podsandbox", "true"))
	searchSource := elastic.NewSearchSource().Query(boolQuery).Size(0)

	const containerIDKey = query.TagKey + ".container_id"
	searchSource.Aggregation(containerIDKey, elastic.NewTermsAggregation().Field(containerIDKey).
		SubAggregation("container", elastic.NewTopHitsAggregation().Size(1).Sort(query.TimestampKey, false).Sort(query.FieldKey+".finished_at", false)))
	resp, err := p.metricq.QueryRaw([]string{"docker_container_summary"},
		[]string{clusterName}, start, end, searchSource)
	if err != nil {
		if esErr, ok := err.(*elastic.Error); ok {
			if esErr.Status < http.StatusInternalServerError {
				return nil
			}
		}
		return fmt.Errorf("fail to query pod and containers: %s", err)
	}
	if resp.Aggregations == nil {
		return nil
	}
	terms, ok := resp.Aggregations.Terms(containerIDKey)
	if ok && terms != nil {
		for _, b := range terms.Buckets {
			if b.Aggregations == nil {
				continue
			}
			topHis, ok := b.Aggregations.TopHits("container")
			if ok && topHis != nil && topHis.Hits != nil && len(topHis.Hits.Hits) > 0 {
				var source map[string]interface{}
				err := json.Unmarshal([]byte(*topHis.Hits.Hits[0].Source), &source)
				if err == nil {
					inst := &PodInfoInstanse{
						ContainerID: b.Key,
						HostIP:      utils.GetMapValue(query.TagKey+".host_ip", source),
						StartedAt:   formatDate(utils.GetMapValue(query.FieldKey+".started_at", source)),
						FinishedAt:  formatDate(utils.GetMapValue(query.FieldKey+".finished_at", source)),
						ExitCode:    utils.GetMapValue(query.FieldKey+".exitcode", source),
						OomKilled:   utils.GetMapValue(query.FieldKey+".oomkilled", source),
					}
					info.Instances = append(info.Instances, inst)
				}
			}
		}
	}
	return nil
}

func formatDate(date interface{}) interface{} {
	val, ok := utils.ConvertInt64(date)
	if !ok {
		return date
	}
	return time.Unix(val/int64(time.Second), val%int64(time.Second)).Format("2006-01-02 15:04:05")
}
