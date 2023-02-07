package fs

import (
	"github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"testing"
)

func TestTaskFsService(t *testing.T) {
	// spider
	spider := &models.Spider{Id: primitive.NewObjectID()}
	err := delegate.NewModelDelegate(spider).Add()
	require.Nil(t, err)

	// task
	task := &models.Task{Id: primitive.NewObjectID(), SpiderId: spider.Id}
	err = delegate.NewModelDelegate(task).Add()
	require.Nil(t, err)

	t.Run("sync-to-workspace", func(t *testing.T) {
		fsSvc, err := NewTaskFsService(task.Id)
		require.Nil(t, err)

		err = fsSvc.GetFsService().SyncToWorkspace()
		require.Nil(t, err)

		require.DirExists(t, fsSvc.GetWorkspacePath())
	})
}
