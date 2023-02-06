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

package kuberneteslogs

import (
	"context"
	"errors"
	"io"
	"math"
	"regexp"
	"strings"
	"time"

	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

type queryFunc func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error)

type cStorage struct {
	log          logs.Logger
	getQueryFunc func(clusterName string) (func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error), error)
	bufferLines  int64
	timeSpan     int64
}

var _ storage.Storage = (*cStorage)(nil)

func (s *cStorage) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	return nil, storekit.ErrOpNotSupported
}

func newPodLogOptions(containerName string, startTime int64, tail int64) *v1.PodLogOptions {
	var sinceTime *metav1.Time
	if startTime > 0 {
		sinceTime = &metav1.Time{Time: time.Unix(startTime/int64(time.Second), startTime%int64(time.Second))}
	}
	var tailLines *int64
	if tail > 0 {
		tailLines = &tail
	}
	return &v1.PodLogOptions{
		Container:                    containerName,
		Follow:                       false,
		Previous:                     false,
		SinceSeconds:                 nil,
		SinceTime:                    sinceTime,
		Timestamps:                   true,
		TailLines:                    tailLines,
		LimitBytes:                   nil,
		InsecureSkipTLSVerifyBackend: true,
	}
}

const (
	defaultBufferLines = 1024
	defaultTimeSpan    = 3 * time.Minute
)

func (s *cStorage) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	var err error
	var podName, containerName, namespace, clusterName, id string

	if sel.Scheme != "container" {
		s.log.Debugf("kubernetes-log not supported query %s of real log", sel.Scheme)
		return storekit.EmptyIterator{}, nil
	}

	live, ok := sel.Options[storage.IsLive]
	if !ok || !live.(bool) {
		s.log.Debugf("kubernetes-log not supported to stop container of real log")
		return storekit.EmptyIterator{}, nil
	}

	matcher := func(data *pb.LogItem, it *logsIterator) bool { return true }
	for _, filter := range sel.Filters {
		if filter.Value == nil {
			continue
		}
		if filter.Key != "content" && filter.Op != storage.EQ {
			s.log.Debugf("%s only support EQ filter, ignore kubernetes logs query", filter.Key)
			return storekit.EmptyIterator{}, nil
		}
		switch filter.Key {
		case "id":
			if len(id) <= 0 {
				id, _ = filter.Value.(string)
			}
		case "source":
			source, _ := filter.Value.(string)
			if len(source) > 0 && source != "container" {
				return storekit.EmptyIterator{}, nil
			}
		case "content":
			switch filter.Op {
			case storage.REGEXP:
				exp, _ := filter.Value.(string)
				if len(exp) > 0 {
					regex, err := regexp.Compile(exp)
					if err != nil {
						s.log.Debugf("invalid regexp %q, ignore kubernetes logs query", exp)
						return storekit.EmptyIterator{}, nil
					}
					matcher = func(data *pb.LogItem, it *logsIterator) bool {
						return regex.MatchString(data.Content)
					}
				}
			case storage.CONTAINS:
				exp, _ := filter.Value.(string)
				if len(exp) > 0 {
					matcher = func(data *pb.LogItem, it *logsIterator) bool {
						return strings.Contains(data.Content, exp)
					}
				}
			case storage.EQ:
				val, _ := filter.Value.(string)
				if len(val) > 0 {
					matcher = func(data *pb.LogItem, it *logsIterator) bool {
						return data.Content == val
					}
				}
			}
		}
	}
	if containerName, ok = sel.Options[storage.ContainerName].(string); !ok || len(containerName) <= 0 {
		return storekit.EmptyIterator{}, nil
	}
	if podName, ok = sel.Options[storage.PodName].(string); !ok || len(containerName) <= 0 {
		return storekit.EmptyIterator{}, nil
	}
	if namespace, ok = sel.Options[storage.PodNamespace].(string); !ok || len(namespace) <= 0 {
		return storekit.EmptyIterator{}, nil
	}
	if clusterName, ok = sel.Options[storage.ClusterName].(string); !ok || len(clusterName) <= 0 {
		return storekit.EmptyIterator{}, nil
	}

	queryFunc, err := s.getQueryFunc(clusterName)
	if err != nil {
		s.log.Debugf("failed to GetClient(%q): %s, ignore kubernetes logs query", clusterName, err)
		return storekit.EmptyIterator{}, nil
	}

	bufferLines := s.bufferLines
	if bufferLines <= 0 {
		bufferLines = defaultBufferLines
	}
	timeSpan := s.timeSpan
	if timeSpan <= 0 {
		timeSpan = int64(defaultTimeSpan)
	}
	return &logsIterator{
		ctx:           ctx,
		sel:           sel,
		debug:         sel.Debug,
		id:            id,
		podNamespace:  namespace,
		podName:       podName,
		containerName: containerName,
		matcher:       matcher,
		queryFunc:     queryFunc,
		pageSize:      bufferLines,
		timeSpan:      timeSpan,
		log:           s.log,
	}, nil
}

type iteratorDir int8

const (
	iteratorInitial = iota
	iteratorForward
	iteratorBackward
)

type logsIterator struct {
	ctx           context.Context
	sel           *storage.Selector
	debug         bool
	id            string
	podNamespace  string
	podName       string
	containerName string
	matcher       func(data *pb.LogItem, it *logsIterator) bool

	queryFunc queryFunc
	pageSize  int64
	timeSpan  int64
	dir       iteratorDir

	lastStartTime int64
	lastEndTime   int64

	lastEndTimestamp int64
	lastEndOffset    int64

	buffer []*pb.LogItem
	offset int
	value  *pb.LogItem
	err    error
	closed bool

	log logs.Logger
}

func (it *logsIterator) First() bool {
	if it.checkClosed() && it.Error() != nil {
		return false
	}
	it.dir = iteratorForward
	it.fetch(it.sel.Start, it.pageSize, false)
	if it.offset < len(it.buffer) {
		it.value = it.buffer[it.offset]
		it.offset++
		return true
	}
	return false
}

func (it *logsIterator) Last() bool {
	if it.checkClosed() && it.Error() != nil {
		return false
	}
	it.dir = iteratorBackward
	startTime := it.sel.End - it.timeSpan
	if startTime < it.sel.Start {
		startTime = it.sel.Start
	}
	it.fetch(startTime, -1, true)
	if it.offset >= 0 && it.offset < len(it.buffer) {
		it.value = it.buffer[it.offset]
		it.offset--
		return true
	}
	return false
}

func (it *logsIterator) Next() bool {
	if it.checkClosed() {
		return false
	}
	if it.dir == iteratorBackward {
		it.err = storekit.ErrOpNotSupported
		return false
	}
	it.dir = iteratorForward
	if it.offset >= 0 && it.offset < len(it.buffer) {
		it.value = it.buffer[it.offset]
		it.offset++
		return true
	}
	if it.err != nil {
		return false
	}
	if it.lastEndTime != 0 {
		if it.lastEndTime <= it.sel.End {
			originLastEndTime := it.lastEndTime
			it.fetch(it.lastEndTime, it.pageSize, false)
			if it.lastEndTime == originLastEndTime {
				it.log.Infof("timespan :%v is no data, skip [%v]", it.lastEndTime, it.timeSpan)
				// May return ineligible data, maybe data is too long, or time format is failed, skip timespan
				it.lastEndTime = it.lastEndTime + it.timeSpan
			}
		}
	} else {
		it.fetch(it.sel.Start, it.pageSize, false)
		if it.lastEndTime == 0 {
			it.log.Infof("timespan :%v is no data, skip [%v]", it.sel.Start, it.timeSpan)
			// May return ineligible data, maybe data is too long, or time format is failed, skip timespan
			it.lastEndTime = it.sel.Start + it.timeSpan
		}
	}
	if it.offset >= 0 && it.offset < len(it.buffer) {
		it.value = it.buffer[it.offset]
		it.offset++
		return true
	}
	return false
}

func (it *logsIterator) Prev() bool {
	if it.checkClosed() {
		return false
	}
	if it.dir == iteratorForward {
		it.err = storekit.ErrOpNotSupported
		return false
	}
	it.dir = iteratorBackward
	if it.offset >= 0 && it.offset < len(it.buffer) {
		it.value = it.buffer[it.offset]
		it.offset--
		return true
	}
	if it.err != nil {
		return false
	}
	if it.lastStartTime != 0 {
		if it.lastStartTime > it.sel.Start {
			startTime := it.lastStartTime - it.timeSpan
			if startTime < it.sel.Start {
				startTime = it.sel.Start
			}
			it.fetch(startTime, -1, true)
		}
	} else {
		v, ok := it.sel.Options[storage.IsFirstQuery]

		if ok && v.(bool) {
			it.fetchByTailLine(it.sel.Start, it.pageSize, true)
		} else {
			startTime := it.sel.End - it.timeSpan
			if startTime < it.sel.Start {
				startTime = it.sel.Start
			}
			it.fetch(startTime, -1, true)
		}
	}
	if it.offset >= 0 && it.offset < len(it.buffer) {
		it.value = it.buffer[it.offset]
		it.offset--
		return true
	}
	return false
}

func (it *logsIterator) Value() interface{} { return it.value }
func (it *logsIterator) Error() error {
	if it.err == io.EOF {
		return nil
	}
	return it.err
}

func (it *logsIterator) Close() error {
	it.closed = true
	return nil
}

func (it *logsIterator) checkClosed() bool {
	if it.closed {
		if it.err == nil {
			it.err = storekit.ErrIteratorClosed
		}
		return true
	}
	select {
	case <-it.ctx.Done():
		if it.err == nil {
			it.err = storekit.ErrIteratorClosed
		}
		return true
	default:
	}
	return false
}

var errLimited = errors.New("limited")

const initialOffset = math.MinInt64

func (it *logsIterator) fetchByTailLine(startTime int64, limit int64, backward bool) {
	opts := newPodLogOptions(it.containerName, startTime, limit)
	reader, err := it.queryFunc(it, opts)

	if err != nil {
		it.err = err
		return
	}
	defer reader.Close()

	var (
		minTime, maxTime int64 = math.MaxInt64, 0
	)

	var lastTimestamp, offset int64 = -1, initialOffset

	err = parseLines(reader, func(line []byte) error {
		text := string(line)
		data, content := parseLine(text, it)

		if data.UnixNano != lastTimestamp {
			lastTimestamp = data.UnixNano
			offset = initialOffset
		} else {
			offset++
		}
		data.Offset = offset

		if data.UnixNano > 0 {
			if data.UnixNano < minTime {
				minTime = data.UnixNano
			}
			if data.UnixNano > maxTime {
				maxTime = data.UnixNano
			}
			it.lastEndTimestamp, it.lastEndOffset = data.UnixNano, data.Offset

			parseContent(content, data)
			if !it.matcher(data, it) {
				return nil
			}
			it.buffer = append(it.buffer, data)
		}

		return nil
	})
	if err != nil && !errors.Is(err, io.EOF) {
		it.err = err
		return
	}
	if backward {
		it.offset = len(it.buffer) - 1
	} else {
		it.offset = 0
	}

	it.lastStartTime = minTime
	if startTime < it.lastStartTime {
		it.lastStartTime = startTime
	}
	if maxTime != 0 && (it.lastEndTime == 0 || !backward) {
		it.lastEndTime = maxTime
	}
}

func (it *logsIterator) fetch(startTime, limit int64, backward bool) {
	it.buffer = nil
	endTime := it.sel.End
	var lastTimestamp, offset int64 = -1, initialOffset
	for it.err == nil && len(it.buffer) <= 0 && startTime >= it.sel.Start {
		func() error {
			opts := newPodLogOptions(it.containerName, startTime, 0)
			reader, err := it.queryFunc(it, opts)
			if err != nil {
				it.err = err
				return it.err
			}
			defer reader.Close()
			var (
				minTime, maxTime int64 = math.MaxInt64, 0
			)
			err = parseLines(reader, func(line []byte) error {
				text := string(line)
				data, content := parseLine(text, it)
				if data.UnixNano != lastTimestamp {
					lastTimestamp = data.UnixNano
					offset = initialOffset
				} else {
					offset++
				}
				data.Offset = offset
				if data.UnixNano > 0 {
					if data.UnixNano < minTime {
						minTime = data.UnixNano
					}
					if data.UnixNano > maxTime {
						maxTime = data.UnixNano
					}
					if backward {
						if data.UnixNano < it.sel.Start {
							return io.EOF
						}
						if endTime <= data.UnixNano || (it.lastStartTime != 0 && it.lastStartTime <= data.UnixNano) {
							if startTime <= it.sel.Start {
								return io.EOF
							}
							return errLimited
						}
					} else {
						if data.UnixNano < it.sel.Start {
							// continue
							return nil
						}
						if it.sel.End <= data.UnixNano {
							return io.EOF
						}
					}
					if int(limit) > 0 && len(it.buffer) >= int(limit) &&
						data.UnixNano != it.buffer[len(it.buffer)-1].UnixNano {
						return errLimited
					}
				}
				parseContent(content, data)
				if it.lastEndTimestamp == data.UnixNano && it.lastEndOffset == data.Offset {
					// remove duplicate
					return nil
				}
				it.lastEndTimestamp, it.lastEndOffset = data.UnixNano, data.Offset

				if !it.matcher(data, it) {
					return nil
				}
				it.buffer = append(it.buffer, data)
				return nil
			})
			it.lastStartTime = minTime
			if startTime < it.lastStartTime {
				it.lastStartTime = startTime
			}
			if maxTime != 0 && (it.lastEndTime == 0 || !backward) {
				it.lastEndTime = maxTime
			}
			if backward {
				startTime = it.lastStartTime - it.timeSpan
				if startTime < it.sel.Start {
					startTime = it.sel.Start
				}
				if maxTime != 0 {
					endTime = maxTime
				}
			}
			if err != nil && err != errLimited {
				it.err = err
				return it.err
			}
			return nil
		}()
	}
	if backward {
		it.offset = len(it.buffer) - 1
	} else {
		it.offset = 0
	}
}
