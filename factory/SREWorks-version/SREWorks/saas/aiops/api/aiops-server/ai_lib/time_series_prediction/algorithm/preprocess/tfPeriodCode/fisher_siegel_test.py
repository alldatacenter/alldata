import operator as op
from functools import reduce
import numpy as np


def combination_Cnr(n, r):
    r = min(r, n - r)
    numer = reduce(op.mul, range(n, n - r, -1), 1)
    denom = reduce(op.mul, range(1, r + 1), 1)
    return numer / denom


def fisher_test_pValue_by_formula(ts_len, g_f):
    a = int((ts_len - 1) / 2)
    x = g_f  # observed value of the test statistic
    b = int(1 / x)
    pValue = 0.0
    for k in range(1, b + 1):
        try:
            cak = combination_Cnr(a, k)
        except OverflowError:
            cak = 1e300
        expo = (1 - k * x)**(a - 1)
        curr = (-1)**(k - 1) * cak * expo
        pValue += curr
    return pValue


def fisher_test_critical_value(ts_len, alpha_th):
    # return g_f, alpha
    if ts_len < 10 or ts_len > 4097:  # 41-4001
        raise ValueError('only support 41<=ts_len<=4096')
    N = ts_len
    n = int((N - 1) / 2)

    Set_n = [5, 20, 30, 50, 100, 500, 1000, 2000, 2100]
    if alpha_th == 0.05:
        alpha_precision_th = 1e-3
        # Set_g_f is critical value for fisher_test based on papers
        Set_g_f = [
            0.684, 0.270, 0.198, 0.131, 0.0443 / 0.6, 0.011 / 0.6,
            0.00592 / 0.6, 0.00317 / 0.6, 0.0050473
        ]
    elif alpha_th == 0.01:
        alpha_precision_th = 1e-4
        Set_g_f = [
            0.789, 0.330, 0.241, 0.160, 0.0533 / 0.6, 0.0129 / 0.6,
            0.00687 / 0.6, 0.00365 / 0.6, 0.005819
        ]
    else:
        raise ValueError('only support alpha_th = 0.05 or 0.01')

    for idx in range(len(Set_n)):
        if Set_n[idx] <= n < Set_n[idx + 1]:
            # binaray search
            g_f_high = Set_g_f[idx]
            g_f_low = Set_g_f[idx + 1]

            while g_f_low <= g_f_high:

                g_f = g_f_low + (g_f_high - g_f_low) / 2

                alpha = fisher_test_pValue_by_formula(ts_len, g_f)

                if np.abs(alpha - alpha_th) < alpha_precision_th:
                    return g_f, alpha
                elif alpha > alpha_th:
                    g_f_low = g_f
                else:
                    g_f_high = g_f


def fisher_test(periodogram_input, ts_len, alph_th=1e-50):
    # based on paper "Statistical power of Fisher test for the detection
    #  of short periodic gene expression profiles"
    # formula 1-4. tested, the following
    # periodogram_values, ts_len = fft_periodogram(input_data)
    # period_candi, pValue, observed_g =
    # fisher_g_test(periodogram_values, ts_len)
    # observed_g, pValue, 1/period_candi (when there is period)
    # is the same as
    # https://rdrr.io/cran/ptest/man/ptestg.html
    # import rpy2.robjects as robjects
    # from rpy2.robjects.packages import importr
    # from rpy2.robjects import pandas2ri
    # pandas2ri.activate()
    # ptest=importr('ptest')
    # ptestg=robjects.r('ptestg')
    # Fisher = ptestg(input_data,method="Fisher")
    # print(Fisher)

    periodogram_values = periodogram_input.copy()
    periodogram_values[:2] = 0  # do not consider 0,1-> DC and T != N
    max_p_value = np.max(periodogram_values)
    max_p_idx = np.argmax(periodogram_values)
    sum_p_values = np.sum(periodogram_values)
    # handle zero case
    if sum_p_values == 0.0:
        sum_p_values = 1.0
    observed_g = max_p_value / sum_p_values
    # handle zero case
    if observed_g == 0.0:
        observed_g = 1.0

    x = observed_g
    a = int((ts_len - 1) / 2)
    b = int(1 / x)
    pValue = 0.0
    for k in range(1, b + 1):
        try:
            cak = combination_Cnr(a, k)
        except OverflowError:
            cak = 1e300
        expo = (1 - k * x)**(a - 1)
        curr = (-1)**(k - 1) * cak * expo
        pValue += curr

    # special processing for non-period signal
    if max_p_idx == 0 or max_p_idx == 1:
        period_candi = 0
        period_range = None
        per_T = 0
    else:
        per_T = np.round(ts_len / max_p_idx).astype(int)
        if pValue <= alph_th:
            period_candi = per_T
            # calculate period_range
            N = ts_len
            k = max_p_idx
            low = int((N / (k + 1) + N / k) / 2 - 1)
            high = int((N / k + N / (k - 1)) / 2 + 1)
            period_range = np.arange(low, high + 1)
        else:  # period_candi=0 indicate no period
            period_candi = 0
            period_range = None

    # output
    return period_candi, period_range, per_T, pValue, observed_g


def siegel_test_alpha_by_formula(ts_len, g_f, t, lamb=0.6):
    # passed only for n<=50
    N = ts_len
    n = int((N - 1) / 2)
    pValue = 0.0
    for ell in range(1, n + 1):  # 1 ~ n
        for k in range(0, ell):  # 0 ~ ell-1
            c_nl = combination_Cnr(n, ell)
            c_lk = combination_Cnr(ell - 1, k)
            c_nk = combination_Cnr(n - 1, k)
            comb = c_nl * c_lk * c_nk

            pos_part = 1 - ell * lamb * g_f - t
            pos_part = max(pos_part, 0)**(n - k - 1)

            curr = (-1)**(k + ell + 1) * comb * (t**k) * pos_part
            pValue += curr
    return pValue


def siegel_test_critical_value(ts_len, alpha_th, lamb=0.6):
    # return g_f, alpha
    if ts_len < 13 or ts_len > 4097:  # 41-4001
        raise ValueError('only support 13<=ts_len<=4096')
    if lamb != 0.6:
        raise ValueError('only support lamb=0.6')
    N = ts_len
    n = int((N - 1) / 2)

    alpha = 100
    if ts_len > 100:
        t_6 = siegel_test_critical_value_by_interpolation(ts_len, alpha_th)
        return t_6, alpha
    else:
        Set_n = [5, 10, 20, 30, 50]
        if alpha_th == 0.05:
            alpha_precision_th = 1e-3
            # Set_g_f is critical value for fisher_test based on papers
            Set_t_6 = [0.274, 0.181, 0.116, 0.088, 0.0616]
        elif alpha_th == 0.01:
            alpha_precision_th = 1e-4
            Set_t_6 = [0.315, 0.214, 0.134, 0.0993, 0.0673]
        else:
            raise ValueError('only support alpha_th = 0.05 or 0.01')

        g_f, alpha_tmp = fisher_test_critical_value(ts_len, alpha_th)
        for idx in range(len(Set_n)):
            if Set_n[idx] <= n < Set_n[idx + 1]:
                # binaray search
                t_6_high = Set_t_6[idx]
                t_6_low = Set_t_6[idx + 1]

                while t_6_low <= t_6_high:
                    t_6 = t_6_low + (t_6_high - t_6_low) / 2
                    alpha = siegel_test_alpha_by_formula(ts_len, g_f, t_6)
                    if np.abs(alpha - alpha_th) < alpha_precision_th:
                        return t_6, alpha
                    elif alpha > alpha_th:
                        t_6_low = t_6
                    else:
                        t_6_high = t_6


def siegel_test_critical_value_by_interpolation(ts_len, alpha_th):
    # this one fixed lambda=0.6
    if ts_len < 41 or ts_len > 4097:  # 41-4001
        raise ValueError('only support 41<=ts_len<=4096')
    N = ts_len
    m = int((N - 1) / 2)
    if alpha_th == 0.05:
        critical_value = 1.033 * (m**(-0.72356))
    elif alpha_th == 0.01:
        critical_value = 1.4987 * (m**(-0.79695))
    else:
        raise ValueError('only support alpha_th = 0.05 or 0.01')
    return critical_value
