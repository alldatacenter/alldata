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

package evictor

import (
	"context"
	"encoding/json"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"

	"github.com/koordinator-sh/koordinator/apis/extension"
	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func init() {
	RegisterEvictor(SoftEvictorName, NewSoftEvictor)
}

const (
	SoftEvictorName = "SoftEviction"
)

type SoftEvictor struct {
	client kubernetes.Interface
}

func NewSoftEvictor(client kubernetes.Interface) (Interface, error) {
	return &SoftEvictor{
		client: client,
	}, nil
}

func (e *SoftEvictor) Evict(ctx context.Context, job *sev1alpha1.PodMigrationJob, pod *corev1.Pod) error {
	trigger, reason := GetEvictionTriggerAndReason(job.Annotations)
	evictionSpec := extension.SoftEvictionSpec{
		Timestamp:     &metav1.Time{Time: time.Now()},
		DeleteOptions: job.Spec.DeleteOptions,
		Initiator:     trigger,
		Reason:        reason,
	}
	evictionSpecData, err := json.Marshal(evictionSpec)
	if err != nil {
		return err
	}
	newPod := pod.DeepCopy()
	if newPod.Annotations == nil {
		newPod.Annotations = make(map[string]string)
	}
	newPod.Annotations[extension.AnnotationSoftEviction] = string(evictionSpecData)

	return util.RetryOnConflictOrTooManyRequests(func() error {
		_, err1 := util.NewPatch().WithClientset(e.client).AddAnnotations(newPod.Annotations).PatchPod(ctx, pod)
		return err1
	})
}
