import numpy as np
# import pandas as pd
from scipy.signal import find_peaks
from .admm_solver_for_lad_huber import huverL2_admm_solver, g_lasso_admm_solver


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


def general_periodogram(
        input_data,
        cost_fun='LS',  # Huber, LAD, LS,
        Huber_t=1,
        remove_dc_toggle=True,
        admm_maxiters=100,
        admm_show_progress=False):

    # reshape data for the correct shape of X_mat
    input_data_tmp = input_data.copy()
    y = input_data_tmp.reshape(-1, 1)
    # remove DC part
    if remove_dc_toggle:
        y = y - np.mean(y)
    ts_len = len(y)
    out_len = ts_len // 2 + 1

    # for general periodogram
    if cost_fun == 'LS':
        y = y.reshape(-1, )
        final_periodogram = np.abs(np.fft.fft(y)[:out_len])**2 / ts_len
    else:
        # for robust-M periodogram
        # note w should be in (0,pi), so later 'k in range(1, out_len-1)'
        periodogram = np.zeros(out_len)
        N = ts_len
        n = np.arange(N).reshape(-1, 1)
        for k in range(1, out_len - 1):
            # note: set periodogram[0:1]=0 for Huber and LAD
            w = 2 * np.pi * k / N
            X_mat = np.hstack((np.cos(w * n), np.sin(w * n)))

            # input-> y, X_mat; output-> bata
            if cost_fun == 'L2':
                X_mat_T = X_mat.T
                Xt_X = X_mat_T.dot(X_mat)
                inv_Xt_X = np.linalg.inv(Xt_X + 1e-20)
                bata = inv_Xt_X.dot(X_mat_T).dot(y)
            elif cost_fun == 'Huber':
                F_admm = np.zeros((1, 2))
                p_admm = np.zeros(1)
                A_admm = X_mat
                b_admm = y
                b_admm = b_admm.ravel()

                R_admm = huverL2_admm_solver(A_admm,
                                             b_admm,
                                             F_admm,
                                             p_admm,
                                             t=Huber_t,
                                             reg_lambda=1.0,
                                             x0=None,
                                             rho=1.0,
                                             MAX_ITER=admm_maxiters,
                                             rel_par=1.,
                                             QUIET=(not admm_show_progress))
                bata = R_admm[0].reshape(-1, 1)

            elif cost_fun == 'LAD':
                # Note: for LAD, use Huber with t=.001 have better stability
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
        # special processing for the N/2 point
        # note that the input data is even length, the fft at N/2 is
        fft_half = np.sum(y[::2] - y[1::2])
        final_periodogram[out_len - 1] = fft_half * fft_half / N

    return final_periodogram, ts_len


def acf_med_from_periodogram(
        input_data,
        periodogram,
        use_periodogram=True,  # default: True
        direct_act_out='acf_ori',  # acf_ori, YIN_1, YIN_2
        acf_peak_th=0.5,
        acf_avg='median',
        acf_out_len=None,
        period_candi=None,
        period_range=None):

    least_peak_nums = 3
    data_length = len(input_data)
    if acf_out_len is None:
        # based on: len(input_data)>=4*T for accurate periodicity detection.
        # if len(input_data)=4*T sin wave, acf contains 3 peaks
        # at 25%len, 50%len, and 75%len. so we use 75%len
        # (acf_out_len = int(len(input_data)*0.75) )
        #  for acf figure and it should contains 3 peaks now.
        # when T<len(input_data)/4, we have more
        # peaks, so we set least_peak_nums = 3
        # acf_out_len = int(data_length * 0.75)
        acf_out_len = int(data_length * 0.9)

    if period_candi is not None and period_candi != 0:
        peak_distance = int(period_candi / 3 * 2)
    else:
        peak_distance = None

    # calcu ACF
    if use_periodogram:
        XX_psd2 = np.hstack(
            (periodogram,
             periodogram[1:-1][::-1]))  # note that assume data_pad is even
        normalizer = 1.0 / (data_length - np.arange(data_length))
        r_xx = np.fft.ifft(XX_psd2)[:data_length].real
        r_xx *= normalizer  # normalize for position, like 1/(N-k)
        if r_xx[0] != 0:
            r_xx /= r_xx[0]  # normalize, r_xx[0] is the variance
        acf_result = r_xx[:acf_out_len]
    else:
        # acf_result = statsmodels_acf(input_data, acf_out_len, True)
        acf_ori_out, YIN_1_out, YIN_2_out = acf_ori_and_diff_fun(
            input_data, nlags=acf_out_len, unbiased=True)
        if direct_act_out == 'acf_ori':
            acf_result = acf_ori_out
        elif direct_act_out == 'YIN_1':
            acf_result = YIN_1_out
        elif direct_act_out == 'YIN_2':
            acf_result = YIN_2_out

    # find peak above acf_peak_th
    peaks_idx, _ = find_peaks(acf_result,
                              height=acf_peak_th,
                              prominence=acf_peak_th / 2,
                              distance=peak_distance)

    # indicate no period form fisher_g_test
    if period_candi == 0:
        passed_check_acf = False
        acf_period = 0
        final_period = 0
        return passed_check_acf, final_period,\
            acf_period, acf_result, peaks_idx

    # check number of peaks
    if len(peaks_idx) < least_peak_nums:
        passed_check_acf = False
        acf_period = 0
        final_period = 0
    else:
        # get the period of diff by doing one more diff and calcu median
        peaks_2nd_diff = (np.diff(np.hstack((0, peaks_idx))))
        if acf_avg == 'mean':
            acf_period = int(np.mean(peaks_2nd_diff))
        elif acf_avg == 'median':
            acf_period = int(np.median(peaks_2nd_diff))

        if period_range is not None:
            passed_check_acf = acf_period in period_range
            final_period = 0 if not passed_check_acf else acf_period
        else:
            passed_check_acf = True
            final_period = acf_period

    return passed_check_acf, final_period, acf_period, acf_result, peaks_idx
