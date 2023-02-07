package test

import (
	"context"
	"github.com/crawlab-team/crawlab-sdk/entity"
	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/options"
	"testing"
	"time"
)

func TestResultService_SaveItem_SaveItems(t *testing.T) {
	var err error
	T.Setup(t)

	n := 10
	b := 100
	for i := 0; i < n; i++ {
		var items []entity.Result
		for j := 0; j < b; j++ {
			item := entity.Result{
				"key": i*b + j,
			}
			items = append(items, item)
		}
		if i%2 == 0 {
			T.resultSvc.SaveItem(items...)
		} else {
			T.resultSvc.SaveItems(items)
		}
	}

	time.Sleep(1000 * time.Millisecond)

	// validate
	fr, err := T.col.Find(context.Background(), bson.M{}, &options.FindOptions{
		Sort: bson.D{{"_id", 1}},
	})
	require.Nil(t, err)
	var results []entity.Result
	err = fr.All(context.Background(), &results)
	require.Nil(t, err)
	require.Equal(t, n*b, len(results))
	for i, r := range results {
		v, ok := r["key"]
		require.True(t, ok)
		vInt, ok := v.(float64)
		require.True(t, ok)
		require.Equal(t, i, int(vInt))
	}
}
