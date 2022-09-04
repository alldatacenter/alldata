# -*- coding: utf-8 -*-

# @author: 丛戎
# @target: 时序预测算法的预处理逻辑

import sys

from ai_lib.time_series_prediction.algorithm.preprocess.data_preprocess_utils import DataPreprocessUtils

PY2 = sys.version_info[0] == 2


class DataPreprocessor(object):

    def data_preprocess(self, kpidata, colname, interval=None, period=None, process_method={}, user_config={}):
        """

        :param kpidata:
        :param colname:
        :param interval: 数据点间隔，单位为秒
        :param period: 一个周期内的点数
        :param process_method: 预处理逻辑的参数，{'fillna':{'startdate': 0, 'enddate':0, 'interval' : '5min', 'fillvalue' :True}}
        :param user_config: 用户在页面的高级配置
        :return:
        """
        preprocessor = DataPreprocessUtils()

        # 首先对数据进行去重处理
        kpidata = kpidata.drop_duplicates(['ts'])
        # 并按ts进行排序
        kpidata = kpidata.sort_values(by=['ts'], ascending=[1])

        # 填补缺失值操作
        if 'fillna' in process_method.keys():
            # 根据数据粒度计算插值频率
            interval_seconds = str(interval) + 'S'
            fillvalue = process_method['fillna']['fillvalue']
            if "fillna_withzero" in user_config['advanced_config_map'].keys():
                fillvalue = False
            kpidata = preprocessor.ts_fill_na(kpidata=kpidata, startdate=kpidata['ts'].min(),
                                              enddate=kpidata['ts'].max(),
                                              freq=interval_seconds,
                                              fillvalue=fillvalue)
        return kpidata