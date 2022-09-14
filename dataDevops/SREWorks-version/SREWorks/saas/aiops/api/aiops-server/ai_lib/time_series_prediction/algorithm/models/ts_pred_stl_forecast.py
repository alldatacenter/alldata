# -*- coding: utf-8 -*-
# @author: 丛戎
# @target: 预测模型，基于stl和arima预测算法
import datetime

import json
import pandas as pd
from statsmodels.tsa.holtwinters import SimpleExpSmoothing

from ai_lib.time_series_prediction.algorithm.preprocess.tfPeriodCode.generalPeriodicity import GeneralPeriodicityDetector
from ai_lib.time_series_prediction.algorithm.preprocess.data_preprocess_utils import DataPreprocessUtils
from sklearn.base import BaseEstimator


class TdataStlPredModel(BaseEstimator):
    """
    首先进行stl周期性分解
    对deseasonal部分用arima进行预测
    对预测结果加回周期性分量
    """

    def __init__(self, period=1440, interval=60, colname='kpi', interval_sigma=2,
                 acf_peak_th=0.15, refine_tolerance=0.05, params=None, user_config=None, forecast_horizon=0,
                 metric_id=None, ad_train_id=None):
        """
        Called when initializing the classifier
        """
        self.interval = interval
        self.period = period
        self.colname = colname
        self.interval_sigma = interval_sigma
        self.acf_peak_th = acf_peak_th
        self.refine_tolerance = refine_tolerance
        self.forecast_horizon = forecast_horizon
        self.params = params
        self.user_config = user_config
        self.forecast_horizon = forecast_horizon
        self.metric_id = metric_id
        self.ad_train_id = ad_train_id
        self.training_end_ts_str = ''
        self.damo_input = ''
        self.damo_output = ''

    def fit(self, X, y=None):
        """
        This should fit classifier. All the "work" should be done here.

        Note: assert is not a good choice here and you should rather
        use try/except blog with exceptions. This is just for short syntax.
        """
        """
        进行训练并直接进行预测，不保存模型，直接进行预测
        :param dataset:预处理后的数据，包含ts和预处理后的kpi值
        :param period:一周期有几个点
        :param interval:点和点之间的时间间隔,秒数
        :param colname:要预测的列名，'kpi'
        :param params:预测模型的一些参数，和预处理部分参数是分开的
        pd_detector = "ACF_Med"
        pd_params = {
            "period_candi": 1440, ## this is important
            "refinePeriod_toggle": True,
            "refine_tolerance": 0.05,
            "acf_peak_th": 0.15
        }
        stl_detector = "Robust_STL"
        interval_sigma = 2
        non_zero_lower_interval=True
        stl_params = {
            "data_T": None,
            "noise_toggle": True,
            "noise_sigma_i": .2,
            "noise_sigma_d": 5,
            "noise_truncate": 2,
            "trend_toggle": True,
            "trend_vlambda": 50,
            "trend_vlambda_diff": 5,
            "trend_solver_method": 'GADMM',
            "trend_solver_maxiters": 50,
            "trend_solver_show_progress": False,
            "trend_down_sample": 6,
            "season_bilateral_period_num": 2,
            "season_neighbour_wdw_size": 20,
            "season_sigma_i": .2,
            "season_sigma_d": 2,
            "latest_decomp_output": True,
            "latest_decomp_length": None
        }

        :param forecast_horizon: 往后预测的秒数
        :param metric_id:
        :param ad_train_id：
        :return:future_predict_df[['ts', 'pred', 'upper', 'lower']]
        """
        pd_detector = self.params['pd_detector']
        pd_params = self.params['pd_params']
        stl_detector = self.params['stl_detector']
        stl_params = self.params['stl_params']
        non_zero_lower_interval = self.params['non_zero_lower_interval']
        # 将预测的秒数转换为点数
        forecast_horizon_cnt = int(self.forecast_horizon / self.interval)
        self.forecast_horizon_cnt = forecast_horizon_cnt
        # 用单拎出来的自定义参数覆盖
        pd_params['acf_peak_th'] = self.acf_peak_th
        pd_params['refine_tolerance'] = self.refine_tolerance
        interval_sigma = self.interval_sigma

        # 同时更新params里面的对应字段
        self.params['pd_params'] = pd_params
        self.params['interval_sigma'] = interval_sigma

        # 首先进行周期性的判别
        period_det = GeneralPeriodicityDetector(pd_params, pd_detector)
        try:
            passed_check_acf, period_output = period_det.fit_transform(X[self.colname].values)
        except Exception:
            passed_check_acf = False
            # raise Exception("周期性检测报错")
        # 将判别结果写回params中
        self.params['passed_check_acf'] = passed_check_acf
        self.training_end_ts_str = str(X['ts'].max())

        # 无论是否有周期性都要用到周期性分解的结果
        self.damo_input = json.dumps(list(X[self.colname].values))
        decomp_result = DataPreprocessUtils().stl_decomp(X, self.colname, self.period)
        decomp_result['ts'] = decomp_result['ts'].astype('str')
        self.damo_output = json.dumps(decomp_result.to_dict('records'))

        if passed_check_acf:
            # 将判别结果写回params中
            self.params['period_output'] = period_output[0]
            # 具备周期性，则进行robust stl分解，再进行预测
            stl_params["data_T"] = period_output[0]
            stl_params["latest_decomp_length"] = forecast_horizon_cnt

            # 拿deseasonal部分进行预测，再将seasonal部分加回来
            ses1 = SimpleExpSmoothing(list(decomp_result["deseasonal"].values)).fit()
            future_predict = ses1.forecast(forecast_horizon_cnt)
            future_predict = future_predict + decomp_result['seasonal'].values[:forecast_horizon_cnt]

            # 计算上下界
            std = pd.Series(decomp_result['remainder']).mad()
            upper_interval = future_predict + interval_sigma * std
            lower_interval = future_predict - interval_sigma * std
            if non_zero_lower_interval:
                lower_interval[lower_interval < 0] = 0

        else:
            # 不具备周期性，则直接进行预测
            kpidata_train = X.rename(columns={"ts": "ds", self.colname: "y"})
            ses = SimpleExpSmoothing(list(kpidata_train["y"].values)).fit()
            future_predict = ses.forecast(forecast_horizon_cnt)
            # 增加上下界，同样使用remainder的mad
            sigma = pd.Series(decomp_result['remainder']).mad()
            upper_interval = future_predict + interval_sigma * sigma
            lower_interval = future_predict - interval_sigma * sigma
            if non_zero_lower_interval:
                lower_interval[lower_interval < 0] = 0

        self.future_predict = future_predict
        self.upper_interval = upper_interval
        self.lower_interval = lower_interval

        return self

    def predict(self, X=None, y=None):
        """
        :param X: 如果X不为None, 则按照X的ts来给出对应的预测结果;为None预测默认配置长度
        :param y: None
        :return:
        """
        future_predict_df = pd.DataFrame(self.future_predict, columns=['pred'])
        training_end_ts = datetime.datetime.strptime(self.training_end_ts_str, "%Y-%m-%d %H:%M:%S")
        # 计算预测时段的开始和结束时间
        pred_start_ts = training_end_ts + datetime.timedelta(seconds=self.interval)
        pred_end_ts = pred_start_ts + datetime.timedelta(seconds=self.forecast_horizon_cnt * self.interval)
        preprocessor = DataPreprocessUtils()
        freq = str(self.interval) + 'S'
        timeindex = preprocessor.get_complete_date_range(pred_start_ts, pred_end_ts, freq, 'ts')
        future_predict_df['ts'] = timeindex
        future_predict_df['upper'] = self.upper_interval
        future_predict_df['lower'] = self.lower_interval
        future_predict_df['ts'] = future_predict_df['ts'].astype('str')
        if X is not None:
            X.loc[:, "ts"] = X.loc[:, "ts"].astype("str")
            future_predict_df = future_predict_df.merge(X, how="inner", on='ts')[["ts", "upper", "pred"]]
        future_predict_df_dict = future_predict_df[['ts', 'upper', 'pred']].to_dict("records")
        return (future_predict_df_dict, self.params, self.damo_input, self.damo_output)
