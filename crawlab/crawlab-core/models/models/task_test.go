package models_test

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	models2 "github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-db/mongo"
	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"testing"
)

func TestTask_Add(t *testing.T) {
	var err error
	SetupTest(t)

	task := &models2.Task{}

	err = delegate.NewModelDelegate(task).Add()
	require.Nil(t, err)
	require.NotNil(t, task.Id)
}

func TestTask_Save(t *testing.T) {
	var err error
	SetupTest(t)

	task := &models2.Task{}
	spider := &models2.Spider{
		Name: "test_task",
	}
	err = delegate.NewModelDelegate(spider).Add()
	require.Nil(t, err)

	err = delegate.NewModelDelegate(task).Add()
	require.Nil(t, err)

	task.SpiderId = spider.Id
	err = delegate.NewModelDelegate(task).Save()
	require.Nil(t, err)

	err = mongo.GetMongoCol(interfaces.ModelColNameTask).FindId(task.Id).One(&task)
	require.Nil(t, err)
	require.Equal(t, spider.Id, task.SpiderId)

	err = mongo.GetMongoCol(interfaces.ModelColNameSpider).FindId(task.SpiderId).One(&spider)
	require.Nil(t, err)
	require.Equal(t, spider.Id, task.SpiderId)
	require.Equal(t, "test_task", spider.Name)
}

func TestTask_Delete(t *testing.T) {
	var err error
	SetupTest(t)

	id := primitive.NewObjectID()
	task := &models2.Task{
		SpiderId: id,
	}

	err = delegate.NewModelDelegate(task).Add()
	require.Nil(t, err)

	err = delegate.NewModelDelegate(task).Delete()
	require.Nil(t, err)

	var a models2.Artifact
	col := mongo.GetMongoCol(interfaces.ModelColNameArtifact)
	err = col.FindId(task.Id).One(&a)
	require.Nil(t, err)
	require.NotNil(t, a.Obj)
	require.True(t, a.Del)

	data, err := bson.Marshal(&a.Obj)
	require.Nil(t, err)
	err = bson.Unmarshal(data, &task)
	require.Nil(t, err)
	require.Equal(t, id, task.SpiderId)
}
