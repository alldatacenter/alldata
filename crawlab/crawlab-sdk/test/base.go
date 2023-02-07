package test

import (
	"context"
	sdk "github.com/crawlab-team/crawlab-sdk"
	"github.com/crawlab-team/crawlab-sdk/interfaces"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"os"
	"testing"
	"time"
)

func init() {
	var err error
	T, err = NewTest()
	if err != nil {
		panic(err)
	}
}

var T *Test

type Test struct {
	c               *mongo.Client
	db              *mongo.Database
	col             *mongo.Collection
	colDc           *mongo.Collection
	TestDbName      string
	TestColName     string
	TestGrpcAddress string
	TestGrpcAuthKey string
	TestTaskId      primitive.ObjectID
	TestSpiderId    primitive.ObjectID
	TestDcId        primitive.ObjectID
	TestDc          bson.M
	resultSvc       interfaces.ResultService
}

func (t *Test) Setup(t2 *testing.T) {
	if err := t.c.Connect(context.Background()); err != nil {
		panic(err)
	}
	if _, err := t.colDc.InsertOne(context.Background(), t.TestDc); err != nil {
		panic(err)
	}
	if _, err := t.db.Collection("tasks").InsertOne(context.Background(), bson.M{"_id": t.TestTaskId, "spider_id": t.TestSpiderId}); err != nil {
		panic(err)
	}
	if _, err := t.db.Collection("spiders").InsertOne(context.Background(), bson.M{"_id": t.TestSpiderId, "col_id": t.TestDcId}); err != nil {
		panic(err)
	}
	if _, err := t.db.Collection("artifacts").InsertOne(context.Background(), bson.M{"_id": t.TestSpiderId}); err != nil {
		panic(err)
	}
	if err := os.Setenv(sdk.TaskIdEnv, t.TestTaskId.Hex()); err != nil {
		panic(err)
	}
	if err := os.Setenv(sdk.GrpcAddressEnv, t.TestGrpcAddress); err != nil {
		panic(err)
	}
	if err := os.Setenv(sdk.GrpcAuthKeyEnv, t.TestGrpcAuthKey); err != nil {
		panic(err)
	}
	if err := os.Setenv("mongo.db", "crawlab_test"); err != nil {
		panic(err)
	}
	t.resultSvc = sdk.GetResultService()
	time.Sleep(100 * time.Millisecond)
	t2.Cleanup(t.Cleanup)
}

func (t *Test) Cleanup() {
	_ = t.col.Drop(context.Background())
	_, _ = t.colDc.DeleteOne(context.Background(), bson.M{"_id": t.TestDcId})
	_, _ = t.db.Collection("spiders").DeleteOne(context.Background(), bson.M{"_id": t.TestSpiderId})
	_, _ = t.db.Collection("tasks").DeleteOne(context.Background(), bson.M{"_id": t.TestTaskId})
	_ = t.c.Disconnect(context.Background())
}

func NewTest() (t *Test, err error) {
	t = &Test{}

	t.TestDbName = "crawlab_test"
	t.TestColName = "test_results"
	t.TestGrpcAddress = "localhost:9999"
	t.TestGrpcAuthKey = "Crawlab2021!"
	t.TestTaskId = primitive.NewObjectID()
	t.TestSpiderId = primitive.NewObjectID()
	t.TestDcId = primitive.NewObjectID()
	t.TestDc = bson.M{
		"_id":  t.TestDcId,
		"name": t.TestColName,
	}

	t.c, err = mongo.NewClient()
	if err != nil {
		return nil, err
	}
	t.db = t.c.Database(t.TestDbName)
	t.col = t.db.Collection(t.TestColName)
	t.colDc = t.db.Collection("data_collections")

	return t, nil
}
