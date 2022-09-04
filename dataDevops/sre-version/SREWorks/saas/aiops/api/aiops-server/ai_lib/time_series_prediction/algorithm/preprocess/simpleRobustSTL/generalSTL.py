""" general season-trend decomposition for time series

Author: Qingsong Wen <qingsong.wen@alibaba-inc.com>
"""
import numpy as np
from statsmodels.tsa.api import Holt
# from ..detrending.robustTrend import RobustTrendFilter
from .RobustSTLcode import RobustSTL
# from .multiseason_robustSTL import MultiSeason_RobustSTL

DEFAULT_PARAM = {
    'Robust_Trend': {
        "robust_cost": 'LAD',
        "huber_th": 1.0,
        "reg_lambda_1": 5,
        "reg_lambda_2": 2,
        "reg_2_order": 2,
        "huber_L1_admm_rho": 6,
        "huber_L1_admm_rho_2": 6,
        "LAD_L1_admm_rho": 1,
        "admm_tol_abs": 1e-4,
        "admm_tol_rel": 1e-2,
        "solver_maxiters": 5,
        "solver_show_progress": False,
        # latest_decomp_out
        "latest_decomp_output": True,
        "latest_decomp_length": 20
    },
    'Robust_STL': {
        "data_T": 24,  # e.g., 24
        "noise_toggle": True,
        "noise_sigma_i": 2.0,
        "noise_sigma_d": 2.5,
        "noise_truncate": 8.0,
        # trend
        "trend_toggle": True,
        "trend_vlambda": 40,
        "trend_vlambda_diff": 20,
        "trend_solver_method": 'GADMM',
        "trend_solver_maxiters": 15,
        "trend_solver_show_progress": False,
        "trend_solver_admm_rho": 1.0,
        "trend_down_sample": 8,
        # season
        "season_toggle": True,
        "season_bilateral_period_num": 2,
        "season_neighbour_wdw_size": 20,
        "season_sigma_i": 2,
        "season_sigma_d": 10,

        # latest_decomp_out
        "latest_decomp_output": True,
        "latest_decomp_length": 20
    },
    'Robust_MSTL': {
        "data_T": [24, 168],  # e.g., [24, 168]
        "noise_toggle": True,
        "noise_sigma_i": 2.0,
        "noise_sigma_d": 2.5,
        "noise_truncate": 8.0,
        # trend
        "trend_toggle": True,
        "trend_vlambda": 40,
        "trend_vlambda_diff": 20,
        "trend_solver_method": 'GADMM',
        "trend_solver_maxiters": 15,
        "trend_solver_show_progress": False,
        "trend_solver_admm_rho": 1.0,
        # season
        "season_toggle": True,
        # same len as data_T, can be none
        "season_period_weight": [1, 10],
        "season_bilateral_period_num": 2,
        "season_neighbour_wdw_size": 20,
        "season_sigma_i": 2,
        "season_sigma_d": 10,
        # multi-season
        "refine_seasons_toggle": False,  # New Para
        "refine_seasons_parameter": None,
        "refine_seasons_maxloop": 20,
        "refine_seasons_admm_maxstep": 100,
        "refine_seasons_debug": False,
        # latest_decomp_out
        "latest_decomp_output": True,
        "latest_decomp_length": 20
    }
}


class GeneralDecompEstimator():
    def __init__(self, params=None, detector="Robust_Trend"):
        _param = DEFAULT_PARAM.get(detector, {}).copy()
        if params is not None:
            # Update default para according to input params
            for k in params:
                if k in _param:
                    _param[k] = params[k]

        self.mstl_refine_seasons_toggle = None
        if "refine_seasons_toggle" in _param:
            self.mstl_refine_seasons_toggle = _param["refine_seasons_toggle"]
            del _param["refine_seasons_toggle"]

        self.latest_decomp_output = False
        if "latest_decomp_output" in _param:
            self.latest_decomp_output = _param["latest_decomp_output"]
            del _param["latest_decomp_output"]

        self.latest_decomp_length = 1
        if "latest_decomp_length" in _param:
            self.latest_decomp_length = _param["latest_decomp_length"]
            del _param["latest_decomp_length"]
        self.d_params = _param
        self.detector = detector
        if self.detector == 'Robust_Trend':
            # decomp_filter = RobustTrendFilter(**self.d_params)
            pass
        elif self.detector == 'Robust_STL':
            decomp_filter = RobustSTL(**self.d_params)
        # elif self.detector == 'Robust_MSTL':
        #     decomp_filter = MultiSeason_RobustSTL(**self.d_params)
        self.decomp_filter = decomp_filter

    def fit_transform(self, ts_data, override=False):
        """
        This method to do fit_transform to get stl components

        Parameters
        ----------
        ts_data : array-like, shape = (n_timelength, 1)
            The input ts data, only support 1D data currently.
        override : boolean
            To be used to override online case, ignore for now.

        Returns
        -------
        X_t : array-like, shape = (n_timelength, 1)
            The output latent variable data
        """

        # update later using design pattern:
        decomp_result = {}
        decomp_result["trend"] = None
        decomp_result["season"] = None
        decomp_result["residual"] = None
        latest_decomp_result = {}
        latest_decomp_result["season"] = None
        latest_decomp_result["trend"] = None
        latest_decomp_result["season_plus_trend"] = None
        latest_len = self.latest_decomp_length
        decomp_filter = self.decomp_filter

        if self.detector == 'Robust_Trend':
            decomposed_data = decomp_filter.fit_transform(ts_data)
            decomp_result["trend"] = decomposed_data[:,
                                                     0].reshape(-1, ).tolist()
            decomp_result["residual"] = decomposed_data[:, 1].reshape(
                -1, ).tolist()
            if self.latest_decomp_output:
                trend_model = Holt(decomp_result["trend"]).fit(
                    smoothing_level=0.5, smoothing_slope=0.3)
                latest_decomp_result["trend"] = trend_model.forecast(
                    latest_len).tolist()
            return decomp_result, latest_decomp_result

        elif self.detector == 'Robust_STL':
            decomposed_data = decomp_filter.fit_transform(ts_data)
            decomp_result["trend"] = decomposed_data[:,
                                                     0].reshape(-1, ).tolist()
            decomp_result["season"] = decomposed_data[:, 1].reshape(
                -1, ).tolist()
            decomp_result["residual"] = decomposed_data[:, 2].reshape(
                -1, ).tolist()
            if self.latest_decomp_output:
                pd_len = self.d_params['data_T']
                season_data = decomp_result["season"]
                # latest_season
                latest_season = np.zeros(latest_len)
                for idx in range(latest_len):
                    latest_season[idx] = season_data[idx % pd_len - pd_len]
                latest_decomp_result["season"] = latest_season.tolist()
                # latest_trend
                trend_model = Holt(decomp_result["trend"],
                                   damped=True).fit(smoothing_level=0.5,
                                                    smoothing_slope=0.3)
                latest_trend = trend_model.forecast(latest_len)
                latest_decomp_result["trend"] = latest_trend.tolist()
                # latest_season_plus_trend
                latest_decomp_result["season_plus_trend"] = (
                    latest_season + latest_trend).tolist()

                # latest_trend+residual (update later)
                trend_res = (decomposed_data[:, 0] +
                             decomposed_data[:, 2]).reshape(-1, ).tolist()
                trend_res_model = Holt(trend_res,
                                       damped=True).fit(smoothing_level=0.5,
                                                        smoothing_slope=0.3)
                latest_trend_res = trend_res_model.forecast(latest_len)
                latest_decomp_result["trend_res"] = latest_trend_res.tolist()
                # latest_season_plus_trend_res
                latest_decomp_result["season_plus_trend_res"] = (
                    latest_season + latest_trend_res).tolist()
            return decomp_result, latest_decomp_result

        # TODO
        elif self.detector == 'Robust_MSTL':
            if not self.mstl_refine_seasons_toggle:
                decomposed_data = decomp_filter.fit_transform(ts_data)
                trend = decomposed_data[:, 0].reshape(-1, ).tolist()
                season = decomposed_data[:, 1].reshape(-1, ).tolist()
                residual = decomposed_data[:, 2].reshape(-1, ).tolist()
                return season, trend, residual
            else:  # refine multi-seasons--> seperate each other
                decomposed_data = decomp_filter.fit_transform_refine_seasons(
                    ts_data)
                # decomposed_data :
                # (final_trend, final_sum_seasons, final_irregular_data,
                # fine_seasons))
                trend = decomposed_data[:, 0].reshape(-1, ).tolist()
                season = decomposed_data[:, 1].reshape(-1, ).tolist()
                residual = decomposed_data[:, 2].reshape(-1, ).tolist()
                fine_seasons = decomposed_data[:, 3].reshape(-1, ).tolist()
                return season, trend, residual, fine_seasons
