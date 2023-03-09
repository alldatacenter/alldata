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

package fieldindex

import (
	"context"
	"fmt"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/cache"
	"sigs.k8s.io/controller-runtime/pkg/client"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

const (
	IndexPodByNodeName        = "pod.spec.nodeName"
	IndexPodByOwnerRefUID     = "pod.ownerRefUID"
	IndexJobByPodUID          = "job.pod.uid"
	IndexJobPodNamespacedName = "job.pod.namespacedName"
	IndexJobByPodNamespace    = "job.pod.namespace"
)

var (
	registerOnce sync.Once
)

func RegisterFieldIndexes(c cache.Cache) error {
	var err error
	registerOnce.Do(func() {
		err = c.IndexField(context.Background(), &corev1.Pod{}, IndexPodByNodeName, func(obj client.Object) []string {
			pod, ok := obj.(*corev1.Pod)
			if !ok {
				return []string{}
			}
			if len(pod.Spec.NodeName) == 0 {
				return []string{}
			}
			return []string{pod.Spec.NodeName}
		})
		if err != nil {
			return
		}

		err = c.IndexField(context.TODO(), &corev1.Pod{}, IndexPodByOwnerRefUID, func(obj client.Object) []string {
			var owners []string
			for _, ref := range obj.GetOwnerReferences() {
				owners = append(owners, string(ref.UID))
			}
			return owners
		})
		if err != nil {
			return
		}

		err = c.IndexField(context.Background(), &sev1alpha1.PodMigrationJob{}, IndexJobByPodUID, func(obj client.Object) []string {
			migrationJob, ok := obj.(*sev1alpha1.PodMigrationJob)
			if !ok {
				return []string{}
			}
			if migrationJob.Spec.PodRef == nil {
				return []string{}
			}
			return []string{string(migrationJob.Spec.PodRef.UID)}
		})
		if err != nil {
			return
		}
		err = c.IndexField(context.Background(), &sev1alpha1.PodMigrationJob{}, IndexJobPodNamespacedName, func(obj client.Object) []string {
			migrationJob, ok := obj.(*sev1alpha1.PodMigrationJob)
			if !ok {
				return []string{}
			}
			if migrationJob.Spec.PodRef == nil {
				return []string{}
			}
			return []string{fmt.Sprintf("%s/%s", migrationJob.Spec.PodRef.Namespace, migrationJob.Spec.PodRef.Name)}
		})
		if err != nil {
			return
		}
		err = c.IndexField(context.Background(), &sev1alpha1.PodMigrationJob{}, IndexJobByPodNamespace, func(obj client.Object) []string {
			migrationJob, ok := obj.(*sev1alpha1.PodMigrationJob)
			if !ok {
				return []string{}
			}
			if migrationJob.Spec.PodRef == nil {
				return []string{}
			}
			return []string{migrationJob.Spec.PodRef.Namespace}
		})
		if err != nil {
			return
		}
	})
	return err
}
