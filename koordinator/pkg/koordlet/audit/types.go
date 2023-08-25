/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package audit

import (
	"fmt"
	"time"
)

var (
	errWriterIsNull = fmt.Errorf("writer is null")
)

// Package audit log implements logging analogous.
// It provides V-style logging controlled by the input verbose.
//
// Basic examples:
//   logger := NewFluentEventLogger(..)
//   logger.V(3).Node().Reason("beOutOfMemory").Do()
//   logger.V(1).Pod("pod1").Reason("killedByMemoryWarterMark").Message("%s", xxx).Do()
//   logger.V(0).Pod("pod1").Container("container1").Reason("killedByMemoryPSIController").Message("%s", xxx).Do()

// Event captures all the information that can be included in an API audit log.
type Event struct {
	CreatedAt time.Time `json:"createdAt,omitempty"`
	Type      string    `json:"type,omitempty"`
	Level     string    `json:"level,omitempty"`
	Namespace string    `json:"namespace,omitempty"`
	Name      string    `json:"name,omitempty"`
	Container string    `json:"container,omitempty"`
	Reason    string    `json:"reason,omitempty"`
	Message   string    `json:"message,omitempty"`
}

// EventHelper is a helper struct use to support fluent APIs
type EventHelper struct {
	Event
	verbose int
	writer  EventWriter
}

// EventWriter is used to record events to audit key changes
type EventWriter interface {
	// Log write an event to the underly storage
	Log(verbose int, event *Event) error

	// Flush flush events to the underly storage
	Flush() error

	// Close close the writer
	Close() error
}

// Fluent APIs to support log in one line: logger.V(1).WithNode().WithReason().Do()
type EventFluentWriter interface {
	// V create an eventHelper with Level verbose
	V(verbose int) *EventHelper

	// Flush flush events to the underly storage
	Flush() error

	// Close close the underly writer
	Close() error
}

// EventInterator is an interator to tail the event log
type EventIterator interface {
	Next() (*Event, error)
	Close() error
}

// EventReader is used to manager the event interators
type EventReader interface {
	NewReverseInterator() EventIterator
}

// Node set the event type to 'node'
func (e *EventHelper) Node() *EventHelper {
	e.Event.Type = "node"
	return e
}

// Pod set the event type to 'pod'
func (e *EventHelper) Pod(ns string, name string) *EventHelper {
	e.Event.Type = "pod"
	e.Event.Namespace = ns
	e.Event.Name = name
	return e
}

// Group set the event type to resource
func (e *EventHelper) Group(name string) *EventHelper {
	e.Event.Type = "group"
	e.Event.Name = name
	return e
}

// Unknown set the event type to unknown object(pod, node or something else)
func (e *EventHelper) Unknown(name string) *EventHelper {
	e.Event.Type = "unknown"
	e.Event.Name = name
	return e
}

// Container set the event container to name
func (e *EventHelper) Container(name string) *EventHelper {
	e.Event.Container = name
	return e
}

// Reason set the event reason to reason
func (e *EventHelper) Reason(reason string) *EventHelper {
	e.Event.Reason = reason
	return e
}

// Message set the message as the inputs
func (e *EventHelper) Message(format string, args ...interface{}) *EventHelper {
	e.Event.Message = fmt.Sprintf(format, args...)
	return e
}

// Do write the event to the writer
func (e *EventHelper) Do() error {
	e.Event.CreatedAt = time.Now().Local()
	if e.writer != nil {
		return e.writer.Log(e.verbose, &e.Event)
	}
	return errWriterIsNull
}
