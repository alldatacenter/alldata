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

package websocket

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/igm/sockjs-go.v2/sockjs"

	"github.com/erda-project/erda/apistructs"
)

type Command string

const (
	Attach Command = "__attach"
	Detach Command = "__detach"
)

type Protocol struct {
	Scope   apistructs.Scope `json:"scope"`
	Command Command          `json:"command"`
}

type Event struct {
	Scope   apistructs.Scope `json:"scope"`
	Type    string           `json:"type"`
	Payload interface{}      `json:"payload"`
}

type eventChan struct {
	Scope apistructs.Scope
	EC    chan *Event
}

type Manager struct {
	registerC chan struct {
		Session sockjs.Session
		EC      chan *Event
	}
	unregisterC chan sockjs.Session
	protocolC   chan struct {
		Session  sockjs.Session
		Protocol Protocol
	}
	eventC chan *Event
	stopC  chan struct{}

	sessions map[sockjs.Session]*eventChan

	subscriber *Subscriber
}

func New() (*Manager, error) {
	eventC := make(chan *Event, 0)
	s, err := NewSubscriber(eventC)
	if err != nil {
		logrus.Errorf("websocket subscribe failed, error %v", err)
		return nil, errors.Wrap(err, "failed to create websocket subscriber")
	}
	m := &Manager{
		registerC: make(chan struct {
			Session sockjs.Session
			EC      chan *Event
		}, 0),
		unregisterC: make(chan sockjs.Session, 0),
		protocolC: make(chan struct {
			Session  sockjs.Session
			Protocol Protocol
		}, 0),
		eventC:   eventC,
		sessions: make(map[sockjs.Session]*eventChan),
		stopC:    make(chan struct{}),

		subscriber: s,
	}

	return m, nil
}

func (m *Manager) Start() {
	go func() {
		for {
			select {
			case r := <-m.registerC:
				logrus.Debugf("register channel to manager: %+v", r)
				m.sessions[r.Session] = &eventChan{
					Scope: apistructs.Scope{},
					EC:    r.EC,
				}

			case u := <-m.unregisterC:
				delete(m.sessions, u)
				logrus.Debugf("unregister channel from manager: %p", &u)

			case p := <-m.protocolC:
				switch p.Protocol.Command {
				case Attach:
					if eventChan, ok := m.sessions[p.Session]; ok {
						eventChan.Scope = p.Protocol.Scope
					} else {
						logrus.Warnf("no session found when attach, session: %+v", p.Session)
					}
				case Detach:
					if eventChan, ok := m.sessions[p.Session]; ok {
						eventChan.Scope = apistructs.Scope{}
					} else {
						logrus.Warnf("no session found when detach, session: %+v", p.Session)
					}
				default:
					logrus.Warnf("unknow protocal to manager: %+v", p)
				}

			case e := <-m.eventC:
				logrus.Debugf("event %+v, Websocket sessions number: %d", e, len(m.sessions))

				for _, eventChan := range m.sessions {

					logrus.Debugf("compare entities, attached scope: %+v,  event scope: %+v", eventChan.Scope, e.Scope)
					if eventChan.Scope != e.Scope {
						continue
					}

					select {
					case eventChan.EC <- e:
					default:
						logrus.Warn("drop event")
					}
				}
			case <-m.stopC:
				logrus.Warn("stop manager")
				return
			}
		}
	}()

	go m.subscriber.Start()

	// for testing sockjs
	//m.publishTestEvent()
}

func (m *Manager) Stop() {
	m.subscriber.Stop()
	m.stopC <- struct{}{}
}

func (m *Manager) Handle(session sockjs.Session) {
	defer session.Close(0, "exit")

	var eventChan = m.register(session)
	defer func() {
		m.unregister(session)
	}()

	var closeChan = make(chan struct{})
	go func() {
		defer close(closeChan)

		for {
			msg, err := session.Recv()
			if err != nil {
				logrus.Debug("read input sockjs message err: ", err)
				// return and close
				return
			}
			logrus.Debug("read input sockjs message: ", msg)

			p := Protocol{}
			err = json.Unmarshal([]byte(msg), &p)
			if err != nil {
				logrus.Warn("unmarshal input sockjs message err: ", err)
				// return and close
				return
			}
			m.updateScopeContext(session, p)
		}
	}()

	for {
		select {
		case e, ok := <-eventChan:
			if ok {
				logrus.Debug("new event: ", e)
				event, err := json.Marshal(e)
				if err != nil {
					logrus.Warn("json err: ", err)
				} else {
					event := string(event)
					logrus.Debugf("send event to sockjs session, event: %s, session: %p", event, &session)
					err = session.Send(event)
					if err != nil {
						logrus.Warn("sockjs write failed: ", err)
					}
				}
			} else {
				logrus.Debug("exit sockjs session")
				return
			}
		case <-closeChan:
			logrus.Debug("exit sockjs session")
			return
		}
	}
}

func (m *Manager) register(c sockjs.Session) chan *Event {
	eventChan := make(chan *Event)
	m.registerC <- struct {
		Session sockjs.Session
		EC      chan *Event
	}{c, eventChan}
	return eventChan
}

func (m *Manager) unregister(c sockjs.Session) {
	m.unregisterC <- c
}

func (m *Manager) updateScopeContext(s sockjs.Session, p Protocol) {
	m.protocolC <- struct {
		Session  sockjs.Session
		Protocol Protocol
	}{s, p}
}

// for testing sockjs
func (m *Manager) publishTestEvent() {
	publisher, _ := NewPublisher()
	go func() {
		ticker := time.NewTicker(time.Second * 5)
		for t := range ticker.C {
			event := Event{
				Type: "YY",
				Scope: apistructs.Scope{
					Type: apistructs.AppScope,
					ID:   "100",
				},
				Payload: map[string]string{
					"time": fmt.Sprintf("%v", t),
				},
			}
			err := publisher.EmitEvent(context.Background(), event)
			logrus.Debugf("publish event error %v", err)
		}
	}()
}
