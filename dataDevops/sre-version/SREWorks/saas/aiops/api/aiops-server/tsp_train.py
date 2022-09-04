#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging
from common.celery_factory import make_celery
from utils.train_id_generator import create_train_id
from ai_lib.time_series_prediction.api.train_wrapper import TrainWrapper
from models.tsp_result_model import TSPResultModel
from utils.data_source.es_adapter import EsAdapter

logger = logging.getLogger(__name__)

celery = make_celery()


@celery.task()
def async_train(metric, tsp_config, metric_instances):
    source_type = metric['source_type']
    adapter = None
    # TODO 可扩展支持其他的数据源适配器
    if source_type == 'es':
        adapter = EsAdapter(metric['source_id'], metric['source_table'])
    if adapter is None:
        raise ValueError("数据源类型%s暂时不支持" % source_type)

    tsp_result_model = TSPResultModel()
    for metric_instance in metric_instances:
        df_series_data = adapter.get_metric_data(tsp_config['default_train_hours'], tsp_config['min_train_hours'],
                                                 tsp_config['series_interval'], metric_instance['index_path'],
                                                 metric_instance['index_tags'])
        logger.info("train data size:%s" % len(df_series_data))
        logger.info((df_series_data.to_dict("records")))
        train_id = create_train_id()
        logger.info("train_id:%s" % train_id)
        tsp_result, new_algorithm_params, data_len = TrainWrapper.train(
            df_series_data, train_id, metric_instance['id'], tsp_config['algorithm_name'],
            tsp_config['algorithm_params'], tsp_config['preprocess_param'], tsp_config['series_interval'],
            tsp_config['horizon'])

        tsp_result_model.save_result(tsp_config['metric_id'], tsp_config['id'], metric_instance['id'], tsp_result)

