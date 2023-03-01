/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import (
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"time"

	admissionv1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	"k8s.io/klog/v2"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

var (
	// RuntimeScheme defines methods for serializing and deserializing API objects
	runtimeScheme = runtime.NewScheme()
	// Codecs  serializers for specific versions and content types
	codecs = serializer.NewCodecFactory(runtimeScheme)
	// Deserializer attempts to load an object from data
	deserializer = codecs.UniversalDeserializer()
	httpClient   = http.Client{
		Timeout: time.Second * 15,
	}
	jsonContentType = "application/json"
)

// AdmissionReviewHandler handles AdmissionReviews and set response in them.
type AdmissionReviewHandler func(ar *admissionv1.AdmissionReview) *admissionv1.AdmissionReview

// handleResponse write message to http response.
func handleResponse(w http.ResponseWriter, status int, message string) {
	w.WriteHeader(status)
	if _, err := w.Write([]byte(message)); err != nil {
		klog.Errorf("write message (%v) failed: %v", message, err)
	}
}

// AdmissionReviewFailed returns error for the AdmissionReview.
func AdmissionReviewFailed(ar *admissionv1.AdmissionReview,
	err error) *admissionv1.AdmissionReview {
	ar.Response = &admissionv1.AdmissionResponse{
		UID: ar.Request.UID,
		Result: &metav1.Status{
			Message: fmt.Sprintf("handle admission review failed: %v", err),
		},
	}
	return ar
}

// AdmissionReviewAllow allows the AdmissionReview.
func AdmissionReviewAllow(ar *admissionv1.AdmissionReview) *admissionv1.AdmissionReview {
	ar.Response = &admissionv1.AdmissionResponse{
		UID:     ar.Request.UID,
		Allowed: true,
	}
	return ar
}

// AdmissionReviewForbidden forbids the AdmissionReview with delete operation.
func AdmissionReviewForbidden(ar *admissionv1.AdmissionReview,
	message string) *admissionv1.AdmissionReview {
	ar.Response = &admissionv1.AdmissionResponse{
		UID: ar.Request.UID,
		Result: &metav1.Status{
			Message: message,
		},
	}
	return ar
}

// AdmissionReviewWithPatches returns the AdmissionReview with patches in response.
func AdmissionReviewWithPatches(ar *admissionv1.AdmissionReview,
	patches []byte) *admissionv1.AdmissionReview {
	ar.Response = &admissionv1.AdmissionResponse{
		UID:     ar.Request.UID,
		Allowed: true,
		Patch:   patches,
		PatchType: func() *admissionv1.PatchType {
			pt := admissionv1.PatchTypeJSONPatch
			return &pt
		}(),
	}
	return ar
}

// WithAdmissionReviewHandler checks before InspectorFunc executes and creates a handleFunc.
func WithAdmissionReviewHandler(handler AdmissionReviewHandler) http.HandlerFunc {
	return func(w http.ResponseWriter, req *http.Request) {
		if req.Body == nil {
			klog.Error("Receive an invalid ar, body is empty")
			handleResponse(w, http.StatusBadRequest, "ar body required")
			return
		}

		data, err := ioutil.ReadAll(req.Body)
		if err != nil {
			klog.Errorf("Read ar body failed: %v", err)
			handleResponse(w, http.StatusInternalServerError,
				fmt.Sprintf("read ar body failed: %v", err))
			return
		}

		ar := &admissionv1.AdmissionReview{}
		if _, _, err = deserializer.Decode(data, nil, ar); err != nil {
			klog.Errorf("Parse ar body failed: %s, %v", string(data), err)
			handleResponse(w, http.StatusBadRequest, fmt.Sprintf("parse ar failed: %v", err))
			return
		}
		klog.V(4).Infof("receive request: %v/%v/%v from %+v, verb: %+v",
			ar.Request.Namespace, ar.Request.Name, ar.Request.UID, ar.Request.UserInfo,
			ar.Request.Operation)
		var respBytes []byte
		respBytes, err = json.Marshal(handler(ar))
		if err != nil {
			handleResponse(w, http.StatusInternalServerError,
				fmt.Sprintf("marshal response failed: %v", err))
			return
		}
		if _, err := w.Write(respBytes); err != nil {
			klog.Errorf("Send response failed: %v", err)
		}
	}
}

// NeedInspectPod returns whether we need to inspect the pod.
func NeedInspectPod(pod *corev1.Pod) bool {
	if pod.DeletionTimestamp != nil || pod.Labels == nil {
		return false
	}
	if val, ok := pod.Labels[constants.LabelShuffleServer]; ok && val == "true" {
		return true
	}
	return false
}

// MetricItem records an item of metric information of shuffle servers.
type MetricItem struct {
	Name        string   `json:"name"`
	LabelNames  []string `json:"labelNames"`
	LabelValues []string `json:"labelValues"`
	Value       float32  `json:"value"`
}

// MetricList records all items of metric information of shuffle servers.
type MetricList struct {
	Metrics   []*MetricItem `json:"metrics"`
	TimeStamp int64         `json:"timestamp"`
}

func getLastAppNum(body []byte) (int, error) {
	resp := &MetricList{}
	if err := json.Unmarshal(body, resp); err != nil {
		klog.Errorf("unmarshal body (%v) failed: %v", string(body), err)
		return 0, err
	}
	for i := range resp.Metrics {
		if resp.Metrics[i].Name == "app_num_with_node" {
			return int(resp.Metrics[i].Value), nil
		}
	}
	return 0, nil
}

// HasZeroApps returns whether there are zero apps in the shuffle server pod.
func HasZeroApps(pod *corev1.Pod) bool {
	port := utils.GetMetricsServerPort(pod)
	if len(port) == 0 {
		return true
	}
	if pod.Status.Phase != corev1.PodRunning {
		return true
	}
	url := fmt.Sprintf("http://%v:%v/metrics/server", pod.Status.PodIP, port)
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		klog.Errorf("new request failed with error: %v->%+v", url, err)
		return true
	}
	// the request accept json response only
	req.Header.Set("Accept", jsonContentType)
	resp, err := httpClient.Do(req)
	if err != nil {
		klog.Errorf("send metrics server request failed: %v->%+v", url, err)
		return true
	}
	if resp.StatusCode != http.StatusOK {
		klog.Errorf("heartbeat response failed: invalid status (%v->%v)", url, resp.Status)
		return false
	}
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		klog.Errorf("heartbeat response failed: read body with err:%+v", err)
		return false
	}
	if num, err := getLastAppNum(body); err != nil {
		klog.Errorf("get last app number of (%v) failed: %v", pod.Spec.NodeName, err)
		return false
	} else if num > 0 {
		klog.V(4).Infof("last %v apps in node %v", num, pod.Spec.NodeName)
		return false
	}
	return true
}
