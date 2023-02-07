package test

import (
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/bson"
	"testing"
	"time"
)

func TestAdminService_Run(t *testing.T) {
	var err error
	T.Setup(t)

	// TODO: implement
	// run
	err = T.adminSvc.Schedule(T.TestSpider.Id, &interfaces.SpiderRunOptions{
		Mode: constants.RunTypeRandom,
	})
	require.Nil(t, err)

	// validate task status
	time.Sleep(5 * time.Second)
	task, err := T.modelSvc.GetTask(bson.M{"spider_id": T.TestSpider.Id}, nil)
	require.Nil(t, err)
	require.False(t, task.Id.IsZero())
	require.Equal(t, constants.TaskStatusFinished, task.Status)
}
