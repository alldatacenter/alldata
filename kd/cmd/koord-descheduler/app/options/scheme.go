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

package options

import (
	appsv1alpha1 "github.com/openkruise/kruise-api/apps/v1alpha1"
	appsv1beta1 "github.com/openkruise/kruise-api/apps/v1beta1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

var scheme = runtime.NewScheme()

func init() {
	_ = clientgoscheme.AddToScheme(scheme)
	_ = sev1alpha1.AddToScheme(clientgoscheme.Scheme)
	_ = appsv1alpha1.AddToScheme(clientgoscheme.Scheme)
	_ = appsv1beta1.AddToScheme(clientgoscheme.Scheme)

	_ = sev1alpha1.AddToScheme(scheme)
	_ = appsv1alpha1.AddToScheme(scheme)
	_ = appsv1beta1.AddToScheme(scheme)

	scheme.AddUnversionedTypes(metav1.SchemeGroupVersion, &metav1.UpdateOptions{}, &metav1.DeleteOptions{}, &metav1.CreateOptions{})
	// +kubebuilder:scaffold:scheme
}
