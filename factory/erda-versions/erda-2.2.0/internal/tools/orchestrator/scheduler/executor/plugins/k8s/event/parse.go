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
	"context"
	"fmt"
	"sort"
	"strings"

	apiv1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/pkg/errors"
	"k8s.io/kubernetes/pkg/kubelet/events"
)

// Message Event message
type Message struct {
	Timestamp int64
	Reason    string
	Message   string
	Comment   string
}

// MessageList 事件消息列表
type MessageList []Message

func (em MessageList) Len() int           { return len(em) }
func (em MessageList) Swap(i, j int)      { em[i], em[j] = em[j], em[i] }
func (em MessageList) Less(i, j int) bool { return em[i].Timestamp < em[j].Timestamp }

// AnalyzePodEvents Analyze pod events and turn them into interpretable content
func (e *Event) AnalyzePodEvents(namespace, name string) (MessageList, error) {
	var ems MessageList

	var (
		eventList *apiv1.EventList
		err       error
	)
	if e.k8sClient != nil {
		eventList, err = e.k8sClient.CoreV1().Events(namespace).List(context.Background(), metav1.ListOptions{})
	} else {
		eventList, err = e.ListByNamespace(namespace)
	}
	if err != nil {
		return nil, errors.Errorf("failed to list pod events, namespace: %s, podName: %s, (%v)",
			namespace, name, err)
	}

	for i := range eventList.Items {
		e := &eventList.Items[i]
		if e.InvolvedObject.Kind != "Pod" || !strings.HasPrefix(e.InvolvedObject.Name, name) {
			continue
		}

		// One-stage analysis, the reasons are intuitively visible
		if cmt, ok := interestedEventCommentFirstMap[e.Reason]; ok {
			ems = append(ems, Message{
				Timestamp: e.LastTimestamp.Unix(),
				Reason:    e.Reason,
				Message:   e.Message,
				Comment:   cmt,
			})
			continue
		}

		// Two-stage analysis requires event.message analysis
		cmt, err := secondAnalyzePodEventComment(e.Reason, e.Message)
		if err == nil {
			ems = append(ems, Message{
				Timestamp: e.LastTimestamp.Unix(),
				Reason:    e.Reason,
				Message:   e.Message,
				Comment:   cmt,
			})
		}
	}

	sort.Sort(ems)
	return ems, nil
}

// One-stage analysis, the reasons are intuitively visible
var interestedEventCommentFirstMap = map[string]string{
	// events.FailedToInspectImage:     errInspectImage,
	events.ErrImageNeverPullPolicy:  errImageNeverPull,
	events.NetworkNotReady:          errNetworkNotReady,
	events.FailedAttachVolume:       errMountVolume,
	events.FailedMountVolume:        errMountVolume,
	events.VolumeResizeFailed:       errMountVolume,
	events.FileSystemResizeFailed:   errMountVolume,
	events.FailedMapVolume:          errMountVolume,
	events.WarnAlreadyMountedVolume: errAlreadyMountedVolume,
	events.NodeRebooted:             errNodeRebooted,
}

// wo-stage analysis requires event.message analysis
func secondAnalyzePodEventComment(reason, message string) (string, error) {
	switch reason {
	case "FailedScheduling":
		return parseFailedScheduling(message)
	case events.FailedToPullImage:
		return parseFailedReason(message)
	default:
		// TODO: 补充更多的 reason
		return "", errors.Errorf("invalid event reason: %s", reason)
	}
}

// Analyze the reason for scheduling failure
func parseFailedScheduling(message string) (string, error) {
	var (
		totalNodes    int
		notMatchNodes int
		tmpStr        string
	)

	splitFunc := func(r rune) bool {
		return r == ':' || r == ','
	}
	msgSlice := strings.FieldsFunc(message, splitFunc)

	// Node resource is unavailable
	// 1. Labes don't match。 Example："0/8 nodes are available: 8 node(s) didn't match node selector."
	// 2. Insufficient CPU。Example："0/8 nodes are available: 5 Insufficient cpu, 5 node(s) didn't match node selector."
	// 3. Insufficient Memory。Example："0/8 nodes are available: 5 node(s) didn't match node selector, 5 Insufficient memory."
	if strings.Contains(message, "nodes are available") {
		_, err := fmt.Sscanf(message, "0/%d nodes are available: %s", &totalNodes, &tmpStr)
		if err != nil {
			return "", errors.Errorf("failed to parse totalNodes num, body: %s, (%v)", message, err)
		}

		for _, msg := range msgSlice {
			if strings.Contains(msg, "node(s) didn't match node selector") {
				_, err := fmt.Sscanf(msg, "%d node(s) didn't match node selector", &notMatchNodes)
				if err != nil {
					return "", errors.Errorf("failed to parse notMatchNodes num, body: %s, (%v)", msg, err)
				}
			}
		}

		if totalNodes > 0 && (totalNodes == notMatchNodes) {
			return errNodeSelectorMismatching, nil
		}

		if strings.Contains(message, "Insufficient cpu") {
			return errInsufficientFreeCPU, nil
		}

		if strings.Contains(message, "Insufficient memory") {
			return errInsufficientFreeMemory, nil
		}
	}

	// TODO: Add more information
	return "", errors.New("unexpected")
}

// Analyze the reason for the failure
func parseFailedReason(message string) (string, error) {
	switch {
	// Invalid image name
	case strings.Contains(message, "InvalidImageName"):
		return errInvalidImageName, nil
	// Invalid image
	case strings.Contains(message, "ImagePullBackOff"):
		return errPullImage, nil
	default:
		// TODO: Analyze the reason for the failure
		return "", errors.New("unexpected")
	}
}
