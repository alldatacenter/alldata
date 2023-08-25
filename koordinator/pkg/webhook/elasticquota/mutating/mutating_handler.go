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
	"context"
	"encoding/json"
	"net/http"
	"reflect"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/runtime/inject"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/pkg/webhook/elasticquota"
)

// ElasticQuotaMutatingHandler handles ElasticQuota
type ElasticQuotaMutatingHandler struct {
	Client client.Client

	// Decoder decodes the objects
	Decoder *admission.Decoder
}

var _ admission.Handler = &ElasticQuotaMutatingHandler{}

func shouldIgnoreIfNotElasticQuotas(req admission.Request) bool {
	// Ignore all calls to sub resources or resources other than pods.
	if len(req.AdmissionRequest.SubResource) != 0 ||
		req.AdmissionRequest.Resource.Resource != "elasticquotas" {
		return true
	}
	return false
}

func (h *ElasticQuotaMutatingHandler) Handle(ctx context.Context, request admission.Request) (resp admission.Response) {
	if shouldIgnoreIfNotElasticQuotas(request) {
		return admission.Allowed("")
	}

	obj := &v1alpha1.ElasticQuota{}
	if err := h.Decoder.Decode(request, obj); err != nil {
		return admission.Errored(http.StatusBadRequest, err)
	}

	var copied runtime.Object = obj.DeepCopy()

	klog.V(5).Infof("Webhook start mutating quota %s", obj.Name)

	plugin := elasticquota.NewPlugin(h.Decoder, h.Client)
	if err := plugin.AdmitQuota(ctx, request, copied); err != nil {
		klog.Errorf("Failed to mutating Quota %s/%s by quotaTopology, err: %v", obj.Namespace, obj.Name, err)
		return admission.Errored(http.StatusBadRequest, err)
	}

	if reflect.DeepEqual(obj, copied) {
		return admission.Allowed("")
	}
	marshaled, err := json.Marshal(copied)
	if err != nil {
		return admission.Errored(http.StatusInternalServerError, err)
	}
	return admission.PatchResponseFromRaw(request.AdmissionRequest.Object.Raw, marshaled)
}

var _ inject.Client = &ElasticQuotaMutatingHandler{}

// InjectClient injects the client into the ElasticQuotaMutatingHandler
func (h *ElasticQuotaMutatingHandler) InjectClient(c client.Client) error {
	h.Client = c
	return nil
}

var _ admission.DecoderInjector = &ElasticQuotaMutatingHandler{}

// InjectDecoder injects the decoder into the ElasticQuotaMutatingHandler
func (h *ElasticQuotaMutatingHandler) InjectDecoder(decoder *admission.Decoder) error {
	h.Decoder = decoder
	return nil
}
