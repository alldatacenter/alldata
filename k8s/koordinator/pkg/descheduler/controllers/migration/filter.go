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

package migration

import (
	"context"
	"fmt"

	gocache "github.com/patrickmn/go-cache"
	"golang.org/x/time/rate"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/fields"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/klog/v2"
	podutil "k8s.io/kubernetes/pkg/api/v1/pod"
	kubecontroller "k8s.io/kubernetes/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/client"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/util"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/fieldindex"
	utilclient "github.com/koordinator-sh/koordinator/pkg/util/client"
)

func (r *Reconciler) forEachAvailableMigrationJobs(listOpts *client.ListOptions, handler func(job *sev1alpha1.PodMigrationJob) bool) {
	jobList := &sev1alpha1.PodMigrationJobList{}
	err := r.Client.List(context.TODO(), jobList, listOpts, utilclient.DisableDeepCopy)
	if err != nil {
		klog.Errorf("failed to get PodMigrationJobList, err: %v", err)
		return
	}

	for i := range jobList.Items {
		job := &jobList.Items[i]
		phase := job.Status.Phase
		if phase == "" || phase == sev1alpha1.PodMigrationJobPending || phase == sev1alpha1.PodMigrationJobRunning {
			if !handler(job) {
				break
			}
		}
	}
	return
}

func (r *Reconciler) filterExistingPodMigrationJob(pod *corev1.Pod) bool {
	return !r.existingPodMigrationJob(pod)
}

func (r *Reconciler) existingPodMigrationJob(pod *corev1.Pod) bool {
	opts := &client.ListOptions{FieldSelector: fields.OneTermEqualSelector(fieldindex.IndexJobByPodUID, string(pod.UID))}
	existing := false
	r.forEachAvailableMigrationJobs(opts, func(job *sev1alpha1.PodMigrationJob) bool {
		if podRef := job.Spec.PodRef; podRef != nil && podRef.UID == pod.UID {
			existing = true
		}
		return !existing
	})
	if !existing {
		opts = &client.ListOptions{FieldSelector: fields.OneTermEqualSelector(fieldindex.IndexJobPodNamespacedName, fmt.Sprintf("%s/%s", pod.Namespace, pod.Name))}
		r.forEachAvailableMigrationJobs(opts, func(job *sev1alpha1.PodMigrationJob) bool {
			if podRef := job.Spec.PodRef; podRef != nil && podRef.Namespace == pod.Namespace && podRef.Name == pod.Name {
				existing = true
			}
			return !existing
		})
	}
	return existing
}

func (r *Reconciler) filterMaxMigratingPerNode(pod *corev1.Pod) bool {
	if pod.Spec.NodeName == "" || r.args.MaxMigratingPerNode == nil || *r.args.MaxMigratingPerNode <= 0 {
		return true
	}

	podList := &corev1.PodList{}
	listOpts := &client.ListOptions{FieldSelector: fields.OneTermEqualSelector(fieldindex.IndexPodByNodeName, pod.Spec.NodeName)}
	err := r.Client.List(context.TODO(), podList, listOpts, utilclient.DisableDeepCopy)
	if err != nil {
		return true
	}
	if len(podList.Items) == 0 {
		return true
	}

	count := 0
	for i := range podList.Items {
		v := &podList.Items[i]
		if v.UID != pod.UID && v.Spec.NodeName == pod.Spec.NodeName {
			if r.existingPodMigrationJob(v) {
				count++
			}
		}
	}

	maxMigratingPerNode := int(*r.args.MaxMigratingPerNode)
	exceeded := count >= maxMigratingPerNode
	if exceeded {
		klog.V(4).Infof("Pod %q fails to check maxMigratingPerNode because the Node %q has %d migrating Pods, exceeding the maxMigratingPerNode(%d)",
			klog.KObj(pod), pod.Spec.NodeName, count, maxMigratingPerNode)
	}
	return !exceeded
}

func (r *Reconciler) filterMaxMigratingPerNamespace(pod *corev1.Pod) bool {
	if r.args.MaxMigratingPerNamespace == nil || *r.args.MaxMigratingPerNamespace <= 0 {
		return true
	}

	opts := &client.ListOptions{FieldSelector: fields.OneTermEqualSelector(fieldindex.IndexJobByPodNamespace, pod.Namespace)}
	count := 0
	r.forEachAvailableMigrationJobs(opts, func(job *sev1alpha1.PodMigrationJob) bool {
		if podRef := job.Spec.PodRef; podRef != nil && podRef.UID != pod.UID && podRef.Namespace == pod.Namespace {
			count++
		}
		return true
	})

	maxMigratingPerNamespace := int(*r.args.MaxMigratingPerNamespace)
	exceeded := count >= maxMigratingPerNamespace
	if exceeded {
		klog.V(4).Infof("Pod %q fails to check maxMigratingPerNamespace because the Namespace %q has %d migrating Pods, exceeding the maxMigratingPerNamespace(%d)",
			klog.KObj(pod), pod.Namespace, count, maxMigratingPerNamespace)
	}
	return !exceeded
}

func (r *Reconciler) filterMaxMigratingOrUnavailablePerWorkload(pod *corev1.Pod) bool {
	ownerRef := metav1.GetControllerOf(pod)
	if ownerRef == nil {
		return true
	}
	pods, expectedReplicas, err := r.controllerFinder.GetPodsForRef(ownerRef.APIVersion, ownerRef.Kind, ownerRef.Name, pod.Namespace, nil, false)
	if err != nil {
		return false
	}

	maxMigrating, err := util.GetMaxMigrating(int(expectedReplicas), r.args.MaxMigratingPerWorkload)
	if err != nil {
		return false
	}
	maxUnavailable, err := util.GetMaxUnavailable(int(expectedReplicas), r.args.MaxUnavailablePerWorkload)
	if err != nil {
		return false
	}

	// TODO(joseph): There are a few special scenarios where should we allow eviction?
	if expectedReplicas == 1 || int(expectedReplicas) == maxMigrating || int(expectedReplicas) == maxUnavailable {
		klog.Warningf("maxMigrating(%d) or maxUnavailable(%d) equals to the replicas(%d) of the workload %s/%s/%s(%s) of Pod %q, or the replicas equals to 1, please increase the replicas or update the defense configurations",
			maxMigrating, maxUnavailable, expectedReplicas, ownerRef.Name, ownerRef.Kind, ownerRef.APIVersion, ownerRef.UID, klog.KObj(pod))
		return false
	}

	opts := &client.ListOptions{FieldSelector: fields.OneTermEqualSelector(fieldindex.IndexJobByPodNamespace, pod.Namespace)}
	migratingPods := map[types.NamespacedName]struct{}{}
	r.forEachAvailableMigrationJobs(opts, func(job *sev1alpha1.PodMigrationJob) bool {
		podRef := job.Spec.PodRef
		if podRef == nil || podRef.UID == pod.UID {
			return true
		}

		podNamespacedName := types.NamespacedName{
			Namespace: podRef.Namespace,
			Name:      podRef.Name,
		}
		p := &corev1.Pod{}
		err := r.Client.Get(context.TODO(), podNamespacedName, p)
		if err != nil {
			klog.Errorf("Failed to get Pod %q, err: %v", podNamespacedName, err)
		} else {
			innerPodOwnerRef := metav1.GetControllerOf(p)
			if innerPodOwnerRef != nil && innerPodOwnerRef.UID == ownerRef.UID {
				migratingPods[podNamespacedName] = struct{}{}
			}
		}
		return true
	})

	if len(migratingPods) > 0 {
		exceeded := len(migratingPods) >= maxMigrating
		if exceeded {
			klog.V(4).Infof("The workload %s/%s/%s(%s) of Pod %q has %d migration jobs that exceed MaxMigratingPerWorkload %d",
				ownerRef.Name, ownerRef.Kind, ownerRef.APIVersion, ownerRef.UID, klog.KObj(pod), len(migratingPods), maxMigrating)
			return false
		}
	}

	unavailablePods := r.getUnavailablePods(pods)
	mergeUnavailableAndMigratingPods(unavailablePods, migratingPods)
	exceeded := len(unavailablePods) >= maxUnavailable
	if exceeded {
		klog.V(4).Infof("The workload %s/%s/%s(%s) of Pod %q has %d unavailable Pods that exceed MaxUnavailablePerWorkload %d",
			ownerRef.Name, ownerRef.Kind, ownerRef.APIVersion, ownerRef.UID, klog.KObj(pod), len(unavailablePods), maxUnavailable)
		return false
	}
	return true
}

func (r *Reconciler) getUnavailablePods(pods []*corev1.Pod) map[types.NamespacedName]struct{} {
	unavailablePods := make(map[types.NamespacedName]struct{})
	for _, pod := range pods {
		if kubecontroller.IsPodActive(pod) && podutil.IsPodReady(pod) {
			continue
		}
		k := types.NamespacedName{
			Namespace: pod.Namespace,
			Name:      pod.Name,
		}
		unavailablePods[k] = struct{}{}
	}
	return unavailablePods
}

func mergeUnavailableAndMigratingPods(unavailablePods, migratingPods map[types.NamespacedName]struct{}) {
	for k, v := range migratingPods {
		unavailablePods[k] = v
	}
}

func (r *Reconciler) trackEvictedPod(pod *corev1.Pod) {
	if r.objectLimiters == nil || r.limiterCache == nil {
		return
	}
	ownerRef := metav1.GetControllerOf(pod)
	if ownerRef == nil {
		return
	}

	objectLimiterArgs, ok := r.args.ObjectLimiters[deschedulerconfig.MigrationLimitObjectWorkload]
	if !ok || objectLimiterArgs.Duration.Seconds() == 0 {
		return
	}

	var maxMigratingReplicas int
	if expectedReplicas, err := r.controllerFinder.GetExpectedScaleForPods([]*corev1.Pod{pod}); err == nil {
		maxMigrating := objectLimiterArgs.MaxMigrating
		if maxMigrating == nil {
			maxMigrating = r.args.MaxMigratingPerWorkload
		}
		maxMigratingReplicas, _ = util.GetMaxMigrating(int(expectedReplicas), maxMigrating)
	}
	if maxMigratingReplicas == 0 {
		return
	}

	r.lock.Lock()
	defer r.lock.Unlock()

	uid := ownerRef.UID
	limit := rate.Limit(maxMigratingReplicas) / rate.Limit(objectLimiterArgs.Duration.Seconds())
	limiter := r.objectLimiters[uid]
	if limiter == nil {
		limiter = rate.NewLimiter(limit, 1)
		r.objectLimiters[uid] = limiter
	} else if limiter.Limit() != limit {
		limiter.SetLimit(limit)
	}

	if !limiter.AllowN(r.clock.Now(), 1) {
		klog.Infof("The workload %s/%s/%s has been frequently descheduled recently and needs to be limited for a period of time", ownerRef.Name, ownerRef.Kind, ownerRef.APIVersion)
	}
	r.limiterCache.Set(string(uid), 0, gocache.DefaultExpiration)
}

func (r *Reconciler) filterLimitedObject(pod *corev1.Pod) bool {
	if r.objectLimiters == nil || r.limiterCache == nil {
		return true
	}
	objectLimiterArgs, ok := r.args.ObjectLimiters[deschedulerconfig.MigrationLimitObjectWorkload]
	if !ok || objectLimiterArgs.Duration.Duration == 0 {
		return true
	}
	if ownerRef := metav1.GetControllerOf(pod); ownerRef != nil {
		r.lock.Lock()
		defer r.lock.Unlock()
		if limiter := r.objectLimiters[ownerRef.UID]; limiter != nil {
			if remainTokens := limiter.Tokens() - float64(1); remainTokens < 0 {
				klog.Infof("Pod %q is filtered by workload %s/%s/%s is limited", klog.KObj(pod), ownerRef.Name, ownerRef.Kind, ownerRef.APIVersion)
				return false
			}
		}
	}
	return true
}
