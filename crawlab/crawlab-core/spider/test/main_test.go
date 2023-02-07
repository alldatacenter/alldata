package test

import (
	"github.com/crawlab-team/crawlab-fs/test"
	"testing"
)

func TestMain(m *testing.M) {
	if err := test.StartTestSeaweedFs(); err != nil {
		panic(err)
	}

	m.Run()

	if err := test.StopTestSeaweedFs(); err != nil {
		panic(err)
	}
}
