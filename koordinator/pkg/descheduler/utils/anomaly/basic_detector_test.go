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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestBasicDetector(t *testing.T) {
	callStateChangeTimes := 0
	opts := Options{
		Timeout: 3 * time.Second,
		AnomalyConditionFn: func(counter Counter) bool {
			return counter.ConsecutiveAbnormalities >= 2
		},
		NormalConditionFn: func(counter Counter) bool {
			return counter.ConsecutiveNormalities >= 1
		},
		OnStateChange: func(name string, from State, to State) {
			callStateChangeTimes++
		},
	}
	detector := NewBasicDetector("test", opts)
	assert.Equal(t, "test", detector.Name())
	assert.Equal(t, StateOK, detector.State())

	state, _ := detector.Mark(true)
	assert.Equal(t, StateOK, state)

	state, _ = detector.Mark(false)
	assert.Equal(t, StateOK, state)

	assert.Equal(t, Counter{
		TotalDetects:             2,
		TotalNormalities:         1,
		TotalAbnormalities:       1,
		ConsecutiveNormalities:   0,
		ConsecutiveAbnormalities: 1,
	}, detector.Counter())

	state, _ = detector.Mark(false)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateOK, state)

	detector.Mark(false)
	detector.Mark(false)

	// wait for expiration
	time.Sleep(opts.Timeout)
	assert.Equal(t, StateOK, detector.State())
	state, _ = detector.Mark(false)
	assert.Equal(t, StateOK, state)

	state, _ = detector.Mark(false)
	assert.Equal(t, StateAnomaly, state)

	assert.NotZero(t, callStateChangeTimes)
	assert.Equal(t, Counter{}, detector.Counter())
}

func TestBasicDetectorWithCustomNormalCondition(t *testing.T) {
	callStateChangeTimes := 0
	opts := Options{
		Timeout: 3 * time.Second,
		AnomalyConditionFn: func(counter Counter) bool {
			return counter.ConsecutiveAbnormalities >= 2
		},
		NormalConditionFn: func(counter Counter) bool {
			return counter.ConsecutiveNormalities > 2
		},
		OnStateChange: func(name string, from State, to State) {
			callStateChangeTimes++
		},
	}
	detector := NewBasicDetector("test", opts)

	state, _ := detector.Mark(false)
	assert.Equal(t, StateOK, state)

	state, _ = detector.Mark(false)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateOK, state)

	state, _ = detector.Mark(false)
	assert.Equal(t, StateOK, state)

	state, _ = detector.Mark(false)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(false)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateAnomaly, state)

	state, _ = detector.Mark(true)
	assert.Equal(t, StateOK, state)
}
