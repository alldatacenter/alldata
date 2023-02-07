package test

import (
	"testing"
)

func TestMain(m *testing.M) {
	m.Run()

	_ = T.modelSvc.DropAll()
	_ = T.client.Stop()
}
