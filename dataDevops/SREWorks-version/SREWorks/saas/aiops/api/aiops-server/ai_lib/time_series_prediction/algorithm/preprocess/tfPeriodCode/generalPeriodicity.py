""" Periodicity Detection (periodicity and its length) for time series

Author: Qingsong Wen <qingsong.wen@alibaba-inc.com>
"""

# import numpy as np
from .existing_periodicity_detection_algs import \
    to_2dnp_array, assert_all_finite, ACF_Med
from .tfPeriod_alg import tfPeriod

DEFAULT_PARAM = {
    'ACF_Med': {
        "period_candi": None,  # key
        "trend_remove_toggle": True,
        "robustTrend_toggle": True,
        "trend_lambda": 1e10,
        "robust_remove_outliers_toggle": False,
        "robust_remove_outliers_ra_fun": 'Huber',  # Huber, Tukey
        "use_periodogram": True,
        "direct_act_out": 'acf_ori',
        "periodogram_cost_fun": 'LS',  # Huber, LS, LAD
        "periodogram_huber_t": 1.0,
        "periodogram_admm_maxiters": 50,
        "acf_out_len": None,
        "acf_peak_th": 0.5,  # key
        "acf_avg": 'median',
        "refinePeriod_toggle": False,
        "refine_tolerance": 0.05,
        "debug": False,
    },
    'tfPeriod': {
        "trend_remove_toggle": True,
        "robustTrend_toggle": True,
        "trend_lambda": 1e10,
        "wavelet_name": 'db4',
        "robust_wavelet_toggle": True,
        "robust_wv_toggle": True,
        "periodogram_cost_fun": 'LS',  # Huber, LS, LAD
        "periodogram_huber_t": 1.0,
        "periodogram_admm_maxiters": 50,
        "fisher_alph_th": 1e-10,
        "acf_out_len": None,
        "acf_peak_th": 0.5,  # key
        "acf_avg": 'median',
        "debug": False,
    },
}


class GeneralPeriodicityDetector():
    def __init__(self, params=None, detector="ACF_Med"):
        if detector == "Default":
            _detector = "tfPeriod"
        else:
            _detector = detector
        _param = DEFAULT_PARAM.get(_detector, {}).copy()
        if params is not None:
            # Update default para according to input params
            for k in params:
                if k in _param:
                    _param[k] = params[k]
        self.d_params = _param
        self.detector = _detector

    def fit_transform(self, ts_data, override=False):
        """
        This method to do fit_transform to get periodicity

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

        real_data = to_2dnp_array(ts_data)
        assert_all_finite(real_data)
        # if no period, the output is: False, [0]
        # if with period, the output is: True, [x]
        if self.detector == 'ACF_Med':
            passed_check_acf, period_output = ACF_Med(real_data,
                                                      **self.d_params)
            period_output = [period_output]
        elif self.detector == 'tfPeriod':
            tfPeriod_period = tfPeriod(real_data, **self.d_params)
            if len(tfPeriod_period) > 0:
                passed_check_acf = True
                period_output = tfPeriod_period
            else:
                passed_check_acf = False
                period_output = [0]

        return passed_check_acf, period_output
