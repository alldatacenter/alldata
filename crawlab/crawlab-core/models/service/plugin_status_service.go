package service

import (
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/interfaces"
	models2 "github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-db/mongo"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

func convertTypePluginStatus(d interface{}, err error) (res *models2.PluginStatus, err2 error) {
	if err != nil {
		return nil, err
	}
	res, ok := d.(*models2.PluginStatus)
	if !ok {
		return nil, errors.ErrorModelInvalidType
	}
	return res, nil
}

func (svc *Service) GetPluginStatusById(id primitive.ObjectID) (res *models2.PluginStatus, err error) {
	d, err := svc.GetBaseService(interfaces.ModelIdPluginStatus).GetById(id)
	return convertTypePluginStatus(d, err)
}

func (svc *Service) GetPluginStatus(query bson.M, opts *mongo.FindOptions) (res *models2.PluginStatus, err error) {
	d, err := svc.GetBaseService(interfaces.ModelIdPluginStatus).Get(query, opts)
	return convertTypePluginStatus(d, err)
}

func (svc *Service) GetPluginStatusList(query bson.M, opts *mongo.FindOptions) (res []models2.PluginStatus, err error) {
	l, err := svc.GetBaseService(interfaces.ModelIdPluginStatus).GetList(query, opts)
	for _, doc := range l.GetModels() {
		d := doc.(*models2.PluginStatus)
		res = append(res, *d)
	}
	return res, nil
}
