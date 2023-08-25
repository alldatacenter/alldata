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
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/klog/v2"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

var _ Interpreter = &interpreterImpl{}

type interpreterImpl struct {
	mgr ctrl.Manager
	client.Client
}

func newInterpreter(mgr ctrl.Manager) Interpreter {
	return &interpreterImpl{
		mgr:    mgr,
		Client: mgr.GetClient(),
	}
}

func (p *interpreterImpl) GetReservationType() client.Object {
	return &sev1alpha1.Reservation{}
}

func (p *interpreterImpl) Preemption() Preemption {
	return nil
}

func (p *interpreterImpl) GetReservation(ctx context.Context, ref *corev1.ObjectReference) (Object, error) {
	reservation := &sev1alpha1.Reservation{}
	namespacedName := types.NamespacedName{Name: ref.Name}
	err := p.Client.Get(ctx, namespacedName, reservation)
	if errors.IsNotFound(err) {
		klog.Warningf("Failed to get Reservation %v, reason: %v", ref.Name, err)
		err = p.mgr.GetAPIReader().Get(ctx, namespacedName, reservation)
	}
	return &Reservation{Reservation: reservation}, err
}

func (p *interpreterImpl) DeleteReservation(ctx context.Context, ref *corev1.ObjectReference) error {
	if ref == nil {
		return nil
	}

	id := GetReservationNamespacedName(ref)
	klog.V(4).Infof("begin to delete Reservation: %v", id)
	reservation, err := p.GetReservation(ctx, ref)
	if err == nil {
		err = p.Client.Delete(ctx, reservation.OriginObject())
		if err != nil {
			klog.Errorf("Failed to delete Reservation %v, err: %v", id, err)
		} else {
			klog.V(4).Infof("Successfully delete Reservation %v", id)
		}
	}
	return err
}

func (p *interpreterImpl) CreateReservation(ctx context.Context, job *sev1alpha1.PodMigrationJob) (Object, error) {
	reservationOptions := job.Spec.ReservationOptions
	if reservationOptions == nil {
		return nil, fmt.Errorf("invalid reservationOptions")
	}

	reservation := &sev1alpha1.Reservation{
		ObjectMeta: reservationOptions.Template.ObjectMeta,
		Spec:       reservationOptions.Template.Spec,
	}

	err := p.Client.Create(ctx, reservation)
	if errors.IsAlreadyExists(err) {
		err = p.Client.Get(ctx, types.NamespacedName{Name: reservation.Name}, reservation)
	}
	if err != nil {
		return nil, err
	}
	return &Reservation{Reservation: reservation}, nil
}
