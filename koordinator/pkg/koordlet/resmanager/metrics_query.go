/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package resmanager

import (
	"fmt"
	"time"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
)

func (r *resmanager) collectNodeMetricsAvg(windowSeconds int64) metriccache.NodeResourceQueryResult {
	queryParam := generateQueryParamsAvg(windowSeconds)
	return r.collectNodeMetric(queryParam)
}

// query data for 2 * collectResUsedIntervalSeconds
func (r *resmanager) collectNodeAndPodMetricLast() (*metriccache.NodeResourceMetric, []*metriccache.PodResourceMetric) {
	queryParam := generateQueryParamsLast(r.collectResUsedIntervalSeconds * 2)
	return r.collectNodeAndPodMetrics(queryParam)
}

func (r *resmanager) collectNodeAndPodMetrics(queryParam *metriccache.QueryParam) (*metriccache.NodeResourceMetric, []*metriccache.PodResourceMetric) {
	// collect node's and all pods' metrics with the same query param
	nodeQueryResult := r.collectNodeMetric(queryParam)
	nodeMetric := nodeQueryResult.Metric

	podsMeta := r.statesInformer.GetAllPods()
	podsMetrics := make([]*metriccache.PodResourceMetric, 0, len(podsMeta))
	for _, podMeta := range podsMeta {
		podQueryResult := r.collectPodMetric(podMeta, queryParam)
		podMetric := podQueryResult.Metric
		if podMetric != nil {
			podsMetrics = append(podsMetrics, podMetric)
		}
	}
	return nodeMetric, podsMetrics
}

func (r *resmanager) collectNodeMetric(queryParam *metriccache.QueryParam) metriccache.NodeResourceQueryResult {
	queryResult := r.metricCache.GetNodeResourceMetric(queryParam)
	if queryResult.Error != nil {
		klog.Warningf("get node resource metric failed, error %v", queryResult.Error)
		return queryResult
	}
	if queryResult.Metric == nil {
		klog.Warningf("node metric not exist")
		return queryResult
	}
	return queryResult
}

func (r *resmanager) collectPodMetric(podMeta *statesinformer.PodMeta, queryParam *metriccache.QueryParam) metriccache.PodResourceQueryResult {
	if podMeta == nil || podMeta.Pod == nil {
		return metriccache.PodResourceQueryResult{QueryResult: metriccache.QueryResult{Error: fmt.Errorf("pod is nil")}}
	}
	podUID := string(podMeta.Pod.UID)
	queryResult := r.metricCache.GetPodResourceMetric(&podUID, queryParam)
	if queryResult.Error != nil {
		klog.V(5).Infof("get pod %v resource metric failed, error %v", podUID, queryResult.Error)
		return queryResult
	}
	if queryResult.Metric == nil {
		klog.V(5).Infof("pod %v metric not exist", podUID)
		return queryResult
	}
	return queryResult
}

func (r *resmanager) collectContainerResMetricLast(containerID *string) metriccache.ContainerResourceQueryResult {
	if containerID == nil {
		return metriccache.ContainerResourceQueryResult{
			QueryResult: metriccache.QueryResult{Error: fmt.Errorf("container is nil")},
		}
	}
	queryParam := generateQueryParamsLast(r.collectResUsedIntervalSeconds * 2)
	queryResult := r.metricCache.GetContainerResourceMetric(containerID, queryParam)
	if queryResult.Error != nil {
		klog.Warningf("get container %v resource metric failed, error %v", containerID, queryResult.Error)
		return queryResult
	}
	if queryResult.Metric == nil {
		klog.Warningf("container %v metric not exist", containerID)
		return queryResult
	}
	return queryResult
}

func (r *resmanager) collectContainerThrottledMetricLast(containerID *string) metriccache.ContainerThrottledQueryResult {
	if containerID == nil {
		return metriccache.ContainerThrottledQueryResult{
			QueryResult: metriccache.QueryResult{Error: fmt.Errorf("container is nil")},
		}
	}
	queryParam := generateQueryParamsLast(r.collectResUsedIntervalSeconds * 5)
	queryResult := r.metricCache.GetContainerThrottledMetric(containerID, queryParam)
	if queryResult.Error != nil {
		klog.V(5).Infof("get container %v throttled metric failed, error %v", *containerID, queryResult.Error)
		return queryResult
	}
	if queryResult.Metric == nil {
		klog.V(5).Infof("container %v metric not exist", *containerID)
		return queryResult
	}
	return queryResult
}

func generateQueryParamsAvg(windowSeconds int64) *metriccache.QueryParam {
	end := time.Now()
	start := end.Add(-time.Duration(windowSeconds) * time.Second)
	queryParam := &metriccache.QueryParam{
		Aggregate: metriccache.AggregationTypeAVG,
		Start:     &start,
		End:       &end,
	}
	return queryParam
}

func generateQueryParamsLast(windowSeconds int64) *metriccache.QueryParam {
	end := time.Now()
	start := end.Add(-time.Duration(windowSeconds) * time.Second)
	queryParam := &metriccache.QueryParam{
		Aggregate: metriccache.AggregationTypeLast,
		Start:     &start,
		End:       &end,
	}
	return queryParam
}
