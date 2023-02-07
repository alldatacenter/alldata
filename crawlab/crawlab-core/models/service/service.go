package service

import (
	"context"
	"github.com/crawlab-team/crawlab-core/color"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-db/mongo"
	"go.mongodb.org/mongo-driver/bson"
	mongo2 "go.mongodb.org/mongo-driver/mongo"
	"go.uber.org/dig"
)

type Service struct {
	env      string
	colorSvc interfaces.ColorService
}

func (svc *Service) DropAll() (err error) {
	db := mongo.GetMongoDb("")
	colNames, err := db.ListCollectionNames(context.Background(), bson.M{})
	if err != nil {
		if err == mongo2.ErrNoDocuments {
			return nil
		}
		return err
	}
	for _, colName := range colNames {
		col := db.Collection(colName)
		if err := col.Drop(context.Background()); err != nil {
			return err
		}
	}
	return nil
}

func (svc *Service) GetBaseService(id interfaces.ModelId) (svc2 interfaces.ModelBaseService) {
	return GetBaseService(id)
}

func NewService(opts ...Option) (svc2 ModelService, err error) {
	// service
	svc := &Service{}

	// options
	for _, opt := range opts {
		opt(svc)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(color.NewService); err != nil {
		return nil, err
	}
	if err := c.Invoke(func(colorSvc interfaces.ColorService) {
		svc.colorSvc = colorSvc
	}); err != nil {
		return nil, err
	}

	return svc, nil
}

var modelSvc ModelService

func GetService(opts ...Option) (svc ModelService, err error) {
	if modelSvc != nil {
		return modelSvc, nil
	}
	return NewService(opts...)
}
