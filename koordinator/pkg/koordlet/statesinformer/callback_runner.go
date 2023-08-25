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

package statesinformer

import (
	"k8s.io/klog/v2"
)

type RegisterType int64

const (
	RegisterTypeNodeSLOSpec RegisterType = iota
	RegisterTypeAllPods
	RegisterTypeNodeTopology
)

func (r RegisterType) String() string {
	switch r {
	case RegisterTypeNodeSLOSpec:
		return "RegisterTypeNodeSLOSpec"
	case RegisterTypeAllPods:
		return "RegisterTypeAllPods"
	case RegisterTypeNodeTopology:
		return "RegisterTypeNodeTopology"

	default:
		return "RegisterTypeUnknown"
	}
}

type updateCallback struct {
	name        string
	description string
	fn          UpdateCbFn
}

type UpdateCbCtx struct{}
type UpdateCbFn func(t RegisterType, obj interface{}, pods []*PodMeta)

type callbackRunner struct {
	callbackChans        map[RegisterType]chan UpdateCbCtx
	stateUpdateCallbacks map[RegisterType][]updateCallback
	statesInformer       StatesInformer
}

func NewCallbackRunner() *callbackRunner {
	c := &callbackRunner{}
	c.callbackChans = map[RegisterType]chan UpdateCbCtx{
		RegisterTypeNodeSLOSpec:  make(chan UpdateCbCtx, 1),
		RegisterTypeAllPods:      make(chan UpdateCbCtx, 1),
		RegisterTypeNodeTopology: make(chan UpdateCbCtx, 1),
	}
	c.stateUpdateCallbacks = map[RegisterType][]updateCallback{
		RegisterTypeNodeSLOSpec:  {},
		RegisterTypeAllPods:      {},
		RegisterTypeNodeTopology: {},
	}
	return c
}

func (c *callbackRunner) Setup(s StatesInformer) {
	c.statesInformer = s
}

func (s *callbackRunner) RegisterCallbacks(rType RegisterType, name, description string, callbackFn UpdateCbFn) {
	callbacks, legal := s.stateUpdateCallbacks[rType]
	if !legal {
		klog.Fatalf("states informer callback register with type %v is illegal", rType.String())
	}
	for _, c := range callbacks {
		if c.name == name {
			klog.Fatalf("states informer callback register %s with type %v already registered", name, rType.String())
		}
	}
	newCb := updateCallback{
		name:        name,
		description: description,
		fn:          callbackFn,
	}
	s.stateUpdateCallbacks[rType] = append(s.stateUpdateCallbacks[rType], newCb)
	klog.V(1).Infof("states informer callback %s has registered for type %v", name, rType.String())
}

func (s *callbackRunner) SendCallback(objType RegisterType) {
	if _, exist := s.callbackChans[objType]; exist {
		select {
		case s.callbackChans[objType] <- struct{}{}:
			return
		default:
			klog.Infof("last callback runner %v has not finished, ignore this time", objType.String())
		}
	} else {
		klog.Warningf("callback runner %v is not exist", objType.String())
	}
}

func (s *callbackRunner) runCallbacks(objType RegisterType, obj interface{}) {
	callbacks, exist := s.stateUpdateCallbacks[objType]
	if !exist {
		klog.Errorf("states informer callbacks type %v not exist", objType.String())
		return
	}
	pods := s.statesInformer.GetAllPods()
	for _, c := range callbacks {
		klog.V(5).Infof("start running callback function %v for type %v", c.name, objType.String())
		c.fn(objType, obj, pods)
	}
}

func (s *callbackRunner) Start(stopCh <-chan struct{}) {
	for t := range s.callbackChans {
		cbType := t
		go func() {
			for {
				select {
				case cbCtx := <-s.callbackChans[cbType]:
					cbObj := s.getObjByType(cbType, cbCtx)
					if cbObj == nil {
						klog.Warningf("callback runner with type %v is not exist")
					} else {
						s.runCallbacks(cbType, cbObj)
					}
				case <-stopCh:
					klog.Infof("callback runner %v loop is exited", cbType.String())
					return
				}
			}
		}()
	}
}

func (s *callbackRunner) getObjByType(objType RegisterType, cbCtx UpdateCbCtx) interface{} {
	switch objType {
	case RegisterTypeNodeSLOSpec:
		nodeSLO := s.statesInformer.GetNodeSLO()
		if nodeSLO != nil {
			return &nodeSLO.Spec
		}
		return nil
	case RegisterTypeAllPods:
		return &struct{}{}
	case RegisterTypeNodeTopology:
		return s.statesInformer.GetNodeTopo()
	}
	return nil
}
