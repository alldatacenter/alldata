# -*- coding: utf-8 -*-

# @author: 丛戎
# @target: 对源数据进行一些预处理的方法,如数据平滑、季节性分解、剔除异常值等

import pandas as pd

from statsmodels.tsa.seasonal import seasonal_decompose


class DataPreprocessUtils(object):

    def get_complete_date_range(self, downdate, update, freq, tsname):
        """
        根据开始和结束日期补全日期
        :param downdate:
        :param update:
        :param freq: 如5H, 3min20S, 3D
        :param tsname:
        :return:
        """
        timeindex = pd.date_range(start=downdate, end=update, freq=freq)
        timeindex = pd.DataFrame(timeindex, columns=[tsname])
        return timeindex

    def data_smoothing_function(self, data, colname_list, windowsize=3, func='avg'):
        """
        将原始数据按照一定的聚合方式进行平滑处理

        :param data: DataFrame格式,包含ts和kpi两列,ts为时间戳格式
        :param colname_list: 需要聚合的列名, 为list类型
        :param windowsize: 平滑的窗口大小
        :param func: avg, min, max, median, sum, var
        :return: 聚合后的DataFrame, 新ts为窗口内的最小ts, kpi为聚合后的值
        """
        # groupby_data = pd.DataFrame(columns=['ts'] + colname_list)

        # 因为传递进来的ts字段是datetime类型,无法直接做rolling, 所以先转化为timestamp格式
        # ts_origin = data['ts'].copy()
        # data['ts'] = data['ts'].apply((lambda x: time.mktime(time.strptime(str(x), "%Y-%m-%d %H:%M:%S"))))
        # groupby_data['ts'] = data['ts'].rolling(window=windowsize, center=False).max()

        groupby_data = data
        # 取移动平均值来平滑数据
        if func == 'avg':
            groupby_data[colname_list] = data[colname_list].rolling(window=windowsize, center=False).mean()
        elif func == 'min':
            groupby_data[colname_list] = data[colname_list].rolling(window=windowsize, center=False).min()
        elif func == 'max':
            groupby_data[colname_list] = data[colname_list].rolling(window=windowsize, center=False).max()
        elif func == 'median':
            groupby_data[colname_list] = data[colname_list].rolling(window=windowsize, center=False).median()
        elif func == 'sum':
            groupby_data[colname_list] = data[colname_list].rolling(window=windowsize, center=False).sum()
        elif func == 'var':
            groupby_data[colname_list] = data[colname_list].rolling(window=windowsize, center=False).var()
        # Coefficient Of Variation变异系数,即sigma/mean,衡量抖动的大小,一般用于检测突然的抖动
        elif func == 'cv':
            for eachcolname in colname_list:
                groupby_data['temp_sigma'] = data[eachcolname].rolling(window=windowsize, center=False).std()
                groupby_data['temp_mean'] = data[eachcolname].rolling(window=windowsize, center=False).mean()
                groupby_data[eachcolname] = 0
                groupby_data.loc[groupby_data['temp_mean'] != 0, eachcolname] = (
                        groupby_data['temp_sigma'] / groupby_data['temp_mean'])

        # Coefficient Of Variation变异系数,即sigma/mean,这里我们取倒数,即mean/sigma,当sigma为0,取很大的数,为了检测打平的情况
        elif func == 'cv_reciprocal':
            for eachcolname in colname_list:
                groupby_data['temp_sigma'] = data[eachcolname].rolling(window=windowsize, center=False).std()
                groupby_data['temp_mean'] = data[eachcolname].rolling(window=windowsize, center=False).mean()
                groupby_data[eachcolname] = 1E20
                groupby_data.loc[groupby_data['temp_sigma'] != 0, eachcolname] = (
                            groupby_data['temp_mean'] / groupby_data['temp_sigma'])

        groupby_data = groupby_data.dropna()
        # groupby_data['ts'] = ts_origin
        return groupby_data

    def stl_decomp(self, data, colname, period):
        """
        stl季节性分解,将数据分解为趋势、季节项和残差三部分

        :param data: DataFrame格式数据
        :param colname: 需要分解的列名,如kpi
        :param period: 一个周期包含几个点
        :return: 返回DataFrame,包含ts, trend, seasonal, remainder, 以及trend+seasonal的decomp_kpi
        """

        res = seasonal_decompose(data[colname], model='additive', freq=period,
                                 two_sided=False, extrapolate_trend='freq')

        decomp_data = data.copy()
        decomp_data['remainder'] = res.resid
        decomp_data['seasonal'] = res.seasonal
        decomp_data['trend'] = res.trend
        decomp_data['decomp_kpi'] = decomp_data['trend'] + decomp_data['seasonal']
        decomp_data['deseasonal'] = decomp_data['trend'] + decomp_data['remainder']
        return decomp_data

    def drop_noise_by_nsigma(self, data, colname, n=3):
        """
        根据n-sigma原则对某一列剔除异常点

        :param data: DataFrame格式数据
        :param colname: 要去噪的列名,如kpi, remainder
        :param n: nsigma系数
        :return: 去除噪音后的数据
        """
        mean = data[colname].mean()
        sigma = data[colname].std()
        data = data[(data[colname] >= mean - n * sigma) & (data[colname] <= mean + n * sigma)]

        return data

    def ts_fill_na(self, kpidata, startdate, enddate, freq, fillvalue=True):
        """
        时间序列数据缺失日期和缺失值补齐

        :param kpidata: DataFrame格式的数据
        :param startdate: 数据的开始日期
        :param enddate: 数据的结束日期
        :param interval: 日期的间隔
        :param fillvalue: 是否要用前后的真实值来填补缺失值, 否则直接用0填充
        :return: 返回填补完日期和缺失值后的DataFrame
        """
        # 是否要用前后的真实值来填补缺失值, 否则直接用0填充
        if fillvalue:
            kpidata['kpi_median'] = kpidata['kpi'].rolling(window=5, center=False).median()
            # 日期补全
            timeindex = self.get_complete_date_range(startdate, enddate, freq, 'ts')
            timeindex['ts'] = pd.to_datetime(timeindex['ts'])
            kpidata['ts'] = pd.to_datetime(kpidata['ts'])
            kpidata = pd.merge(timeindex, kpidata.copy(), on=['ts'], how='left')

            kpidata['kpi_median'] = kpidata['kpi_median'].fillna(method='pad')
            kpidata['kpi_median'] = kpidata['kpi_median'].fillna(method='bfill')
            kpidata.loc[kpidata['kpi'].isnull(), 'kpi'] = kpidata['kpi_median']
            kpidata = kpidata.drop('kpi_median', 1)
            kpidata = kpidata.fillna(0)
        else:
            # 缺失值直接全部填0
            # 日期补全
            timeindex = self.get_complete_date_range(startdate, enddate, freq, 'ts')
            timeindex['ts'] = pd.to_datetime(timeindex['ts'])
            kpidata['ts'] = pd.to_datetime(kpidata['ts'])
            kpidata = pd.merge(timeindex, kpidata.copy(), on=['ts'], how='left')
            kpidata = kpidata.fillna(0)
        return kpidata

    def ts_clean_and_fill(self, dataset, colname, period, interval):
        """
        时序分解、异常值剔除、缺失值补全的方法封装

        :param dataset: DataFrame格式
        :param colname: 需要预处理的列名
        :param period: 一个周期的点数
        :param interval: 点与点之间的时间间隔（按秒计）
        :return: 预处理完毕的DataFrame
        """
        dataset = self.stl_decomp(dataset, colname, period)
        dataset = self.drop_noise_by_nsigma(dataset, 'remainder')
        # 利用trend+seasonal组合成的值进行3sigma的去噪
        clean_dataset = self.drop_noise_by_nsigma(dataset, 'decomp_kpi')
        # 数据补全
        freq = str(interval) + 's'
        clean_dataset = self.ts_fill_na(clean_dataset, clean_dataset['ts'].min(), clean_dataset['ts'].max(), freq, True)
        return clean_dataset
