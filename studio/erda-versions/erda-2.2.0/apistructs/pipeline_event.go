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

import "time"

// PipelineEvent is k8s-event-like stream event.
type PipelineEvent struct {
	// Optional; this should be a short, machine understandable string that gives the reason
	// for this event being generated. For example, if the event is reporting that a container
	// can't start, the Reason might be "ImageNotFound".
	// +optional
	Reason string `json:"reason,omitempty"`

	// Optional. A human-readable description of the status of this operation.
	// +optional
	Message string `json:"message,omitempty"`

	// Optional. The component reporting this event. Should be a short machine understandable string.
	// +optional
	Source PipelineEventSource `json:"source,omitempty"`

	// The time at which the event was first recorded. (Time of server receipt is in TypeMeta.)
	// +optional
	FirstTimestamp time.Time `json:"firstTimestamp,omitempty"`

	// The time at which the most recent occurrence of this event was recorded.
	// +optional
	LastTimestamp time.Time `json:"lastTimestamp,omitempty"`

	// The number of times this event has occurred.
	// +optional
	Count int32 `json:"count,omitempty"`

	// Type of this event (Normal, Warning), new types could be added in the future.
	// +optional
	Type string `json:"type,omitempty"`
}

// PipelineEventSource represents the source from which an event is generated
type PipelineEventSource struct {
	// Component from which the event is generated.
	// +optional
	Component string `json:"component,omitempty"`

	// Node name on which the event is generated.
	// +optional
	Host string `json:"host,omitempty"`
}
