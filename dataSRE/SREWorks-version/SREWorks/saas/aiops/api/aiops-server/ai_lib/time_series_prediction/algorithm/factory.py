# -*- coding: utf-8 -*-
from ai_lib.time_series_prediction.algorithm.models.ts_pred_robust_forecast import TdataRobustPredModel
from ai_lib.time_series_prediction.algorithm.models.ts_pred_robust_forecast_new import TdataRobustPredNewModel
from ai_lib.time_series_prediction.algorithm.models.ts_pred_stl_forecast import TdataStlPredModel
from ai_lib.time_series_prediction.algorithm.preprocess.DataPreprocessor import DataPreprocessor


class AlgorithmFactory(object):

    @classmethod
    def get_algorithm(cls, name, period, interval, colname,  params, user_config, forecast_horizon,
                 metric_id, ad_train_id):
        if name == 'quantile_pred':
            interval_sigma = params['interval_sigma']
            quantile = params['quantile']
            acf_peak_th = params['pd_params']['acf_peak_th']
            refine_tolerance = params['pd_params']['refine_tolerance']

            return TdataRobustPredModel(period, interval, colname, interval_sigma, acf_peak_th, refine_tolerance, quantile,
                                           params, user_config, forecast_horizon,
                                           metric_id, ad_train_id)

        elif name == 'robust_pred':
            interval_sigma = params['interval_sigma']
            acf_peak_th = params['pd_params']['acf_peak_th']
            refine_tolerance = params['pd_params']['refine_tolerance']
            return TdataRobustPredNewModel(period, interval, colname,  interval_sigma, acf_peak_th, refine_tolerance, params, user_config, forecast_horizon,
                 metric_id, ad_train_id)
        elif name == 'stl_pred':
            interval_sigma = params['interval_sigma']
            acf_peak_th = params['pd_params']['acf_peak_th']
            refine_tolerance = params['pd_params']['refine_tolerance']
            return TdataStlPredModel(period, interval, colname, interval_sigma, acf_peak_th, refine_tolerance,
                                           params, user_config, forecast_horizon,
                                           metric_id, ad_train_id)

    @classmethod
    def data_preprocess(cls, name, kpidata, colname, interval=None, period=None, process_method=None, user_config=None):
        if not process_method:
            process_method = {}

        if not user_config:
            user_config = {}

        if name in ['robust_pred', 'stl_pred', 'quantile_pred']:
            return DataPreprocessor().data_preprocess(kpidata, colname, interval, period , process_method, user_config)