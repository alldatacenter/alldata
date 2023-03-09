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

package metricsquery

import (
	"fmt"
	"time"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
)

type MetricsQuery interface {
	CollectNodeAndPodMetricLast(collectResUsedIntervalSeconds int64) (
		*metriccache.NodeResourceMetric, []*metriccache.PodResourceMetric)

	CollectNodeMetricsAvg(windowSeconds int64) metriccache.NodeResourceQueryResult

	CollectNodeAndPodMetrics(queryParam *metriccache.QueryParam) (
		*metriccache.NodeResourceMetric, []*metriccache.PodResourceMetric)

	CollectContainerResMetricLast(containerID *string, collectResUsedIntervalSeconds int64) metriccache.ContainerResourceQueryResult

	CollectContainerThrottledMetricLast(containerID *string, collectResUsedIntervalSeconds int64) metriccache.ContainerThrottledQueryResult

	CollectPodMetric(podMeta *statesinformer.PodMeta, queryParam *metriccache.QueryParam) metriccache.PodResourceQueryResult
}

var _ MetricsQuery = &metricsQuery{}

// NewMetricsQuery creates an instance which implements interface MetricsQuery.
func NewMetricsQuery(metricCache metriccache.MetricCache, statesInformer statesinformer.StatesInformer) MetricsQuery {
	return &metricsQuery{
		metricCache:    metricCache,
		statesInformer: statesInformer,
	}
}

type metricsQuery struct {
	metricCache    metriccache.MetricCache
	statesInformer statesinformer.StatesInformer
}

// CollectNodeMetricsAvg impl plugins.MetricQuery interface.
func (r *metricsQuery) CollectNodeMetricsAvg(windowSeconds int64) metriccache.NodeResourceQueryResult {
	queryParam := GenerateQueryParamsAvg(windowSeconds)
	return r.collectNodeMetric(queryParam)
}

// CollectNodeAndPodMetricLast impl plugins.MetricQuery interface.
func (r *metricsQuery) CollectNodeAndPodMetricLast(collectResUsedIntervalSeconds int64) (
	*metriccache.NodeResourceMetric, []*metriccache.PodResourceMetric) {

	queryParam := GenerateQueryParamsLast(collectResUsedIntervalSeconds * 2)
	return r.CollectNodeAndPodMetrics(queryParam)
}

// CollectNodeAndPodMetrics impl plugins.MetricQuery interface.
func (r *metricsQuery) CollectNodeAndPodMetrics(queryParam *metriccache.QueryParam) (
	*metriccache.NodeResourceMetric, []*metriccache.PodResourceMetric) {

	// collect node's and all pods' metrics with the same query param
	nodeQueryResult := r.collectNodeMetric(queryParam)
	nodeMetric := nodeQueryResult.Metric

	podsMeta := r.statesInformer.GetAllPods()
	podsMetrics := make([]*metriccache.PodResourceMetric, 0, len(podsMeta))
	for _, podMeta := range podsMeta {
		podQueryResult := r.CollectPodMetric(podMeta, queryParam)
		podMetric := podQueryResult.Metric
		if podMetric != nil {
			podsMetrics = append(podsMetrics, podMetric)
		}
	}
	return nodeMetric, podsMetrics
}

func (r *metricsQuery) collectNodeMetric(queryParam *metriccache.QueryParam) metriccache.NodeResourceQueryResult {
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

func (r *metricsQuery) CollectPodMetric(podMeta *statesinformer.PodMeta,
	queryParam *metriccache.QueryParam) metriccache.PodResourceQueryResult {

	if podMeta == nil || podMeta.Pod == nil {
		return metriccache.PodResourceQueryResult{QueryResult: metriccache.QueryResult{Error: fmt.Errorf("pod is nil")}}
	}
	podUID := string(podMeta.Pod.UID)
	queryResult := r.metricCache.GetPodResourceMetric(&podUID, queryParam)
	if queryResult.Error != nil {
		klog.Warningf("get pod %v resource metric failed, error %v", podUID, queryResult.Error)
		return queryResult
	}
	if queryResult.Metric == nil {
		klog.Warningf("pod %v metric not exist", podUID)
		return queryResult
	}
	return queryResult
}

// CollectContainerResMetricLast creates an instance which implements interface MetricsQuery.
func (r *metricsQuery) CollectContainerResMetricLast(containerID *string, collectResUsedIntervalSeconds int64) metriccache.ContainerResourceQueryResult {
	if containerID == nil {
		return metriccache.ContainerResourceQueryResult{
			QueryResult: metriccache.QueryResult{Error: fmt.Errorf("container is nil")},
		}
	}
	queryParam := GenerateQueryParamsLast(collectResUsedIntervalSeconds * 2)
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

// CollectContainerThrottledMetricLast creates an instance which implements interface MetricsQuery.
func (r *metricsQuery) CollectContainerThrottledMetricLast(containerID *string, collectResUsedIntervalSeconds int64) metriccache.ContainerThrottledQueryResult {
	if containerID == nil {
		return metriccache.ContainerThrottledQueryResult{
			QueryResult: metriccache.QueryResult{Error: fmt.Errorf("container is nil")},
		}
	}
	queryParam := GenerateQueryParamsLast(collectResUsedIntervalSeconds * 2)
	queryResult := r.metricCache.GetContainerThrottledMetric(containerID, queryParam)
	if queryResult.Error != nil {
		klog.Warningf("get container %v throttled metric failed, error %v", containerID, queryResult.Error)
		return queryResult
	}
	if queryResult.Metric == nil {
		klog.Warningf("container %v metric not exist", containerID)
		return queryResult
	}
	return queryResult
}

func GenerateQueryParamsAvg(windowSeconds int64) *metriccache.QueryParam {
	end := time.Now()
	start := end.Add(-time.Duration(windowSeconds) * time.Second)
	queryParam := &metriccache.QueryParam{
		Aggregate: metriccache.AggregationTypeAVG,
		Start:     &start,
		End:       &end,
	}
	return queryParam
}

func GenerateQueryParamsLast(windowSeconds int64) *metriccache.QueryParam {
	end := time.Now()
	start := end.Add(-time.Duration(windowSeconds) * time.Second)
	queryParam := &metriccache.QueryParam{
		Aggregate: metriccache.AggregationTypeLast,
		Start:     &start,
		End:       &end,
	}
	return queryParam
}
