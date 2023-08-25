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

package anomaly

import (
	"fmt"
)

// State is a type that represents a state of Detector.
type State int

// These constants are states of Detector.
const (
	StateOK State = iota
	StateAnomaly
)

// String implements stringer interface.
func (s State) String() string {
	switch s {
	case StateOK:
		return "ok"
	case StateAnomaly:
		return "anomaly"
	default:
		return fmt.Sprintf("unknown state: %d", s)
	}
}

type Detector interface {
	Name() string
	Mark(normality bool) (State, error)
	State() State
	Reset()
}
