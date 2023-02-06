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

package k8s

import (
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/strutil"
)

// MakeNamespace Generate a Namespace name
// Each runtime corresponds to a k8s namespace on k8s,
// format is ${runtimeNamespace}--${runtimeName}
func MakeNamespace(sg *apistructs.ServiceGroup) string {
	if IsGroupStateful(sg) {
		// Create a new namespace for the servicegroup that needs to be split into multiple statefulsets, that is, add the group- prefix
		if v, ok := sg.Labels[groupNum]; ok && v != "" && v != "1" {
			return strutil.Concat("group-", sg.Type, "--", sg.ID)
		}
	}
	return strutil.Concat(sg.Type, "--", sg.ID)
}

// CreateNamespace create namespace
func (k *Kubernetes) CreateNamespace(ns string, sg *apistructs.ServiceGroup) error {
	notfound, err := k.NotfoundNamespace(ns)
	if err != nil {
		return err
	}

	if !notfound {
		registryInfos := k.composeRegistryInfos(sg)
		err := k.UpdateImageSecret(ns, registryInfos)
		if err != nil {
			logrus.Errorf("failed to update secret %s on namespace %s, err: %v", AliyunRegistry, ns, err)
			return err
		}
		if sg.ProjectNamespace != "" {
			return nil
		}
		return errors.Errorf("failed to create namespace, ns: %s, (namespace already exists)", ns)
	}

	labels := map[string]string{}

	if sg.Labels["service-mesh"] == "on" {
		labels["istio-injection"] = "enabled"
	}

	if err = k.namespace.Create(ns, labels); err != nil {
		return err
	}
	// Create imagePullSecret under this namespace
	if err = k.NewRuntimeImageSecret(ns, sg); err != nil {
		logrus.Errorf("failed to create imagePullSecret, namespace: %s, (%v)", ns, err)
	}
	return nil
}

// UpdateNamespace
func (k *Kubernetes) UpdateNamespace(ns string, sg *apistructs.ServiceGroup) error {
	notfound, err := k.NotfoundNamespace(ns)
	if err != nil {
		return err
	}
	if notfound {
		return errors.Errorf("not found ns: %v", ns)
	}

	labels := map[string]string{}

	if sg.Labels["service-mesh"] == "on" {
		labels["istio-injection"] = "enabled"
	}

	return k.namespace.Update(ns, labels)
}

// NotfoundNamespace not found namespace
func (k *Kubernetes) NotfoundNamespace(ns string) (bool, error) {
	err := k.namespace.Exists(ns)
	if err != nil {
		if k8serror.NotFound(err) {
			return true, nil
		}
		return false, err
	}
	return false, nil
}

// DeleteNamespace delete namepsace
func (k *Kubernetes) DeleteNamespace(ns string) error {
	return k.namespace.Delete(ns)
}
