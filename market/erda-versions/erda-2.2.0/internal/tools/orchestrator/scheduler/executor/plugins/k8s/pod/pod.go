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

// Package pod manipulates the k8s api of pod object
package pod

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	apiv1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// Pod is the object to encapsulate docker
type Pod struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an Pod
type Option func(*Pod)

// New news an Pod
func New(options ...Option) *Pod {
	ns := &Pod{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(p *Pod) {
		p.addr = addr
		p.client = client
	}
}

// Get gets a k8s pod
func (p *Pod) Get(namespace, name string) (*apiv1.Pod, error) {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/pods/", name)

	resp, err := p.client.Get(p.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get pod, namespace: %s, name: %s, (%v)", namespace, name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get pod, namespace: %s, name: %s, statuscode: %v, body: %v",
			namespace, name, resp.StatusCode(), b.String())
	}
	pod := &apiv1.Pod{}
	if err := json.NewDecoder(&b).Decode(pod); err != nil {
		return nil, err
	}
	return pod, nil
}

func (p *Pod) Delete(namespace, podname string) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/pods/", podname)
	resp, err := p.client.Delete(p.addr).
		Path(path).
		Do().
		Body(&b)
	if err != nil {
		return err
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil
		}
		return errors.Errorf("failed to get pod, namespace: %s, podname: %s, statuscode: %v, body: %v",
			namespace, podname, resp.StatusCode(), b.String())
	}
	return nil
}

type rawevent struct {
	Type   string          `json:"type"`
	Object json.RawMessage `json:"object"`
}

func (p *Pod) WatchAllNamespace(ctx context.Context, addfunc, updatefunc, deletefunc func(*apiv1.Pod)) error {
	for {
		var b bytes.Buffer
		resp, err := p.client.Get(p.addr).
			Path("/api/v1/pods").
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
		podlist := apiv1.PodList{}
		if err := json.NewDecoder(&b).Decode(&podlist); err != nil {
			return err
		}
		lastResourceVersion := podlist.ListMeta.ResourceVersion
		body, resp, err := p.client.Get(p.addr).
			Path("/api/v1/watch/pods").
			Header("Portal-SSE", "on").
			Param("fieldSelector", strutil.Join([]string{
				"metadata.namespace!=kube-system",
			}, ",")).
			Param("resourceVersion", lastResourceVersion).
			Do().
			StreamBody()
		if err != nil {
			logrus.Errorf("failed to get resp from k8s pods watcher, (%v)", err)
			return err
		}

		if !resp.IsOK() {
			errMsg := fmt.Sprintf("failed to get resp from k8s pods watcher, resp is not OK")
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
				logrus.Errorf("failed to decode event: %v", err)
				body.Close()
				break
			}
			switch strutil.ToUpper(e.Type) {
			case "ADDED":
				pod := apiv1.Pod{}
				if err := json.Unmarshal(e.Object, &pod); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				addfunc(&pod)
			case "MODIFIED":
				pod := apiv1.Pod{}
				if err := json.Unmarshal(e.Object, &pod); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				updatefunc(&pod)
			case "DELETED":
				pod := apiv1.Pod{}
				if err := json.Unmarshal(e.Object, &pod); err != nil {
					logrus.Errorf("failed to unmarshal event obj, err: %v, raw: %s", err, string(e.Object))
					body.Close()
					continue
				}
				deletefunc(&pod)
			case "ERROR", "BOOKMARK":
				logrus.Infof("ignore ERROR or BOOKMARK event: %v", string(e.Object))
			}
		}
		if body != nil {
			body.Close()
		}
	}
	panic("unreachable")
}

func (p *Pod) LimitedListAllNamespace(limit int, cont *string) (*apiv1.PodList, *string, error) {
	var podlist apiv1.PodList
	var b bytes.Buffer
	req := p.client.Get(p.addr).
		Path("/api/v1/pods")
	if cont != nil {
		req = req.Param("continue", *cont)
	}
	resp, err := req.Param("fieldSelector", strutil.Join([]string{
		"metadata.namespace!=default",
		"metadata.namespace!=kube-system"}, ",")).
		Param("limit", fmt.Sprintf("%d", limit)).
		Do().Body(&b)
	if err != nil {
		return &podlist, nil, err
	}
	if !resp.IsOK() {
		return &podlist, nil, fmt.Errorf("failed to list pods, statuscode: %v, addr: %s, body: %v",
			resp.StatusCode(), p.addr, b.String())
	}
	if err := json.Unmarshal(b.Bytes(), &podlist); err != nil {
		return &podlist, nil, err
	}
	if podlist.ListMeta.Continue != "" {
		return &podlist, &podlist.ListMeta.Continue, nil
	}
	return &podlist, nil, nil
}

func (p *Pod) ListAllNamespace(fieldSelectors []string) (*apiv1.PodList, error) {
	var podlist apiv1.PodList
	var b bytes.Buffer
	req := p.client.Get(p.addr, httpclient.RetryOption{}).
		Path("/api/v1/pods")
	body, resp, err := req.Param("fieldSelector", strutil.Join(fieldSelectors, ",")).
		Do().StreamBody()
	if err != nil {
		return &podlist, err
	}
	defer func() {
		if body != nil {
			body.Close()
		}
	}()
	if !resp.IsOK() {
		return &podlist, fmt.Errorf("failed to list pods, statuscode: %v, addr: %s, body: %v",
			resp.StatusCode(), p.addr, b.String())
	}
	d := json.NewDecoder(body)
	for {
		t, err := d.Token()
		if err != nil {
			return nil, err
		}
		v, ok := t.(string)
		if !ok {
			continue
		}
		if v == "items" {
			break
		}
	}
	if _, err := d.Token(); err != nil {
		return nil, err
	}
	pods := []apiv1.Pod{}
	for d.More() {
		pod := apiv1.Pod{}
		if err := d.Decode(&pod); err != nil {
			r, _ := ioutil.ReadAll(d.Buffered())
			logrus.Infof("failed to decode pod: buffered string: %s", string(r[:200]))
			return nil, err
		}
		pods = append(pods, pod)
	}
	podlist.Items = pods
	return &podlist, nil
}

func (p *Pod) ListNamespacePods(namespace string) (*apiv1.PodList, error) {
	var podlist apiv1.PodList
	var b bytes.Buffer
	resp, err := p.client.Get(p.addr).
		Path(fmt.Sprintf("/api/v1/namespaces/%s/pods", namespace)).
		Do().Body(&b)
	if err != nil {
		return nil, err
	}
	if !resp.IsOK() {
		return nil, fmt.Errorf("failed to list pods, statuscode: %v, addr: %s, body: %v",
			resp.StatusCode(), p.addr, b.String())
	}
	if err := json.Unmarshal(b.Bytes(), &podlist); err != nil {
		return nil, err
	}
	return &podlist, nil
}

type UnreadyReason string

const (
	None                  UnreadyReason = "None"
	ImagePullFailed       UnreadyReason = "ImagePullFailed"
	InsufficientResources UnreadyReason = "InsufficientResources"
	Unschedulable         UnreadyReason = "Unschedulable"
	ProbeFailed           UnreadyReason = "ProbeFailed"
	ContainerCannotRun    UnreadyReason = "ContainerCannotRun"
)

func (p *Pod) UnreadyPodReason(pod *apiv1.Pod) (UnreadyReason, string) {
	// image pull failed
	for _, container := range pod.Status.ContainerStatuses {
		if container.State.Waiting != nil && container.State.Waiting.Reason == "ImagePullBackOff" {
			return ImagePullFailed, container.State.Waiting.Message
		}
	}

	// insufficient resources
	for _, cond := range pod.Status.Conditions {
		if cond.Type == "PodScheduled" &&
			cond.Status == apiv1.ConditionFalse &&
			cond.Reason == "Unschedulable" &&
			strutil.Contains(cond.Message, "nodes are available") &&
			strutil.Contains(cond.Message, "Insufficient memory", "Insufficient cpu") {
			return InsufficientResources, cond.Message
		}
	}

	// other unschedule reasons
	for _, cond := range pod.Status.Conditions {
		if cond.Type == "PodScheduled" && cond.Status == apiv1.ConditionFalse && cond.Reason == "Unschedulable" {
			return Unschedulable, cond.Message
		}
	}

	// container cannot run
	if pod.Status.Phase == "Running" {
		for _, cond := range pod.Status.Conditions {
			if cond.Type == "ContainersReady" && cond.Status == apiv1.ConditionFalse && cond.Reason == "ContainersNotReady" {
				for _, container := range pod.Status.ContainerStatuses {
					if container.Ready == false &&
						container.Started != nil &&
						*container.Started == false &&
						container.LastTerminationState.Terminated != nil &&
						container.LastTerminationState.Terminated.Reason == "ContainerCannotRun" {
						return ContainerCannotRun, container.LastTerminationState.Terminated.Message
					}
				}
			}
		}
	}
	// probe failed
	if pod.Status.Phase == "Running" {
		for _, cond := range pod.Status.Conditions {
			if cond.Type == "ContainersReady" && cond.Status == apiv1.ConditionFalse && cond.Reason == "ContainersNotReady" {
				return ProbeFailed, cond.Message
			}
		}
	}

	return None, ""
}

type PodStatus struct {
	Reason  UnreadyReason
	Message string
}

func (p *Pod) GetNamespacedPodsStatus(pods []apiv1.Pod, serviceName string) ([]PodStatus, error) {

	r := []PodStatus{}
	for _, pod := range pods {
		if serviceName == "" || !strings.Contains(pod.Name, serviceName) {
			continue
		}
		reason, message := p.UnreadyPodReason(&pod)
		switch reason {
		case None:
		case ImagePullFailed:
			r = append(r, PodStatus{Reason: ImagePullFailed, Message: message})
		case InsufficientResources:
			r = append(r, PodStatus{Reason: InsufficientResources, Message: message})
		case Unschedulable:
			r = append(r, PodStatus{Reason: Unschedulable, Message: message})
		case ProbeFailed:
			r = append(r, PodStatus{Reason: ProbeFailed, Message: message})
		case ContainerCannotRun:
			r = append(r, PodStatus{Reason: ContainerCannotRun, Message: message})
		}
	}
	return r, nil
}
