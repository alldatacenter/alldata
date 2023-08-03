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
	"sync"
	"time"
)

const (
	defaultTimeout = 60 * time.Second
)

func defaultAnomalyCondition(counter Counter) bool {
	return counter.ConsecutiveAbnormalities > 5
}

func defaultNormalCondition(counter Counter) bool {
	return counter.ConsecutiveNormalities > 3
}

// Options configures BasicDetector
type Options struct {
	// Timeout is the period of the open state,
	// after which the state of the BasicDetector becomes half-open.
	// If Timeout is less than or equal to 0, the timeout value of the BasicDetector is set to 60 seconds.
	Timeout time.Duration
	// NormalConditionFn is called with a copy of Counter whenever a request success in the anomaly state.
	// If NormalConditionFn returns true, the BasicDetector will be placed into the ok state.
	// If NormalConditionFn is nil, default NormalConditionFn is used.
	// Default NormalConditionFn returns true when the number of consecutive normalities is more than 3.
	NormalConditionFn func(counter Counter) bool
	// AnomalyConditionFn is called with a copy of Counter whenever a request fails in the ok state.
	// If AnomalyConditionFn returns true, the BasicDetector will be placed into the anomaly state.
	// If AnomalyConditionFn is nil, default AnomalyConditionFn is used.
	// Default AnomalyConditionFn returns true when the number of consecutive abnormalities is more than 5.
	AnomalyConditionFn func(counter Counter) bool
	// OnStateChange is called whenever the state of the BasicDetector changes.
	OnStateChange func(name string, from State, to State)
}

var _ Detector = &BasicDetector{}

// BasicDetector is a state machine to prevent sending requests that are likely to fail.
type BasicDetector struct {
	name               string
	timeout            time.Duration
	anomalyConditionFn func(counts Counter) bool
	normalConditionFn  func(counts Counter) bool
	onStateChange      func(name string, from State, to State)

	mutex      sync.Mutex
	state      State
	generation uint64
	counter    Counter
	expiration time.Time
}

// NewBasicDetector returns a new BasicDetector configured with the given Options.
func NewBasicDetector(name string, opts Options) *BasicDetector {
	d := &BasicDetector{
		name:               name,
		timeout:            defaultTimeout,
		anomalyConditionFn: defaultAnomalyCondition,
		normalConditionFn:  defaultNormalCondition,
		onStateChange:      opts.OnStateChange,
	}
	if opts.Timeout > 0 {
		d.timeout = opts.Timeout
	}
	if opts.AnomalyConditionFn != nil {
		d.anomalyConditionFn = opts.AnomalyConditionFn
	}
	if opts.NormalConditionFn != nil {
		d.normalConditionFn = opts.NormalConditionFn
	}

	d.toNewGeneration(time.Now())
	return d
}

// Name returns the name of the BasicDetector.
func (d *BasicDetector) Name() string {
	return d.name
}

// State returns the current state of the BasicDetector.
func (d *BasicDetector) State() State {
	d.mutex.Lock()
	defer d.mutex.Unlock()

	now := time.Now()
	state := d.currentState(now)
	return state
}

// Counter returns internal counters
func (d *BasicDetector) Counter() Counter {
	d.mutex.Lock()
	defer d.mutex.Unlock()

	return d.counter
}

func (d *BasicDetector) Mark(normality bool) (State, error) {
	d.mutex.Lock()
	defer d.mutex.Unlock()

	now := time.Now()
	state := d.currentState(now)

	d.counter.onMark()
	if normality {
		d.onNormality(state, now)
	} else {
		d.onAbnormalities(state, now)
	}
	return d.currentState(now), nil
}

func (d *BasicDetector) Reset() {
	d.mutex.Lock()
	defer d.mutex.Unlock()
	d.setState(StateOK, time.Now())
}

func (d *BasicDetector) onNormality(state State, now time.Time) {
	switch state {
	case StateOK:
		d.counter.onNormality()
	case StateAnomaly:
		d.counter.onNormality()
		if d.normalConditionFn(d.counter) {
			d.setState(StateOK, now)
		}
	}
}

func (d *BasicDetector) onAbnormalities(state State, now time.Time) {
	switch state {
	case StateOK:
		d.counter.onAbnormalities()
		if d.anomalyConditionFn(d.counter) {
			d.setState(StateAnomaly, now)
		}
	case StateAnomaly:
		d.counter.onAbnormalities()
		d.setState(StateAnomaly, now)
	}
}

func (d *BasicDetector) currentState(now time.Time) State {
	switch d.state {
	case StateOK:
		if !d.expiration.IsZero() && d.expiration.Before(now) {
			d.toNewGeneration(now)
		}
	case StateAnomaly:
		if d.expiration.Before(now) || d.normalConditionFn(d.counter) {
			d.setState(StateOK, now)
		}
	}
	return d.state
}

func (d *BasicDetector) setState(state State, now time.Time) {
	if d.state == state {
		return
	}

	prev := d.state
	d.state = state

	d.toNewGeneration(now)

	if d.onStateChange != nil {
		d.onStateChange(d.name, prev, state)
	}
}

func (d *BasicDetector) toNewGeneration(now time.Time) {
	d.counter.clear()

	var zero time.Time
	switch d.state {
	case StateOK:
		d.expiration = zero
	case StateAnomaly:
		d.expiration = now.Add(d.timeout)
	default:
		d.expiration = zero
	}
}
