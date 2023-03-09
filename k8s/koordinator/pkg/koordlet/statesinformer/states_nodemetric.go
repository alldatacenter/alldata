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

package statesinformer

import (
	"context"
	"encoding/json"
	"fmt"
	"reflect"
	"sync"
	"time"

	"golang.org/x/time/rate"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/apimachinery/pkg/watch"
	clientcorev1 "k8s.io/client-go/kubernetes/typed/core/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/tools/record"
	"k8s.io/client-go/util/retry"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	clientsetbeta1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	clientbeta1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/slo/v1alpha1"
	listerbeta1 "github.com/koordinator-sh/koordinator/pkg/client/listers/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	nodeMetricInformerName pluginName = "nodeMetricInformer"

	// defaultAggregateDurationSeconds is the default metric aggregate duration by seconds
	minAggregateDurationSeconds     = 60
	defaultAggregateDurationSeconds = 300

	defaultReportIntervalSeconds = 60
	minReportIntervalSeconds     = 30

	// metric is valid only if its (lastSample.Time - firstSample.Time) > 0.5 * targetTimeRange
	// used during checking node aggregate usage for cold start
	validateTimeRangeRatio = 0.5
)

var (
	scheme = runtime.NewScheme()

	defaultNodeMetricSpec = slov1alpha1.NodeMetricSpec{
		CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
			AggregateDurationSeconds: pointer.Int64(defaultAggregateDurationSeconds),
			ReportIntervalSeconds:    pointer.Int64(defaultReportIntervalSeconds),
			NodeAggregatePolicy: &slov1alpha1.AggregatePolicy{
				Durations: []metav1.Duration{
					{Duration: 5 * time.Minute},
					{Duration: 10 * time.Minute},
					{Duration: 30 * time.Minute},
				},
			},
		},
	}
)

type nodeMetricInformer struct {
	reportEnabled      bool
	nodeName           string
	nodeMetricInformer cache.SharedIndexInformer
	nodeMetricLister   listerbeta1.NodeMetricLister
	eventRecorder      record.EventRecorder
	statusUpdater      *statusUpdater

	podsInformer *podsInformer
	metricCache  metriccache.MetricCache

	rwMutex    sync.RWMutex
	nodeMetric *slov1alpha1.NodeMetric
}

func NewNodeMetricInformer() *nodeMetricInformer {
	return &nodeMetricInformer{}
}

func (r *nodeMetricInformer) HasSynced() bool {
	if !r.reportEnabled {
		return true
	}
	if r.nodeMetricInformer == nil {
		return false
	}
	synced := r.nodeMetricInformer.HasSynced()
	klog.V(5).Infof("node metric informer has synced %v", synced)
	return synced
}

func (r *nodeMetricInformer) Setup(ctx *pluginOption, state *pluginState) {
	r.reportEnabled = ctx.config.EnableNodeMetricReport
	r.nodeName = ctx.NodeName
	r.nodeMetricInformer = newNodeMetricInformer(ctx.KoordClient, ctx.NodeName)
	r.nodeMetricLister = listerbeta1.NewNodeMetricLister(r.nodeMetricInformer.GetIndexer())

	eventBroadcaster := record.NewBroadcaster()
	eventBroadcaster.StartRecordingToSink(&clientcorev1.EventSinkImpl{Interface: ctx.KubeClient.CoreV1().Events("")})
	r.eventRecorder = eventBroadcaster.NewRecorder(scheme, corev1.EventSource{Component: "koordlet-NodeMetric", Host: ctx.NodeName})

	r.statusUpdater = newStatusUpdater(ctx.KoordClient.SloV1alpha1().NodeMetrics())

	r.metricCache = state.metricCache
	podsInformerIf := state.informerPlugins[podsInformerName]
	podsInformer, ok := podsInformerIf.(*podsInformer)
	if !ok {
		klog.Fatalf("pods informer format error")
	}
	r.podsInformer = podsInformer

	r.nodeMetricInformer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			nodeMetric, ok := obj.(*slov1alpha1.NodeMetric)
			if ok {
				r.updateMetricSpec(nodeMetric)
			} else {
				klog.Errorf("node metric informer add func parse nodeMetric failed")
			}
		},
		UpdateFunc: func(oldObj, newObj interface{}) {
			oldNodeMetric, oldOK := oldObj.(*slov1alpha1.NodeMetric)
			newNodeMetric, newOK := newObj.(*slov1alpha1.NodeMetric)
			if !oldOK || !newOK {
				klog.Errorf("unable to convert object to *slov1alpha1.NodeMetric, old %T, new %T", oldObj, newObj)
				return
			}
			if reflect.DeepEqual(oldNodeMetric.Spec, newNodeMetric.Spec) {
				klog.V(5).Infof("find nodeMetric spec %s has not changed.", newNodeMetric.Name)
				return
			}
			klog.Infof("update node metric spec %v", newNodeMetric.Spec)
			r.updateMetricSpec(newNodeMetric)
		},
	})
}

func (r *nodeMetricInformer) ReportEvent(object runtime.Object, eventType, reason, message string) {
	r.eventRecorder.Eventf(object, eventType, reason, message)
}

func (r *nodeMetricInformer) Start(stopCh <-chan struct{}) {
	defer utilruntime.HandleCrash()
	klog.Infof("starting nodeMetricInformer")

	if !r.reportEnabled {
		klog.Infof("node metric report is disabled.")
		return
	}

	go r.nodeMetricInformer.Run(stopCh)
	if !cache.WaitForCacheSync(stopCh, r.nodeMetricInformer.HasSynced, r.podsInformer.HasSynced) {
		klog.Errorf("timed out waiting for node metric caches to sync")
	}
	go r.syncNodeMetricWorker(stopCh)

	klog.Info("start nodeMetricInformer successfully")
	<-stopCh
	klog.Info("shutting down nodeMetricInformer daemon")
}

func (r *nodeMetricInformer) syncNodeMetricWorker(stopCh <-chan struct{}) {
	reportInterval := r.getNodeMetricReportInterval()
	for {
		select {
		case <-stopCh:
			return
		case <-time.After(reportInterval):
			r.sync()
			reportInterval = r.getNodeMetricReportInterval()
		}
	}
}

func (r *nodeMetricInformer) getNodeMetricReportInterval() time.Duration {
	r.rwMutex.RLock()
	defer r.rwMutex.RUnlock()
	if r.nodeMetric == nil || r.nodeMetric.Spec.CollectPolicy == nil || r.nodeMetric.Spec.CollectPolicy.ReportIntervalSeconds == nil {
		return time.Duration(defaultReportIntervalSeconds) * time.Second
	}
	reportIntervalSeconds := util.MaxInt64(*r.nodeMetric.Spec.CollectPolicy.ReportIntervalSeconds, minReportIntervalSeconds)
	return time.Duration(reportIntervalSeconds) * time.Second
}

func (r *nodeMetricInformer) getNodeMetricAggregateDuration() time.Duration {
	r.rwMutex.RLock()
	defer r.rwMutex.RUnlock()
	if r.nodeMetric.Spec.CollectPolicy == nil || r.nodeMetric.Spec.CollectPolicy.AggregateDurationSeconds == nil {
		return time.Duration(defaultAggregateDurationSeconds) * time.Second
	}
	aggregateDurationSeconds := util.MaxInt64(*r.nodeMetric.Spec.CollectPolicy.AggregateDurationSeconds, minAggregateDurationSeconds)
	return time.Duration(aggregateDurationSeconds) * time.Second
}

func (r *nodeMetricInformer) getNodeMetricSpec() *slov1alpha1.NodeMetricSpec {
	r.rwMutex.RLock()
	defer r.rwMutex.RUnlock()
	if r.nodeMetric == nil {
		return &defaultNodeMetricSpec
	}
	return r.nodeMetric.Spec.DeepCopy()
}

func (r *nodeMetricInformer) sync() {
	if !r.isNodeMetricInited() {
		klog.Warningf("node metric has not initialized, skip this round.")
		return
	}

	nodeMetricInfo, podMetricInfo := r.collectMetric()
	if nodeMetricInfo == nil {
		klog.Warningf("node metric is not ready, skip this round.")
		return
	}

	newStatus := &slov1alpha1.NodeMetricStatus{
		UpdateTime: &metav1.Time{Time: time.Now()},
		NodeMetric: nodeMetricInfo,
		PodsMetric: podMetricInfo,
	}
	retErr := retry.RetryOnConflict(retry.DefaultBackoff, func() error {
		nodeMetric, err := r.nodeMetricLister.Get(r.nodeName)
		if errors.IsNotFound(err) {
			klog.Warningf("nodeMetric %v not found, skip", r.nodeName)
			return nil
		} else if err != nil {
			klog.Warningf("failed to get %s nodeMetric: %v", r.nodeName, err)
			return err
		}
		err = r.statusUpdater.updateStatus(nodeMetric, newStatus)
		return err
	})

	if retErr != nil {
		klog.Warningf("update node metric status failed, status %v, err %v", util.DumpJSON(newStatus), retErr)
	} else {
		klog.V(4).Infof("update node metric status success, detail: %v", util.DumpJSON(newStatus))
	}
}

func newNodeMetricInformer(client clientsetbeta1.Interface, nodeName string) cache.SharedIndexInformer {
	tweakListOptionsFunc := func(opt *metav1.ListOptions) {
		opt.FieldSelector = "metadata.name=" + nodeName
	}

	return cache.NewSharedIndexInformer(
		&cache.ListWatch{
			ListFunc: func(options metav1.ListOptions) (runtime.Object, error) {
				tweakListOptionsFunc(&options)
				return client.SloV1alpha1().NodeMetrics().List(context.TODO(), options)
			},
			WatchFunc: func(options metav1.ListOptions) (watch.Interface, error) {
				tweakListOptionsFunc(&options)
				return client.SloV1alpha1().NodeMetrics().Watch(context.TODO(), options)
			},
		},
		&slov1alpha1.NodeMetric{},
		time.Hour*12,
		cache.Indexers{},
	)
}

func (r *nodeMetricInformer) isNodeMetricInited() bool {
	r.rwMutex.RLock()
	defer r.rwMutex.RUnlock()
	return r.nodeMetric != nil
}

func (r *nodeMetricInformer) updateMetricSpec(newNodeMetric *slov1alpha1.NodeMetric) {
	r.rwMutex.Lock()
	defer r.rwMutex.Unlock()
	if newNodeMetric == nil {
		klog.Error("failed to merge with nil nodeMetric, new is nil")
		return
	}
	r.nodeMetric = newNodeMetric.DeepCopy()
	data, _ := json.Marshal(newNodeMetric.Spec)
	r.nodeMetric.Spec = *defaultNodeMetricSpec.DeepCopy()
	_ = json.Unmarshal(data, &r.nodeMetric.Spec)
}

// generateQueryDuration generate query params. It assumes the nodeMetric is initialized
func (r *nodeMetricInformer) generateQueryDuration() (start time.Time, end time.Time) {
	aggregateDuration := r.getNodeMetricAggregateDuration()
	end = time.Now()
	start = end.Add(-aggregateDuration * time.Second)
	return
}

func (r *nodeMetricInformer) collectMetric() (*slov1alpha1.NodeMetricInfo, []*slov1alpha1.PodMetricInfo) {
	spec := r.getNodeMetricSpec()
	endTime := time.Now()
	startTime := endTime.Add(-time.Duration(*spec.CollectPolicy.AggregateDurationSeconds) * time.Second)

	nodeMetricInfo := &slov1alpha1.NodeMetricInfo{
		NodeUsage:            r.queryNodeMetric(startTime, endTime, metriccache.AggregationTypeAVG, false),
		AggregatedNodeUsages: r.collectNodeAggregateMetric(endTime, spec.CollectPolicy.NodeAggregatePolicy),
	}

	podsMeta := r.podsInformer.GetAllPods()
	podsMetricInfo := make([]*slov1alpha1.PodMetricInfo, 0, len(podsMeta))
	podQueryParam := &metriccache.QueryParam{
		Aggregate: metriccache.AggregationTypeAVG,
		Start:     &startTime,
		End:       &endTime,
	}
	for _, podMeta := range podsMeta {
		podMetric := r.collectPodMetric(podMeta, podQueryParam)
		if podMetric != nil {
			podsMetricInfo = append(podsMetricInfo, podMetric)
		}
	}

	return nodeMetricInfo, podsMetricInfo
}

func (r *nodeMetricInformer) queryNodeMetric(start time.Time, end time.Time, aggregateType metriccache.AggregationType,
	coldStartFilter bool) slov1alpha1.ResourceMap {
	queryParam := &metriccache.QueryParam{
		Aggregate: aggregateType,
		Start:     &start,
		End:       &end,
	}
	queryResult := r.metricCache.GetNodeResourceMetric(queryParam)
	if queryResult.Error != nil {
		klog.Warningf("get node resource metric failed, error %v", queryResult.Error)
		return slov1alpha1.ResourceMap{}
	}
	if queryResult.Metric == nil {
		klog.Warningf("node metric not exist")
		return slov1alpha1.ResourceMap{}
	}

	if coldStartFilter && metricsInColdStart(start, end, &queryResult.QueryResult) {
		klog.V(4).Infof("metrics is in cold start, no need to report, current result sample duration %v",
			queryResult.AggregateInfo.TimeRangeDuration().String())
		return slov1alpha1.ResourceMap{}
	}

	return convertNodeMetricToResourceMap(queryResult.Metric)
}

func metricsInColdStart(queryStart, queryEnd time.Time, queryResult *metriccache.QueryResult) bool {
	if queryResult == nil || queryResult.AggregateInfo == nil {
		return true
	}
	targetDuration := queryEnd.Sub(queryStart)
	actualDuration := queryResult.AggregateInfo.TimeRangeDuration()
	return actualDuration.Seconds() < targetDuration.Seconds()*validateTimeRangeRatio
}

func (r *nodeMetricInformer) collectNodeAggregateMetric(endTime time.Time, aggregatePolicy *slov1alpha1.AggregatePolicy) []slov1alpha1.AggregatedUsage {
	aggregateUsages := []slov1alpha1.AggregatedUsage{}
	if aggregatePolicy == nil {
		return aggregateUsages
	}
	for _, d := range aggregatePolicy.Durations {
		start := endTime.Add(-d.Duration)
		aggregateUsage := slov1alpha1.AggregatedUsage{
			Usage: map[slov1alpha1.AggregationType]slov1alpha1.ResourceMap{
				slov1alpha1.P50: r.queryNodeMetric(start, endTime, metriccache.AggregationTypeP50, true),
				slov1alpha1.P90: r.queryNodeMetric(start, endTime, metriccache.AggregationTypeP90, true),
				slov1alpha1.P95: r.queryNodeMetric(start, endTime, metriccache.AggregationTypeP95, true),
				slov1alpha1.P99: r.queryNodeMetric(start, endTime, metriccache.AggregationTypeP99, true),
			},
			Duration: d,
		}
		aggregateUsages = append(aggregateUsages, aggregateUsage)
	}
	return aggregateUsages
}

func (r *nodeMetricInformer) collectPodMetric(podMeta *PodMeta, queryParam *metriccache.QueryParam) *slov1alpha1.PodMetricInfo {
	if podMeta == nil || podMeta.Pod == nil {
		return nil
	}
	podUID := string(podMeta.Pod.UID)
	queryResult := r.metricCache.GetPodResourceMetric(&podUID, queryParam)
	if queryResult.Error != nil {
		klog.Warningf("get pod %v resource metric failed, error %v", podUID, queryResult.Error)
		return nil
	}
	if queryResult.Metric == nil {
		klog.Warningf("pod %v metric not exist", podUID)
		return nil
	}
	return &slov1alpha1.PodMetricInfo{
		Namespace: podMeta.Pod.Namespace,
		Name:      podMeta.Pod.Name,
		PodUsage:  *convertPodMetricToResourceMap(queryResult.Metric),
	}
}

const (
	statusUpdateQPS   = 0.1
	statusUpdateBurst = 2
)

type statusUpdater struct {
	nodeMetricClient  clientbeta1.NodeMetricInterface
	previousTimestamp time.Time
	rateLimiter       *rate.Limiter
}

func newStatusUpdater(nodeMetricClient clientbeta1.NodeMetricInterface) *statusUpdater {
	return &statusUpdater{
		nodeMetricClient:  nodeMetricClient,
		previousTimestamp: time.Now().Add(-time.Hour * 24),
		rateLimiter:       rate.NewLimiter(statusUpdateQPS, statusUpdateBurst),
	}
}

func (su *statusUpdater) updateStatus(nodeMetric *slov1alpha1.NodeMetric, newStatus *slov1alpha1.NodeMetricStatus) error {
	if !su.rateLimiter.Allow() {
		msg := fmt.Sprintf("Updating status is limited qps=%v burst=%v", statusUpdateQPS, statusUpdateBurst)
		return fmt.Errorf(msg)
	}

	newNodeMetric := nodeMetric.DeepCopy()
	newNodeMetric.Status = *newStatus

	_, err := su.nodeMetricClient.UpdateStatus(context.TODO(), newNodeMetric, metav1.UpdateOptions{})
	su.previousTimestamp = time.Now()
	return err
}

func convertNodeMetricToResourceMap(nodeMetric *metriccache.NodeResourceMetric) slov1alpha1.ResourceMap {
	var deviceInfos []schedulingv1alpha1.DeviceInfo
	if len(nodeMetric.GPUs) > 0 {
		for _, gpu := range nodeMetric.GPUs {
			memoryRatioRaw := 100 * float64(gpu.MemoryUsed.Value()) / float64(gpu.MemoryTotal.Value())
			gpuInfo := schedulingv1alpha1.DeviceInfo{
				UUID:  gpu.DeviceUUID,
				Minor: &gpu.Minor,
				Type:  schedulingv1alpha1.GPU,
				// TODO: how to check the health status of GPU
				Resources: map[corev1.ResourceName]resource.Quantity{
					apiext.GPUCore:        *resource.NewQuantity(int64(gpu.SMUtil), resource.BinarySI),
					apiext.GPUMemory:      gpu.MemoryUsed,
					apiext.GPUMemoryRatio: *resource.NewQuantity(int64(memoryRatioRaw), resource.BinarySI),
				},
			}
			deviceInfos = append(deviceInfos, gpuInfo)
		}
	}
	return slov1alpha1.ResourceMap{
		ResourceList: corev1.ResourceList{
			corev1.ResourceCPU:    nodeMetric.CPUUsed.CPUUsed,
			corev1.ResourceMemory: nodeMetric.MemoryUsed.MemoryWithoutCache,
		},
		Devices: deviceInfos,
	}
}

func convertPodMetricToResourceMap(podMetric *metriccache.PodResourceMetric) *slov1alpha1.ResourceMap {
	var deviceInfos []schedulingv1alpha1.DeviceInfo
	if len(podMetric.GPUs) > 0 {
		for _, gpu := range podMetric.GPUs {
			memoryRatioRaw := 100 * float64(gpu.MemoryUsed.Value()) / float64(gpu.MemoryTotal.Value())
			gpuInfo := schedulingv1alpha1.DeviceInfo{
				UUID:  gpu.DeviceUUID,
				Minor: &gpu.Minor,
				Type:  schedulingv1alpha1.GPU,
				// TODO: how to check the health status of GPU
				Resources: map[corev1.ResourceName]resource.Quantity{
					apiext.GPUCore:        *resource.NewQuantity(int64(gpu.SMUtil), resource.DecimalSI),
					apiext.GPUMemory:      gpu.MemoryUsed,
					apiext.GPUMemoryRatio: *resource.NewQuantity(int64(memoryRatioRaw), resource.DecimalSI),
				},
			}
			deviceInfos = append(deviceInfos, gpuInfo)
		}
	}
	return &slov1alpha1.ResourceMap{
		ResourceList: corev1.ResourceList{
			corev1.ResourceCPU:    podMetric.CPUUsed.CPUUsed,
			corev1.ResourceMemory: podMetric.MemoryUsed.MemoryWithoutCache,
		},
		Devices: deviceInfos,
	}
}
