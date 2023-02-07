package service_test

import (
	"github.com/crawlab-team/crawlab-core/models/delegate"
	models2 "github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestTagService_GetModel(t *testing.T) {
	SetupTest(t)

	node := &models2.Node{
		Name:     "test node",
		IsMaster: true,
	}
	err := delegate.NewModelNodeDelegate(node).Add()
	require.Nil(t, err)

	_, err = service.NewService()
	require.Nil(t, err)
}

func TestTagService_GetModelById(t *testing.T) {
	SetupTest(t)

	node := &models2.Node{
		Name:     "test node",
		IsMaster: true,
	}
	err := delegate.NewModelNodeDelegate(node).Add()
	require.Nil(t, err)

	_, err = service.NewService()
	require.Nil(t, err)
}
