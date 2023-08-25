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

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/evictor"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

// Evict evicts a pod
func (r *Reconciler) Evict(ctx context.Context, pod *corev1.Pod, evictOptions framework.EvictOptions) bool {
	framework.FillEvictOptionsFromContext(ctx, &evictOptions)

	if r.args.DryRun {
		klog.Infof("%s tries to evict Pod %q via dryRun mode since %s", evictOptions.PluginName, klog.KObj(pod), evictOptions.Reason)
		return true
	}

	if !r.Filter(pod) {
		klog.Errorf("Pod %q cannot be evicted since failed to filter", klog.KObj(pod))
		return false
	}

	err := CreatePodMigrationJob(ctx, pod, evictOptions, r.Client, r.args)
	return err == nil
}

func CreatePodMigrationJob(ctx context.Context, pod *corev1.Pod, evictOptions framework.EvictOptions, client client.Client, args *deschedulerconfig.MigrationControllerArgs) error {
	if evictOptions.DeleteOptions == nil {
		evictOptions.DeleteOptions = args.DefaultDeleteOptions
	}
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name: string(UUIDGenerateFn()),
			Annotations: map[string]string{
				evictor.AnnotationEvictReason:  evictOptions.Reason,
				evictor.AnnotationEvictTrigger: evictOptions.PluginName,
			},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: pod.Namespace,
				Name:      pod.Name,
				UID:       pod.UID,
			},
			Mode:          sev1alpha1.PodMigrationJobMode(args.DefaultJobMode),
			TTL:           args.DefaultJobTTL.DeepCopy(),
			DeleteOptions: evictOptions.DeleteOptions,
		},
		Status: sev1alpha1.PodMigrationJobStatus{
			Phase: sev1alpha1.PodMigrationJobPending,
		},
	}

	jobCtx := FromContext(ctx)
	if err := jobCtx.ApplyTo(job); err != nil {
		klog.Errorf("Failed to apply JobContext to PodMigrationJob for Pod %s/%s, err: %v", pod.Namespace, pod.Name, err)
		return err
	}

	err := client.Create(ctx, job)
	if err != nil {
		klog.Errorf("Failed to create PodMigrationJob for Pod %s/s, err: %v", pod.Namespace, pod.Name, err)
		return err
	}
	return nil
}
