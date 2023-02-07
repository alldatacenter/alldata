package sdk

import (
	"github.com/apex/log"
	"github.com/crawlab-team/crawlab-sdk/interfaces"
)

type LoggerOption func(l log.Interface)

type ClientOption func(c interfaces.Client)

type ResultServiceOption func(svc interfaces.ResultService)
