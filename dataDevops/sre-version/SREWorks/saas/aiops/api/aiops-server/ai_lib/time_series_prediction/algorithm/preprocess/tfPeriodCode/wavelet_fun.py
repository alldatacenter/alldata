import numpy as np
# import pandas as pd
from pywt import swt
from astropy.stats import biweight_midvariance


def median_mad(ts_data, mad_scaled=True):
    median = np.median(ts_data)
    if mad_scaled:
        mad = 1.4826 * np.median(np.abs(ts_data - median))
    else:
        mad = np.median(np.abs(ts_data - median))
    # handle mad is zero due to constant values of input
    if mad == 0.0:
        mad = 1.0
    return median, mad


def self_biweight_midvariance(wv_coeff):
    # 7ms for len(wv_coeff)=2048;
    # 280us from astropy.stats.biweight_midvariance
    w = wv_coeff
    med, mad = median_mad(w, mad_scaled=False)

    u = (w - med) / (9 * mad)
    M = len(w)
    sum_numerator = 0
    sum_denominator = 0
    for t in range(M):
        if np.abs(u[t]) < 1:
            u2 = u[t]**2
            sum_numerator += ((w[t] - med)**2) * ((1 - u2)**4)
            sum_denominator += (1 - u2) * (1 - 5 * u2)
    # handle sum_denominator is zero due to constant values of input
    if sum_denominator == 0.0:
        sum_denominator = 1.0
    return M * sum_numerator / (sum_denominator**2)


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


def robust_MODWT_var(input_data,
                     wavelet_name='db4',
                     robust_wavelet_toggle=True,
                     robust_wv_toggle=True,
                     robust_wavelet_ra_fun='Huber'):
    # Suppose len(input_data)>=3*T or 4*T for accurate periodicity detection.
    # if len(input_data)=3*T with sigle sin wave, now the sin wave
    # should be in the int(np.log2(len(input_data_scaled))) - 2 level,
    # for T < len(input_data)/3, the signal should in lower level.
    level_num = int(np.log2(len(input_data))) - 2
    if robust_wavelet_toggle:
        input_data_scaled = ra_estimator(
            input_data,
            ra_est_fun=robust_wavelet_ra_fun,
            ra_est_para=None,
            mad_scaled=True)
    else:
        input_data_scaled = input_data
    # note: norm=True in swt corresponding to MODWT
    coeffs = swt(input_data_scaled.reshape(-1,), wavelet_name, level=level_num, norm=True)
    wavelet_level_vars = []
    for ii in range(level_num):
        # wavelet_level = ii + 1
        (data, coeff_d) = coeffs[level_num - ii - 1]
        if wavelet_name == 'db4':
            L_1 = 8
            L_j = (2**ii - 1) * (L_1 - 1) + 1
        else:
            assert (wavelet_name == 'db4')
        coeff_nonboundary_effect = coeff_d[(L_j - 1):]
        if robust_wv_toggle:
            rob_unbiased_var = biweight_midvariance(coeff_nonboundary_effect)
        else:
            rob_unbiased_var = np.var(coeff_nonboundary_effect)
        wavelet_level_vars.append(rob_unbiased_var)

    return input_data_scaled, coeffs, wavelet_level_vars, level_num
