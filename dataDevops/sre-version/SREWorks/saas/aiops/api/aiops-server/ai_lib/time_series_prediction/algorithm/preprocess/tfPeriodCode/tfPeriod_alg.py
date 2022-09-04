import numpy as np
from sklearn.preprocessing import StandardScaler
# from matplotlib import pyplot as plt
from .trend_filters import HPTrendFilter_trend
from .wavelet_fun import robust_MODWT_var
from .periodogram_acf_fun import general_periodogram, acf_med_from_periodogram
from .fisher_siegel_test import fisher_test
from .util import plot_signal_periodogram_acf
figsize = (9, 2)


def two_value_similar(a, b, margin_percent=10):
    return np.abs(a - b) / max(a, b) <= margin_percent / 100.0


# assume the similar values at most 2 (next to each other since modwt)
def merge_similar_periods(periods_candi, margin_percent=10):
    pd_num = len(periods_candi)
    if pd_num <= 1:
        return periods_candi

    output = set()
    ii = 0
    while ii < pd_num:
        if ii == pd_num - 1:
            a = periods_candi[ii]
            output.add(a)
            break
        a = periods_candi[ii]
        b = periods_candi[ii + 1]
        if two_value_similar(a, b, margin_percent):
            output.add(int((a + b) / 2))
            ii += 2
        else:
            output.add(a)
            ii += 1
    return sorted(output)


def merge_two_periods(per_a, per_b, eng_a, eng_b):
    out_eng = eng_a + eng_b
    out_per = (per_a * eng_a + per_b * eng_b) / out_eng
    return out_per, out_eng


def merge_similar_periods_by_ranking(in_periods_candi,
                                     in_periods_candi_energy,
                                     margin_percent=10):
    pd_num = len(in_periods_candi)
    if pd_num <= 1:
        return in_periods_candi, in_periods_candi_energy

    # ranking based on in_periods_candi in order to merge later
    sorted_idx = np.argsort(in_periods_candi)
    periods_candi = np.array(in_periods_candi)[sorted_idx].tolist()
    periods_candi_energy = np.array(
        in_periods_candi_energy)[sorted_idx].tolist()

    out_periods_candi = []
    out_periods_candi_energy = []
    ii = 0
    while ii < pd_num:
        if ii == pd_num - 1:  # the last one
            per_a, eng_a = periods_candi[ii], periods_candi_energy[ii]
            out_periods_candi.append(per_a)
            out_periods_candi_energy.append(eng_a)
            break
        per_a, eng_a = periods_candi[ii], periods_candi_energy[ii]
        per_b, eng_b = periods_candi[ii + 1], periods_candi_energy[ii + 1]

        if two_value_similar(per_a, per_b, margin_percent):
            out_per, out_eng = merge_two_periods(per_a, per_b, eng_a, eng_b)
            ii += 2
        else:
            out_per, out_eng = per_a, eng_a
            ii += 1
        out_periods_candi.append(out_per)
        out_periods_candi_energy.append(out_eng)

    return out_periods_candi, out_periods_candi_energy


def tfPeriod(original_data,
             trend_remove_toggle=True,
             robustTrend_toggle=True,
             trend_lambda=1e8,
             wavelet_name='db4',
             robust_wavelet_toggle=True,
             robust_wv_toggle=True,
             robust_wavelet_ra_fun='Huber',  # Huber, Tukey
             periodogram_cost_fun='Huber',  # Huber, LS, LAD
             # (# Note: for LAD, use Huber with t=.001 have better stability)
             periodogram_huber_t=1.0,
             periodogram_admm_maxiters=50,
             fisher_alph_th=1e-5,
             acf_out_len=None,
             acf_peak_th=0.5,
             acf_avg='median',
             output_ranking=False,  # added new feature
             output_period_maxnum=None,
             debug=False):

    output_periods_set = set()
    output_periods_list = []
    output_periods_energy_list = []
    # at least 16 points length
    if len(original_data) < 16:
        return sorted(output_periods_set)

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
        fig, axarr = plt.subplots(nrows=level_num, ncols=3, figsize=(9, 12))

    # each level processing of wavelet coefficient
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
        passed_check_acf, final_period, acf_period, acf_result, peaks_idx = \
            acf_med_from_periodogram(coeff_d,
                                     periodogram_values,
                                     acf_peak_th=acf_peak_th,
                                     acf_avg=acf_avg,
                                     acf_out_len=acf_out_len,
                                     period_candi=period_candi,
                                     period_range=period_range)
        if passed_check_acf:
            output_periods_set.add(final_period)
            # for ranking (TODO: "for ii in range(level_num)"" based on wv)
            output_periods_list.append(final_period)
            output_periods_energy_list.append(wavelet_level_vars[ii])

        if debug:
            #  plot Detail Coef TS
            axarr[ii, 0].plot(coeff_d, 'g')
            axarr[ii, 0].set_title("Wavelet Coef: Var=%.3f" %
                                   (wavelet_level_vars[ii]),
                                   fontsize=fontsize)
            axarr[ii, 0].set_ylabel("Level {}".format(wavelet_level),
                                    fontsize=fontsize,
                                    rotation=90)
            #  plot periodogram
            axarr[ii, 1].plot(periodogram_values, 'r')
            axarr[ii, 1].set_title("PER: p=%.2e; per_T=%d,fin_T=%d" %
                                   (pValue, per_T, period_candi),
                                   fontsize=fontsize)
            # ## tmp for paper fig
            # axarr[ii, 1].set_title("Periodogram: p=%.2e; per_T=%d" %
            #                        (pValue, period_candi),
            #                        fontsize=fontsize)
            #  plot ACF
            acf_result_pk_th = acf_peak_th * np.ones_like(acf_result)
            acf_result_peaks = acf_result[peaks_idx]
            axarr[ii, 2].plot(acf_result, 'b')
            axarr[ii, 2].plot(acf_result_pk_th, 'g')
            if passed_check_acf:
                axarr[ii, 2].plot(peaks_idx, acf_result_peaks, 'r*')
            axarr[ii,
                  2].set_title("ACF: acf_T=%d,fin_T=%d; Period=%s" %
                               (acf_period, final_period, passed_check_acf),
                               fontsize=fontsize)

    if debug:
        plt.tight_layout()
        plt.show()
        # plt.savefig("simEx-WaveFFTACF.pdf")
        # print("wavelet_level_vars")
        # print(wavelet_level_vars)
        # plot WV
        plt.figure(figsize=(9, 3.7))
        fontsize=14
        plt.plot(range(1, level_num + 1), (wavelet_level_vars),
                 '-*',
                 label='robust wavelet variance')
        plt.ylim((0, 0.23))
        plt.legend(loc='upper left',fontsize=fontsize)
        plt.xlabel("Wavelet Level")
        plt.ylabel("Wavelet Variance")
        plt.show()
        # plt.savefig("simEx-WaveVariance.pdf")

    if output_ranking:
        for idx in range(5):
            output_periods_list, output_periods_energy_list = \
                merge_similar_periods_by_ranking(
                    output_periods_list, output_periods_energy_list)

        # final output: rank and round
        sorted_idx = np.argsort(output_periods_energy_list)[::-1]
        output_periods_list = np.array(output_periods_list)[sorted_idx].astype(
            int).tolist()
        # output_periods_energy_list = np.array(
        #     output_periods_energy_list)[sorted_idx].tolist()
        final_out = output_periods_list
        if output_periods_list is not None and len(output_periods_list) > 1:
            final_out = output_periods_list[:output_period_maxnum]
    else:
        tmp_out = sorted(output_periods_set)
        tmp_out1 = merge_similar_periods(tmp_out)
        final_out = merge_similar_periods(tmp_out1)

    return final_out
