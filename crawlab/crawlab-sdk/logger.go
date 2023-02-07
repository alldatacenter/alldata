package sdk

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/apex/log"
	"github.com/cenkalti/backoff/v4"
	grpc "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/crawlab-sdk/entity"
	"github.com/crawlab-team/crawlab-sdk/interfaces"
	"github.com/crawlab-team/go-trace"
	"time"
)

var L *Logger

type Logger struct {
	log.Interface

	// internals
	sub grpc.TaskService_SubscribeClient
}

func (l *Logger) Log(s string) {
	l.log("", s)
}

func (l *Logger) Debug(s string) {
	l.log(LogLevelDebug, s)
}

func (l *Logger) Info(s string) {
	l.log(LogLevelInfo, s)
}

func (l *Logger) Warn(s string) {
	l.log(LogLevelWarn, s)
}

func (l *Logger) Error(s string) {
	l.log(LogLevelError, s)
}

func (l *Logger) Fatal(s string) {
	l.log(LogLevelFatal, s)
}

func (l *Logger) Logf(s string, i ...interface{}) {
	l.log("", fmt.Sprintf(s, i...))
}

func (l *Logger) Debugf(s string, i ...interface{}) {
	l.log(LogLevelDebug, fmt.Sprintf(s, i...))
}

func (l *Logger) Infof(s string, i ...interface{}) {
	l.log(LogLevelInfo, fmt.Sprintf(s, i...))
}

func (l *Logger) Warnf(s string, i ...interface{}) {
	l.log(LogLevelWarn, fmt.Sprintf(s, i...))
}

func (l *Logger) Errorf(s string, i ...interface{}) {
	l.log(LogLevelError, fmt.Sprintf(s, i...))
}

func (l *Logger) Fatalf(s string, i ...interface{}) {
	l.log(LogLevelFatal, fmt.Sprintf(s, i...))
}

func (l *Logger) log(level string, s string) {
	if level != "" {
		s = fmt.Sprintf("[%s] %s", level, s)
	}
	data, err := json.Marshal(&entity.StreamMessageTaskData{
		TaskId: GetTaskId(),
		Logs:   []string{s},
	})
	if err != nil {
		trace.PrintError(err)
		return
	}
	if err := l.sub.Send(&grpc.StreamMessage{
		Code: grpc.StreamMessageCode_INSERT_LOGS,
		Data: data,
	}); err != nil {
		trace.PrintError(err)
		return
	}
}

func (l *Logger) init() (err error) {
	op := l._init
	b := backoff.NewExponentialBackOff()
	notify := func(err error, duration time.Duration) {
		log.Errorf("init logger error: %v, re-attempt in %.1f seconds", err, duration.Seconds())
	}
	if err := backoff.RetryNotify(op, b, notify); err != nil {
		return trace.TraceError(err)
	}
	return nil
}

func (l *Logger) _init() (err error) {
	l.sub, err = GetClient().GetTaskClient().Subscribe(context.Background())
	if err != nil {
		return trace.TraceError(err)
	}
	return nil
}

func GetLogger(opts ...LoggerOption) interfaces.Logger {
	if L != nil {
		return L
	}

	// logger
	l := &Logger{Interface: log.Log}

	// apply options
	for _, opt := range opts {
		opt(l)
	}

	// initialize
	if err := l.init(); err != nil {
		panic(err)
	}

	L = l

	return l
}
