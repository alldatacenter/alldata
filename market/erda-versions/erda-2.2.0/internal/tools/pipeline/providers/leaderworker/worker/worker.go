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

package worker

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/buraksezer/consistent"
)

type Type string

func (t Type) String() string { return string(t) }
func (t Type) Valid() bool {
	for _, at := range AllTypes {
		if at == t {
			return true
		}
	}
	return false
}

var (
	Official  Type = "official"
	Candidate Type = "candidate"

	AllTypes = []Type{Official, Candidate}
)

// Worker .
type Worker interface {
	consistent.Member
	json.Marshaler
	json.Unmarshaler
	HeartbeatDetector

	GetID() ID
	GetType() Type
	SetType(typ Type)
	GetCreatedAt() time.Time
	Handle(ctx context.Context, task LogicTask)
}

type OpFunc func(*defaultWorker)

func WithID(id ID) OpFunc {
	return func(dw *defaultWorker) {
		dw.ID = id
	}
}
func WithType(typ Type) OpFunc {
	return func(dw *defaultWorker) {
		dw.typ = typ
	}
}
func WithHandler(h handler) OpFunc {
	return func(dw *defaultWorker) {
		dw.handlers = append(dw.handlers, h)
	}
}
func WithHeartbeatDetector(h func(ctx context.Context) bool) OpFunc {
	return func(dw *defaultWorker) {
		dw.heartbeatDetector = h
	}
}

func New(ops ...OpFunc) Worker {
	var dw defaultWorker
	dw.ID = NewID()
	dw.CreatedAt = time.Now()
	dw.typ = Candidate
	for _, op := range ops {
		op(&dw)
	}
	return &dw
}

func NewFromBytes(bytes []byte) (Worker, error) {
	var dw defaultWorker
	if err := dw.UnmarshalJSON(bytes); err != nil {
		return nil, err
	}
	return &dw, nil
}

type defaultWorker struct {
	ID        ID        `json:"id"`
	CreatedAt time.Time `json:"createdAt"`

	typ  Type
	lock sync.Mutex

	handlers          []handler
	heartbeatDetector func(ctx context.Context) bool
}

type handler func(ctx context.Context, task LogicTask)

func (dw *defaultWorker) UnmarshalJSON(bytes []byte) error {
	type alias defaultWorker
	var a alias
	if err := json.Unmarshal(bytes, &a); err != nil {
		return err
	}
	*dw = *(*defaultWorker)(&a)
	return nil
}
func (dw *defaultWorker) MarshalJSON() ([]byte, error) {
	type alias defaultWorker
	return json.Marshal((*alias)(dw))
}
func (dw *defaultWorker) String() string          { return dw.GetID().String() }
func (dw *defaultWorker) GetID() ID               { return dw.ID }
func (dw *defaultWorker) GetType() Type           { return dw.typ }
func (dw *defaultWorker) SetType(typ Type)        { dw.typ = typ }
func (dw *defaultWorker) GetCreatedAt() time.Time { return dw.CreatedAt }
func (dw *defaultWorker) Handle(ctx context.Context, task LogicTask) {
	dw.lock.Lock()
	handlers := make([]handler, len(dw.handlers))
	copy(handlers, dw.handlers)
	dw.lock.Unlock()

	if len(handlers) == 0 {
		panic(fmt.Errorf("worker have no handler to handle task, workerID: %s, logicTaskID: %s", dw.GetID(), task.GetLogicID()))
	}

	finishChan := make(chan struct{}, len(handlers))
	finishedNum := 0
	wctx, wcancel := context.WithCancel(ctx)
	defer wcancel()
	for _, h := range handlers {
		go func(h handler) {
			h(wctx, task)
			finishChan <- struct{}{}
		}(h)
	}
	for {
		select {
		case <-ctx.Done():
			return
		case <-finishChan:
			finishedNum++
			if finishedNum == len(handlers) {
				close(finishChan)
				return
			}
		}
	}
}
func (dw *defaultWorker) DetectHeartbeat(ctx context.Context) (alive bool) {
	finish := make(chan bool)
	go func() {
		if dw.heartbeatDetector != nil {
			finish <- dw.heartbeatDetector(ctx)
			return
		}
		// default true
		finish <- true
	}()
	for {
		select {
		case <-ctx.Done():
			return false
		case alive := <-finish:
			return alive
		}
	}
}
