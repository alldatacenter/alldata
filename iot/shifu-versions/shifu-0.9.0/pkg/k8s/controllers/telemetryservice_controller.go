/*
Copyright 2021.

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

package controllers

import (
	"context"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log"
)

// TelemetryServiceReconciler reconciles a TelemetryService object
type TelemetryServiceReconciler struct {
	client.Client
	Scheme *runtime.Scheme
}

//+kubebuilder:rbac:groups=shifu.edgenesis.io,resources=telemetryservices,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=shifu.edgenesis.io,resources=telemetryservices/status,verbs=get;update;patch
//+kubebuilder:rbac:groups=shifu.edgenesis.io,resources=telemetryservices/finalizers,verbs=update

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
// TODO(user): Modify the Reconcile function to compare the state specified by
// the TelemetryService object against the actual cluster state, and then
// perform operations to make the cluster state reflect the state specified by
// the user.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.12.1/pkg/reconcile
func (r *TelemetryServiceReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	rlog := log.FromContext(ctx)

	ts := &v1alpha1.TelemetryService{}
	if err := r.Get(ctx, req.NamespacedName, ts); err != nil {
		rlog.Error(err, "Unable to fetch TelemetryService")
		// we'll ignore not-found errors, since they can't be fixed by an immediate
		// requeue (we'll need to wait for a new notification), and we can get them
		// on deleted requests.
		return ctrl.Result{}, client.IgnoreNotFound(err)
	}

	// TODO: reconcile TelemetryService services
	rlog.Info("Hello World! This is telemetryservice_controller reconciling")

	return ctrl.Result{}, nil
}

// SetupWithManager sets up the controller with the Manager.
func (r *TelemetryServiceReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&v1alpha1.TelemetryService{}).
		Complete(r)
}
