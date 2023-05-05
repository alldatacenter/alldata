// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package agentinjector

import (
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"mime"
	"net/http"
	"strings"

	v1 "k8s.io/api/admission/v1"
	"k8s.io/api/admission/v1beta1"
	admissionregistrationv1beta1 "k8s.io/api/admissionregistration/v1beta1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/serializer"
)

var (
	runtimeScheme = runtime.NewScheme()
	codecs        = serializer.NewCodecFactory(runtimeScheme)
	deserializer  = codecs.UniversalDeserializer()

	v1version      = v1.SchemeGroupVersion.String()
	v1beta1version = v1beta1.SchemeGroupVersion.String()

	admissionPatchType = v1.PatchTypeJSONPatch
)

func init() {
	_ = corev1.AddToScheme(runtimeScheme)
	_ = v1beta1.AddToScheme(runtimeScheme)
	_ = v1.AddToScheme(runtimeScheme)
	_ = admissionregistrationv1beta1.AddToScheme(runtimeScheme)
}

func (p *provider) writeError(rw http.ResponseWriter, code int, msg string) {
	p.Log.Errorf("code=%d, %s", code, msg)
	rw.WriteHeader(code)
	rw.Write([]byte(msg))
}

func (p *provider) newFailedAdmissionReview(msg string) *v1.AdmissionReview {
	return &v1.AdmissionReview{
		Response: &v1.AdmissionResponse{
			Allowed: false,
			Result: &metav1.Status{
				Message: msg,
			},
		},
	}
}

// patchOperation is an operation of a JSON patch, see https://tools.ietf.org/html/rfc6902 .
type patchOperation struct {
	Op    string      `json:"op"`
	Path  string      `json:"path"`
	Value interface{} `json:"value,omitempty"`
}

func (p *provider) processAdmissionReview(rw http.ResponseWriter, r *http.Request, handler func(review *v1.AdmissionReview) (*v1.AdmissionReview, error)) {
	req := p.readAdmissionReview(rw, r)
	if req == nil {
		return
	}
	if req.Request == nil {
		resp := p.newFailedAdmissionReview("malformed admission review: request is nil")
		resp.TypeMeta = req.TypeMeta
		p.writeAdmissionReview(rw, resp)
		return
	}
	resp, err := handler(req)
	if err != nil {
		p.writeError(rw, http.StatusInternalServerError, err.Error())
		return
	}
	resp.TypeMeta = req.TypeMeta
	if resp.Response != nil {
		resp.Response.UID = req.Request.UID
		resp.Response.PatchType = &admissionPatchType
	}
	p.writeAdmissionReview(rw, resp)
}

func (p *provider) readAdmissionReview(rw http.ResponseWriter, r *http.Request) *v1.AdmissionReview {
	// check method
	if r.Method != http.MethodPost {
		p.writeError(rw, http.StatusMethodNotAllowed, http.StatusText(http.StatusMethodNotAllowed))
		return nil
	}

	// check content type
	contentType := r.Header.Get("Content-Type")
	mtype, _, err := mime.ParseMediaType(contentType)
	if err != nil {
		p.writeError(rw, http.StatusUnsupportedMediaType, err.Error())
		return nil
	}
	if mtype != "application/json" && !(strings.HasPrefix(mtype, "application/vnd.") && strings.HasSuffix(mtype, "+json")) {
		p.writeError(rw, http.StatusUnsupportedMediaType, fmt.Sprintf("Content-Type got %q, expect application/json", contentType))
		return nil
	}

	// read body
	var body []byte
	if r.Body != nil {
		data, err := ioutil.ReadAll(r.Body)
		if err != nil {
			p.writeError(rw, http.StatusBadRequest, fmt.Sprintf("failed to read body: %s", err))
			return nil
		}
		body = data
	}
	if len(body) <= 0 {
		p.writeError(rw, http.StatusBadRequest, fmt.Sprintf("failed to read body: %s", io.ErrUnexpectedEOF))
		return nil
	}

	ar := v1.AdmissionReview{}
	if err := json.Unmarshal(body, &ar); err != nil {
		p.writeError(rw, http.StatusBadRequest, fmt.Sprintf("failed to decode body: %s", err))
		return nil
	}
	if ar.APIVersion != v1version && ar.APIVersion != v1beta1version {
		p.writeError(rw, http.StatusBadRequest, fmt.Sprintf("not support APIVersion %q", ar.APIVersion))
		return nil
	}
	return &ar
}

func (p *provider) writeAdmissionReview(rw http.ResponseWriter, review *v1.AdmissionReview) {
	body, err := json.Marshal(review)
	if err != nil {
		p.writeError(rw, http.StatusInternalServerError, fmt.Sprintf("failed to encode AdmissionReview: %s", err))
		return
	}
	rw.Header().Set("Content-Type", "application/json")
	if _, err := rw.Write(body); err != nil {
		p.writeError(rw, http.StatusInternalServerError, fmt.Sprintf("failed to write AdmissionReview into ResponseWriter: %s", err))
		return
	}
	if review != nil && review.Response != nil && review.Response.Allowed == false {
		if review.Response.Result != nil {
			p.Log.Errorf("not allowed admissionReview{APIVersion=%s, Kind=%s}: %s", review.APIVersion, review.Kind, review.Response.Result.Message)
		} else {
			p.Log.Errorf("not allowed admissionReview{APIVersion=%s, Kind=%s}", review.APIVersion, review.Kind)
		}
	} else {
		p.Log.Infof("process admissionReview{APIVersion=%s, Kind=%s} ok", review.APIVersion, review.Kind)
	}
}
