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

// Package statefulset manipulates the k8s api of statefulset object
package statefulset

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// StatefulSet is the object to manipulate k8s api of statefulset
type StatefulSet struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures a StatefulSet
type Option func(*StatefulSet)

// New news a StatefulSet
func New(options ...Option) *StatefulSet {
	sts := &StatefulSet{}

	for _, op := range options {
		op(sts)
	}

	return sts
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(sts *StatefulSet) {
		sts.addr = addr
		sts.client = client
	}
}

// Create creates a k8s statefulset
func (sts *StatefulSet) Create(statefulset *appsv1.StatefulSet) error {
	var b bytes.Buffer
	path := strutil.Concat("/apis/apps/v1/namespaces/", statefulset.Namespace, "/statefulsets")

	resp, err := sts.client.Post(sts.addr).
		Path(path).
		JSONBody(statefulset).
		Do().
		Body(&b)

	if err != nil {
		return errors.Wrapf(err, "failed to create statefulset, name: %s, (%v)", statefulset.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create statefulset, name: %s, statuscode: %v, body: %v",
			statefulset.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Delete deletes a k8s statefulset
func (sts *StatefulSet) Delete(namespace, name string) error {
	var b bytes.Buffer
	path := strutil.Concat("/apis/apps/v1/namespaces/", namespace, "/statefulsets/", name)

	resp, err := sts.client.Delete(sts.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return errors.Wrapf(err, "failed to delete statefulset, name: %s, (%v)", name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to delete statefulset, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	return nil
}

// Get gets a k8s statefulset
func (sts *StatefulSet) Get(namespace, name string) (*appsv1.StatefulSet, error) {
	var b bytes.Buffer
	path := strutil.Concat("/apis/apps/v1/namespaces/", namespace, "/statefulsets/", name)

	resp, err := sts.client.Get(sts.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get statefulset info, name: %s", name)
	}

	if !resp.IsOK() {
		//logrus.Errorf("get statefulset name: %s, body: %v, resp: %+v", name, b.String(), resp)
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get statefulset info, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}

	statefulset := &appsv1.StatefulSet{}
	if err := json.NewDecoder(&b).Decode(statefulset); err != nil {
		return nil, err
	}
	return statefulset, nil
}

func (sts *StatefulSet) List(namespace string) (appsv1.StatefulSetList, error) {
	var stslist appsv1.StatefulSetList
	var b bytes.Buffer
	resp, err := sts.client.Get(sts.addr).
		Path("/apis/apps/v1/namespaces/" + namespace + "/statefulsets").
		Do().
		Body(&b)
	if err != nil {
		return stslist, errors.Errorf("failed to list sts list, ns: %v, (%v)", namespace, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return stslist, k8serror.ErrNotFound
		}
		return stslist, errors.Errorf("failed to list sts list, ns: %s, statuscode: %v, body: %v",
			namespace, resp.StatusCode(), b.String())
	}
	if err := json.NewDecoder(&b).Decode(&stslist); err != nil {
		return stslist, err
	}
	return stslist, nil
}

// Put updates a k8s statefulset
func (sts *StatefulSet) Put(statefulset *appsv1.StatefulSet) error {
	var b bytes.Buffer
	resp, err := sts.client.Put(sts.addr).
		Path("/apis/apps/v1/namespaces/" + statefulset.Namespace + "/statefulsets/" + statefulset.Name).
		JSONBody(statefulset).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to put statefuleset name: %s namespace: %s, (%v)", statefulset.Name, statefulset.Namespace, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to put statefuleset name: %s  namespace: %s, statuscode: %v, body: %v",
			statefulset.Name, statefulset.Namespace, resp.StatusCode(), b.String())
	}
	return nil
}

type rawevent struct {
	Type   string          `json:"type"`
	Object json.RawMessage `json:"object"`
}

func (sts *StatefulSet) WatchAllNamespace(ctx context.Context,
	addfunc, updatefunc, deletefunc func(*appsv1.StatefulSet)) error {
	for {
		body, resp, err := sts.client.Get(sts.addr).
			Path("/apis/apps/v1/watch/statefulsets").
			Header("Portal-SSE", "on").
			Param("fieldSelector", strutil.Join([]string{
				"metadata.namespace!=kube-system",
			}, ",")).
			Do().
			StreamBody()

		if err != nil {
			logrus.Errorf("failed to get resp from k8s sts watcher, (%v)", err)
			return err
		}
		if !resp.IsOK() {
			errMsg := fmt.Sprintf("failed to get resp from k8s sts watcher, resp is not OK")
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
				sts := appsv1.StatefulSet{}
				if err := json.Unmarshal(e.Object, &sts); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				addfunc(&sts)
			case "MODIFIED":
				sts := appsv1.StatefulSet{}
				if err := json.Unmarshal(e.Object, &sts); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				updatefunc(&sts)
			case "DELETED":
				sts := appsv1.StatefulSet{}
				if err := json.Unmarshal(e.Object, &sts); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				deletefunc(&sts)
			case "ERROR", "BOOKMARK":
				logrus.Errorf("ignore ERROR or BOOKMARK event")
			}
		}
		if body != nil {
			body.Close()
		}
	}
	panic("unreachable")
}

func (sts *StatefulSet) LimitedListAllNamespace(limit int, cont *string) (*appsv1.StatefulSetList, *string, error) {
	var stsList appsv1.StatefulSetList
	var b bytes.Buffer
	req := sts.client.Get(sts.addr).
		Path("/apis/apps/v1/statefulsets")
	if cont != nil {
		req = req.Param("continue", *cont)
	}
	resp, err := req.Param("fieldSelector", strutil.Join([]string{
		"metadata.namespace!=default",
		"metadata.namespace!=kube-system"}, ",")).
		Param("limit", fmt.Sprintf("%d", limit)).
		Do().Body(&b)
	if err != nil {
		return &stsList, nil, err
	}
	if !resp.IsOK() {
		return &stsList, nil, fmt.Errorf("failed to list statefulsets, statuscode: %v, body: %v",
			resp.StatusCode(), b.String())
	}
	if err := json.NewDecoder(&b).Decode(&stsList); err != nil {
		return &stsList, nil, err
	}
	if stsList.ListMeta.Continue != "" {
		return &stsList, &stsList.ListMeta.Continue, nil
	}
	return &stsList, nil, nil
}
