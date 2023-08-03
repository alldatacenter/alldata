package main

import (
	"github.com/edgenesis/shifu/pkg/telemetryservice"
)

func main() {
	stop := make(chan struct{})
	telemetryservice.New(stop)
}
