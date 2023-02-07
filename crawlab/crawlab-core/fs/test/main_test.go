package test

import (
	"github.com/crawlab-team/crawlab-fs/test"
	"testing"
)

func TestMain(m *testing.M) {
	// before test
	if err := test.StartTestSeaweedFs(); err != nil {
		panic(err)
	}

	// test
	m.Run()

	// after test
	_ = test.StopTestSeaweedFs()
}
