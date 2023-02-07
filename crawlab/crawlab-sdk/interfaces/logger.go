package interfaces

import "github.com/apex/log"

type Logger interface {
	log.Interface
	Log(s string)
	Logf(s string, i ...interface{})
}
