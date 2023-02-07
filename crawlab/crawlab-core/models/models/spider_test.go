package models_test

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	models2 "github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-db/mongo"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestSpider_Add(t *testing.T) {
	SetupTest(t)

	s := &models2.Spider{}

	err := delegate.NewModelDelegate(s).Add()
	require.Nil(t, err)
	require.NotNil(t, s.Id)
}

func TestSpider_Save(t *testing.T) {
	SetupTest(t)

	s := &models2.Spider{}

	err := delegate.NewModelDelegate(s).Add()
	require.Nil(t, err)

	name := "test_spider"
	s.Name = name
	err = delegate.NewModelDelegate(s).Save()
	require.Nil(t, err)

	err = mongo.GetMongoCol(interfaces.ModelColNameSpider).FindId(s.Id).One(&s)
	require.Nil(t, err)
	require.Equal(t, name, s.Name)
}

func TestSpider_Delete(t *testing.T) {
	SetupTest(t)

	s := &models2.Spider{
		Name: "test_spider",
	}

	err := delegate.NewModelDelegate(s).Add()
	require.Nil(t, err)

	err = delegate.NewModelDelegate(s).Delete()
	require.Nil(t, err)

	var a models2.Artifact
	col := mongo.GetMongoCol(interfaces.ModelColNameArtifact)
	err = col.FindId(s.Id).One(&a)
	require.Nil(t, err)
	require.NotNil(t, a.Obj)
	require.True(t, a.Del)
}
