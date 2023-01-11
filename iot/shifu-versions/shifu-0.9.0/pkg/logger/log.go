package logger

import (
	"os"
	"sync"

	"go.uber.org/zap"
)

var zlog *zap.SugaredLogger
var once sync.Once

func init() {
	once.Do(func() {
		zlog = NewLogger()
		zlog = zlog.WithOptions(zap.AddCallerSkip(1))
	})
}

func NewLogger() *zap.SugaredLogger {
	atom := zap.NewAtomicLevel()
	logLevel := os.Getenv("LOG_LEVEL")

	switch logLevel {
	case "debug":
		atom.SetLevel(zap.DebugLevel)
	case "warning":
		atom.SetLevel(zap.WarnLevel)
	case "error":
		atom.SetLevel(zap.ErrorLevel)
	default:
		atom.SetLevel(zap.InfoLevel)
	}

	cfg := zap.NewProductionConfig()
	cfg.Level = atom
	logger := zap.Must(cfg.Build())
	sugaredLogger := logger.Sugar()
	return sugaredLogger
}

func Debugf(format string, args ...interface{}) {
	zlog.Debugf(format, args...)
}

func Infof(format string, args ...interface{}) {
	zlog.Infof(format, args...)
}

func Warnf(format string, args ...interface{}) {
	zlog.Warnf(format, args...)
}

func Errorf(format string, args ...interface{}) {
	zlog.Errorf(format, args...)
}

func Fatalf(format string, args ...interface{}) {
	zlog.Fatalf(format, args...)
}

func Debug(args ...interface{}) {
	zlog.Debug(args...)
}

func Info(args ...interface{}) {
	zlog.Info(args...)
}

func Warn(args ...interface{}) {
	zlog.Warn(args...)
}

func Error(args ...interface{}) {
	zlog.Error(args...)
}

func Fatal(args ...interface{}) {
	zlog.Fatal(args...)
}

func Debugln(args ...interface{}) {
	zlog.Debugln(args...)
}

func Infoln(args ...interface{}) {
	zlog.Infoln(args...)
}

func Warnln(args ...interface{}) {
	zlog.Warnln(args...)
}

func Errorln(args ...interface{}) {
	zlog.Errorln(args...)
}

func Fatalln(args ...interface{}) {
	zlog.Fatal(args...)
}
