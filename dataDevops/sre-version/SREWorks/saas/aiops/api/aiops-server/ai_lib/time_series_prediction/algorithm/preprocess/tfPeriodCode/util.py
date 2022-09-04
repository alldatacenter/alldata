import numpy as np
from statsmodels.tsa.stattools import acf as statsmodels_acf
# from matplotlib import pyplot as plt
from .periodogram_acf_fun import general_periodogram


def mu_var_power_fun(signal):
    return (np.mean(signal), np.std(signal)**2, np.mean(np.abs(signal)**2))


def plot_signal_periodogram_acf(original_signal):
    # plot ori signal
    from matplotlib import pyplot as plt
    fontsize = 8
    mu, var, power = mu_var_power_fun(original_signal)
    fig, axarr = plt.subplots(nrows=1, ncols=3, figsize=(9, 2))
    axarr[0].plot(original_signal, 'b')
    axarr[0].set_title("Signal: u,v,p=(%.3f ,#, %.2f,%.2f)" % (mu, var, power),
                       fontsize=fontsize)
    periodogram_values, _ = general_periodogram(original_signal, cost_fun='LS')
    # periodogram_values, _ = fft_periodogram(original_signal)
    axarr[1].plot(periodogram_values)
    axarr[1].set_title("Periodogram", fontsize=fontsize)
    # acf_ori_and_diff_fun, statsmodels_acf
    acf_data_sm = statsmodels_acf(original_signal,
                                  nlags=int(len(original_signal) * 0.75),
                                  unbiased=True)
    axarr[2].plot(acf_data_sm)
    axarr[2].set_title("ACF", fontsize=fontsize)
    plt.tight_layout()
    plt.show()
