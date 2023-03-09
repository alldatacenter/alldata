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

package reservation

import (
	"strconv"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	podutil "k8s.io/kubernetes/pkg/api/v1/pod"

	"github.com/koordinator-sh/koordinator/apis/extension"
	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func GetReservationNamespacedName(ref *corev1.ObjectReference) types.NamespacedName {
	return types.NamespacedName{
		Namespace: ref.Namespace,
		Name:      ref.Name,
	}
}

func CreateOrUpdateReservationOptions(job *sev1alpha1.PodMigrationJob, pod *corev1.Pod) *sev1alpha1.PodMigrateReservationOptions {
	reservationOptions := job.Spec.ReservationOptions
	if reservationOptions == nil {
		reservationOptions = &sev1alpha1.PodMigrateReservationOptions{}
	} else {
		reservationOptions = reservationOptions.DeepCopy()
	}
	if reservationOptions.Template == nil {
		reservationOptions.Template = &sev1alpha1.ReservationTemplateSpec{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: pod.Namespace,
				Name:      string(job.UID),
			},
			Spec: sev1alpha1.ReservationSpec{
				Template: &corev1.PodTemplateSpec{
					ObjectMeta: pod.ObjectMeta,
					Spec:       pod.Spec,
				},
				Owners: GenerateReserveResourceOwners(pod),
			},
		}
	} else {
		if reservationOptions.Template.ObjectMeta.Name == "" {
			reservationOptions.Template.ObjectMeta.Name = string(job.UID)
		}

		if reservationOptions.Template.Spec.Template == nil {
			reservationOptions.Template.Spec.Template = &corev1.PodTemplateSpec{
				ObjectMeta: pod.ObjectMeta,
				Spec:       pod.Spec,
			}
		}
		if len(reservationOptions.Template.Spec.Owners) == 0 {
			reservationOptions.Template.Spec.Owners = GenerateReserveResourceOwners(pod)
		}
	}

	// Reservation used for migration is no longer reused after consumed
	reservationOptions.Template.Spec.AllocateOnce = true
	// force removed the assigned nodeName of target Pod to request new Node
	reservationOptions.Template.Spec.Template.Spec.NodeName = ""

	if reservationOptions.Template.ObjectMeta.Labels == nil {
		reservationOptions.Template.ObjectMeta.Labels = map[string]string{}
	}
	reservationOptions.Template.ObjectMeta.Labels[LabelCreatedBy] = DefaultCreator
	reservationOptions.Template.ObjectMeta.Labels[extension.LabelReservationOrder] = strconv.FormatInt(time.Now().UnixMilli(), 10)

	if (reservationOptions.Template.Spec.TTL == nil && reservationOptions.Template.Spec.Expires == nil) &&
		job.Spec.TTL != nil && job.Spec.TTL.Duration > 0 {
		reservationOptions.Template.Spec.TTL = job.Spec.TTL
	}

	return reservationOptions
}

func GenerateReserveResourceOwners(pod *corev1.Pod) []sev1alpha1.ReservationOwner {
	if pod.Status.Phase == corev1.PodPending {
		_, condition := podutil.GetPodCondition(&pod.Status, corev1.PodScheduled)
		if condition != nil && condition.Status == corev1.ConditionFalse {
			return []sev1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						UID:       pod.UID,
						Kind:      "Pod",
						Name:      pod.Name,
						Namespace: pod.Namespace,
					},
				},
			}
		}
	}
	for _, v := range pod.OwnerReferences {
		if v.Controller != nil && *v.Controller {
			return []sev1alpha1.ReservationOwner{
				{
					Controller: &sev1alpha1.ReservationControllerReference{
						OwnerReference: v,
						Namespace:      pod.Namespace,
					},
				},
			}
		}
	}
	return nil
}
