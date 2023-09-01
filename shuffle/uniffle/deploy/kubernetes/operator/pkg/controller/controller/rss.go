/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controller

import (
	"context"
	"fmt"
	"strings"
	"sync"
	"time"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes"
	appslister "k8s.io/client-go/listers/apps/v1"
	corelisters "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/tools/record"
	"k8s.io/client-go/util/retry"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/manager"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/config"
	controllerconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/sync/coordinator"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/sync/shuffleserver"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/util"
	kubeutil "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/util/kubernetes"
	propertiestutil "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/util/properties"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/clientset/versioned"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/informers/externalversions"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/listers/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

var _ RSSController = &rssController{}

const (
	controllerName = "rss-controller"
	appNameIndexer = "AppNameIndexer"
)

// RSSController is responsible for synchronizing rss objects.
type RSSController interface {
	manager.Runnable
}

// NewRSSController creates a RSSController.
func NewRSSController(cfg *config.Config) RSSController {
	return newRSSController(cfg)
}

// newRSSController creates a rssController.
func newRSSController(cfg *config.Config) *rssController {
	rc := &rssController{
		workers:                      cfg.Workers,
		kubeClient:                   cfg.KubeClient,
		rssClient:                    cfg.RSSClient,
		rssInformerFactory:           externalversions.NewSharedInformerFactory(cfg.RSSClient, 0),
		shuffleServerInformerFactory: utils.BuildShuffleServerInformerFactory(cfg.KubeClient),
		coordinatorInformerFactory:   utils.BuildCoordinatorInformerFactory(cfg.KubeClient),
		rssQueue: workqueue.NewNamedRateLimitingQueue(workqueue.DefaultControllerRateLimiter(),
			"rss-queue"),
		shuffleServerQueue: workqueue.NewNamedRateLimitingQueue(workqueue.DefaultControllerRateLimiter(),
			"pod-queue"),
		eventRecorder: util.CreateRecorder(cfg.KubeClient, "rss-controller"),
	}

	rssInformer := rc.rssInformerFactory.Uniffle().V1alpha1().RemoteShuffleServices().Informer()
	rssInformer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc:    rc.addRSS,
		UpdateFunc: rc.updateRSS,
	})
	rc.rssInformer = rssInformer
	rc.rssLister = rc.rssInformerFactory.Uniffle().V1alpha1().RemoteShuffleServices().Lister()

	rc.stsInformer = rc.shuffleServerInformerFactory.Apps().V1().StatefulSets().Informer()
	rc.stsLister = rc.shuffleServerInformerFactory.Apps().V1().StatefulSets().Lister()

	rc.podInformer = rc.shuffleServerInformerFactory.Core().V1().Pods().Informer()
	rc.podInformer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		DeleteFunc: rc.deleteShuffleServer,
	})
	if err := rc.podInformer.AddIndexers(cache.Indexers{appNameIndexer: getAppName}); err != nil {
		klog.Fatalf("add app name indexer failed: %v", err)
	}
	rc.podIndexer = rc.podInformer.GetIndexer()
	rc.podLister = rc.shuffleServerInformerFactory.Core().V1().Pods().Lister()

	rc.cmInformer = rc.coordinatorInformerFactory.Core().V1().ConfigMaps().Informer()
	rc.cmLister = rc.coordinatorInformerFactory.Core().V1().ConfigMaps().Lister()

	return rc
}

// rssController implements the RSSController interface.
type rssController struct {
	workers                      int
	kubeClient                   kubernetes.Interface
	rssClient                    versioned.Interface
	rssInformerFactory           externalversions.SharedInformerFactory
	rssInformer                  cache.SharedIndexInformer
	rssLister                    v1alpha1.RemoteShuffleServiceLister
	shuffleServerInformerFactory informers.SharedInformerFactory
	stsInformer                  cache.SharedIndexInformer
	stsLister                    appslister.StatefulSetLister
	podInformer                  cache.SharedIndexInformer
	podIndexer                   cache.Indexer
	podLister                    corelisters.PodLister
	coordinatorInformerFactory   informers.SharedInformerFactory
	cmInformer                   cache.SharedIndexInformer
	cmLister                     corelisters.ConfigMapLister
	rssQueue                     workqueue.RateLimitingInterface
	shuffleServerQueue           workqueue.RateLimitingInterface
	eventRecorder                record.EventRecorder
}

// Start starts the RSSController.
func (r *rssController) Start(ctx context.Context) error {
	klog.V(2).Infof("%v is starting", controllerName)
	r.rssInformerFactory.Start(ctx.Done())
	r.shuffleServerInformerFactory.Start(ctx.Done())
	r.coordinatorInformerFactory.Start(ctx.Done())
	if !cache.WaitForCacheSync(ctx.Done(), r.rssInformer.HasSynced, r.stsInformer.HasSynced,
		r.podInformer.HasSynced, r.cmInformer.HasSynced) {
		return fmt.Errorf("wait for cache synced failed")
	}
	klog.V(2).Infof("%v started", controllerName)
	for i := 0; i < r.workers; i++ {
		go wait.Until(r.runRssWorker, time.Second, ctx.Done())
	}
	for i := 0; i < r.workers; i++ {
		go wait.Until(r.runShuffleServerWorker, time.Second, ctx.Done())
	}
	<-ctx.Done()
	return nil
}

// runRssWorker runs a thread that dequeues rss objects, handles them, and marks them done.
func (r *rssController) runRssWorker() {
	for r.processNextWorkRss() {
	}
}

// processNextWorkRss deals with one rss key off the rssQueue, it returns false when it's time to quit.
func (r *rssController) processNextWorkRss() bool {
	rssKey, quit := r.rssQueue.Get()
	if quit {
		return false
	}
	defer r.rssQueue.Done(rssKey)

	rssNamespace, rssName, err := cache.SplitMetaNamespaceKey(rssKey.(string))
	if err != nil {
		klog.Errorf("parsed rss key (%v) failed: %v", rssKey, err)
		return true
	}

	var retryChecking bool
	retryChecking, err = r.processRss(rssNamespace, rssName)
	if err != nil {
		klog.Errorf("processed rss %v failed : %v", rssKey, err)
		r.rssQueue.AddRateLimited(rssKey)
	} else if retryChecking {
		klog.V(4).Infof("we will retry checking rss (%v) for upgrading or terminating", rssKey)
		r.rssQueue.AddAfter(rssKey, time.Second*3)
	} else {
		r.rssQueue.Forget(rssKey)
	}
	return true
}

// runShuffleServerWorker runs a thread that dequeues shuffle server pods, handles them, and marks them done.
func (r *rssController) runShuffleServerWorker() {
	for r.processNextWorkShuffleServer() {
	}
}

// processNextWorkShuffleServer deals with one shuffle server pod key off the shuffleServerQueue,
// it returns false when it's time to quit.
func (r *rssController) processNextWorkShuffleServer() bool {
	key, quit := r.shuffleServerQueue.Get()
	if quit {
		return false
	}
	defer r.shuffleServerQueue.Done(key)

	err := r.processShuffleServer(key.(string))
	if err == nil {
		r.shuffleServerQueue.Forget(key)
		return true
	}

	klog.Errorf("processed shuffle server %v failed : %v", key, err)
	r.shuffleServerQueue.AddRateLimited(key)
	return true
}

// processShuffleServer process current shuffle server pod key.
func (r *rssController) processShuffleServer(key string) error {
	klog.V(4).Infof("processing shuffle server (%v)", key)
	namespace, rssName, currentKey := parsePodCacheKey(key)
	rss, err := r.rssLister.RemoteShuffleServices(namespace).Get(rssName)
	if err != nil {
		klog.Errorf("get rss by key (%v) failed: %v", key, err)
		if !apierrors.IsNotFound(err) {
			return err
		}
		return nil
	}

	return r.updateTargetAndDeletedKeys(rss, currentKey)
}

// updateTargetAndDeletedKeys updates target and deleted keys in status of rss objects.
func (r *rssController) updateTargetAndDeletedKeys(rss *unifflev1alpha1.RemoteShuffleService,
	shuffleServerKey string) error {
	deletedKeys := sets.NewString(rss.Status.DeletedKeys...)
	if deletedKeys.Has(shuffleServerKey) {
		klog.V(3).Infof("shuffle server (%v) has already been deleted for rss (%v)",
			shuffleServerKey, utils.UniqueName(rss))
		return nil
	}

	deletedKeys.Insert(shuffleServerKey)
	oldTargetKeys := sets.NewString(rss.Status.TargetKeys...)
	newTargetKeys := oldTargetKeys.Difference(deletedKeys)

	cm, err := r.getExcludeNodesCM(rss)
	if err != nil {
		klog.Errorf("get exclude nodes in configMap for rss (%v) failed: %v",
			utils.UniqueName(rss), err)
		return err
	}

	// calculate new exclude nodes.
	excludeNodesFileKey := utils.GetExcludeNodesConfigMapKey(rss)
	oldExcludeNodes := sets.NewString(strings.Split(
		cm.Data[excludeNodesFileKey], "\n")...)
	newExcludeNodes := utils.ConvertShuffleServerKeysToNodes(newTargetKeys)
	if !newExcludeNodes.Equal(oldExcludeNodes) {
		cmCopy := cm.DeepCopy()
		cmCopy.Data[excludeNodesFileKey] = strings.Join(
			utils.GetSortedList(newExcludeNodes), "\n")
		if _, err = r.kubeClient.CoreV1().ConfigMaps(cmCopy.Namespace).
			Update(context.Background(), cmCopy, metav1.UpdateOptions{}); err != nil {
			klog.Errorf("update exclude nodes in configMap (%v) for rss (%v) failed: %v",
				cmCopy.Name, utils.UniqueName(rss), err)
			return err
		}
	}

	if err = r.updateDeletedKeys(rss, shuffleServerKey); err != nil {
		klog.Errorf("add (%v) to deleted keys of rss (%v) failed: %v",
			shuffleServerKey, utils.UniqueName(rss), err)
		return err
	}
	return nil
}

func (r *rssController) clearExcludeNodes(rss *unifflev1alpha1.RemoteShuffleService) error {
	cm, err := r.getExcludeNodesCM(rss)
	if err != nil {
		klog.Errorf("get exclude nodes in configMap for rss (%v) failed: %v",
			utils.UniqueName(rss), err)
		return err
	}
	cmCopy := cm.DeepCopy()
	cmCopy.Data[utils.GetExcludeNodesConfigMapKey(rss)] = ""
	if _, err = r.kubeClient.CoreV1().ConfigMaps(cmCopy.Namespace).
		Update(context.Background(), cmCopy, metav1.UpdateOptions{}); err != nil {
		klog.Errorf("update exclude nodes in configMap (%v) for rss (%v) failed: %v",
			cmCopy.Name, utils.UniqueName(rss), err)
		return err
	}
	return nil
}

// getExcludeNodesCM returns configMap records exclude nodes.
func (r *rssController) getExcludeNodesCM(rss *unifflev1alpha1.RemoteShuffleService) (
	*corev1.ConfigMap, error) {
	namespace := rss.Namespace
	name := utils.GenerateCoordinatorName(rss)
	cm, err := r.cmLister.ConfigMaps(namespace).Get(name)
	if err != nil {
		klog.Errorf("get configMap (%v) for rss failed: %v", name, utils.UniqueName(rss), err)
		return nil, err
	}
	return cm, nil
}

// updateDeletedKeys updates deleted keys in status of rss.
func (r *rssController) updateDeletedKeys(rss *unifflev1alpha1.RemoteShuffleService,
	key string) error {
	if rss.Status.Phase != unifflev1alpha1.RSSUpgrading && rss.Status.Phase != unifflev1alpha1.RSSTerminating {
		return nil
	}
	rssStatus := rss.Status.DeepCopy()
	rssStatus.DeletedKeys = utils.GetSortedList(sets.NewString(rssStatus.DeletedKeys...).Insert(key))
	return r.updateRssStatus(rss, rssStatus)
}

// processRss process current rss by its namespace and name, and returns if we need to try again to check upgrading.
func (r *rssController) processRss(namespace, name string) (bool, error) {
	klog.V(4).Infof("processing rss (%v/%v)", namespace, name)
	startTime := time.Now()
	defer func() {
		klog.V(4).Infof("finished processing rss (%v/%v) cost %v",
			namespace, name, time.Since(startTime))
	}()

	rss, err := r.rssLister.RemoteShuffleServices(namespace).Get(name)
	if err != nil {
		if apierrors.IsNotFound(err) {
			klog.V(5).Infof("ignored deleted rss (%v/%v)", namespace, name)
			return false, nil
		}
		klog.Errorf("get rss (%v/%v) failed: %v", namespace, name, err)
		return false, err
	}

	if rss.DeletionTimestamp != nil {
		return r.processDeleting(rss)
	}
	return r.processNormal(rss)
}

// processDeleting process the deleting rss.
func (r *rssController) processDeleting(rss *unifflev1alpha1.RemoteShuffleService) (bool, error) {
	klog.V(4).Infof("process rss (%v) to be deleted in %v phase",
		utils.UniqueName(rss), rss.Status.Phase)
	if rss.Status.Phase == unifflev1alpha1.RSSRunning || rss.Status.Phase == unifflev1alpha1.RSSPending ||
		rss.Status.Phase == unifflev1alpha1.RSSUpgrading {
		return false, r.updateRssStatus(rss, &unifflev1alpha1.RemoteShuffleServiceStatus{
			Phase: unifflev1alpha1.RSSTerminating,
		})
	} else if rss.Status.Phase == unifflev1alpha1.RSSTerminating {
		if ok, err := r.canDeleteRss(rss); err == nil && ok {
			return false, r.removeFinalizer(rss)
		}
		return r.scaleDownShuffleServer(rss)
	}
	return false, nil
}

func (r *rssController) canDeleteRss(rss *unifflev1alpha1.RemoteShuffleService) (bool, error) {
	objs, err := r.podIndexer.ByIndex(appNameIndexer, utils.GenerateShuffleServerName(rss))
	if err != nil {
		klog.Errorf("get objects by indexer (%v) failed: %v", appNameIndexer, err)
		return false, err
	}
	lastPods := 0
	for _, obj := range objs {
		pod, ok := obj.(*corev1.Pod)
		if ok && pod.DeletionTimestamp == nil {
			lastPods++
		}
	}
	return lastPods == 0, nil
}

func (r *rssController) scaleDownShuffleServer(rss *unifflev1alpha1.RemoteShuffleService) (
	bool, error) {
	namespace := rss.Namespace
	stsName := utils.GenerateShuffleServerName(rss)
	sts, err := r.stsLister.StatefulSets(namespace).Get(stsName)
	if err != nil {
		if apierrors.IsNotFound(err) {
			klog.V(5).Infof("ignored deleted statefulSet (%v/%v)", namespace, stsName)
			return false, nil
		}
		klog.Errorf("get statefulSet (%v) for rss (%v) failed: %v",
			stsName, utils.UniqueName(rss), err)
		return false, err
	}
	if *sts.Spec.Replicas == 0 {
		return true, nil
	}
	stsCopy := sts.DeepCopy()
	stsCopy.Spec.Replicas = pointer.Int32(0)
	_, err = kubeutil.PatchStatefulSet(r.kubeClient, sts, stsCopy)
	return true, err
}

// processNormal process the normal rss, and determines whether we need to check again.
func (r *rssController) processNormal(rss *unifflev1alpha1.RemoteShuffleService) (bool, error) {
	klog.V(4).Infof("process rss (%v) in %v phase", utils.UniqueName(rss), rss.Status.Phase)
	switch rss.Status.Phase {
	case unifflev1alpha1.RSSPending:
		return false, r.processPendingRSS(rss)
	case unifflev1alpha1.RSSRunning:
		return false, r.processRunningRSS(rss)
	case unifflev1alpha1.RSSUpgrading:
		return r.processUpgradingRSS(rss)
	case unifflev1alpha1.RSSFailed:
		klog.Errorf("failed to process rss (%v): %v", utils.UniqueName(rss), rss.Status.Reason)
	default:
		return false, r.updateRssStatus(rss,
			&unifflev1alpha1.RemoteShuffleServiceStatus{Phase: unifflev1alpha1.RSSPending})
	}
	return false, nil
}

// processPendingRSS processes the pending rss by synchronizing generated objects of coordinators and shuffle servers.
func (r *rssController) processPendingRSS(rss *unifflev1alpha1.RemoteShuffleService) error {
	if err := r.syncObjects(rss); err != nil {
		klog.Errorf("sync objects for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	return r.updateRssStatus(rss,
		&unifflev1alpha1.RemoteShuffleServiceStatus{Phase: unifflev1alpha1.RSSRunning})
}

// processRunningRSS processes the running rss.
func (r *rssController) processRunningRSS(rss *unifflev1alpha1.RemoteShuffleService) error {
	if err := r.syncObjects(rss); err != nil {
		klog.Errorf("sync objects for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	if *rss.Spec.ShuffleServer.Sync {
		return r.prepareForUpgrading(rss)
	}
	return nil
}

// syncObjects synchronizes objects related to coordinators and shuffle servers.
func (r *rssController) syncObjects(rss *unifflev1alpha1.RemoteShuffleService) error {
	if err := r.syncConfigMap(rss); err != nil {
		klog.Errorf("sync configMap for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	if err := r.syncCoordinator(rss); err != nil {
		klog.Errorf("sync coordinators for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	if err := r.syncShuffleServer(rss); err != nil {
		klog.Errorf("sync shuffle servers for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	return nil
}

func (r *rssController) syncConfigMap(rss *unifflev1alpha1.RemoteShuffleService) error {
	cm, err := r.kubeClient.CoreV1().ConfigMaps(rss.Namespace).
		Get(context.Background(), rss.Spec.ConfigMapName, metav1.GetOptions{})
	if err != nil {
		klog.Errorf("get configMap of rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	if owner := util.GetConfigMapOwner(cm); len(owner) > 0 && owner != rss.Name {
		return fmt.Errorf("conflict configMap name of rss (%v <> %v)", owner, rss.Name)
	}
	if _, ok := cm.Data[constants.CoordinatorConfigKey]; !ok {
		return fmt.Errorf("%v not exist", constants.CoordinatorConfigKey)
	}
	if _, ok := cm.Data[constants.ShuffleServerConfigKey]; !ok {
		return fmt.Errorf("%v not exist", constants.ShuffleServerConfigKey)
	}
	if _, ok := cm.Data[constants.Log4jPropertiesKey]; !ok {
		return fmt.Errorf("%v not exist", constants.Log4jPropertiesKey)
	}

	cm.Data[constants.CoordinatorConfigKey] = propertiestutil.UpdateProperties(
		cm.Data[constants.CoordinatorConfigKey], coordinator.GenerateProperties(rss))
	cm.Data[constants.ShuffleServerConfigKey] = propertiestutil.UpdateProperties(
		cm.Data[constants.ShuffleServerConfigKey], shuffleserver.GenerateProperties(rss))
	if cm.Labels == nil {
		cm.Labels = make(map[string]string)
	}
	cm.Labels[controllerconstants.OwnerLabel] = rss.Name
	if _, err = r.kubeClient.CoreV1().ConfigMaps(cm.Namespace).
		Update(context.Background(), cm, metav1.UpdateOptions{}); err != nil {
		klog.Errorf("update configMap (%v) of rss (%v) failed: %v", rss.Spec.ConfigMapName,
			utils.UniqueName(rss), err)
	}
	return err
}

// processUpgradingRSS processes the upgrading rss, and determines whether we need to check again.
func (r *rssController) processUpgradingRSS(rss *unifflev1alpha1.RemoteShuffleService) (bool, error) {
	switch rss.Spec.ShuffleServer.UpgradeStrategy.Type {
	case unifflev1alpha1.FullUpgrade:
		return r.processFullUpgrade(rss)
	case unifflev1alpha1.PartitionUpgrade:
		return r.processPartitionUpgrade(rss)
	case unifflev1alpha1.SpecificUpgrade:
		return r.processSpecificUpgrade(rss)
	case unifflev1alpha1.FullRestart:
		return r.processFullRestart(rss)
	}
	return false, nil
}

// processFullUpgrade process the rss needs full upgrade, and determines whether we need to check again.
func (r *rssController) processFullUpgrade(rss *unifflev1alpha1.RemoteShuffleService) (bool, error) {
	return r.processRollingUpgrade(rss, 0)
}

// processPartitionUpgrade process the rss needs partition upgrade, and determines whether we need to check again.
func (r *rssController) processPartitionUpgrade(
	rss *unifflev1alpha1.RemoteShuffleService) (bool, error) {
	return r.processRollingUpgrade(rss, *rss.Spec.ShuffleServer.UpgradeStrategy.Partition)
}

// // processSpecificUpgrade process the rss needs specific upgrade, and determines whether we need to check again.
func (r *rssController) processSpecificUpgrade(rss *unifflev1alpha1.RemoteShuffleService) (
	bool, error) {
	if err := r.serZeroPartition(rss, 0); err != nil {
		klog.Errorf("update partition of statefulSet of rss (%v) failed: %v",
			utils.UniqueName(rss), err)
		return false, err
	}

	targetNames := getNamesNeedToBeUpgraded(rss)
	return r.handleTargetPods(targetNames, rss)
}

// handleTargetPods handles all target pods need to be upgraded or restarted,
// and determines whether we need to check again.
func (r *rssController) handleTargetPods(targetNames []string, rss *unifflev1alpha1.RemoteShuffleService) (
	bool, error) {
	if len(targetNames) == 0 {
		if err := r.clearExcludeNodes(rss); err != nil {
			klog.Errorf("clear exclude nodes for finished upgrade of rss (%v) failed: %v",
				utils.UniqueName(rss))
			return true, err
		}
		return false, r.updateRssStatus(rss,
			&unifflev1alpha1.RemoteShuffleServiceStatus{Phase: unifflev1alpha1.RSSRunning})
	}
	r.deleteTargetPods(targetNames, rss)
	return true, nil
}

// deleteTargetPods tries to delete all target pods for upgrading or restarting.
func (r *rssController) deleteTargetPods(targetNames []string, rss *unifflev1alpha1.RemoteShuffleService) {
	wg := sync.WaitGroup{}
	wg.Add(len(targetNames))
	for i := range targetNames {
		go func(podName string) {
			defer wg.Done()
			klog.V(5).Infof("try to delete shuffler server %v/%v", rss.Namespace, podName)
			if err := r.kubeClient.CoreV1().Pods(rss.Namespace).Delete(context.Background(),
				podName, metav1.DeleteOptions{}); err != nil {
				klog.V(5).Infof("deleted shuffler server %v/%v failed: %v",
					rss.Namespace, podName, err)
			}
		}(targetNames[i])
	}
	wg.Wait()
}

// // processSpecificUpgrade process the rss needs full restart, and determines whether it has finished restarting.
func (r *rssController) processFullRestart(rss *unifflev1alpha1.RemoteShuffleService) (
	bool, error) {
	targetNames, err := getNamesNeedToBeRestarted(r.podLister, rss)
	if err != nil {
		klog.Errorf("get all pod names of rss (%v) to be restarted failed: %v", utils.UniqueName(rss), err)
		return false, err
	}
	return r.handleTargetPods(targetNames, rss)
}

// syncCoordinator synchronizes objects related to coordinators.
func (r *rssController) syncCoordinator(rss *unifflev1alpha1.RemoteShuffleService) error {
	if !*rss.Spec.Coordinator.Sync {
		return nil
	}
	serviceAccount, configMap, services, deployments := coordinator.GenerateCoordinators(rss)
	if err := kubeutil.SyncServiceAccount(r.kubeClient, serviceAccount); err != nil {
		klog.Errorf("sync serviceAccount (%v) for rss (%v) failed: %v",
			utils.UniqueName(serviceAccount), utils.UniqueName(rss), err)
		return err
	}
	if err := kubeutil.SyncConfigMap(r.kubeClient, configMap); err != nil {
		klog.Errorf("sync configMap (%v) for rss (%v) failed: %v",
			utils.UniqueName(serviceAccount), utils.UniqueName(rss), err)
		return err
	}
	if err := kubeutil.SyncServices(r.kubeClient, services); err != nil {
		klog.Errorf("sync services for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	if err := kubeutil.SyncDeployments(r.kubeClient, deployments); err != nil {
		klog.Errorf("sync deployments for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	return nil
}

// syncShuffleServer synchronizes objects related to shuffle servers.
func (r *rssController) syncShuffleServer(rss *unifflev1alpha1.RemoteShuffleService) error {
	if rss.Status.Phase == unifflev1alpha1.RSSRunning && !*rss.Spec.ShuffleServer.Sync {
		return nil
	}
	// we don't need to generate svc for shuffle servers:
	// shuffle servers are access directly through coordinator's shuffler assignments. service for shuffle server is
	// pointless. For spark apps running in the cluster, executor containers could access shuffler server via container
	// network(overlay or host network). If shuffle servers should be exposed to external, host network should be used
	// and external executor should access the host node ip:port directly.
	serviceAccount, statefulSet := shuffleserver.GenerateShuffleServers(rss)
	if err := kubeutil.SyncServiceAccount(r.kubeClient, serviceAccount); err != nil {
		klog.Errorf("sync SA (%v) for rss (%v) failed: %v",
			utils.UniqueName(serviceAccount), utils.UniqueName(rss), err)
		return err
	}
	if _, _, err := kubeutil.SyncStatefulSet(r.kubeClient, statefulSet, true); err != nil {
		klog.Errorf("sync StatefulSet for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return err
	}
	return nil
}

// prepareForUpgrading prepares for the upgrade by disable syncing shuffle servers.
func (r *rssController) prepareForUpgrading(rss *unifflev1alpha1.RemoteShuffleService) error {
	updated, err := r.disableSyncingShuffleServer(rss)
	if err != nil {
		klog.Errorf("disabled syncing shuffle server failed: %v", err)
		return err
	}
	err = r.updateRssStatus(updated,
		&unifflev1alpha1.RemoteShuffleServiceStatus{Phase: unifflev1alpha1.RSSUpgrading})
	if err != nil {
		r.eventRecorder.Event(rss, corev1.EventTypeWarning, controllerconstants.UpdateStatusError,
			fmt.Sprintf("set %v phase failed: %v", unifflev1alpha1.RSSUpgrading, err))
	}
	return err
}

// disableSyncingShuffleServer disables syncing of shuffle servers before upgrading.
func (r *rssController) disableSyncingShuffleServer(rss *unifflev1alpha1.RemoteShuffleService) (
	*unifflev1alpha1.RemoteShuffleService, error) {
	rssCopy := rss.DeepCopy()
	rssCopy.Spec.ShuffleServer.Sync = pointer.Bool(false)
	updated, err := r.rssClient.UniffleV1alpha1().RemoteShuffleServices(rssCopy.Namespace).
		Update(context.Background(), rssCopy, metav1.UpdateOptions{})
	if err != nil {
		klog.Errorf("updated .spec.shuffleServer.sync of rss (%v) failed: %v",
			utils.UniqueName(rssCopy), err)
		return nil, err
	}
	return updated, nil
}

// updateRssStatus updates status of rss.
func (r *rssController) updateRssStatus(rss *unifflev1alpha1.RemoteShuffleService,
	status *unifflev1alpha1.RemoteShuffleServiceStatus) error {
	rssCopy := rss.DeepCopy()
	rssCopy.Status = *status
	_, err := r.rssClient.UniffleV1alpha1().RemoteShuffleServices(rssCopy.Namespace).
		UpdateStatus(context.Background(), rssCopy, metav1.UpdateOptions{})
	if err == nil {
		return nil
	}
	klog.Errorf("updated status of rss (%v) failed: %v", utils.UniqueName(rssCopy), err)
	if !apierrors.IsConflict(err) {
		return err
	}
	return retry.RetryOnConflict(retry.DefaultRetry, func() error {
		latestRss, getErr := r.rssClient.UniffleV1alpha1().RemoteShuffleServices(rss.Namespace).
			Get(context.Background(), rss.Name, metav1.GetOptions{})
		if getErr != nil {
			klog.Errorf("get rss (%v) failed: %v", utils.UniqueName(rss), getErr)
			return getErr
		}
		latestRss.Status = *status
		_, updateErr := r.rssClient.UniffleV1alpha1().RemoteShuffleServices(rss.Namespace).
			UpdateStatus(context.Background(), latestRss, metav1.UpdateOptions{})
		if updateErr != nil {
			klog.Errorf("retry updating status of rss (%v) failed: %v",
				utils.UniqueName(latestRss), updateErr)
		}
		return updateErr
	})
}

// addRSS handles the added rss.
func (r *rssController) addRSS(obj interface{}) {
	r.enqueueRss(obj)
}

// updateRSS handles the updated rss.
func (r *rssController) updateRSS(_, newObj interface{}) {
	r.enqueueRss(newObj)
}

// deleteShuffleServer handles the deleted shuffler server pod.
func (r *rssController) deleteShuffleServer(obj interface{}) {
	r.enqueueShuffleServer(obj)
}

// enqueueRss enqueues a rss.
func (r *rssController) enqueueRss(obj interface{}) {
	rss, ok := obj.(*unifflev1alpha1.RemoteShuffleService)
	if !ok {
		klog.Errorf("object is not a rss: %+v", obj)
		return
	}

	key, err := cache.DeletionHandlingMetaNamespaceKeyFunc(rss)
	if err != nil {
		klog.Errorf("can not get key of rss: %v", utils.UniqueName(rss))
		return
	}
	r.rssQueue.Add(key)
}

// enqueueShuffleServer enqueues a shuffle server pod.
func (r *rssController) enqueueShuffleServer(obj interface{}) {
	var pod *corev1.Pod
	switch t := obj.(type) {
	case *corev1.Pod:
		pod = t
	case cache.DeletedFinalStateUnknown:
		if p, ok := t.Obj.(*corev1.Pod); ok {
			pod = p
		}
	}
	if pod == nil {
		klog.Errorf("object is not a Pod object: %+v", obj)
		return
	}
	rssName := utils.GetRssNameByPod(pod)
	if len(rssName) != 0 {
		key := buildPodCacheKey(pod.Namespace, rssName, utils.BuildShuffleServerKey(pod))
		r.shuffleServerQueue.Add(key)
	}
}

// removeFinalizer removes the finalizer of rss for deleting it.
func (r *rssController) removeFinalizer(rss *unifflev1alpha1.RemoteShuffleService) error {
	rssCopy := rss.DeepCopy()
	var finalizers []string
	for _, f := range rssCopy.Finalizers {
		if f == constants.RSSFinalizerName {
			continue
		}
		finalizers = append(finalizers, f)
	}
	rssCopy.Finalizers = finalizers
	_, err := r.rssClient.UniffleV1alpha1().RemoteShuffleServices(rssCopy.Namespace).
		Update(context.Background(), rssCopy, metav1.UpdateOptions{})
	if err != nil {
		klog.Errorf("removed finalizer from rss (%v) failed: %v", utils.UniqueName(rssCopy), err)
		return err
	}
	klog.V(3).Infof("removed finalizer from rss (%v)", utils.UniqueName(rssCopy))
	return nil
}

// processRollingUpgrade processes full or partition upgrade in rolling mode.
func (r *rssController) processRollingUpgrade(rss *unifflev1alpha1.RemoteShuffleService,
	minPartition int32) (bool, error) {
	finished, oldSts, err := r.checkUpgradeFinished(rss, minPartition)
	if err != nil {
		klog.Errorf("checked whether the current upgrade of rss (%v) has been finished failed: %v",
			utils.UniqueName(rss), err)
		return false, err
	}
	if finished {
		if err = r.clearExcludeNodes(rss); err != nil {
			klog.Errorf("clear exclude nodes for finished upgrade of rss (%v) failed: %v",
				utils.UniqueName(rss))
			return true, err
		}
		return false, r.updateRssStatus(rss,
			&unifflev1alpha1.RemoteShuffleServiceStatus{Phase: unifflev1alpha1.RSSRunning})
	}

	return true, r.decreasePartition(oldSts, minPartition)
}

// checkUpgradeFinished checks whether we have finished the current upgrade.
func (r *rssController) checkUpgradeFinished(rss *unifflev1alpha1.RemoteShuffleService,
	minPartition int32) (bool, *appsv1.StatefulSet, error) {
	stsName := shuffleserver.GenerateName(rss)
	oldSts, err := r.stsLister.StatefulSets(rss.Namespace).Get(stsName)
	if err != nil {
		klog.Errorf("get StatefulSet (%v/%v) failed: %v", rss.Namespace, stsName, err)
		return false, nil, err
	}
	if oldSts.Status.CurrentRevision == oldSts.Status.UpdateRevision {
		minPartition = 0
	}
	klog.V(4).Infof("current updatedReplicas: %v, replicas: %v, minPartition: %v",
		oldSts.Status.UpdatedReplicas, *oldSts.Spec.Replicas, minPartition)
	if oldSts.Status.UpdatedReplicas >= *oldSts.Spec.Replicas-minPartition &&
		*oldSts.Spec.Replicas == oldSts.Status.ReadyReplicas {
		klog.V(4).Infof("do not need to update partition of statefulSet (%v)",
			utils.UniqueName(oldSts))
		return true, nil, nil
	}
	return false, oldSts, nil
}

// decreasePartition decrease the partition value of statefulSet used by shuffle server if we do net finish the upgrade.
func (r *rssController) decreasePartition(oldSts *appsv1.StatefulSet, minPartition int32) error {
	newSts := oldSts.DeepCopy()
	targetPartition := *newSts.Spec.Replicas - oldSts.Status.UpdatedReplicas - 1
	if targetPartition < minPartition {
		targetPartition = minPartition
	}
	klog.V(4).Infof("set partition of statefulSet (%v) to %v >= %v",
		utils.UniqueName(newSts), targetPartition, minPartition)
	newSts.Spec.UpdateStrategy.RollingUpdate.Partition = &targetPartition
	_, err := kubeutil.PatchStatefulSet(r.kubeClient, oldSts, newSts)
	return err
}

// serZeroPartition sets zero value for partition of statefulSet used by shuffle servers for specify upgrade,
// because we need to
func (r *rssController) serZeroPartition(rss *unifflev1alpha1.RemoteShuffleService,
	partition int32) error {
	stsName := shuffleserver.GenerateName(rss)
	oldSts, err := r.stsLister.StatefulSets(rss.Namespace).Get(stsName)
	if err != nil {
		klog.Errorf("get StatefulSet (%v/%v) failed: %v", rss.Namespace, stsName, err)
		return err
	}
	newSts := oldSts.DeepCopy()
	newSts.Spec.UpdateStrategy.RollingUpdate.Partition = &partition
	_, err = kubeutil.PatchStatefulSet(r.kubeClient, oldSts, newSts)
	return err
}

// buildPodCacheKey builds a key for shuffle server pod queue.
func buildPodCacheKey(namespace, rssName, shuffleServerKey string) string {
	return namespace + "|" + rssName + "|" + shuffleServerKey
}

// parsePodCacheKey parses the key in shuffle server pod queue.
func parsePodCacheKey(key string) (namespace, rssName, shuffleServerKey string) {
	values := strings.Split(key, "|")
	if len(values) == 3 {
		namespace = values[0]
		rssName = values[1]
		shuffleServerKey = values[2]
	}
	return
}

// getNamesNeedToBeDeleted returns the names of shuffle server pods need to be deleted currently.
func getNamesNeedToBeDeleted(targetNames []string, rss *unifflev1alpha1.RemoteShuffleService) []string {
	deletedKeys := rss.Status.DeletedKeys
	var waitingNames []string
	for _, target := range targetNames {
		found := false
		for _, key := range deletedKeys {
			_, podName, _ := utils.ParseShuffleServerKey(key)
			if podName == target {
				found = true
				break
			}
		}
		if !found {
			waitingNames = append(waitingNames, target)
		}
	}
	return waitingNames
}

// getNamesNeedToBeUpgraded returns the names of shuffle server pods need to be upgraded currently.
func getNamesNeedToBeUpgraded(rss *unifflev1alpha1.RemoteShuffleService) []string {
	specificNames := sets.NewString(rss.Spec.ShuffleServer.UpgradeStrategy.SpecificNames...).List()
	return getNamesNeedToBeDeleted(specificNames, rss)
}

// getNamesNeedToBeRestarted returns the names of shuffle server pods need to be restarted currently.
func getNamesNeedToBeRestarted(podLister corelisters.PodLister, rss *unifflev1alpha1.RemoteShuffleService) (
	[]string, error) {
	pods, err := podLister.Pods(rss.Namespace).List(labels.SelectorFromSet(utils.GenerateShuffleServerLabels(rss)))
	if err != nil {
		klog.Errorf("get pods of rss (%v) failed: %v", utils.UniqueName(rss), err)
		return nil, err
	}
	targetNames := sets.NewString()
	for i := 0; i < len(pods); i++ {
		targetNames.Insert(pods[i].Name)
	}
	return getNamesNeedToBeDeleted(targetNames.List(), rss), nil
}

func getAppName(obj interface{}) ([]string, error) {
	pod, ok := obj.(*corev1.Pod)
	if !ok {
		return nil, fmt.Errorf("not a pod")
	}
	return []string{pod.Labels["app"]}, nil
}
