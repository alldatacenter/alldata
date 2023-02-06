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

// Package deployment manipulates the k8s api of deployment object
package deployment

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"
	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// Deployment is the object to manipulate k8s api of deployment
type Deployment struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures a Deployment
type Option func(*Deployment)

// New news a Deployment
func New(options ...Option) *Deployment {
	ns := &Deployment{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(d *Deployment) {
		d.addr = addr
		d.client = client
	}
}

type PatchStruct struct {
	Spec DeploymentSpec `json:"spec"`
}

type DeploymentSpec struct {
	Template PodTemplateSpec `json:"template"`
}

type PodTemplateSpec struct {
	Spec PodSpec `json:"spec"`
}

type PodSpec struct {
	Containers []v1.Container `json:"containers"`
}

// Patch patchs the k8s deployment object
func (d *Deployment) Patch(namespace, deploymentName, containerName string, snippet v1.Container) error {
	snippet.Name = containerName
	spec := PatchStruct{
		Spec: DeploymentSpec{
			Template: PodTemplateSpec{
				Spec: PodSpec{
					Containers: []v1.Container{
						snippet,
					},
				},
			},
		},
	}
	var b bytes.Buffer
	resp, err := d.client.Patch(d.addr).
		Path("/apis/apps/v1/namespaces/"+namespace+"/deployments/"+deploymentName).
		JSONBody(spec).
		Header("Content-Type", "application/strategic-merge-patch+json").
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to patch deployment, name: %s, (%v)", deploymentName, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to patch deployment, name: %s, statuscode: %v, body: %v",
			deploymentName, resp.StatusCode(), b.String())
	}
	return nil
}

// Create creates a k8s deployment object
func (d *Deployment) Create(deploy *appsv1.Deployment) error {
	var b bytes.Buffer

	resp, err := d.client.Post(d.addr).
		Path("/apis/apps/v1/namespaces/" + deploy.Namespace + "/deployments").
		JSONBody(deploy).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create deployment, name: %s", deploy.Name)
	}

	if !resp.IsOK() {
		errMsg := fmt.Sprintf("failed to create k8s deployment statuscode: %v, body: %v",
			resp.StatusCode(), b.String())
		return errors.Errorf(errMsg)
	}
	return nil
}

// Get gets a k8s deployment object
func (d *Deployment) Get(namespace, name string) (*appsv1.Deployment, error) {
	var b bytes.Buffer
	resp, err := d.client.Get(d.addr).
		Path("/apis/apps/v1/namespaces/" + namespace + "/deployments/" + name).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get deployment info, name: %s", name)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get deployment info, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}

	deployment := &appsv1.Deployment{}
	if err := json.NewDecoder(&b).Decode(deployment); err != nil {
		return nil, err
	}
	return deployment, nil
}

// List lists deployments under specific namespace
func (d *Deployment) List(namespace string, labelSelector map[string]string) (appsv1.DeploymentList, error) {
	var deployList appsv1.DeploymentList
	var params url.Values
	if len(labelSelector) > 0 {
		var kvs []string
		params = make(url.Values, 0)

		for key, value := range labelSelector {
			kvs = append(kvs, fmt.Sprintf("%s=%s", key, value))
		}
		params.Add("labelSelector", strings.Join(kvs, ","))
	}
	var b bytes.Buffer
	resp, err := d.client.Get(d.addr).
		Path("/apis/apps/v1/namespaces/" + namespace + "/deployments").
		Params(params).
		Do().
		Body(&b)

	if err != nil {
		return deployList, errors.Errorf("failed to get deployment list, ns: %s, (%v)", namespace, err)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return deployList, k8serror.ErrNotFound
		}
		return deployList, errors.Errorf("failed to get deployment list, ns: %s, statuscode: %v, body: %v",
			namespace, resp.StatusCode(), b.String())
	}

	if err := json.NewDecoder(&b).Decode(&deployList); err != nil {
		return deployList, err
	}
	return deployList, nil
}

// Put updates a k8s deployment
func (d *Deployment) Put(deployment *appsv1.Deployment) error {
	var b bytes.Buffer
	resp, err := d.client.Put(d.addr).
		Path("/apis/apps/v1/namespaces/" + deployment.Namespace + "/deployments/" + deployment.Name).
		JSONBody(deployment).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to put deployment, name: %s, (%v)", deployment.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to put deployment, name: %s, statuscode: %v, body: %v",
			deployment.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Delete deletes a k8s deployment
func (d *Deployment) Delete(namespace, name string) error {
	var b bytes.Buffer
	resp, err := d.client.Delete(d.addr).
		Path("/apis/apps/v1/namespaces/" + namespace + "/deployments/" + name).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete deployment, name: %s, (%v)", name, err)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}
		return errors.Errorf("failed to delete deployment, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	return nil
}

type rawevent struct {
	Type   string          `json:"type"`
	Object json.RawMessage `json:"object"`
}

func (d *Deployment) WatchAllNamespace(ctx context.Context, addfunc, updatefunc, deletefunc func(*appsv1.Deployment)) error {
	for { // apiserver will close stream periodically?
		body, resp, err := d.client.Get(d.addr).
			Path("/apis/apps/v1/watch/deployments").
			Header("Portal-SSE", "on").
			Param("fieldSelector", strutil.Join([]string{
				"metadata.namespace!=kube-system",
			}, ",")).
			Do().
			StreamBody()
		defer func() {
			if body != nil {
				body.Close()
			}
		}()
		if err != nil {
			logrus.Errorf("failed to get resp from k8s deployments watcher, (%v)", err)
			return err
		}

		if !resp.IsOK() {
			errMsg := fmt.Sprintf("failed to get resp from k8s deployments watcher, resp is not OK")
			logrus.Errorf(errMsg)
			return errors.New(errMsg)
		}

		decoder := json.NewDecoder(body)
		for {
			select {
			case <-ctx.Done():
				return nil
			default:
			}
			e := rawevent{}
			if err := decoder.Decode(&e); err != nil {
				body.Close()
				break
			}
			switch strutil.ToUpper(e.Type) {
			case "ADDED":
				deploy := appsv1.Deployment{}
				if err := json.Unmarshal(e.Object, &deploy); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				addfunc(&deploy)
			case "MODIFIED":
				deploy := appsv1.Deployment{}
				if err := json.Unmarshal(e.Object, &deploy); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				updatefunc(&deploy)
			case "DELETED":
				deploy := appsv1.Deployment{}
				if err := json.Unmarshal(e.Object, &deploy); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				deletefunc(&deploy)
			case "ERROR", "BOOKMARK":
				logrus.Infof("ignore ERROR or BOOKMARK event: %v", string(e.Object))
			}
		}
	}
	panic("unreachable")
}

func (d *Deployment) LimitedListAllNamespace(limit int, cont *string) (*appsv1.DeploymentList, *string, error) {
	var deployList appsv1.DeploymentList
	var b bytes.Buffer
	req := d.client.Get(d.addr).
		Path("/apis/apps/v1/deployments")
	if cont != nil {
		req = req.Param("continue", *cont)
	}
	resp, err := req.Param("fieldSelector", strutil.Join([]string{
		"metadata.namespace!=default",
		"metadata.namespace!=kube-system"}, ",")).
		Param("limit", fmt.Sprintf("%d", limit)).
		Do().Body(&b)
	if err != nil {
		return &deployList, nil, err
	}
	if !resp.IsOK() {
		return &deployList, nil, fmt.Errorf("failed to list deployments, statuscode: %v, body: %v",
			resp.StatusCode(), b.String())
	}
	if err := json.NewDecoder(&b).Decode(&deployList); err != nil {
		return &deployList, nil, err
	}
	if deployList.ListMeta.Continue != "" {
		return &deployList, &deployList.ListMeta.Continue, nil
	}
	return &deployList, nil, nil
}
