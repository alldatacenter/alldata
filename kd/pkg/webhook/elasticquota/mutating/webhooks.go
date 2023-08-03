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

package mutating

import (
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"
)

// +kubebuilder:webhook:path=/mutate-scheduling-sigs-k8s-io-v1alpha1-elasticquota,mutating=true,failurePolicy=fail,sideEffects=None,admissionReviewVersions=v1;v1beta1,groups=scheduling.sigs.k8s.io,resources=elasticquotas,verbs=create,versions=v1alpha1,name=melasticquota.kb.io

var (
	// HandlerMap contains admission webhook handlers
	HandlerMap = map[string]admission.Handler{
		"mutate-scheduling-sigs-k8s-io-v1alpha1-elasticquota": &ElasticQuotaMutatingHandler{},
	}
)
