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

// Package event manipulates the k8s api of event object
package event

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	apiv1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/pkg/clientgo/kubernetes"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// Event is the object to encapsulate docker
type Event struct {
	addr      string
	client    *httpclient.HTTPClient
	k8sClient *kubernetes.Clientset
}

// Option configures an Event
type Option func(*Event)

// New news an Event
func New(options ...Option) *Event {
	ns := &Event{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(e *Event) {
		e.addr = addr
		e.client = client
	}
}

func WithKubernetesClient(client *kubernetes.Clientset) Option {
	return func(event *Event) {
		event.k8sClient = client
	}
}

type rawevent struct {
	Type   string          `json:"type"`
	Object json.RawMessage `json:"object"`
}

func (e *Event) WatchPodEventsAllNamespaces(ctx context.Context, callback func(*apiv1.Event)) error {
	for {
		var b bytes.Buffer
		resp, err := e.client.Get(e.addr).
			Path("/api/v1/events").
			Param("limit", "10").
			Do().Body(&b)
		if err != nil {
			return err
		}
		if !resp.IsOK() {
			content, _ := ioutil.ReadAll(&b)
			errMsg := fmt.Sprintf("failed to get resp from k8s pods watcher, resp is not OK, body: %v",
				string(content))
			logrus.Errorf(errMsg)
			return errors.New(errMsg)
		}
		eventlist := apiv1.EventList{}
		if err := json.NewDecoder(&b).Decode(&eventlist); err != nil {
			return err
		}
		lastResourceVersion := eventlist.ListMeta.ResourceVersion
		body, resp, err := e.client.Get(e.addr).
			Path("/api/v1/watch/events").
			Header("Portal-SSE", "on").
			Param("fieldSelector", strutil.Join([]string{
				"metadata.namespace!=kube-system",
				"metadata.namespace!=kube-public",
				"involvedObject.kind=Pod",
			}, ",")).
			Param("resourceVersion", lastResourceVersion).
			Do().
			StreamBody()

		if err != nil {
			logrus.Errorf("failed to get resp from k8s events watcher, (%v)", err)
			return err
		}
		if !resp.IsOK() {
			errMsg := fmt.Sprintf("failed to get resp from k8s events watcher, resp is not OK")
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
			raw := rawevent{}
			if err := decoder.Decode(&raw); err != nil {
				logrus.Errorf("failed to decode k8s event: %v", err)
				body.Close()
				break
			}
			ke := apiv1.Event{}
			if err := json.Unmarshal(raw.Object, &ke); err != nil {
				logrus.Errorf("failed to unmarshal k8s event obj, err: %v, raw: %s", err, string(raw.Object))
				body.Close()
				continue
			}
			callback(&ke)

		}
		if body != nil {
			body.Close()
		}
	}
}

// LimitedListAllNamespace limit list event all namespaces
func (e *Event) LimitedListAllNamespace(limit int, cont *string) (*apiv1.EventList, *string, error) {
	var eventlist apiv1.EventList
	var b bytes.Buffer
	req := e.client.Get(e.addr).
		Path("/api/v1/events")
	if cont != nil {
		req = req.Param("continue", *cont)
	}
	resp, err := req.Param("fieldSelector", strutil.Join([]string{
		"metadata.namespace!=default",
		"metadata.namespace!=kube-system",
		"metadata.namespace!=kube-public",
	}, ",")).
		Param("limit", fmt.Sprintf("%d", limit)).
		Do().Body(&b)
	if err != nil {
		return &eventlist, nil, err
	}
	if !resp.IsOK() {
		return &eventlist, nil, fmt.Errorf("failed to list k8s events, statuscode: %v, body: %v",
			resp.StatusCode(), b.String())
	}
	if err := json.NewDecoder(&b).Decode(&eventlist); err != nil {
		return &eventlist, nil, err
	}

	if eventlist.ListMeta.Continue != "" {
		return &eventlist, &eventlist.ListMeta.Continue, nil
	}
	return &eventlist, nil, nil
}

// ListByNamespace list event by namespace
func (e *Event) ListByNamespace(namespace string) (*apiv1.EventList, error) {
	var eventlist apiv1.EventList
	var b bytes.Buffer
	resp, err := e.client.Get(e.addr).
		Path("/api/v1/namespaces/" + namespace + "/events").
		Do().
		Body(&b)
	if err != nil {
		return &eventlist, err
	}
	if !resp.IsOK() {
		return &eventlist, fmt.Errorf("failed to list k8s events, namespace: %s, statuscode: %v, body: %v",
			namespace, resp.StatusCode(), b.String())
	}
	if err := json.NewDecoder(&b).Decode(&eventlist); err != nil {
		return &eventlist, err
	}

	if eventlist.ListMeta.Continue != "" {
		return &eventlist, nil
	}
	return &eventlist, nil
}
