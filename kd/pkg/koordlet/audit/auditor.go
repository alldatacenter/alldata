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
	"container/list"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"sync"
	"time"

	"github.com/google/uuid"
	"k8s.io/klog/v2"
)

var (
	// Default is the global object to simplify the cost of use, instead of frequently passing objects.
	Default = NewEmptyAuditor()
)

func NewAuditor(c *Config) Auditor {
	logWriter := NewFluentEventLogger(c.LogDir, c.MaxDiskSpaceMB, c.Verbose)
	logReader := NewEventReader(c.LogDir)
	return &auditor{
		config:        c,
		logWriter:     logWriter,
		logReader:     logReader,
		activeReaders: list.New(),
	}
}

func NewEmptyAuditor() Auditor {
	return &emptyAuditor{}
}

type Auditor interface {
	Run(stopCh <-chan struct{}) error
	LoggerWriter() EventFluentWriter
	HttpHandler() func(http.ResponseWriter, *http.Request)
}

type JsonResponse struct {
	NextPageToken string
	Events        []*Event
}

type readerContext struct {
	mutex           sync.Mutex
	pageToken       string
	refreshAt       time.Time
	reverseIterator EventIterator
	closed          bool
}

type auditor struct {
	config    *Config
	logWriter EventFluentWriter
	logReader EventReader

	activeReadersMutex sync.Mutex
	activeReaders      *list.List
}

func (a *auditor) LoggerWriter() EventFluentWriter {
	return a.logWriter
}

func (a *auditor) findActiveReader(token string) *readerContext {
	a.activeReadersMutex.Lock()
	defer a.activeReadersMutex.Unlock()
	for e := a.activeReaders.Front(); e != nil; e = e.Next() {
		if e.Value.(*readerContext).pageToken == token {
			return e.Value.(*readerContext)
		}
	}
	return nil
}

func (a *auditor) pushActiveReader(reader *readerContext) {
	a.activeReadersMutex.Lock()
	a.activeReaders.PushBack(reader)
	expiredReaders := a.popExpiredReaderNoLock()
	a.activeReadersMutex.Unlock()
	// gc the expired readers outside the lock
	a.gcExpiredReaders(expiredReaders)
}

func (a *auditor) popExpiredReaderNoLock() []*readerContext {
	var expired []*readerContext
	minExpired := a.activeReaders.Len() - a.config.MaxConcurrentReaders
	now := time.Now()
	for e := a.activeReaders.Front(); e != nil; e = e.Next() {
		if minExpired > 0 || now.After(e.Value.(*readerContext).refreshAt.Add(a.config.ActiveReaderTTL)) {
			a.activeReaders.Remove(e)
			expired = append(expired, e.Value.(*readerContext))
		}

		minExpired--
	}
	return expired
}

func (a *auditor) gcExpiredReaders(expiredReaders []*readerContext) {
	for _, expired := range expiredReaders {
		klog.Infof("reader %v is expired", expired.pageToken)
		expired.mutex.Lock()
		expired.closed = true
		expired.reverseIterator.Close()
		expired.mutex.Unlock()
	}
}

func (a *auditor) HttpHandler() func(http.ResponseWriter, *http.Request) {
	return func(rw http.ResponseWriter, r *http.Request) {
		sizeStr := r.URL.Query().Get("size")
		pageToken := r.URL.Query().Get("pageToken")

		klog.Infof("handle query client=%v pageToken=%v size=%v", r.RemoteAddr, pageToken, sizeStr)

		size := a.config.DefaultEventsLimit
		if s, err := strconv.Atoi(sizeStr); err == nil {
			if s > a.config.MaxEventsLimit {
				http.Error(rw, fmt.Sprintf("size(%v) exceeds the limit(%v)", s, a.config.MaxEventsLimit), http.StatusBadRequest)
				return
			}
			size = s
		}

		var activeReader *readerContext
		if pageToken == "" {
			tokenUUID, err := uuid.NewRandom()
			if err != nil {
				http.Error(rw, "internal error", http.StatusInternalServerError)
				return
			}
			activeReader = &readerContext{
				pageToken:       tokenUUID.String(),
				refreshAt:       time.Now(),
				reverseIterator: a.logReader.NewReverseInterator(),
			}
			a.pushActiveReader(activeReader)
		} else {
			activeReader = a.findActiveReader(pageToken)
			if activeReader == nil {
				http.Error(rw, fmt.Sprintf("invalid pageToken %s", pageToken), http.StatusConflict)
				return
			}
		}

		readEOF := false
		events := make([]*Event, 0, size)
		func() {
			activeReader.mutex.Lock()
			defer activeReader.mutex.Unlock()
			if activeReader.closed {
				http.Error(rw, fmt.Sprintf("reader %v is expired", activeReader.pageToken), http.StatusConflict)
				return
			}
			activeReader.refreshAt = time.Now()
			for i := 0; i < size; i++ {
				event, err := activeReader.reverseIterator.Next()
				if err == io.EOF {
					readEOF = true
					break
				}
				if err != nil {
					klog.V(4).Infof("reader %v failed: %v", activeReader.pageToken, err)
					continue
				}

				events = append(events, event)
			}
		}()

		acceptJson := false
		acceptTypes := r.Header["Accept"]
		for i := range acceptTypes {
			if acceptTypes[i] == "application/json" {
				acceptJson = true
				break
			}
		}
		if acceptJson {
			response := &JsonResponse{Events: events}
			if !readEOF {
				response.NextPageToken = activeReader.pageToken
			}
			data, err := json.Marshal(response)
			if err != nil {
				http.Error(rw, fmt.Sprintf("marshal events failed: %v", err), http.StatusInternalServerError)
				return
			}
			rw.Header().Set("Content-Type", "application/json; charset=utf-8")
			rw.Write(data)
		} else {
			for i := range events {
				data, err := json.Marshal(events[i])
				if err != nil {
					http.Error(rw, fmt.Sprintf("read events failed: %v", err), http.StatusInternalServerError)
					return
				}
				rw.Header().Set("Content-Type", "text/plain; charset=utf-8")
				if !readEOF {
					rw.Header().Set("Next-Page-Token", activeReader.pageToken)
				}
				rw.Write(data)
				rw.Write([]byte{'\n'})
			}
		}
	}
}

func (a *auditor) Run(stopCh <-chan struct{}) error {
	timer := time.NewTicker(a.config.TickerDuration)
	defer timer.Stop()
	for {
		select {
		case <-stopCh:
			return nil
		case <-timer.C:
			a.activeReadersMutex.Lock()
			expiredReaders := a.popExpiredReaderNoLock()
			a.activeReadersMutex.Unlock()
			// gc the expired readers outside the lock
			a.gcExpiredReaders(expiredReaders)
		}
	}
}

// emptyAuditor do nothing to mock Auditor
type emptyAuditor struct {
}

func (a *emptyAuditor) Run(stopCh <-chan struct{}) error {
	return nil
}

func (a *emptyAuditor) LoggerWriter() EventFluentWriter {
	return &emptyEventFluentWriter{}
}

func (a *emptyAuditor) HttpHandler() func(http.ResponseWriter, *http.Request) {
	return func(rw http.ResponseWriter, r *http.Request) {}
}

type emptyEventFluentWriter struct {
}

func (e *emptyEventFluentWriter) V(verbose int) *EventHelper {
	return &EventHelper{verbose: verbose, writer: &emptyEventWriter{}}
}

func (e *emptyEventFluentWriter) Flush() error {
	return nil
}

func (e *emptyEventFluentWriter) Close() error {
	return nil
}

type emptyEventWriter struct {
}

func (e *emptyEventWriter) Log(verbose int, event *Event) error {
	return nil
}

func (e *emptyEventWriter) Flush() error {
	return nil
}

func (e *emptyEventWriter) Close() error {
	return nil
}

// SetupDefaultAuditor initialize the `Default` auditor.
func SetupDefaultAuditor(c *Config, stopCh <-chan struct{}) {
	Default = NewAuditor(c)
	go Default.Run(stopCh)
}

// V create an EventHelper with Level verbose to record audit events with the `Default` auditor.
func V(verbose int) *EventHelper {
	return Default.LoggerWriter().V(verbose)
}

// HttpHandler return the http handler to read audit events with the `Default` auditor.
func HttpHandler() func(http.ResponseWriter, *http.Request) {
	return Default.HttpHandler()
}
