package service_test

import (
	"fmt"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	models2 "github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/bson"
	"testing"
)

func TestNodeService_GetModelById(t *testing.T) {
	SetupTest(t)

	node := &models2.Node{
		Name:     "test node",
		IsMaster: true,
	}
	err := delegate.NewModelDelegate(node).Add()
	require.Nil(t, err)

	svc, err := service.NewService()
	require.Nil(t, err)

	node, err = svc.GetNodeById(node.Id)
	require.Nil(t, err)
}

func TestNodeService_GetModel(t *testing.T) {
	SetupTest(t)

	node := &models2.Node{
		Name:     "test node",
		IsMaster: true,
	}
	err := delegate.NewModelDelegate(node).Add()
	require.Nil(t, err)

	svc, err := service.NewService()
	require.Nil(t, err)

	node, err = svc.GetNode(bson.M{"name": "test node"}, nil)
	require.Nil(t, err)
	require.False(t, node.Id.IsZero())
}

func TestNodeService_GetModelList(t *testing.T) {
	SetupTest(t)

	n := 10
	for i := 0; i < n; i++ {
		node := &models2.Node{
			Name:     fmt.Sprintf("test node %d", i),
			IsMaster: true,
		}
		err := delegate.NewModelDelegate(node).Add()
		require.Nil(t, err)
	}

	svc, err := service.NewService()
	require.Nil(t, err)

	nodes, err := svc.GetNodeList(nil, nil)
	require.Nil(t, err)

	for i := 0; i < n; i++ {
		node := nodes[i]
		require.False(t, node.Id.IsZero())
	}
}
