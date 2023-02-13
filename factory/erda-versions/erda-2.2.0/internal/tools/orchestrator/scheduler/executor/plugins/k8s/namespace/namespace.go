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

// Package namespace manipulates the k8s api of namespace object
package namespace

import (
	"bytes"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	apiv1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// Namespace is the object to manipulate k8s api of namespace
type Namespace struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures a Namespace
type Option func(*Namespace)

// New news a Namespace
func New(options ...Option) *Namespace {
	ns := &Namespace{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(n *Namespace) {
		n.addr = addr
		n.client = client
	}
}

// Create creates a k8s namespace
// TODO: Need to pass in the namespace structure
func (n *Namespace) Create(ns string, labels map[string]string) error {
	namespace := &apiv1.Namespace{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "v1",
			Kind:       "Namespace",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:   ns,
			Labels: labels,
		},
	}

	var b bytes.Buffer
	resp, err := n.client.Post(n.addr).
		Path("/api/v1/namespaces").
		JSONBody(namespace).
		Do().
		Body(&b)

	if err != nil {
		return errors.Wrapf(err, "failed to create namespace, ns: %s, (%v)", ns, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create namespace, ns: %s, statuscode: %v, body: %v", ns, resp.StatusCode(), b.String())
	}
	logrus.Infof("succeed to create namespace %s", ns)
	return nil
}

func (n *Namespace) Update(ns string, labels map[string]string) error {
	namespace := &apiv1.Namespace{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "v1",
			Kind:       "Namespace",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:   ns,
			Labels: labels,
		},
	}

	var b bytes.Buffer
	resp, err := n.client.Put(n.addr).
		Path("/api/v1/namespaces/" + ns).
		JSONBody(namespace).
		Do().
		Body(&b)

	if err != nil {
		return errors.Wrapf(err, "failed to update namespace, ns: %s, (%v)", ns, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to update namespace, ns: %s, statuscode: %v, body: %v", ns, resp.StatusCode(), b.String())
	}
	logrus.Infof("succeed to update namespace, ns: %s", ns)
	return nil
}

// Exists decides whether a namespace exists
func (n *Namespace) Exists(ns string) error {
	path := strutil.Concat("/api/v1/namespaces/", ns)
	resp, err := n.client.Get(n.addr).
		Path(path).
		Do().
		DiscardBody()

	if err != nil {
		return err
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}
		return errors.Errorf("failed to get namespace, ns: %s, statuscode: %v", ns, resp.StatusCode())

	}
	return nil
}

// Delete deletes a k8s namespace (deletes all dependents in the foreground)
func (n *Namespace) Delete(ns string) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", ns)

	resp, err := n.client.Delete(n.addr).
		Path(path).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete namespace, ns: %s, (%v)", ns, err)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			logrus.Debugf("namespace not found, ns: %s", ns)
			return k8serror.ErrNotFound
		}
		//When the deletion fails, the namespace is forced to be deleted, and the spec is set to empty
		if resp.StatusCode() == 409 {
			return n.DeleteForce(ns)
		}
		return errors.Errorf("failed to delete namespace, ns: %s, statuscode: %v, body: %v",
			ns, resp.StatusCode(), b.String())
	}

	return nil
}

// Delete deletes a k8s namespace with empty
func (n *Namespace) DeleteForce(ns string) error {
	namespace := &apiv1.Namespace{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "v1",
			Kind:       "Namespace",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name: ns,
		},
	}

	var b bytes.Buffer
	_, err := n.client.Put(n.addr).
		Path("/api/v1/namespaces/" + ns + "/finalize").
		JSONBody(namespace).
		Do().
		Body(&b)

	if err != nil {
		logrus.Errorf("failed to update(force delete finalize) namespace, ns: %s, (%v)", ns, err)
		return errors.Wrapf(err, "failed to update(force delete finalize) namespace, ns: %s, (%v)", ns, err)
	}
	return nil
}
