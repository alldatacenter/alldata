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

package k8sspark

import (
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	jobKind                 = "SparkApplication"
	sparkVersion            = "2.4.0"
	jobAPIVersion           = "sparkoperator.k8s.io/v1beta2"
	rbacAPIVersion          = "rbac.authorization.k8s.io/v1"
	rbacAPIGroup            = "rbac.authorization.k8s.io"
	sparkServiceAccountName = "spark"
	sparkRoleName           = "spark-role"
	sparkRoleBindingName    = "spark-role-binding"
	imagePullPolicyAlways   = "Always"
	prefetechVolumeName     = "pre-fetech-volume"

	K8SLabelPrefix = "dice/"
)

func stringptr(s string) *string {
	return &s
}

func int32ptr(n int32) *int32 {
	return &n
}

func int64ptr(n int64) *int64 {
	return &n
}

func float32ptr(n float32) *float32 {
	return &n
}

func float64ptr(n float64) *float64 {
	return &n
}

func addMainApplicationFile(conf *apistructs.BigdataConf) (string, error) {
	var appResource = conf.Spec.Resource

	if strings.HasPrefix(appResource, "local://") || strings.HasPrefix(appResource, "http://") {
		return appResource, nil
	}

	if strings.HasPrefix(appResource, "/") {
		return strutil.Concat("local://", appResource), nil
	}

	return "", errors.Errorf("invalid job spec, resource %s", appResource)
}

func addLabels(conf *apistructs.BigdataConf) map[string]string {
	labels := make(map[string]string)

	// "dice/job": ""
	jobKey := strutil.Concat(K8SLabelPrefix, apistructs.TagJob)
	labels[jobKey] = ""

	// "dice/bigdata": ""
	bigdataKey := strutil.Concat(K8SLabelPrefix, apistructs.TagBigdata)
	labels[bigdataKey] = ""

	return labels
}
