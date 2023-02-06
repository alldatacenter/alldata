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

package apistructs

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/pkg/strutil"
)

const (
	SteveErrorType = "error"
)

var (
	BadRequest         = SteveErrorCode{"BadRequest", 400}
	Unauthorized       = SteveErrorCode{"Unauthorized", 401}
	PermissionDenied   = SteveErrorCode{"PermissionDenied", 403}
	NotFound           = SteveErrorCode{"NotFound", 404}
	MethodNotAllowed   = SteveErrorCode{"MethodNotAllowed", 405}
	Conflict           = SteveErrorCode{"Conflict", 409}
	InvalidBodyContent = SteveErrorCode{"InvalidBodyContent", 422}
	ServerError        = SteveErrorCode{"ServerError", 500}
)

type SteveErrorCode struct {
	Code   string `json:"code,omitempty"`
	Status int    `json:"status,omitempty"`
}

// SteveError is an error returned from steve server.
type SteveError struct {
	SteveErrorCode
	Type    string `json:"type,omitempty"`
	Message string `json:"message,omitempty"`
}

func NewSteveError(errorCode SteveErrorCode, msg string) *SteveError {
	return &SteveError{
		SteveErrorCode: errorCode,
		Type:           "error",
		Message:        msg,
	}
}

func (s *SteveError) Error() string {
	return fmt.Sprintf("code: %s, status: %d, message: %s", s.Code, s.Status, s.Message)
}

func (s SteveError) JSON() []byte {
	data, _ := json.Marshal(s)
	return data
}

type K8SResType string

const (
	K8SPod         K8SResType = "pods"
	K8SNode        K8SResType = "nodes"
	K8SDeployment  K8SResType = "apps.deployments"
	K8SReplicaSet  K8SResType = "apps.replicasets"
	K8SDaemonSet   K8SResType = "apps.daemonsets"
	K8SStatefulSet K8SResType = "apps.statefulsets"
	K8SJob         K8SResType = "batch.jobs"
	K8SCronJob     K8SResType = "batch.cronjobs"
	K8SNamespace   K8SResType = "namespace"
	K8SEvent       K8SResType = "events"
)

// SteveRequest used to query steve server by bundle.
type SteveRequest struct {
	// Only support in GetSteveResource and ListSteveResource !
	// If true, request steve as admin, no need UserID and OrgID.
	NoAuthentication bool
	UserID           string     // used to authentication, required
	OrgID            string     // used to authentication, required
	Type             K8SResType // type of resource, required
	ClusterName      string     // cluster name, required
	Name             string     // name of resource，required when Get, Delete, Update
	Namespace        string     // namespace of resource
	// label selector, optional when list
	// format: "key=value"，or "key in (value1, value2)"，or "key notin (value1, value2)"
	LabelSelector []string
	// field selector, optional when list
	// format: "field=value", or "field==value", or "field!=value"
	// Supported field selectors vary by k8s resource type
	// All resource types support the metadata.name and metadata.namespace fields
	// Using unsupported field selectors produces an error
	FieldSelector []string
	// required in  Update, Create，obj is a pointer of original k8s resource，like *v1.pod, *v1.node
	Obj interface{}
}

// URLQueryString converts label selectors to url query params.
func (k *SteveRequest) URLQueryString() map[string][]string {
	query := make(map[string][]string)

	if len(k.LabelSelector) != 0 {
		labels := strutil.Join(k.LabelSelector, ",", true)
		query["labelSelector"] = []string{labels}
	}
	if len(k.FieldSelector) != 0 {
		fields := strutil.Join(k.FieldSelector, ",", true)
		query["fieldSelector"] = []string{fields}
	}
	return query
}
