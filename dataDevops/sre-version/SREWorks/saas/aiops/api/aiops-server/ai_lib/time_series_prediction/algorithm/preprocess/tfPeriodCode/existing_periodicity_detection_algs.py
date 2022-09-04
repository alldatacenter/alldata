import numpy as np
import pandas as pd
import scipy.sparse as sp
from numbers import Number
from scipy.signal import find_peaks
# from matplotlib import pyplot as plt
from statsmodels.tsa.stattools import acf as statsmodels_acf
from sklearn.preprocessing import StandardScaler

from .trend_filters import HPTrendFilter_trend, L1TrendFilter_trend
from .wavelet_fun import robust_MODWT_var
from .periodogram_acf_fun import general_periodogram, acf_med_from_periodogram
from .fisher_siegel_test import \
    fisher_test, fisher_test_critical_value, siegel_test_critical_value
from .util import plot_signal_periodogram_acf
from .admm_solver_for_lad_huber import g_lasso_admm_solver
figsize = (9, 2)


def median_mad(ts_data, mad_scaled=True):
    median = np.median(ts_data)
    if mad_scaled:
        mad = 1.4826 * np.median(np.abs(ts_data - median))
    else:
        mad = np.median(np.abs(ts_data - median))
    return median, mad


def rob_normalize(ts_data, mad_scaled=True):
    rob_m, rob_s = median_mad(ts_data, mad_scaled)
    return (ts_data - rob_m) / rob_s


def ra_estimator(ts_data,
                 ra_est_fun='Tukey',
                 ra_est_para=None,
                 mad_scaled=True):

    ts_data = rob_normalize(ts_data, mad_scaled)

    if ra_est_fun == 'Tukey':
        if ra_est_para is None:
            ra_est_para = 4.68
        ts_data[np.abs(ts_data) > ra_est_para] = 0
        out = ts_data * (1 - ts_data**2 / ra_est_para**2)**2
    elif ra_est_fun == 'Huber':
        if ra_est_para is None:
            ra_est_para = 1.37
        # ra_est_para = 1.37 if ra_est_para is None
        out = np.sign(ts_data) * np.minimum(np.abs(ts_data), ra_est_para)
    else:
        raise ValueError('ra_est_fun must be Tukey or Huber')
    return out


# periodogram_cost_fun for robustness
def ACF_Med(
    original_data,
    period_candi=None,
    trend_remove_toggle=True,
    robustTrend_toggle=True,
    trend_lambda=1e8,
    robust_remove_outliers_toggle=False,
    robust_remove_outliers_ra_fun='Huber',  # Huber, Tukey
    use_periodogram=True,  # default: True
    direct_act_out='acf_ori',  # acf_ori, YIN_1, YIN_2
    periodogram_cost_fun='Huber',  # Huber, LS, LAD
    # (# Note: for LAD, use Huber with t=.001 have better stability)
    periodogram_huber_t=1.0,
    periodogram_admm_maxiters=50,
    acf_out_len=None,
    acf_peak_th=0.5,
    acf_avg='median',
    refinePeriod_toggle=False,
    refine_tolerance=0.05,
        debug=False):

    # normalize data: scale to around ~N(0,1)
    original_signal = original_data.reshape(-1, 1)
    scaler = StandardScaler()
    real_data = scaler.fit_transform(original_signal)
    original_signal_norm = scaler.fit_transform(original_signal)

    # remove outlier
    _upper = np.percentile(real_data, 99)
    _lower = np.percentile(real_data, 1)
    real_data = np.clip(real_data, _lower, _upper)
    # real_data = ra_estimator(real_data)

    # (1) (robust) coarse trend removing
    if trend_remove_toggle:
        detrend_filter = HPTrendFilter_trend(lambda_reg=trend_lambda,
                                             quasi_robust=robustTrend_toggle,
                                             upper_percent=0.9,
                                             lower_percent=0.1)
        trend_data = detrend_filter.fit_transform(real_data)
        input_data_detrend = real_data - trend_data
    else:
        trend_data = np.zeros_like(real_data)
        input_data_detrend = real_data
    real_data2 = input_data_detrend.reshape(-1, )

    # add robust pre-processing to remove outliers
    if robust_remove_outliers_toggle:
        real_data2 = ra_estimator(
            real_data2,
            ra_est_fun=robust_remove_outliers_ra_fun,
            ra_est_para=None,
            mad_scaled=True)

    # (3) robust Huber-Periodogram based Fisher-test
    # zero padding
    data_pad = np.hstack((real_data2, np.zeros(len(real_data2))))
    periodogram_values, ts_len = general_periodogram(
        input_data=data_pad,
        cost_fun=periodogram_cost_fun,
        Huber_t=periodogram_huber_t)

    # final ACF
    passed_check_acf, final_period, acf_period, acf_result, peaks_idx = \
        acf_med_from_periodogram(real_data2, periodogram_values,
                                 use_periodogram=use_periodogram,
                                 direct_act_out=direct_act_out,
                                 acf_peak_th=acf_peak_th,
                                 acf_avg=acf_avg,
                                 acf_out_len=acf_out_len,
                                 period_candi=period_candi)

    # final refinePeriod
    if refinePeriod_toggle and period_candi is not None:
        # step-7 prior_periods to refine periodicity length candidates:
        passed_check_acf, final_period = refinePeriods_byInputPrior(
            passed_check_acf, [final_period], period_candi, refine_tolerance)

    if debug:
        from matplotlib import pyplot as plt
        fontsize = 8
        fig, axarr = plt.subplots(nrows=1, ncols=2, figsize=(9, 2))

        axarr[0].plot(original_signal_norm, 'y', label='original_signal')
        axarr[0].plot(real_data, 'b', label='original_signal_clip')
        # axarr[0].plot(trend_data, 'y', label='trend_data')
        axarr[0].plot(input_data_detrend, 'r', label='after detrend and clip')
        if robust_remove_outliers_toggle:
            axarr[0].plot(real_data2, 'k',
                          label='after detrend, clip, re_outlier')
        axarr[0].legend()  # loc='upper right'  upper, lower, center
        acf_result_pk_th = acf_peak_th * np.ones_like(acf_result)
        acf_result_peaks = acf_result[peaks_idx]
        axarr[1].plot(acf_result, 'b')
        axarr[1].plot(acf_result_pk_th, 'g')
        if passed_check_acf:
            axarr[1].plot(peaks_idx, acf_result_peaks, 'r*')
        axarr[1].set_title("Robust-ACF: acf_T=%d,fin_T=%d; Period=%s" %
                           (acf_period, final_period, passed_check_acf),
                           fontsize=fontsize)

    return passed_check_acf, final_period


# periodogram_cost_fun for robustness
def Fisher_PD(
    original_data,
    trend_remove_toggle=True,
    robustTrend_toggle=True,
    trend_lambda=1e8,
    periodogram_cost_fun='Huber',  # Huber, LS, LAD
    # (# Note: for LAD, use Huber with t=.001 have better stability)
    periodogram_huber_t=1.0,
    periodogram_admm_maxiters=50,
    fisher_alph_th=1e-5,
        debug=False):

    # normalize data: scale to around ~N(0,1)
    original_signal = original_data.reshape(-1, 1)
    scaler = StandardScaler()
    real_data = scaler.fit_transform(original_signal)

    # (1) (robust) coarse trend removing
    if trend_remove_toggle:
        detrend_filter = HPTrendFilter_trend(lambda_reg=trend_lambda,
                                             quasi_robust=robustTrend_toggle,
                                             upper_percent=0.9,
                                             lower_percent=0.1)
        trend_data = detrend_filter.fit_transform(real_data)
        input_data_detrend = real_data - trend_data
    else:
        trend_data = np.zeros_like(real_data)
        input_data_detrend = real_data

    real_data = input_data_detrend.reshape(-1, )
    # (3) robust Huber-Periodogram based Fisher-test
    # zero padding
    data_pad = np.hstack((real_data, np.zeros(len(real_data))))
    periodogram_values, ts_len = general_periodogram(
        input_data=data_pad,
        cost_fun=periodogram_cost_fun,
        Huber_t=periodogram_huber_t)

    # fisher test
    period_candi, period_range, per_T, pValue, _ = fisher_test(
        periodogram_values, ts_len, fisher_alph_th)

    if debug:
        from matplotlib import pyplot as plt
        fontsize = 8
        fig, axarr = plt.subplots(nrows=1, ncols=2, figsize=(9, 2))
        axarr[0].plot(original_signal, 'b', label='original signal')
        axarr[0].legend()  # loc='upper right'  upper, lower, center
        peak_idx = np.argmax(periodogram_values)
        peri_peak = periodogram_values[peak_idx]
        if period_candi != 0:
            axarr[1].plot(peak_idx,
                          peri_peak,
                          'r*',
                          label='max value of periodogram')
        axarr[1].plot(periodogram_values, 'b', label='periodogram values')
        axarr[1].legend()  # loc='upper right'  upper, lower, center
        axarr[1].set_title(
            "Periodogram, possible period by max value=%d, pValue=%.2e" %
            (per_T, pValue),
            fontsize=fontsize)
        plt.tight_layout()
        plt.show()

    return period_candi, pValue, per_T


# multiple periods detection
def Siegel_PD(
        original_data,
        trend_remove_toggle=True,
        robustTrend_toggle=True,
        trend_lambda=1e8,
        alpha_th=0.01,  # only 0.01 or 0.05
        lamb=0.6,  # fixed
        debug=False):

    # normalize data: scale to around ~N(0,1)
    original_signal = original_data.reshape(-1, 1)
    scaler = StandardScaler()
    real_data = scaler.fit_transform(original_signal)

    # (1) (robust) coarse trend removing
    if trend_remove_toggle:
        detrend_filter = HPTrendFilter_trend(lambda_reg=trend_lambda,
                                             quasi_robust=robustTrend_toggle,
                                             upper_percent=0.9,
                                             lower_percent=0.1)
        trend_data = detrend_filter.fit_transform(real_data)
        input_data_detrend = real_data - trend_data
    else:
        trend_data = np.zeros_like(real_data)
        input_data_detrend = real_data

    input_data = input_data_detrend.reshape(-1, )
    ts_len = len(input_data)
    input_data -= np.mean(input_data)
    periodogram_values = np.abs(np.fft.rfft(input_data, norm='ortho'))**2
    periodogram_values = periodogram_values.reshape(-1, 1)
    periodogram_values /= np.sum(periodogram_values)  # important

    g_f, alpha_tmp = fisher_test_critical_value(ts_len=ts_len,
                                                alpha_th=alpha_th)
    lamb_gf = lamb * g_f

    observed_t = 0.0
    diff_vals = (periodogram_values - lamb_gf).reshape(-1, )
    idx_set = []
    periods_power = []
    for idx, val in enumerate(diff_vals):
        if val > 0:
            observed_t += val
            periods_power.append(val)
            idx_set.append(idx)

    idx_set = np.array(idx_set)
    periods_power = np.array(periods_power)
    sorted_idx = np.argsort(periods_power)[::-1]
    idx_set = idx_set[sorted_idx]

    cv_siegel, alpha_tmp = siegel_test_critical_value(ts_len,
                                                      alpha_th,
                                                      lamb=lamb)

    if observed_t > cv_siegel:
        per_T = np.round(ts_len / idx_set).astype(int)
        # print('siegel test is True, we have multiple periods')
        # print(per_T)
    else:
        # print('siegel test is False, we do not have multiple periods')
        per_T = 0

    if debug:
        from matplotlib import pyplot as plt
        pd.DataFrame(input_data).plot(figsize=figsize)
        tmp_plotdata = np.hstack(
            (periodogram_values, lamb_gf * np.ones_like(periodogram_values)))
        pd.DataFrame(tmp_plotdata).plot(figsize=figsize)
        print('observed_t,cv_siegel')
        print(observed_t, cv_siegel)
        print('idx_set')
        print(idx_set)
        print('per_T')
        print(per_T)

    return per_T


def Siegel_ACF_PD(
    original_data,
    trend_remove_toggle=True,
    robustTrend_toggle=True,
    trend_lambda=1e8,
    periodogram_cost_fun='Huber',  # Huber, LS, LAD
    # (# Note: for LAD, use Huber with t=.001 have better stability)
    periodogram_huber_t=1.0,
    periodogram_admm_maxiters=50,
    acf_peak_th=0,  # note here not single period, so not 0.5
    alpha_th=0.01,  # only 0.01 or 0.05
    lamb=0.6,  # fixed
        debug=False):

    # normalize data: scale to around ~N(0,1)
    original_signal = original_data.reshape(-1, 1)
    scaler = StandardScaler()
    real_data = scaler.fit_transform(original_signal)

    # (1) (robust) coarse trend removing
    if trend_remove_toggle:
        detrend_filter = HPTrendFilter_trend(lambda_reg=trend_lambda,
                                             quasi_robust=robustTrend_toggle,
                                             upper_percent=0.9,
                                             lower_percent=0.1)
        trend_data = detrend_filter.fit_transform(real_data)
        input_data_detrend = real_data - trend_data
    else:
        trend_data = np.zeros_like(real_data)
        input_data_detrend = real_data

    real_data = input_data_detrend.reshape(-1, )
    # (3) robust Huber-Periodogram based Fisher-test
    # zero padding
    data_pad = np.hstack((real_data, np.zeros(len(real_data))))
    periodogram_values, ts_len = general_periodogram(
        input_data=data_pad,
        cost_fun=periodogram_cost_fun,
        Huber_t=periodogram_huber_t)

    # Siegel test
    # note ts_len in fisher_test_critical_value/siegel_test_critical_value
    data_length = len(real_data)  # for later ACF
    periodogram = periodogram_values.copy()  # for later ACF
    periodogram_values = periodogram_values.reshape(-1, 1)
    periodogram_values /= np.sum(periodogram_values)  # important

    g_f, alpha_tmp = fisher_test_critical_value(ts_len=ts_len / 2,
                                                alpha_th=alpha_th)  # ts_len/2
    lamb_gf = lamb * g_f

    observed_t = 0.0
    diff_vals = (periodogram_values - lamb_gf).reshape(-1, )
    idx_set = []
    periods_power = []
    for idx, val in enumerate(diff_vals):
        if val > 0:
            observed_t += val
            periods_power.append(val)
            idx_set.append(idx)

    idx_set = np.array(idx_set)
    periods_power = np.array(periods_power)
    sorted_idx = np.argsort(periods_power)[::-1]
    idx_set = idx_set[sorted_idx]

    cv_siegel, alpha_tmp = siegel_test_critical_value(ts_len / 2,
                                                      alpha_th,
                                                      lamb=lamb)  # ts_len/2

    output_periods_set = set()
    per_T = 0
    if observed_t > cv_siegel:
        per_T = np.round(ts_len / idx_set).astype(int)
        # find peak above acf_peak_th
        acf_out_len = int(data_length * 0.75)
        # acf_result = statsmodels_acf(input_data, acf_out_len, True)
        XX_psd2 = np.hstack(
            (periodogram,
             periodogram[1:-1][::-1]))  # note that assume data_pad is even
        normalizer = 1.0 / (data_length - np.arange(data_length))
        r_xx = np.fft.ifft(XX_psd2)[:data_length].real
        r_xx *= normalizer  # normalize for position, like 1/(N-k)
        r_xx /= r_xx[0]  # normalize, r_xx[0] is the variance
        acf_result = r_xx[:acf_out_len]
        peaks_idx, _ = find_peaks(acf_result,
                                  height=acf_peak_th,
                                  prominence=acf_peak_th / 2,
                                  distance=None)

        for idx in idx_set:
            if idx == 0 or idx == 1:
                continue
            # period_candi = np.round(ts_len / idx).astype(int)
            # calculate period_range
            N = ts_len
            k = idx
            low = int((N / (k + 1) + N / k) / 2 - 1)
            high = int((N / k + N / (k - 1)) / 2 + 1)
            period_range = np.arange(low, high + 1)

            for ii in peaks_idx:
                if ii in period_range:
                    output_periods_set.add(ii)

    if debug:
        # for siegel
        from matplotlib import pyplot as plt
        pd.DataFrame(original_signal).plot(figsize=figsize)
        tmp_plotdata = np.hstack(
            (periodogram_values, lamb_gf * np.ones_like(periodogram_values)))
        pd.DataFrame(tmp_plotdata).plot(figsize=figsize)
        print('observed_t,cv_siegel')
        print(observed_t, cv_siegel)
        print('idx_set')
        print(idx_set)
        print('per_T')
        print(per_T)
        # for acf
        # fontsize = 8
        fig, axarr = plt.subplots(nrows=1, ncols=2, figsize=(9, 2))
        axarr[0].plot(original_signal, 'b', label='original_signal')
        axarr[0].legend()  # loc='upper right'  upper, lower, center
        acf_result_pk_th = acf_peak_th * np.ones_like(acf_result)
        # acf_result_peaks = acf_result[peaks_idx]
        axarr[1].plot(acf_result, 'b')
        axarr[1].plot(acf_result_pk_th, 'g')

    return np.array(list(output_periods_set))


def Wavelet_Fisher(
    original_data,
    trend_remove_toggle=True,
    robustTrend_toggle=False,
    trend_lambda=1e8,
    wavelet_name='db4',
    robust_wavelet_toggle=False,
    robust_wv_toggle=False,
    robust_wavelet_ra_fun='Huber',  # Huber, Tukey
    periodogram_cost_fun='LS',  # Huber, LS, LAD
    # (# Note: for LAD, use Huber with t=.001 have better stability)
    periodogram_huber_t=1.0,
    periodogram_admm_maxiters=50,
    fisher_alph_th=1e-5,
        debug=False):

    # normalize data: scale to around ~N(0,1)
    original_signal = original_data.reshape(-1, 1)
    scaler = StandardScaler()
    real_data = scaler.fit_transform(original_signal)

    # (1) (robust) coarse trend removing
    if trend_remove_toggle:
        detrend_filter = HPTrendFilter_trend(lambda_reg=trend_lambda,
                                             quasi_robust=robustTrend_toggle,
                                             upper_percent=0.9,
                                             lower_percent=0.1)
        trend_data = detrend_filter.fit_transform(real_data)
        input_data_detrend = real_data - trend_data
    else:
        trend_data = np.zeros_like(real_data)
        input_data_detrend = real_data

    # (1-1): truncate or pad to 2**n length
    ori_len = len(input_data_detrend)
    final_len_large = 2**int(np.ceil(np.log2(ori_len)))
    final_len_small = 2**int(np.floor(np.log2(ori_len)))
    if final_len_small == final_len_large:
        input_data_adj = input_data_detrend
    elif (ori_len - final_len_small) / final_len_small <= 0.5:
        # truncate
        input_data_adj = input_data_detrend[:final_len_small]
    else:
        # padding
        final_len = final_len_large
        pad_len = final_len - ori_len
        pad_len_begin = 0
        pad_len_end = pad_len
        input_data_adj = np.pad(input_data_detrend.reshape(-1, ),
                                (pad_len_begin, pad_len_end),
                                'wrap')  # wrap, symmetric is better
        input_data_adj = input_data_adj.reshape(-1, 1)

    # (2) robust Wavelet and robust WV
    input_data_scaled, coeffs, wavelet_level_vars, level_num = \
        robust_MODWT_var(input_data=input_data_adj,
                         wavelet_name=wavelet_name,
                         robust_wavelet_toggle=robust_wavelet_toggle,
                         robust_wv_toggle=robust_wv_toggle,
                         robust_wavelet_ra_fun=robust_wavelet_ra_fun)

    if debug:
        # Fig 1: ori, peri, acf
        from matplotlib import pyplot as plt
        plot_signal_periodogram_acf(original_signal)
        # Fig 2: remove trend and robust prepressing for wavelet
        fig, axarr = plt.subplots(nrows=1, ncols=2, figsize=(9, 2))
        axarr[0].plot(real_data, 'b', label='real_data (normalized)')
        axarr[0].plot(trend_data, 'r', label='trend_data')
        axarr[0].legend()  # loc='upper right'  upper, lower, center
        axarr[1].plot(input_data_detrend, 'b', label='detrended_signal')
        axarr[1].plot(input_data_scaled,
                      'r',
                      label='signal after robust prepressing for wavelet')
        axarr[1].legend()
        plt.tight_layout()
        plt.show()
        # Fig 3: before Wavelet: signal, peri, acf
        plot_signal_periodogram_acf(input_data_scaled)
        # Fig 4: for each level of wavelet
        fontsize = 8
        fig, axarr = plt.subplots(nrows=level_num, ncols=2, figsize=(9, 12))

    # each level processing of wavelet coefficient
    output_periods_set = set()
    for ii in range(level_num):
        wavelet_level = ii + 1
        # print('wavelet_level=%d' % (wavelet_level))
        (data, coeff_d) = coeffs[level_num - ii - 1]

        # (3) robust Huber-Periodogram based Fisher-test
        # zero padding
        data_pad = np.hstack((coeff_d, np.zeros(len(coeff_d))))
        periodogram_values, ts_len = general_periodogram(
            input_data=data_pad,
            cost_fun=periodogram_cost_fun,
            Huber_t=periodogram_huber_t)

        # fisher test
        period_candi, period_range, per_T, pValue, _ = fisher_test(
            periodogram_values, ts_len, fisher_alph_th)

        # final ACF
        output_periods_set.add(period_candi)

        if debug:
            #  plot Detail Coef TS
            axarr[ii, 0].plot(coeff_d, 'g')
            axarr[ii, 0].set_title("Detail Coef: Var=%.3f)" %
                                   (wavelet_level_vars[ii]),
                                   fontsize=fontsize)
            axarr[ii, 0].set_ylabel("Level {}".format(wavelet_level),
                                    fontsize=fontsize,
                                    rotation=90)

            peak_idx = np.argmax(periodogram_values)
            peri_peak = periodogram_values[peak_idx]
            if period_candi != 0:
                axarr[ii, 1].plot(peak_idx,
                                  peri_peak,
                                  'r*',
                                  label='max value of periodogram')
            axarr[ii, 1].plot(periodogram_values,
                              'b',
                              label='periodogram values')
            axarr[ii, 1].legend()  # loc='upper right'  upper, lower, center
            axarr[ii, 1].set_title(
                "Periodogram, possible period by max value=%d, pValue=%.2e" %
                (per_T, pValue),
                fontsize=fontsize)
    if debug:
        plt.tight_layout()
        plt.show()
        # plot WV
        plt.figure(figsize=figsize)
        plt.plot(range(1, level_num + 1), (wavelet_level_vars),
                 '-*',
                 label='robust wv: biweight')
        plt.legend()
        plt.show()

    return sorted(output_periods_set)


def to_2dnp_array(X):
    """Convert array-like data (list/numpy array/pandas) to 2D
    numpy array.
    """
    if isinstance(X, np.ndarray):
        if X.ndim == 1:
            return X.reshape((-1, 1))
        if X.ndim == 2:
            return X
    if isinstance(X, Number):
        X = [X]
    X = np.array(X)
    X = X.reshape([-1, np.prod(X.shape) // X.shape[0]])
    return X


def assert_all_finite(X, allow_nan=False):
    """Throw a ValueError if X contains NaN or infinity.
    Parameters

    ----------
    X : array or sparse matrix
    allow_nan : bool
    """
    _assert_all_finite(X.data if sp.issparse(X) else X, allow_nan)


def _assert_all_finite(X, allow_nan=False):
    """Like assert_all_finite, but only for ndarray."""
    X = np.asanyarray(X)
    # First try an O(n) time, O(1) space solution for the common case that
    # everything is finite; fall back to O(n) space np.isfinite to prevent
    # false positives from overflow in sum method.
    is_float = X.dtype.kind in 'fc'
    if is_float and np.isfinite(X.sum()):
        pass
    elif is_float:
        msg_err = "Input contains {} or a value too large for {!r}."
        if (allow_nan and np.isinf(X).any()
                or not allow_nan and not np.isfinite(X).all()):
            type_err = 'infinity' if allow_nan else 'NaN, infinity'
            raise ValueError(msg_err.format(type_err, X.dtype))


def getPeriodogram(input_data):
    # reshape data for the use of np.fft.rfft
    input_data = input_data.reshape(-1, )
    # input_data -= np.mean(input_data) # remove dc
    # calcu periodogram for real-valued input_data
    periodogram = np.abs(np.fft.rfft(input_data, norm='ortho'))**2
    # same as np.abs(np.fft.rfft(input_data))**2 / len(input_data)
    # same as np.abs(np.fft.fft(input_data)
    #              [:(len(input_data)//2+1)])**2 / len(input_data)
    # note: set periodogram[0:2]=0
    periodogram[0:2] = 0
    return periodogram


def robust_getPeriodogram(input_data,
                          cost_fun='L1',
                          admm_maxiters=30,
                          admm_show_progress=False):
    # reshape data for the correct shape of X_mat
    input_data_tmp = input_data.copy()
    y = input_data_tmp.reshape(-1, 1)
    # remove DC part
    y -= np.mean(y)

    N = len(y)
    # out_len = N//2+1
    out_len = N // 2
    periodogram = np.zeros(out_len)
    n = np.arange(N).reshape(-1, 1)
    for k in range(2, out_len):  # note: set periodogram[0:1]=0
        w = 2 * np.pi * k / N
        X_mat = np.hstack((np.cos(w * n), np.sin(w * n)))

        # input-> y, X_mat; output-> bata
        if cost_fun == 'L2':
            X_mat_T = X_mat.T
            Xt_X = X_mat_T.dot(X_mat)
            inv_Xt_X = np.linalg.inv(Xt_X + 1e-20)
            bata = inv_Xt_X.dot(X_mat_T).dot(y)
        elif cost_fun == 'L1':

            X_mat_T = X_mat.T
            Xt_X = X_mat_T.dot(X_mat)
            inv_Xt_X = np.linalg.inv(Xt_X + 1e-20)
            S_inv = inv_Xt_X

            A_admm = np.zeros((1, 2))
            b_admm = np.zeros(1)
            F_admm = X_mat
            p_admm = y
            p_admm = p_admm.ravel()
            R_admm = g_lasso_admm_solver(A_admm,
                                         b_admm,
                                         F_admm,
                                         p_admm,
                                         reg_lambda=1.0,
                                         x0=None,
                                         rho=1.0,
                                         MAX_ITER=admm_maxiters,
                                         rel_par=1.0,
                                         QUIET=(not admm_show_progress),
                                         tol_abs=1e-4,
                                         S_inv=S_inv,
                                         warm_start_toggle=False)
            bata = R_admm[0].reshape(-1, 1)

        periodogram[k] = np.sum(np.abs(bata)**2)
        final_periodogram = (N / 4.0) * periodogram
        ts_len = N
    return final_periodogram, ts_len


def iniPeriods_byPeriodogram(Q,
                             candidates_num=None,
                             P_threshold_scale=1,
                             use_M_Periodogram=False,
                             P_threshold_approx=True,
                             k=100,
                             percentile=99):
    if use_M_Periodogram:
        getPeriodogram_fun = robust_getPeriodogram
    else:
        getPeriodogram_fun = getPeriodogram
    # based on getPeriodHints in the paper
    Q = Q.reshape(-1, )
    data_len = len(Q)
    maxPower = np.array([])
    periods_power = np.array([])
    periods_index = np.array([]).astype(int)

    for i in range(k):
        Qp = np.random.permutation(Q)

        if P_threshold_approx:  # approxi, speedup
            P = getPeriodogram(Qp - np.mean(Qp))
        else:
            P = getPeriodogram_fun(Qp)
        # do not consider the idx=0, must for pulse signal
        P[0] = 0
        power = np.max(P)
        maxPower = np.append(maxPower, power)

    maxPower.sort()
    P_threshold = maxPower[int(k * (percentile / 100)) - 1] * P_threshold_scale

    P = getPeriodogram_fun(Q)
    # Period trimming: remove sorted_idx>=data_len/2 and <=2
    # P[:3] = 0, P[len(P) // 2:] = 0
    for i in range(3, len(P) // 2):
        if P[i] > P_threshold:
            periods_power = np.append(periods_power, P[i])
            periods_index = np.append(periods_index, i)

    sorted_idx = np.argsort(periods_power)[::-1]
    if candidates_num is not None:
        sorted_idx = sorted_idx[:candidates_num]

    periods_index = periods_index[sorted_idx]
    periods_index_power = periods_power[sorted_idx]
    periods_candidates = np.round(data_len / periods_index).astype(int)

    return periods_candidates, periods_index, periods_index_power, P_threshold


def getACF(input_data):
    # acf_result = sm.tsa.acf(input_data, nlags=len(input_data))
    acf_result = statsmodels_acf(input_data,
                                 nlags=len(input_data) // 2,
                                 unbiased=True)
    return acf_result


def acf_ori_and_diff_fun(input_data, nlags, unbiased=False):
    # https://www.mathworks.com/help/econ/autocorr.html#btzjcln-4
    # www.statsmodels.org/dev/generated/statsmodels.tsa.stattools.acf.html
    # same results for the two functions
    # acf_data_sm = statsmodels_acf(
    #               real_data, nlags=len(real_data)//2, unbiased=True)
    # acf_data_our = acf_ori_and_diff_fun(real_data,
    #                nlags=len(real_data)//2, unbiased=True)

    T = len(input_data)
    result_len = nlags + 1  # +1 mean add the point at index=0
    result = np.zeros(result_len)
    result_diffFun = np.zeros(result_len)
    y = input_data
    ybar = np.mean(y)
    y_minus_ybar = y - ybar
    for k in range(result_len):
        normalizer = 1.0 / (T)
        if unbiased is True:
            normalizer = 1.0 / (T - k)
        first = y_minus_ybar[:(T - k)]
        second = y[k:] - ybar
        c_k = normalizer * np.sum(first * second)
        result[k] = c_k

        c_k_diffFun = normalizer * np.sum(np.square(first - second))
        result_diffFun[k] = c_k_diffFun

    c_0 = result[0]  # c_0 is sample variance of the TS
    # normalize result
    result /= c_0

    # use non-normalized result_diffFun for result_diffFun_new--> not good
    result_diffFun_new = result_diffFun.copy()
    result_diffFun_new[0] = 1  # for indx=0
    for tao in range(1, nlags + 1):  # for indx>0
        sum_d = np.sum(result_diffFun_new[1:(tao + 1)])
        sum_d *= (1.0 / tao)
        result_diffFun_new[tao] /= sum_d

    # normalize result_diffFun
    result_diffFun /= c_0
    return result, 1 - result_diffFun, result_diffFun_new
    # return result, 2 - result_diffFun, result_diffFun_new


def refinePeriods_byACF2ndDerivative(acf_result,
                                     periods_candidates_idx,
                                     ACF_L1trend_lambda,
                                     ori_data_len,
                                     ACF_threshold=None):

    if ACF_threshold is None:
        ACF_threshold = 1e-20  # indicate no acf_th
    # l1 trend for acf, for better results of later 2nd_derivative
    detrend_filter = L1TrendFilter_trend(piece_wise_fitting_method='linear',
                                         lambda_reg=ACF_L1trend_lambda)
    acf_trend = detrend_filter.fit_transform(acf_result).reshape(-1, )

    # ACF_2nd_Derivative
    acf_2nd_derivative = np.gradient(np.gradient(acf_trend))

    # refine candidate
    acf_periods = np.array([]).astype(int)
    N = ori_data_len
    for k in periods_candidates_idx:
        if k == 0 or k == 1:
            break
        low = int((N / (k + 1) + N / k) / 2 - 1)
        high = int((N / k + N / (k - 1)) / 2 + 1)
        periods_range = np.arange(low, high + 1)
        search_data = acf_2nd_derivative[periods_range]
        # if all(search_data < 0):
        # np.sum(search_data < 1e-5) / len(search_data) > 0.8:
        if np.sum(search_data < 1e-5) / len(search_data) > 0.6:
            min_idx = np.argmin(search_data)
            acf_period_candi = periods_range[min_idx]
            if acf_trend[acf_period_candi] > ACF_threshold:
                acf_periods = np.append(acf_periods, acf_period_candi)
    passed_check = len(acf_periods) > 0

    return passed_check, acf_periods, acf_trend, acf_2nd_derivative


def refinePeriods_byInputPrior(acf_passedCheck,
                               acf_periods,
                               prior_periods,
                               refine_tolerance=0.1,
                               candidates_num=1):

    # current just single period, TODO: multiple periods for prior_periods
    if candidates_num != 1:
        raise ValueError("current version of robust_period_length_detection\
             with input prior_periods only support candidates_num == 1.")
    final_passed_check = False
    final_periods = 0
    if acf_passedCheck and prior_periods is not None:
        for candi in acf_periods:
            if np.abs(candi -
                      prior_periods) / prior_periods < refine_tolerance:
                final_periods = prior_periods
                final_passed_check = True

    return final_passed_check, final_periods


def MAD(a):
    median = np.median(a)
    return 1.4826 * np.median(np.abs(a - median))


def mad_scale_filter(a, n_sigma, method='Hampel'):
    filtered_out = np.copy(a)

    lad_ori = MAD(a)
    lad = lad_ori * n_sigma
    mean = np.mean(a)
    delta = (a - mean)

    if method == 'Compress':
        delta[delta > lad] = np.log10(1 + delta[delta > lad] - lad)
        delta[delta < -lad] = -np.log10(1 - lad - delta[delta < -lad])
    elif method == 'Hampel':
        delta[delta > lad] = 0
        delta[delta < -lad] = 0
    else:
        raise ValueError("input method must be Compress or Hampel!")

    filtered_out = mean + delta

    return filtered_out


class Extended_AutoPeriod_PD():
    """AutoPeriod based Periodicity (period length) Detection for time series

    AutoPeriod based on the both time-frequency processing

    Parameters
    ----------
    candidates_num : int
        [default=1]
        The final most possible periodicity number
    prior_periods : int or array-like
        If the periods_prior is provided and there is periodity
        based on the detection,
    the period_length will be one of periods_prior.
    ACF_threshold : float
        [default=0.2]
        threshold used in ACF to control candidate quality
    Periodogram_th_scale : float
        [default=1.0]
        threshold scale used in Periodogram to control candidate quality
    mad_scale_sigma : float
        [default=2.0]
        mad sigma used in scale filter for input signal to scale down
        outlier points and patterns
    robustTrend_lambda : float
        [default=1e9]
        parameter used to control the robustTrend filter for input signal
    ACF_L1trend_lambda : float
        [default=0.01]
        parameter used in the ACF before doing 2nd_derivative
        to get better signal
    """

    def __init__(self,
                 candidates_num=1,
                 prior_periods=None,
                 ACF_threshold=0.2,
                 Periodogram_th_scale=1.0,
                 mad_scale_sigma=3.0,
                 robustTrend_lambda=1e9,
                 ACF_L1trend_lambda=1.0,
                 open_preprocessing=True,
                 permute_k=100,
                 permute_percentile=95,
                 debug=False):

        self.candidates_num = candidates_num
        self.prior_periods = prior_periods

        self.ACF_threshold = ACF_threshold
        self.Periodogram_th_scale = Periodogram_th_scale
        self.mad_scale_sigma = mad_scale_sigma
        self.robustTrend_lambda = robustTrend_lambda
        self.ACF_L1trend_lambda = ACF_L1trend_lambda
        self.open_preprocessing = open_preprocessing
        self.permute_k = permute_k
        self.permute_percentile = permute_percentile
        self.debug = debug

    def _plot_Periodicity_results(
            self, open_preprocessing, real_data, denoised_real_data,
            trend_data, input_data_denoisedetrend, input_data_scaled,
            Periodogram_threshold, periods_candidates_idx, periods_candidates,
            acf_result, acf_trend, ACF_threshold, acf_2nd_derivative,
            acf_passedCheck, acf_periods, final_passed_check, final_periods):
        from matplotlib import pyplot as plt
        figsize = (9, 2)
        if open_preprocessing:
            pd_denoise_trend = pd.DataFrame(real_data, columns=['input_data'])
            # pd_denoise_trend['denoised_real_data'] = pd.DataFrame(
            #     denoised_real_data)
            pd_denoise_trend['trend_data'] = pd.DataFrame(trend_data)
            pd_denoise_trend.plot(figsize=figsize)

            # input data to Periodogram and ACF
            pd_inputData = pd.DataFrame(input_data_denoisedetrend,
                                        columns=['data after detrend'])
            pd_inputData[
                'data further after mad_scaled (to Periodogram and ACF)'] = \
                pd.DataFrame(input_data_scaled)
            pd_inputData.plot(figsize=figsize)

        # results from Periodogram
        Periodogram_data = getPeriodogram(input_data_scaled.reshape(-1, ))
        pd_periodogram = pd.DataFrame(Periodogram_data,
                                      columns=['Periodogram'])
        pd_periodogram['P_threshold'] = pd.DataFrame(
            Periodogram_threshold * np.ones_like(Periodogram_data))
        print("Periodogram_threshold is %f" % Periodogram_threshold)
        print("periods_candidates_idx in Periodogram are:")
        print(periods_candidates_idx)
        print("period candidates from Periodogram are:")
        print(periods_candidates)
        print("candidates number from Periodogram is %d" %
              len(periods_candidates))

        # results from ACF
        pd_acf_only = pd.DataFrame(acf_result, columns=['ACF'])
        pd_acf = pd.DataFrame(acf_result, columns=['ACF'])
        pd_acf['ACF_Trend'] = pd.DataFrame(acf_trend)
        pd_acf['ACF_threshold'] = pd.DataFrame(ACF_threshold *
                                               np.ones_like(acf_trend))
        pd_acf_2nd_derivative = pd.DataFrame(acf_2nd_derivative,
                                             columns=['ACF_2nd_derivative'])

        # plot
        pd_periodogram.plot(figsize=figsize)
        pd_acf.plot(figsize=figsize)
        pd_acf_2nd_derivative.plot(figsize=figsize)

        print("(has_period, period_len) from ACF are:")
        print((acf_passedCheck, acf_periods))
        print("candidates number from ACF is %d" % len(acf_periods))
        print("\nFinal results ==>")
        print((final_passed_check, final_periods))
        return pd_periodogram, pd_acf_only

    def fit_transform(self, X, override=False):
        """
        This method performs hampel filtering with a sliding window from left
        to right side of data.

        Parameters
        ----------
        X : array-like, shape = (n_timelength, 1)
            The input data, only support 1D data currently.
        override : boolean
            To be used to override online case, ignore for now.

        Returns
        -------
        X_t : array-like, shape = (n_timelength, 1)
            The output latent variable data
        """

        candidates_num = self.candidates_num
        prior_periods = self.prior_periods

        permute_k = self.permute_k
        permute_percentile = self.permute_percentile
        ACF_threshold = self.ACF_threshold
        Periodogram_th_scale = self.Periodogram_th_scale
        mad_scale_sigma = self.mad_scale_sigma
        robustTrend_lambda = self.robustTrend_lambda
        ACF_L1trend_lambda = self.ACF_L1trend_lambda
        open_preprocessing = self.open_preprocessing
        debug = self.debug  # false

        # real_data = np.array(X).flatten().reshape(-1, 1)
        real_data = to_2dnp_array(X)
        assert_all_finite(real_data)
        ori_data_len = len(real_data)

        # tmp for current version
        # if prior_periods is None:
        #     raise ValueError(
        #         "current version of robust_period_length_detection\
        #          must input prior_periods")
        if open_preprocessing:
            # step-1: normalize data by StandardScaler
            scaler = StandardScaler()  # scale to around ~N(0,1)
            real_data = scaler.fit_transform(real_data)

            # # step-2: denosie data by BilateralFilter (no big diff)
            # denoise_filter = BilateralFilter(
            #     sigma_i=2.0, sigma_d=2.5, truncate=8.0)
            # denoised_real_data = denoise_filter.fit_transform(real_data)
            denoised_real_data = real_data

            # step-3: remove trend by robustTrend filter
            detrend_filter = HPTrendFilter_trend(lambda_reg=robustTrend_lambda,
                                                 quasi_robust=True,
                                                 upper_percent=0.9,
                                                 lower_percent=0.1)
            trend_data = detrend_filter.fit_transform(denoised_real_data)
            input_data_denoisedetrend = denoised_real_data - trend_data

            # step-4: scale down outlier point and outlier pattern
            # note: the output should be (-1,) shape for getPeriodHints or ACF
            input_data_scaled = mad_scale_filter(input_data_denoisedetrend,
                                                 mad_scale_sigma).reshape(
                                                     -1, )
        else:
            denoised_real_data = real_data
            trend_data = real_data
            input_data_denoisedetrend = real_data
            input_data_scaled = real_data

        input_data_with_preprocess = input_data_scaled.copy()
        # step-5: freq-domain process for periodicity candidates by periodogram
        periods_candidates, periods_candidates_idx, _, Periodogram_th = \
            iniPeriods_byPeriodogram(
                Q=input_data_scaled,
                candidates_num=candidates_num * 2,
                P_threshold_scale=Periodogram_th_scale,
                use_M_Periodogram=False,
                P_threshold_approx=True,
                k=permute_k,
                percentile=permute_percentile)
        # iniPeriods_byPeriodogram(
        #     input_data_scaled, candidates_num * 2, Periodogram_th_scale)

        # step-6: time-domain to refine periodicity length candidates:
        acf_result = getACF(input_data_scaled)
        acf_passedCheck, acf_periods, acf_trend, acf_2nd_derivative = \
            refinePeriods_byACF2ndDerivative(acf_result,
                                             periods_candidates_idx,
                                             ACF_L1trend_lambda,
                                             ori_data_len,
                                             ACF_threshold)
        if prior_periods is None:
            final_passed_check = acf_passedCheck
            final_periods = acf_periods[:candidates_num]

        elif prior_periods is not None:
            # step-7 prior_periods to refine periodicity length candidates:
            final_passed_check, final_periods = refinePeriods_byInputPrior(
                acf_passedCheck, acf_periods, prior_periods, candidates_num)

        if debug:
            pd_periodogram, pd_acf_only = self._plot_Periodicity_results(
                open_preprocessing, real_data, denoised_real_data, trend_data,
                input_data_denoisedetrend, input_data_scaled, Periodogram_th,
                periods_candidates_idx, periods_candidates, acf_result,
                acf_trend, ACF_threshold, acf_2nd_derivative, acf_passedCheck,
                acf_periods, final_passed_check, final_periods)
            return final_passed_check, final_periods,\
                input_data_with_preprocess, pd_periodogram, pd_acf_only

        return final_passed_check, final_periods, input_data_with_preprocess
