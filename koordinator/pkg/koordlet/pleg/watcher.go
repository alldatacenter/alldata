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

package pleg

import (
	"fmt"
	"runtime"

	"k8s.io/utils/inotify"
)

/*
Watcher implements a wrapper for the Linux inotify system.

Example:
    watcher, err := NewWatcher(path, nil)
    if err != nil {
        log.Fatal(err)
    }
    err = watcher.Watch("/tmp")
    if err != nil {
        log.Fatal(err)
    }
    for {
        select {
        case ev := <-watcher.Event:
            log.Println("event:", ev)
        case err := <-watcher.Error:
            log.Println("error:", err)
        }
    }
*/

type EventType int

const (
	DirCreated EventType = iota
	DirRemoved
	UnknownType
)

var errNotSupported = fmt.Errorf("watch not supported on %s", runtime.GOOS)

type Watcher interface {
	AddWatch(path string) error
	RemoveWatch(path string) error
	Event() chan *inotify.Event
	Error() chan error
	Close() error
}

// for test only
const (
	IN_CREATE = 0x100
	IN_DELETE = 0x200
	IN_ISDIR  = 0x40000000
)
