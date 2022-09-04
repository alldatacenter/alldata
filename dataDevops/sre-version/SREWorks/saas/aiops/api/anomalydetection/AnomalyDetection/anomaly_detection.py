# -*- coding: utf-8 -*-
import pandas as pd
import json
import datetime
import re
import sys
import os
import pytz
import traceback

sys.path.append(os.path.dirname(os.path.realpath(__file__)))

from bentoml.types import InferenceTask
from bentoml import env, artifacts, api, BentoService
from bentoml.adapters import DataframeInput, JsonInput, StringInput
from bentoml.frameworks.sklearn import SklearnModelArtifact




@env(infer_pip_packages=True)
@artifacts([SklearnModelArtifact('model')])
class AnomalyDetection(BentoService):
    """
    A minimum prediction service exposing a Scikit-learn model
    """

    @api(input=JsonInput())
    def analyze(self, param: json, task: InferenceTask):
        """
        An inference API named `analyse` with Dataframe input adapter, which codifies
        how HTTP requests or CSV files are converted to a pandas Dataframe object as the
        inference API function iwnput
        """
        ## 必备参数检查 parameters check
        necessary_params_list = ['series']
        for each_param in necessary_params_list:
            try:
                if each_param not in param.keys():
                    raise Exception()
            except Exception as ex:
                result = {}
                result['code'] = 'MissingArgument'
                result['message'] = 'Missing input for the parameters, ' + each_param + ' is necessary.'
                return task.discard(http_status=400, err_msg=str(result))

        ## 获取检测数据
        try:
            data = pd.DataFrame(param['series'], columns=['ts', 'kpi'])
            data = data.sort_values(by=['ts'], ascending=[1])
        except Exception as ex:
            result = {}
            result['code'] = 'IllegalArgument.Unsuitable'
            result['message'] = 'Input data is unsuitable for processing.'
            return task.discard(http_status=400, err_msg=str(result))

        ## 获取其他参数
        # try:
        if 'algoParam' in param.keys() and 'seriesInterval' in param['algoParam'].keys():
            interval = int(param['algoParam']['seriesInterval'])
        else:
            data['ts_diff'] = data['ts'].diff()
            interval = int(data['ts_diff'].dropna().median())
            data = data[['ts', 'kpi']]

        if 'algoParam' in param.keys() and 'abnormalConditions' in param['algoParam'].keys():
            abnormal_conditions = param['algoParam']['abnormalConditions']
        else:
            abnormal_conditions = {"tolerance": 3}
        if "tolerance" not in abnormal_conditions:
            abnormal_conditions['tolerance'] = 3



        if 'algoParam' in param.keys() and 'detectDirection' in param['algoParam'].keys():
            detect_direction = param['algoParam']['detectDirection']
        else:
            detect_direction = 'both'

        if 'algoParam' in param.keys() and 'trainTsStart' in param['algoParam'].keys():
            train_ts_start = int(param['algoParam']['trainTsStart'])
        else:
            train_ts_start = data['ts'].min()
        ist = pytz.timezone("Asia/Shanghai")
        train_ts_start = datetime.datetime.fromtimestamp(train_ts_start, ist)
        train_ts_start = train_ts_start.strftime("%Y-%m-%d %H:%M:%S")

        if 'algoParam' in param.keys() and 'trainTsEnd' in param['algoParam'].keys():
            train_ts_end = int(param['algoParam']['trainTsEnd'])
        else:
            train_ts_end = data[:-1]['ts'].max()
        ist = pytz.timezone("Asia/Shanghai")
        train_ts_end = datetime.datetime.fromtimestamp(train_ts_end, ist)
        train_ts_end = train_ts_end.strftime("%Y-%m-%d %H:%M:%S")

        if 'algoParam' in param.keys() and 'detectTsStart' in param['algoParam'].keys():
            detect_ts_start = int(param['algoParam']['detectTsStart'])
        else:
            detect_ts_start = data['ts'].max()
        ist = pytz.timezone("Asia/Shanghai")
        detect_ts_start = datetime.datetime.fromtimestamp(detect_ts_start , ist)
        detect_ts_start = detect_ts_start.strftime("%Y-%m-%d %H:%M:%S")

        if 'algoParam' in param.keys() and 'detectTsEnd' in param['algoParam'].keys():
            detect_ts_end = int(param['algoParam']['detectTsEnd'])
        else:
            detect_ts_end = data['ts'].max()
        ist = pytz.timezone("Asia/Shanghai")
        detect_ts_end = datetime.datetime.fromtimestamp(detect_ts_end , ist)
        detect_ts_end = detect_ts_end.strftime("%Y-%m-%d %H:%M:%S")

        if 'algoParam' in param.keys() and 'returnBounds' in param['algoParam'].keys():
            return_bounds = param['algoParam']['returnBounds']
        else:
            return_bounds = False

        if 'algoParam' in param.keys() and 'returnOriginSeries' in param['algoParam'].keys():
            return_origin_series = param['algoParam']['returnOriginSeries']
        else:
            return_origin_series = False

        ## 输入数据转化,将timestamp转化为datetime
        try:
            # transform timestamp to datetime
            data['ts'] = pd.to_datetime(data['ts'].astype(int), unit='s')
            data['ts'] = data['ts'] + datetime.timedelta(hours=8)
            data = data.sort_values(by=['ts'], ascending=[1])
            data = data.drop_duplicates(['ts'])

        except Exception as ex:
            result = {}
            result['code'] = 'IllegalArgument.Unsuitable'
            result['message'] = 'Input data is unsuitable for processing.'
            return task.discard(http_status=400, err_msg=str(result))

        try:
            # 检查training data长度
            origin_training_data = data[(data['ts'] >= train_ts_start) & (data['ts'] <= train_ts_end)]
            if len(origin_training_data) < 15:
                raise Exception()
        except Exception as ex:
            result = {}
            result['code'] = 'IllegalArgument.LackingData'
            result['message'] = 'Input training data is not enough.'
            return task.discard(http_status=400, err_msg=str(result))

        try:
            origin_detect_data = data[(data['ts'] >= detect_ts_start) & (data['ts'] <= detect_ts_end)]
            if len(origin_detect_data) == 0:
                raise Exception()
        except Exception as ex:
            result = {}
            result['code'] = 'IllegalArgument.LackingData'
            result['message'] = 'Input detect data is null'
            return task.discard(http_status=400, err_msg=str(result))


        try:
            # 默认检测算法参数
            detect_params = {"direction": detect_direction,
                             "n": abnormal_conditions['tolerance']}

            # 训练
            sigma = origin_training_data['kpi'].std()
            mean = origin_training_data['kpi'].mean()

        except Exception as ex:
            result = {}
            result['code'] = 'InternalError.DetectionAlgo'
            result['message'] = 'Failed to run the offline anomaly detection algorithm.'
            return task.discard(http_status=400, err_msg=str(result))

        try:
            # 检测
            n = detect_params['n']
            up_limit = mean + n * sigma
            down_limit = mean - n * sigma
            origin_detect_data['anomaly'] = 0
            if detect_direction == 'up':
                origin_detect_data['upperbound'] = up_limit
                origin_detect_data.loc[origin_detect_data['kpi'] >= up_limit, 'anomaly'] = 1
            elif detect_direction == 'down':
                origin_detect_data['lowerbound'] = down_limit
                origin_detect_data.loc[origin_detect_data['kpi'] <= up_limit, 'anomaly'] = 1
            elif detect_direction == 'both':
                origin_detect_data.loc[(origin_detect_data['kpi'] >= up_limit) | (origin_detect_data['kpi'] <= down_limit), 'anomaly'] = 1
                origin_detect_data['upperbound'] = up_limit
                origin_detect_data['lowerbound'] = down_limit
            detect_result = origin_detect_data
            detect_result.rename(columns={'kpi': 'value'}, inplace=True)
        except Exception as ex:
            result = {}
            result['code'] = 'InternalError.DetectionAlgo'
            result['message'] = 'Failed to run the online anomaly detection algorithm.'
            return task.discard(http_status=400, err_msg=str(result))


        try:
            def str2timestamp(dt):
                try:
                    day = datetime.datetime.strptime(dt, "%Y-%m-%d %H:%M:%S")
                except Exception:
                    day = datetime.datetime.strptime(dt, "%Y-%m-%d")
                timezone = pytz.timezone("Asia/Shanghai")
                day_timezone = timezone.localize(day)
                return int(datetime.datetime.timestamp(day_timezone))

            detect_result['ts'] = detect_result['ts'].astype(str)
            detect_result['timestamp'] = detect_result['ts'].apply(str2timestamp)
            result_column_list = ['timestamp', 'value', 'anomaly']

            if return_bounds and detect_direction == 'both':
                result_column_list.extend(['upperbound', 'lowerbound'])
            elif return_bounds and detect_direction == 'up':
                result_column_list.extend(['upperbound'])
            elif return_bounds and detect_direction == 'down':
                result_column_list.extend(['lowerbound'])

            result_df = detect_result[result_column_list]
            result_df = result_df.reset_index(drop=True)

            ## 根据用户输入返回结果
            result = {}
            result['data'] = {
                "detectSeries": result_df.values,
                "detectSeriesColumns": list(result_df.columns)}

            if return_origin_series:
                result['data']['originSeries'] = param['series']
        except Exception as ex:
            result = {}
            result['code'] = 'InternalError.DetectionAlgo'
            result['message'] = 'Failed to transform the final format'
            return task.discard(http_status=400, err_msg=str(result))

        return result

    @api(input=StringInput(), route="status.taobao")
    def status(self, message: str):
        return "success"

    @api(input=StringInput())
    def doc(self, message: str):
        """
        get README.md
        """
        f = open("README.md")
        doc = f.read()
        f.close()
        return doc

    @api(input=StringInput(), route="input")
    def input(self, message: str):
        with open(os.path.dirname(os.path.realpath(__file__)) + '/mock.json') as json_file:
            mock_params = json.load(json_file)
        mock_params = json.loads(mock_params)
        mock_params['taskType'] = 'sync'
        return mock_params

    @api(input=StringInput())
    def doc(self, message: str):
        """
        get README.md
        """
        f = open("README.md")
        doc = f.read()
        f.close()
        return doc


