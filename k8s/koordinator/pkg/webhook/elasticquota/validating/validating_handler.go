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

package validating

import (
	"context"
	"encoding/json"
	"net/http"

	v1 "k8s.io/api/admission/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	clientcache "k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/cache"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/runtime/inject"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/pkg/util"
	"github.com/koordinator-sh/koordinator/pkg/webhook/elasticquota"
)

// +kubebuilder:rbac:groups=scheduling.sigs.k8s.io,resources=elasticquotas,verbs=get;list;watch

type ElasticQuotaValidatingHandler struct {
	Client client.Client

	// Decoder decodes objects
	Decoder *admission.Decoder
}

var _ admission.Handler = &ElasticQuotaValidatingHandler{}

func shouldIgnoreIfNotElasticQuotas(req admission.Request) bool {
	// Ignore all calls to sub resources or resources other than pods.
	if len(req.AdmissionRequest.SubResource) != 0 ||
		req.AdmissionRequest.Resource.Resource != "elasticquotas" {
		return true
	}
	return false
}

func (h *ElasticQuotaValidatingHandler) Handle(ctx context.Context, request admission.Request) (resp admission.Response) {
	if shouldIgnoreIfNotElasticQuotas(request) {
		return admission.Allowed("")
	}

	obj := &v1alpha1.ElasticQuota{}

	var err error
	if request.Operation != v1.Delete {
		if err = h.Decoder.Decode(request, obj); err != nil {
			return admission.Errored(http.StatusBadRequest, err)
		}
	} else {
		if len(request.OldObject.Raw) != 0 {
			if err = h.Decoder.DecodeRaw(request.OldObject, obj); err != nil {
				return admission.Errored(http.StatusBadRequest, err)
			}
		}
	}

	defer func() {
		if !resp.Allowed {
			klog.Warningf("Webhook finish validating quota %s, allowed: %v, result: %v",
				obj.Name, resp.Allowed, util.DumpJSON(resp.Result))
		}
	}()

	plugin := elasticquota.NewPlugin(h.Decoder, h.Client)
	if err = plugin.ValidateQuota(ctx, request, obj); err != nil {
		return admission.Errored(http.StatusBadRequest, err)
	}

	return admission.ValidationResponse(true, "")
}

var _ inject.Client = &ElasticQuotaValidatingHandler{}

// InjectClient injects the client into the ElasticQuotaValidatingHandler
func (h *ElasticQuotaValidatingHandler) InjectClient(c client.Client) error {
	h.Client = c
	return nil
}

var _ admission.DecoderInjector = &ElasticQuotaValidatingHandler{}

// InjectDecoder injects the client into the ElasticQuotaValidatingHandler
func (h *ElasticQuotaValidatingHandler) InjectDecoder(d *admission.Decoder) error {
	h.Decoder = d
	return nil
}

var _ inject.Cache = &ElasticQuotaValidatingHandler{}

func (h *ElasticQuotaValidatingHandler) InjectCache(cache cache.Cache) error {
	ctx := context.TODO()
	quotaInformer, err := cache.GetInformer(ctx, &v1alpha1.ElasticQuota{
		TypeMeta: metav1.TypeMeta{
			Kind:       "ElasticQuota",
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
		},
	})
	if err != nil {
		return err
	}
	plugin := elasticquota.NewPlugin(h.Decoder, h.Client)
	qt := plugin.QuotaTopo
	quotaInformer.AddEventHandler(clientcache.ResourceEventHandlerFuncs{
		AddFunc:    qt.OnQuotaAdd,
		UpdateFunc: qt.OnQuotaUpdate,
		DeleteFunc: qt.OnQuotaDelete,
	})
	return nil
}

var _ http.Handler = &ElasticQuotaValidatingHandler{}

func (h *ElasticQuotaValidatingHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	plugin := elasticquota.NewPlugin(h.Decoder, h.Client)
	allQuotaTopologySummary := plugin.GetQuotaTopologyInfo()
	allQuotaTopologySummaryJson, _ := json.Marshal(allQuotaTopologySummary)

	w.WriteHeader(200)
	w.Write(allQuotaTopologySummaryJson)
}
