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
	"encoding/json"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"sync"
	"time"

	"k8s.io/klog/v2"
)

const (
	MaxFileSize                   = 1 << 21 // 2MB
	DefaultLogDirMode fs.FileMode = 0755
)

// NewEventLogger create an EventWriter, it won't open the underly file until do a log call.
// verbose=0 means no restrictions on verbose
func NewEventLogger(dir string, sizeMB int, verbose int) EventWriter {
	if sizeMB <= 0 {
		klog.Fatalf("sizeMB [%d] should be larger than 0", sizeMB)
	}

	// we should ensure the log directory exist and have write to permission
	if _, err := os.Stat(dir); err != nil && !os.IsNotExist(err) {
		klog.Fatalf("failed to get log directory [%s] fileInfo: %v", dir, err)
	} else if err != nil {
		if err := os.MkdirAll(dir, DefaultLogDirMode); err != nil && !os.IsExist(err) {
			klog.Fatalf("failed to create log directory [%s]: %v", dir, err)
		}
	}
	if err := os.Chmod(dir, DefaultLogDirMode); err != nil {
		klog.Fatalf("failed to chmod log directory [%s]: %v", dir, err)
	}

	return &eventWriter{
		dir:         dir,
		maxFileSize: MaxFileSize,
		maxFileNum:  sizeMB * (1 << 20) / MaxFileSize,
		verbose:     verbose,
	}
}

// NewFluentEventLogger create an EventFluentWriter to simplify the audit.
func NewFluentEventLogger(dir string, sizeMB int, verbose int) EventFluentWriter {
	writer := NewEventLogger(dir, sizeMB, verbose)
	return &eventFluentWriter{writer: writer}
}

func NewEventReader(dir string) EventReader {
	return &eventReader{dir: dir}
}

type eventWriter struct {
	mutex       sync.Mutex
	dir         string
	verbose     int
	maxFileSize int
	maxFileNum  int

	logWriter LogWriter
}

// Log write an event to the underly storage
func (e *eventWriter) Log(verbose int, event *Event) error {
	if e.verbose > 0 && verbose > e.verbose {
		return nil
	}
	if event == nil {
		return nil
	}

	bs, err := json.Marshal(event)
	if err != nil {
		return err
	}

	if e.logWriter == nil {
		err := e.openNewLogWriter()
		if err != nil {
			return err
		}
	}

	e.mutex.Lock()
	defer e.mutex.Unlock()

	err = e.logWriter.Append(bs)
	if err == nil && e.logWriter.Size() >= e.maxFileSize {
		// reset err to rotate status
		err = e.openNewLogWriter()
	}
	return err
}

func (e *eventWriter) openNewLogWriter() error {
	if e.logWriter != nil {
		err := e.logWriter.Close()
		if err != nil {
			klog.Errorf("failed to close log writer: %v", err)
		}
		e.logWriter = nil

		// rename audit.log to audit-2021-04-05-12-05-01.log
		timestamp := time.Now().Local().Format("2006-01-02-15-04-05")
		oldFile := e.dir + "/audit.log"
		newFile := fmt.Sprintf("%s/audit-%s-0.log", e.dir, timestamp)
		for i := 1; i <= 8; i++ {
			_, err := os.Stat(newFile)
			if err != nil && os.IsNotExist(err) {
				break
			}
			newFile = fmt.Sprintf("%s/audit-%s-%d.log", e.dir, timestamp, i)
		}
		err = os.Rename(oldFile, newFile)
		if err != nil {
			klog.Errorf("failed to rename %s to %s", oldFile, newFile)
			// if rename failed, return error
			return err
		}

		// remove the old files
		pattern := fmt.Sprintf("%s/audit-*.log", e.dir)
		files, err := filepath.Glob(pattern)
		if err != nil {
			klog.Errorf("failed to list files %v", pattern)
		} else {
			for i := 0; i < len(files)-e.maxFileNum+1; i++ {
				klog.Infof("clean audit file %v", files[i])
				if err := os.Remove(files[i]); err != nil {
					klog.Errorf("clean audit file %v failed: %v", files[i], err)
				}
			}
		}
	}

	newLogWriter, err := OpenLogWriter(e.dir + "/audit.log")
	if err != nil {
		return err
	}
	e.logWriter = newLogWriter
	return nil
}

// Flush flush events to the underly storage
func (e *eventWriter) Flush() error {
	e.mutex.Lock()
	defer e.mutex.Unlock()

	if e.logWriter != nil {
		return e.logWriter.Flush()
	}
	return nil
}

// Close close the writer
func (e *eventWriter) Close() error {
	e.mutex.Lock()
	defer e.mutex.Unlock()

	if e.logWriter != nil {
		return e.logWriter.Close()
	}
	return nil
}

type eventFluentWriter struct {
	writer EventWriter
}

// V create an eventHelper with Level verbose
func (e *eventFluentWriter) V(verbose int) *EventHelper {
	return &EventHelper{
		verbose: verbose,
		writer:  e.writer,
	}
}

// Flush flush events to the underly storage
func (e *eventFluentWriter) Flush() error {
	return e.writer.Flush()
}

// Close close the underly writer
func (e *eventFluentWriter) Close() error {
	return e.writer.Close()
}

type eventReader struct {
	dir string
}

func (e *eventReader) NewReverseInterator() EventIterator {
	return newReverseEventIterator(e.dir)
}

func newReverseEventIterator(dir string) *reverseEventIterator {
	return &reverseEventIterator{dir: dir}
}

func listAuditfiles(dir string) ([]string, error) {
	var auditFiles []string
	_, err := os.Stat(dir + "/audit.log")
	if err == nil {
		auditFiles = append(auditFiles, dir+"/audit.log")
	}
	pattern := fmt.Sprintf("%s/audit-*.log", dir)
	files, err := filepath.Glob(pattern)
	if err == nil {
		// reverse files
		for i, j := 0, len(files)-1; i < j; i, j = i+1, j-1 {
			files[i], files[j] = files[j], files[i]
		}
		auditFiles = append(auditFiles, files...)
	}
	return auditFiles, nil
}

type reverseEventIterator struct {
	dir        string
	curentFile string
	reader     LogReader
}

func (e *reverseEventIterator) openNextLogReader() error {
	auditFiles, err := listAuditfiles(e.dir)
	if err != nil {
		return err
	}

	prevFile := ""
	for i := 0; i < len(auditFiles); i++ {
		if prevFile == e.curentFile {
			newReader, err := OpenlogReader(auditFiles[i])
			if err != nil {
				return err
			}
			e.curentFile = auditFiles[i]
			e.reader = newReader
			return nil
		}
		prevFile = auditFiles[i]
	}
	return io.EOF
}

func (e *reverseEventIterator) Next() (*Event, error) {
	if e.reader == nil {
		err := e.openNextLogReader()
		if err != nil {
			return nil, err
		}
	}

	bs, err := e.reader.Read()
	if err != nil && err != io.EOF {
		return nil, err
	}
	if err == io.EOF {
		e.reader.Close()
		err = e.openNextLogReader()
		if err != nil {
			return nil, err
		}
		bs, _ = e.reader.Read()
	}

	var event Event
	err = json.Unmarshal(bs, &event)
	if err != nil {
		return nil, err
	}
	return &event, nil
}

func (e *reverseEventIterator) Close() error {
	if e.reader == nil {
		return nil
	}
	return e.reader.Close()
}
