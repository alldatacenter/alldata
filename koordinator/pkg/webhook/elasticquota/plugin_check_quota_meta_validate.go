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

package elasticquota

import (
	"context"
	"fmt"

	v1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
)

type QuotaMetaChecker struct {
	client.Client
	*admission.Decoder
	QuotaTopo *quotaTopology
}

var (
	quotaMetaCheck = &QuotaMetaChecker{
		QuotaTopo: nil,
	}
)

func NewPlugin(decoder *admission.Decoder, client client.Client) *QuotaMetaChecker {
	quotaMetaCheck.Client = client
	quotaMetaCheck.Decoder = decoder
	if quotaMetaCheck.QuotaTopo == nil {
		quotaMetaCheck.QuotaTopo = NewQuotaTopology(client)
	}
	return quotaMetaCheck
}

func (c *QuotaMetaChecker) AdmitQuota(ctx context.Context, req admission.Request, obj runtime.Object) error {
	klog.V(5).Infof("start to admit quota: %+v", obj)
	if req.Operation != v1.Create {
		return nil
	}

	quotaObj := obj.(*v1alpha1.ElasticQuota)
	return c.QuotaTopo.fillQuotaDefaultInformation(quotaObj)
}

func (c *QuotaMetaChecker) ValidateQuota(ctx context.Context, req admission.Request, obj runtime.Object) error {
	quotaObj := obj.(*v1alpha1.ElasticQuota)

	klog.V(5).Infof("start to validate quota :%+v", quotaObj)

	switch req.AdmissionRequest.Operation {
	case v1.Create:
		return c.QuotaTopo.ValidAddQuota(quotaObj)
	case v1.Update:
		oldQuota := &v1alpha1.ElasticQuota{}
		err := c.Decode(admission.Request{
			AdmissionRequest: v1.AdmissionRequest{
				Object: req.AdmissionRequest.OldObject,
			},
		}, oldQuota)
		if err != nil {
			return fmt.Errorf("failed to get quota from old object, err:%+v", err)
		}
		return c.QuotaTopo.ValidUpdateQuota(oldQuota, quotaObj)
	case v1.Delete:
		return c.QuotaTopo.ValidDeleteQuota(quotaObj)
	}
	return nil
}

func (c *QuotaMetaChecker) ValidatePod(ctx context.Context, req admission.Request) error {
	pod := &corev1.Pod{}
	if err := c.Decoder.DecodeRaw(req.Object, pod); err != nil {
		return err
	}
	switch req.AdmissionRequest.Operation {
	case v1.Create:
		return c.QuotaTopo.ValidateAddPod(pod)
	case v1.Update:
		oldPod := &corev1.Pod{}
		err := c.Decode(admission.Request{
			AdmissionRequest: v1.AdmissionRequest{
				Object: req.AdmissionRequest.OldObject,
			},
		}, oldPod)
		if err != nil {
			return fmt.Errorf("failed to decode pod from old object, err :%v", err)
		}
		return c.QuotaTopo.ValidateUpdatePod(oldPod, pod)
	}
	return nil
}

func (c *QuotaMetaChecker) GetQuotaTopologyInfo() *QuotaTopologySummary {
	if c.QuotaTopo == nil {
		return nil
	}
	return quotaMetaCheck.QuotaTopo.getQuotaTopologyInfo()
}
