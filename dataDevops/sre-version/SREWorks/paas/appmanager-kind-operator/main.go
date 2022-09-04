/*


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

package main

import (
	"appmanager-operator/helper"
	"flag"
	"net/http"
	_ "net/http/pprof"
	"os"

	"k8s.io/apimachinery/pkg/runtime"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	_ "k8s.io/client-go/plugin/pkg/client/auth/gcp"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"

	appsv1 "appmanager-operator/api/v1"
	"appmanager-operator/controllers"
	kruiseapi "github.com/openkruise/kruise-api"
	// +kubebuilder:scaffold:imports
)

var (
	scheme   = runtime.NewScheme()
	setupLog = ctrl.Log.WithName("setup")
)

func init() {
	_ = clientgoscheme.AddToScheme(scheme)

	_ = appsv1.AddToScheme(scheme)
	_ = kruiseapi.AddToScheme(scheme)
	// +kubebuilder:scaffold:scheme
}

func main() {
	var namespace string
	var metricsAddr string
	var enableLeaderElection bool
	var leaderElection bool
	var globalListener bool
	flag.StringVar(&namespace, "namespace", "apsara-bigdata-manager", "The namespace binds to.")
	flag.StringVar(&metricsAddr, "metrics-addr", ":8080", "The address the metric endpoint binds to.")
	flag.BoolVar(&leaderElection, "leader-elect", true,
		"Enable leader election for controller manager. "+
			"Enabling this will ensure there is only one active controller manager.")
	flag.BoolVar(&enableLeaderElection, "enable-leader-election", true,
		"Enable leader election for controller manager. "+
			"Enabling this will ensure there is only one active controller manager.")
	flag.BoolVar(&globalListener, "global-listener", false, "Change to global namespaces listener.")
	flag.Parse()

	if leaderElection {
		enableLeaderElection = true
	}

	ctrl.SetLogger(zap.New(zap.UseDevMode(true)))
	config := ctrl.GetConfigOrDie()
	config.UserAgent = "abm-appmanager-operator"
	var options ctrl.Options
	if globalListener {
		options = ctrl.Options{
			Scheme:             scheme,
			MetricsBindAddress: metricsAddr,
			Port:               9443,
			LeaderElection:     enableLeaderElection,
			LeaderElectionID:   namespace + ".leader.abm.io",
		}
	} else {
		options = ctrl.Options{
			Scheme:             scheme,
			MetricsBindAddress: metricsAddr,
			Port:               9443,
			LeaderElection:     enableLeaderElection,
			LeaderElectionID:   namespace + ".leader.abm.io",
			Namespace:          namespace,
		}
	}
	mgr, err := ctrl.NewManager(config, options)
	if err != nil {
		setupLog.Error(err, "unable to start manager")
		os.Exit(1)
	}

	if err = (&controllers.MicroserviceReconciler{
		ReconcilerBase: helper.NewReconcilerBase(mgr.GetClient(), mgr.GetScheme(), mgr.GetConfig(), mgr.GetEventRecorderFor("AppManagerController"), mgr.GetAPIReader()),
		Client:         mgr.GetClient(),
		Log:            ctrl.Log.WithName("controllers").WithName("Microservice"),
		Scheme:         mgr.GetScheme(),
	}).SetupWithManager(mgr); err != nil {
		setupLog.Error(err, "unable to create controller", "controller", "Microservice")
		os.Exit(1)
	}
	if err = (&controllers.JobReconciler{
		Client: mgr.GetClient(),
		Log:    ctrl.Log.WithName("controllers").WithName("Job"),
		Scheme: mgr.GetScheme(),
	}).SetupWithManager(mgr); err != nil {
		setupLog.Error(err, "unable to create controller", "controller", "Job")
		os.Exit(1)
	}

	go func() {
		setupLog.Info("Starting init pprof.")
		err := http.ListenAndServe("0.0.0.0:19028", nil)
		if err != nil {
			setupLog.Error(err, "unable init pprof")
			return
		}
	}()

	setupLog.Info("starting manager")
	if err := mgr.Start(ctrl.SetupSignalHandler()); err != nil {
		setupLog.Error(err, "problem running manager")
		os.Exit(1)
	}
}
