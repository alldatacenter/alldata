package telemetryservice

import (
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

const (
	unitTestServerAddress = "localhost:18927"
)

func TestStart(t *testing.T) {
	testCases := []struct {
		name     string
		mux      *http.ServeMux
		stopChan chan struct{}
		addr     string
	}{
		{
			name:     "case1 pass",
			mux:      http.NewServeMux(),
			addr:     unitTestServerAddress,
			stopChan: make(chan struct{}, 1),
		}, {
			name:     "case2 without mux",
			addr:     unitTestServerAddress,
			stopChan: make(chan struct{}, 1),
		},
	}

	for _, c := range testCases {
		go func() {
			time.Sleep(time.Microsecond * 100)
			c.stopChan <- struct{}{}
		}()
		err := Start(c.stopChan, c.mux, c.addr)
		assert.Nil(t, err)
	}
}

func TestNew(t *testing.T) {
	stop := make(chan struct{}, 1)
	go func() {
		time.After(time.Millisecond * 100)
		stop <- struct{}{}
	}()
	New(stop)
}
