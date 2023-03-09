/*
Copyright 2022 The Koordinator Authors.
Copyright 2020 The Kubernetes Authors.

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

package controller

import (
	"fmt"
	"sort"
	"strings"
	"time"

	"context"
	"reflect"

	v1 "k8s.io/api/core/v1"
	apierrs "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	coreinformer "k8s.io/client-go/informers/core/v1"
	corelister "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/klog/v2"
	schedv1alpha1 "sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
	schedclientset "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned"
	schedinformer "sigs.k8s.io/scheduler-plugins/pkg/generated/informers/externalversions/scheduling/v1alpha1"
	schedlister "sigs.k8s.io/scheduler-plugins/pkg/generated/listers/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/core"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/util"
)

const (
	maxGCTime              = 48 * time.Hour
	PodGroupControllerName = "PodGroupController"
)

// PodGroupController  is used to control that process pod groups using provided Handler interface
type PodGroupController struct {
	pgQueue         workqueue.RateLimitingInterface
	pgLister        schedlister.PodGroupLister
	podLister       corelister.PodLister
	pgListerSynced  cache.InformerSynced
	podListerSynced cache.InformerSynced
	pgClient        schedclientset.Interface
	pgManager       core.Manager
	workers         int
}

// NewPodGroupController returns a new *PodGroupController
func NewPodGroupController(
	pgInformer schedinformer.PodGroupInformer,
	podInformer coreinformer.PodInformer,
	pgClient schedclientset.Interface,
	podGroupManager *core.PodGroupManager,
	workers int,
) *PodGroupController {
	ctrl := &PodGroupController{
		pgManager:       podGroupManager,
		pgClient:        pgClient,
		pgLister:        pgInformer.Lister(),
		podLister:       podInformer.Lister(),
		pgListerSynced:  pgInformer.Informer().HasSynced,
		podListerSynced: podInformer.Informer().HasSynced,
		pgQueue:         workqueue.NewNamedRateLimitingQueue(workqueue.DefaultControllerRateLimiter(), "PodGroup"),
		workers:         workers,
	}

	pgInformer.Informer().AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc:    ctrl.pgAdded,
		UpdateFunc: ctrl.pgUpdated,
	})

	podInformer.Informer().AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc:    ctrl.podAdded,
		UpdateFunc: ctrl.podUpdated,
	})
	return ctrl
}

func (ctrl PodGroupController) Name() string {
	return PodGroupControllerName
}

func (ctrl *PodGroupController) Start() {
	go ctrl.Run(context.TODO().Done())
}

// Run starts listening on channel events
func (ctrl *PodGroupController) Run(stopCh <-chan struct{}) {
	defer ctrl.pgQueue.ShutDown()
	klog.Infof("Starting Pod Group SyncHandler")
	defer klog.Infof("Shutting Pod Group SyncHandler")

	if !cache.WaitForCacheSync(stopCh, ctrl.pgListerSynced, ctrl.podListerSynced) {
		klog.Errorf("Cannot sync caches")
		return
	}
	klog.Infof("Pod Group sync finished")

	for i := 0; i < ctrl.workers; i++ {
		go wait.Until(ctrl.worker, time.Second, stopCh)
	}

	<-stopCh
}

// pgAdded reacts to a PG creation
func (ctrl *PodGroupController) pgAdded(obj interface{}) {
	key, err := cache.MetaNamespaceKeyFunc(obj)
	if err != nil {
		runtime.HandleError(err)
		return
	}
	pg := obj.(*schedv1alpha1.PodGroup)
	if pg.Status.Phase == schedv1alpha1.PodGroupFinished || pg.Status.Phase == schedv1alpha1.PodGroupFailed {
		return
	}
	// If startScheduleTime - createTime > 2days, do not enqueue again because pod may have been GCed
	if pg.Status.Scheduled == pg.Spec.MinMember && pg.Status.Running == 0 &&
		pg.Status.ScheduleStartTime.Sub(pg.CreationTimestamp.Time) > maxGCTime {
		return
	}
	klog.Infof("Enqueue podGroup, podGroup: %v", key)
	ctrl.pgQueue.Add(key)
}

// pgUpdated reacts to a PG update
func (ctrl *PodGroupController) pgUpdated(old, new interface{}) {
	ctrl.pgAdded(new)
}

// podAdded reacts to a PG creation
func (ctrl *PodGroupController) podAdded(obj interface{}) {
	pod := obj.(*v1.Pod)
	pgName := util.GetGangNameByPod(pod)
	if len(pgName) == 0 {
		return
	}
	pg, err := ctrl.pgLister.PodGroups(pod.Namespace).Get(pgName)
	if err != nil {
		klog.Errorf("Error while adding pod, err: %v", err)
		return
	}
	klog.Infof("Add podGroup when pod gets added, podGroup: %v, pod: %v", util.GetId(pg.Namespace, pg.Name),
		util.GetId(pod.Namespace, pod.Name))
	ctrl.pgAdded(pg)
}

// pgUpdated reacts to a PG update
func (ctrl *PodGroupController) podUpdated(old, new interface{}) {

	ctrl.podAdded(new)
}

func (ctrl *PodGroupController) worker() {
	for ctrl.processNextWorkItem() {
	}
}

// processNextWorkItem deals with one key off the queue.  It returns false when it's time to quit.
func (ctrl *PodGroupController) processNextWorkItem() bool {
	keyObj, quit := ctrl.pgQueue.Get()
	if quit {
		return false
	}
	defer ctrl.pgQueue.Done(keyObj)

	key, ok := keyObj.(string)
	if !ok {
		ctrl.pgQueue.Forget(keyObj)
		runtime.HandleError(fmt.Errorf("expected string in workqueue but got %#v", keyObj))
		return true
	}
	if err := ctrl.syncHandler(key); err != nil {
		runtime.HandleError(err)
		klog.Errorf("Error syncing podGroup, podGroup: %v, err: %v", key, err)
		return true
	}
	return true
}

// syncHandle syncs pod group and convert status
func (ctrl *PodGroupController) syncHandler(key string) error {
	// Convert the namespace/name string into a distinct namespace and name
	namespace, name, err := cache.SplitMetaNamespaceKey(key)
	if err != nil {
		runtime.HandleError(fmt.Errorf("invalid resource key: %s", key))
		return nil
	}
	defer func() {
		if err != nil {
			ctrl.pgQueue.AddRateLimited(key)
			return
		}
	}()
	pg, err := ctrl.pgLister.PodGroups(namespace).Get(name)
	if err != nil {
		if apierrs.IsNotFound(err) {
			klog.Warningf("PodGroup has been deleted, podGroup: %v", key)
			return nil
		}
		klog.Errorf("Unable to retrieve podGroup from store err, podGroup: %v, err: %v", key, err)
		return err
	}

	pgCopy := pg.DeepCopy()
	// get all pods belong to the PogGroup from gangCache
	podsInGangCache := ctrl.pgManager.GetAllPodsFromGang(util.GetId(pg.Namespace, pg.Name))
	// when update the pod's Status, gangCache has not changed,
	// so we get the pods' status from the informer according to pods' keys in gangCache
	// it may happen that when pod is created, we may get the onAdd event here before the gangCache,
	// so we lost the information of the pods, but the lost message can be repaired after the update event
	pods := make([]*v1.Pod, 0)
	for _, pod := range podsInGangCache {
		podFromInformer, err := ctrl.podLister.Pods(pod.Namespace).Get(pod.Name)
		if err != nil {
			klog.ErrorS(err, "PodGroupController get pod from informer error ", "pod", klog.KObj(pod))
			return err
		}
		pods = append(pods, podFromInformer)
	}

	switch pgCopy.Status.Phase {
	case "":
		pgCopy.Status.Phase = schedv1alpha1.PodGroupPending
	case schedv1alpha1.PodGroupPending:
		if len(pods) >= int(pg.Spec.MinMember) {
			pgCopy.Status.Phase = schedv1alpha1.PodGroupPreScheduling
			fillOccupiedObj(pgCopy, pods[0])
		}
	default:
		var (
			running   int32 = 0
			succeeded int32 = 0
			failed    int32 = 0
		)
		if len(pods) != 0 {
			for _, pod := range pods {
				switch pod.Status.Phase {
				case v1.PodRunning:
					running++
				case v1.PodSucceeded:
					succeeded++
				case v1.PodFailed:
					failed++
				}
			}
		}
		pgCopy.Status.Failed = failed
		pgCopy.Status.Succeeded = succeeded
		pgCopy.Status.Running = running

		if len(pods) == 0 {
			pgCopy.Status.Phase = schedv1alpha1.PodGroupPending
			break
		}

		if pgCopy.Status.Scheduled >= pgCopy.Spec.MinMember && pgCopy.Status.Phase == schedv1alpha1.PodGroupScheduling {
			pgCopy.Status.Phase = schedv1alpha1.PodGroupScheduled
		}

		if pgCopy.Status.Succeeded+pgCopy.Status.Running >= pg.Spec.MinMember && pgCopy.Status.Phase == schedv1alpha1.PodGroupScheduled {
			pgCopy.Status.Phase = schedv1alpha1.PodGroupRunning
		}
		// Final state of pod group
		if pgCopy.Status.Failed != 0 && pgCopy.Status.Failed+pgCopy.Status.Running+pgCopy.Status.Succeeded >= pg.Spec.MinMember {
			pgCopy.Status.Phase = schedv1alpha1.PodGroupFailed
		}
		if pgCopy.Status.Succeeded >= pg.Spec.MinMember {
			pgCopy.Status.Phase = schedv1alpha1.PodGroupFinished
		}
	}

	err = ctrl.patchPodGroup(pg, pgCopy)
	if err == nil {
		ctrl.pgQueue.Forget(pg)
	}
	return err
}

func (ctrl *PodGroupController) patchPodGroup(old, new *schedv1alpha1.PodGroup) error {
	if reflect.DeepEqual(old, new) {
		return nil
	}

	patch, err := util.CreateMergePatch(old, new)
	if err != nil {
		return err
	}

	_, err = ctrl.pgClient.SchedulingV1alpha1().PodGroups(old.Namespace).Patch(context.TODO(),
		old.Name, types.MergePatchType, patch, metav1.PatchOptions{})
	return err
}

func fillOccupiedObj(pg *schedv1alpha1.PodGroup, pod *v1.Pod) {
	var refs []string
	for _, ownerRef := range pod.OwnerReferences {
		refs = append(refs, fmt.Sprintf("%s/%s", pod.Namespace, ownerRef.Name))
	}
	if len(pg.Status.OccupiedBy) == 0 {
		return
	}
	if len(refs) != 0 {
		sort.Strings(refs)
		pg.Status.OccupiedBy = strings.Join(refs, ",")
	}
}
