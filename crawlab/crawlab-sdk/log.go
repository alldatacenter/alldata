package sdk

func Log(s string) {
	GetLogger().Log(s)
}

func Debug(s string) {
	GetLogger().Debug(s)
}

func Info(s string) {
	GetLogger().Info(s)
}

func Warn(s string) {
	GetLogger().Warn(s)
}

func Error(s string) {
	GetLogger().Error(s)
}

func Fatal(s string) {
	GetLogger().Fatal(s)
}

func Logf(s string, i ...interface{}) {
	GetLogger().Logf(s, i...)
}

func Debugf(s string, i ...interface{}) {
	GetLogger().Debugf(s, i...)
}

func Infof(s string, i ...interface{}) {
	GetLogger().Infof(s, i...)
}

func Warnf(s string, i ...interface{}) {
	GetLogger().Warnf(s, i...)
}

func Errorf(s string, i ...interface{}) {
	GetLogger().Errorf(s, i...)
}

func Fatalf(s string, i ...interface{}) {
	GetLogger().Fatalf(s, i...)
}
