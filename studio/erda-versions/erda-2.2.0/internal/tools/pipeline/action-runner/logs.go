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

package actionrunner

import (
	"bytes"
	"fmt"
	"io"
	"time"

	"github.com/sirupsen/logrus"
)

// Logger .
type Logger interface {
	Stdout() io.Writer
	Stderr() io.Writer

	Debug(args ...interface{})
	Info(args ...interface{})
	Warn(args ...interface{})
	Error(args ...interface{})
	Panic(args ...interface{})
	Fatal(args ...interface{})

	Debugf(template string, args ...interface{})
	Infof(template string, args ...interface{})
	Warnf(template string, args ...interface{})
	Errorf(template string, args ...interface{})
	Panicf(template string, args ...interface{})
	Fatalf(template string, args ...interface{})
	Flush()
}

// LogEntry .
type LogEntry struct {
	Source    string            `json:"source"`
	ID        string            `json:"id"`
	Stream    string            `json:"stream"`
	Content   string            `json:"content"`
	Offset    int64             `json:"offset"`
	Timestamp int64             `json:"timestamp"`
	Tags      map[string]string `json:"tags"`
}

var emptyTags = map[string]string{}

type logger struct {
	url       string
	ch        chan *LogEntry
	buf       []*LogEntry
	batchSize int
	flushTime time.Duration
	offset    int64
}

func newLogger(url string, batchSize int, flush time.Duration) *logger {
	log := &logger{
		url:       url,
		ch:        make(chan *LogEntry, 100),
		buf:       make([]*LogEntry, 0, batchSize),
		batchSize: batchSize,
		flushTime: flush,
	}
	go log.loop()
	return log
}

func (l *logger) loop() {
	tick := time.Tick(l.flushTime)
	for {
		select {
		case entry := <-l.ch:
			entry.Offset = l.offset
			l.offset++
			for len(l.buf) >= l.batchSize {
				l.push()
			}
			l.buf = append(l.buf, entry)
		case <-tick:
			l.push()
		}
	}
}

func (l *logger) Add(entry *LogEntry) {
	fmt.Print(entry.Content)
	l.ch <- entry
}

func (l *logger) push() {
	if len(l.buf) <= 0 {
		return
	}
	for i := 0; i < 3; i++ {
		err := l.collectLogs(l.buf)
		if err == nil {
			break
		}
		logrus.Errorf("fail to push log: %s", err)
	}
	l.buf = l.buf[:0]
}

func (r *Runner) newLogger() *logger {
	return newLogger(
		r.Conf.OpenAPI, // modify to address
		15, 3*time.Second,
	)
}

func newJobLogger(log *logger, id, token string) Logger {
	return &jobLogger{
		logger: log,
		id:     id,
		token:  token,
		stdout: &jobStd{
			logger: log,
			id:     id,
			stream: "stdout",
		},
		stderr: &jobStd{
			logger: log,
			id:     id,
			stream: "stderr",
		},
	}
}

type jobLogger struct {
	*logger
	id, token      string
	stdout, stderr *jobStd
}

func (jl *jobLogger) Stdout() io.Writer { return jl.stdout }
func (jl *jobLogger) Stderr() io.Writer { return jl.stderr }

func (jl *jobLogger) print(level string, args ...interface{}) {
	now := time.Now()
	jl.logger.Add(&LogEntry{
		Source:    "job",
		ID:        jl.id,
		Stream:    "stdout",
		Timestamp: now.UnixNano(),
		Content:   fmt.Sprintf("[%s][%s][job] %s\n", level, now.Format(time.RFC3339), fmt.Sprint(args...)),
		Tags:      emptyTags,
	})
}

func (jl *jobLogger) printf(level string, f string, args ...interface{}) {
	now := time.Now()
	jl.logger.Add(&LogEntry{
		Source:    "job",
		ID:        jl.id,
		Stream:    "stdout",
		Timestamp: now.UnixNano(),
		Content:   fmt.Sprintf("[%s][%s][job] %s\n", level, now.Format(time.RFC3339), fmt.Sprintf(f, args...)),
		Tags:      emptyTags,
	})
}

func (jl *jobLogger) Debug(args ...interface{})            { jl.print("DEBUG", args...) }
func (jl *jobLogger) Info(args ...interface{})             { jl.print("INFO", args...) }
func (jl *jobLogger) Warn(args ...interface{})             { jl.print("WARN", args...) }
func (jl *jobLogger) Error(args ...interface{})            { jl.print("ERROR", args...) }
func (jl *jobLogger) Panic(args ...interface{})            { jl.print("PANIC", args...) }
func (jl *jobLogger) Fatal(args ...interface{})            { jl.print("FATAL", args...) }
func (jl *jobLogger) Debugf(f string, args ...interface{}) { jl.printf("DEBUG", f, args...) }
func (jl *jobLogger) Infof(f string, args ...interface{})  { jl.printf("INFO", f, args...) }
func (jl *jobLogger) Warnf(f string, args ...interface{})  { jl.printf("WARN", f, args...) }
func (jl *jobLogger) Errorf(f string, args ...interface{}) { jl.printf("ERROR", f, args...) }
func (jl *jobLogger) Panicf(f string, args ...interface{}) { jl.printf("PANIC", f, args...) }
func (jl *jobLogger) Fatalf(f string, args ...interface{}) { jl.printf("FATAL", f, args...) }

func (jl *jobLogger) Flush() {
	jl.stdout.Flush()
	jl.stderr.Flush()
}

type jobStd struct {
	*logger
	id, stream string
	buf        []byte
}

var lineSpliters = [][]byte{
	{'\r', '\n'},
	{'\r'},
	{'\n'},
}

func (js *jobStd) Write(p []byte) (n int, err error) {
	lines := p
	for {
		idx := -1
		for _, spliter := range lineSpliters {
			idx = bytes.Index(lines, spliter)
			if idx >= 0 {
				break
			}
		}
		if idx < 0 {
			js.buf = append(js.buf, lines...)
			break
		}
		line := lines[0 : idx+1]
		if len(js.buf) > 0 {
			line = append(js.buf, line...)
			js.buf = js.buf[0:0]
		}
		js.logger.Add(&LogEntry{
			Source:    "job",
			ID:        js.id,
			Stream:    js.stream,
			Content:   string(line),
			Timestamp: time.Now().UnixNano(),
			Tags:      emptyTags,
		})
		lines = lines[idx+1:]
	}
	return len(p), nil
}

func (js *jobStd) Flush() (n int) {
	if len(js.buf) > 0 {
		js.logger.Add(&LogEntry{
			Source:    "job",
			ID:        js.id,
			Stream:    js.stream,
			Content:   string(js.buf),
			Timestamp: time.Now().UnixNano(),
			Tags:      emptyTags,
		})
		n = len(js.buf)
		js.buf = js.buf[0:0]
		return n
	}
	return 0
}
