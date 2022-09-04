"""Our Proposed Robust STL based on
Spare Model and Non-local Season bilateral filtering

Input: original_data - T x 1
Output: decomposed_data - T x 3 (trend, season, irregular)

Author: Qingsong <qingsong.wen@alibaba-inc.com>
"""
from numpy.linalg import norm, cholesky
import scipy.sparse as sparse
import time
import numbers
# import logging
from numbers import Number
# import scipy.sparse as sp
# from scipy.sparse.linalg import lsqr
# from scipy.sparse import csc_matrix
from sklearn.preprocessing import StandardScaler
from cvxopt import matrix
import pandas as pd
import numpy as np
from scipy.linalg import circulant
from .fast_stl_utils import DetailA, PDHG, gADMMSolver
"""two functions to get trend and season:
Sparse and robust method to get trend
Bilateral season adjust to get season

Author: Qingsong <qingsong.wen@alibaba-inc.com>
"""


def gaussian_expfun(x, sigma):
    minimal_sigma_n = 1e-6
    sigma = minimal_sigma_n if sigma < minimal_sigma_n else sigma
    return np.exp(-(x * x) / (2.0 * sigma * sigma))


def gaussian_kernel(sigma, radius):
    """Computes 1D Gaussian kernel weights

    The length of weights is 2*radius + 1
    """
    if sigma <= 0:
        raise ValueError('sigma must be larger than zero.')
    tmp_x = np.arange(-radius, radius + 1)
    phi_x = gaussian_expfun(tmp_x, sigma)
    phi_x /= phi_x.sum()
    return phi_x


def calcu_noise_sigma(ts_data):
    """Calculate the eatimated noise's Standard Deviation

    This method here is based on the paper:
    Theo Gasser, Lothar Sroka, and Christine Jennen-Stienmetz.
    Residual variance and residual pattern in nonlinear regression.
    """
    ts_data_len = len(ts_data)
    if ts_data_len < 3:
        raise Exception('ts_data_len should be at least 3')
    y_ip1 = ts_data[2:]
    y_i = ts_data[1:-1]
    y_im1 = ts_data[:-2]
    sigma_n2 = (2.0 / (3 * len(y_ip1))) * sum(
        (0.5 * y_im1 - y_i + 0.5 * y_ip1)**2)
    sigma_n = np.sqrt(sigma_n2)
    # sigma_n = 1e-5 if sigma_n < 1e-5 else sigma_n
    minimal_sigma_n = 1e-6
    sigma_n = minimal_sigma_n if sigma_n < minimal_sigma_n else sigma_n
    return sigma_n


def data_validity_check(ts_data):

    # old version-->if isinstance(ts_data, int) or isinstance(ts_data, float):
    if isinstance(ts_data, numbers.Number):
        ts_data = np.array([ts_data])

    if not (isinstance(ts_data, list) or isinstance(ts_data, np.ndarray)
            or isinstance(ts_data, pd.DataFrame)):
        raise ValueError('For denoising, input data must be list, np.ndarray,'
                         'or pd.DataFrame (for online, input can be single'
                         'point with int/flaot data type)!')

    # check pandas
    if isinstance(ts_data, pd.DataFrame):
        if ts_data.shape[1] != 1:
            raise ValueError('For denoising, if input data is pandas dataframe'
                             ', it must be 1D data!')
    # check array or list
    if isinstance(ts_data, list):
        ts_data = np.array(ts_data)
    if isinstance(ts_data, np.ndarray):
        if ts_data.ndim > 2:
            raise ValueError('For denoising, input data cannot be 3D or higher'
                             'dimentional array or list!')
        if ts_data.ndim == 2:
            if not (ts_data.shape[0] == 1 or ts_data.shape[1] == 1):
                raise ValueError('For denoising, if input data is 2D array or'
                                 'list, one of the dim must be 1!')


def bilateral_weighted_data(input_data,
                            sigma_i=4,
                            leftmost_dist_w=0.8,
                            sigma_d=None,
                            refer_value=None,
                            normalize_toggle=True):
    # check leftmost_dist_w
    if leftmost_dist_w <= 0 or leftmost_dist_w >= 1.0:
        raise ValueError('leftmost_dist_w should 0 < leftmost_dist_w < 1')
    # Ini
    ts_data_ori = to_2dnp_array(input_data)
    data_len = len(ts_data_ori)
    # normalize
    if normalize_toggle:
        scaler = StandardScaler()
        ts_data = scaler.fit_transform(ts_data_ori)
    else:
        ts_data = ts_data_ori.copy()
    if refer_value is None:
        refer_value = ts_data[-1]
    # distance_weights
    refer_idx = -1  # default, the last point for distance_refer_idx
    leftmost_idx = data_len + refer_idx
    dx = np.arange(-leftmost_idx, -refer_idx).reshape(-1, 1)
    if sigma_d is None:
        sigma_d = np.sqrt(-0.5 * leftmost_idx * leftmost_idx /
                          np.log(leftmost_dist_w))
    distance_weights = gaussian_expfun(dx, sigma_d)
    # intensity_weights
    sigma_n = np.std(ts_data)
    sig_I = sigma_i * sigma_n
    dy = ts_data - refer_value
    intensity_weights = gaussian_expfun(dy, sig_I)
    # final bilateral_weights
    bilateral_weights = distance_weights * intensity_weights
    weighted_data = ts_data_ori * bilateral_weights
    return weighted_data, distance_weights,\
        intensity_weights, bilateral_weights


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


"""Two Bilateral filter classes for denoising time series.

Input: ts_data - T x 1
Output: filterd_ts_data - T x 1

"""


class BilateralFilter():
    """Basic Bilateral Filter for denoising time series

    It is used to as a baseline for denoising time signals under different
    Bilateral Filters.

    The filtering width is around 2*(truncate*sigma) + 1

    Parameters
    ----------
    sigma_i : double, optional
        [default=2.5]
        The standard deviation for intensity used in bilateral gaussian kernel
    sigma_d : double, optional
        [default=2.5]
        The standard deviation for distance used in bilateral gaussian kernel
    truncate: double, optional
        [default=6.0]
        Used for the radius of the filter, which equals to truncate times
        sigma_d
    pad_mode: str, 'symmetric' or 'reflect'
        [default='symmetric']
        The scheme used for padding edge data points
        Suppose we have data (a b c d ...... e f g h):
        For the edge data, if they are extended using 'symmetric' about edge of
        the last pixel, we have (c b a | a b c d ...... e f g h | h g f)
        For the edge data, if they are extended using 'reflect' about the edge
        of the last pixel, we have (d c b | a b c d ...... e f g h | g f e)
    """

    def __init__(self,
                 sigma_i=2.0,
                 sigma_d=2.5,
                 truncate=8.0,
                 pad_mode='symmetric'):
        self.sigma_i = sigma_i
        self.sigma_d = sigma_d
        self.truncate = truncate
        self.pad_mode = pad_mode

    def fit_transform(self, ts_data, override=True):
        """
        This method to do denoising filtering process for time sereis.

        Parameters
        ----------
        ts_data : array-like, shape = (n_timelength, 1) or (n_timelength,)
            The input data, 1D data: can be numpy array, list,
            or pandas dataframe.
        override : boolean
            To be used for online case, ignore for now.

        Returns
        -------
        filterd_out : array-like, shape = (n_timelength, 1)
            The output of denoised time series
        """
        sigma_i = self.sigma_i
        sigma_d = self.sigma_d
        truncate = self.truncate
        pad_mode = self.pad_mode
        data_validity_check(ts_data)
        ts_data = np.array(ts_data).flatten()

        radius = int(truncate * sigma_d + 0.5)
        dx = np.arange(-radius, radius + 1)
        sig_D = sigma_d
        exp_D = gaussian_expfun(dx, sig_D)

        # calculate the eatimated noise variance
        sigma_n = calcu_noise_sigma(ts_data)
        sig_I = sigma_i * sigma_n  # note sigma_i is just a multiplicative para

        if (pad_mode == 'symmetric') or (pad_mode == 'reflect'):
            ts_data_pad = np.pad(ts_data, radius, pad_mode)
        else:
            raise RuntimeError(
                'The pad_mode is supported; only support symmetric or reflect')

        data_len = len(ts_data)
        filterd_out = np.zeros(data_len)
        for idx in range(data_len):

            idx_pad = idx + radius
            data_window = ts_data_pad[(idx_pad - radius):(idx_pad + radius +
                                                          1)]

            dy = data_window - ts_data_pad[idx_pad]
            exp_I = gaussian_expfun(dy, sig_I)
            bilateral_weights = exp_I * exp_D
            bilateral_weights /= bilateral_weights.sum()

            filterd_out[idx] = sum(data_window * bilateral_weights)

        filterd_out = filterd_out.reshape(-1, 1)
        return filterd_out


class CausalBilateralFilter():
    """Causal Bilateral Filter for denoising time series

    It is used in our real situations, and there is no pad_mode compared with
    BilateralFilter, since we only use previous points to filter noise
    of current point.

    The filtering width is around 2*(truncate*sigma) + 1

    Parameters
    ----------
    sigma_i : double, optional
        [default=2.5]
        The standard deviation for intensity used in bilateral gaussian kernel
    sigma_d : double, optional
        [default=2.5]
        The standard deviation for distance used in bilateral gaussian kernel
    truncate: double, optional
        [default=6.0]
        Used for the radius of the filter, which equals to truncate
        times sigma_d
    """

    def __init__(self, sigma_i=2.0, sigma_d=2.5, truncate=8.0):
        self.sigma_i = sigma_i
        self.sigma_d = sigma_d
        self.truncate = truncate
        self.ts_data = None
        self.filtered_ts_data = None

    def fit_transform(self, ts_data, override=True):
        """
        This method generates the latent variables on seasonal data
        using T statistics and frequency.

        Parameters
        ----------
        X : array-like, shape = (n_timelength, 1)
            The input data, only support 1D data currently.
        override : boolean
            To be used for online case, ignore for now.

        Returns
        -------
        X_t : array-like, shape = (n_timelength, n_t_dim)
            The output latent variable data, could be high dimensional
            latent representation
        debug: dict
            Store the intermediate variables for debugging purpose
        """
        sigma_i = self.sigma_i
        sigma_d = self.sigma_d
        truncate = self.truncate
        data_validity_check(ts_data)
        ts_data = np.array(ts_data).flatten()

        radius = int(truncate * sigma_d + 0.5)
        dx = np.arange(-radius, 1)
        sig_D = sigma_d
        exp_D = gaussian_expfun(dx, sig_D)

        # calculate the eatimated noise variance
        sigma_n = calcu_noise_sigma(ts_data)
        sig_I = sigma_i * sigma_n  # note sigma_i is just a multiplicative para

        filterd_out = np.copy(ts_data)
        data_len = len(ts_data)
        for idx in range(radius, data_len):

            data_window = ts_data[(idx - radius):(idx + 1)]

            dy = data_window - ts_data[idx]
            exp_I = gaussian_expfun(dy, sig_I)
            bilateral_weights = exp_I * exp_D
            bilateral_weights /= bilateral_weights.sum()

            filterd_out[idx] = sum(data_window * bilateral_weights)

        filterd_out = filterd_out.reshape(-1, 1)
        self.radius = radius
        self.exp_D = exp_D
        self.ts_data = ts_data
        self.filterd_out = filterd_out
        return filterd_out

    def fit_transform_online(self, new_ts_minibatch):
        """Update the model with newX to transform the data

        Online version to do the fit transform with much faster speed.

        Parameters
        ---------
        new_ts_minibatch : array-like, shape = (n_minibatch, 1)
        or (n_minibatch,)
            Single time series of length mini_batch_timelength.

        Returns
        -------
        X_t : array-like, shape = (n_timelength,)
            A transformed time series of the same time length as the
            original ts_data fed into the model. Will need to maintain the
            state of the old data in order to return the transformed
            time series with the correct timelength.

        """
        if self.ts_data is None:
            raise ValueError("fit_transform_online can not be called in the\
                             very first time, should followed fit_transform.")

        radius = self.radius
        exp_D = self.exp_D
        ts_data = self.ts_data
        filterd_out = self.filterd_out
        sigma_i = self.sigma_i

        # check new_ts_minibatch and change format to 1d np.array
        data_validity_check(new_ts_minibatch)
        new_ts_minibatch = np.array(new_ts_minibatch).flatten()

        # update ts_data
        mini_batch_len = len(new_ts_minibatch)
        ts_data = np.concatenate([ts_data[mini_batch_len:], new_ts_minibatch])

        # calculate the eatimated noise variance
        sigma_n = calcu_noise_sigma(ts_data)
        sig_I = sigma_i * sigma_n  # note sigma_i is just a multiplicative para

        new_filterd_out = np.copy(new_ts_minibatch)
        data_len = len(ts_data)
        for idx in range((data_len - mini_batch_len), data_len):
            data_window = ts_data[(idx - radius):(idx + 1)]
            dy = data_window - ts_data[idx]
            exp_I = gaussian_expfun(dy, sig_I)
            bilateral_weights = exp_I * exp_D
            bilateral_weights /= bilateral_weights.sum()
            new_filterd_out[idx - (data_len - mini_batch_len)] = sum(
                data_window * bilateral_weights)

        # update filterd_out
        new_filterd_out = new_filterd_out.reshape(-1, 1)
        filterd_out = np.concatenate(
            [filterd_out[mini_batch_len:], new_filterd_out])
        filterd_out = filterd_out.reshape(-1, 1)
        self.ts_data = ts_data
        self.filterd_out = filterd_out
        return filterd_out

    def _get_prev_transform(self):
        # debug purpose
        return self.filterd_out


class BilateralSeasonTstat():
    """
    This method generates the latent variables on seasonal data
    using T statistics and frequency with the idea of Bilateral Filter.

    Parameters
    ----------
    num_period : int
        The number of previous history periods data.
        num_period can be 0,1,2,3...,
        when num_period=0, it only uses local data
    period_len : int
        The length of one period in terms of data points.
        When num_period=0, this para is not used
    neighbour_wdw_size : int
        The half length of the window used in historical period data
    sigma_i : double, optional
        [default=2.5]
        The standard deviation for intensity used in bilateral gaussian kernel
    sigma_d : double, optional
        [default=2.5]
        The standard deviation for distance used in bilateral gaussian kernel
    adj_win_open : Boolean, [default=False]
        A toggle to control if we will use adjacent data (local data, not
        period data).
    adj_win_size : int
        The length of the window used in local adjacent data
    adj_win_weight : float
        the weight used to control the contribution from local adjacent data
    lamb: float, [0.,1.], optional
        [default=0]
        The float number to control the ratio of local/global variance.
        Default 0 means we only use the local variance from seasonal data.
        Set the value to 0.5 means we are using the geometric mean of
        local/global variance. Set to 1 to use only global variance.
    div_var_toggle: boolean
        [default=False]
        The toggle that determines whether to divide (x - _mean) by the
        harmonic mean of sigma_global and sigma when calculating the latent
        representation. Default False means that it will not divide.
    """

    def __init__(self,
                 num_period=2,
                 period_len=1440,
                 neighbour_wdw_size=10,
                 sigma_i=1.0,
                 sigma_d=1e10,
                 adj_win_open=False,
                 adj_win_size=10,
                 adj_win_weight=0.001,
                 lamb=0,
                 div_var_toggle=False,
                 fit_transform_len_spec=None):
        self.period_len = period_len
        self.num_period = num_period
        self.sigma_i = sigma_i
        self.sigma_d = sigma_d
        self.neighbour_wdw_size = neighbour_wdw_size
        self.adj_win_open = adj_win_open
        self.adj_win_size = adj_win_size
        self.adj_win_weight = adj_win_weight
        self.lamb = lamb
        self.div_var_toggle = div_var_toggle

        self.ts_data = None
        self.filtered_out = None
        self.fit_transform_len_spec = fit_transform_len_spec

        self._required_len = neighbour_wdw_size + period_len * num_period

    def fit_transform(self, ts_data, override=True):
        """
        This method to do denoising filtering process for time sereis.

        Parameters
        ----------
        ts_data : array-like, shape = (n_timelength, 1) or (n_timelength,)
            The input data, 1D data: can be numpy array, list,
            or pandas dataframe.
        override : boolean
            To be used for online case, ignore for now.

        Returns
        -------
        filtered_out : array-like, shape = (n_timelength, 1)
            The output of denoised time series
        """

        period_len = self.period_len
        num_period = self.num_period
        sigma_i = self.sigma_i
        sigma_d = self.sigma_d
        adj_win_open = self.adj_win_open
        adj_win_weight = self.adj_win_weight
        adj_win_size = self.adj_win_size
        # truncate = self.truncate
        neighbour_wdw_size = self.neighbour_wdw_size
        lamb = self.lamb
        div_var_toggle = self.div_var_toggle
        fit_transform_len_spec = self.fit_transform_len_spec

        data_validity_check(ts_data)
        ts_data = np.array(ts_data).flatten()

        # radius = int(truncate * sigma_d + 0.5)
        adj_radius = adj_win_size
        radius = neighbour_wdw_size
        sig_D = sigma_d

        dx = np.arange(-adj_radius, 0)
        exp_D = gaussian_expfun(dx, sig_D)
        dx_2s = np.arange(-radius, radius + 1)
        exp_D_2s = gaussian_expfun(dx_2s, sig_D)

        # calculate the eatimated noise variance
        # sigma_n = calcu_noise_sigma(ts_data)
        sigma_n = np.std(ts_data)
        # minimal_sigma_n = 1e-2
        # sigma_n = minimal_sigma_n if sigma_n < minimal_sigma_n else sigma_n
        sig_I = sigma_i * sigma_n  # note sigma_i is just a multiplicative para

        # filtered_out = np.copy(ts_data)
        filtered_out = np.zeros(ts_data.shape)
        data_len = len(ts_data)
        if data_len <= radius + period_len * num_period:
            raise Exception('data length is not right')
        # specific fit_transform_len if necessary
        if fit_transform_len_spec is None:
            start_point = radius + period_len * 1
        else:
            start_point = data_len - fit_transform_len_spec
        # do filtering
        for idx in range(start_point, data_len):
            # this part indicates using local neiborhood data (diff)
            data_window_combined = np.array([])
            bilateral_weights_combined = np.array([])
            if adj_win_open:
                data_window = ts_data[(idx - adj_radius):idx]
                dy = data_window - ts_data[idx]
                exp_I = gaussian_expfun(dy, sig_I)
                bilateral_weights = adj_win_weight * exp_I * exp_D
                # concatenate data and weights
                data_window_combined = np.concatenate(
                    (data_window_combined, data_window))
                bilateral_weights_combined = np.concatenate(
                    (bilateral_weights_combined, bilateral_weights))

            # this part indicates using non-local neiborhood data
            # from previous period signals
            actual_num_period = 0
            for idx_period in range(1, num_period + 1):
                if idx - radius - period_len * idx_period >= 0:
                    actual_num_period += 1
                    data_window = ts_data[(idx - radius -
                                           period_len * idx_period):(
                                               idx + radius + 1 -
                                               period_len * idx_period)]
                    dy = data_window - ts_data[idx]
                    exp_I = gaussian_expfun(dy, sig_I)
                    bilateral_weights = exp_I * exp_D_2s
                    # concatenate data and weights
                    data_window_combined = np.concatenate(
                        (data_window_combined, data_window))
                    bilateral_weights_combined = np.concatenate(
                        (bilateral_weights_combined, bilateral_weights))

            weights_sum = bilateral_weights_combined.sum()
            if weights_sum == 0:
                bilateral_weights_combined = 1.0 / len(
                    bilateral_weights_combined)
            else:
                bilateral_weights_combined /= weights_sum

            _mean = np.sum(data_window_combined * bilateral_weights_combined)
            # If div_var_toggle is True, calculate the necessary statistic
            # and divide (x - _mean) by the harmonic mean of 2 var
            if div_var_toggle:
                # adjusted local var
                _local_diff_square = np.square(data_window_combined - _mean)
                _local_var = np.sum(_local_diff_square *
                                    bilateral_weights_combined)
                _local_var = 1e-10 if _local_var < 1e-10 else _local_var
                # adjusted global var
                _data_global = ts_data[(idx - radius -
                                        period_len * actual_num_period):idx]
                _global_var = np.var(_data_global)
                _global_var = 1e-10 if _global_var < 1e-10 else _global_var
                # harmonic mean of sigma_local and sigma_global
                _harmonic_std_inv = np.sqrt(lamb / _global_var +
                                            (1 - lamb) / _local_var)
                # multipy it by _harmonic_std_inv
                filtered_out[idx] = (ts_data[idx] - _mean) * _harmonic_std_inv
            # Otherwise, only use (x - _mean)
            else:
                filtered_out[idx] = ts_data[idx] - _mean

        filtered_out = filtered_out.reshape(-1, 1)
        self.radius = radius
        self.adj_radius = adj_radius
        self.exp_D = exp_D
        self.exp_D_2s = exp_D_2s
        self.ts_data = ts_data
        self.filtered_out = filtered_out
        return filtered_out

    def fit_transform_online(self, new_ts_minibatch):
        """Update the model with newX to transform the data

        Online version to do the fit transform with much faster speed.

        Parameters
        ---------
        new_ts_minibatch : array-like, shape = (n_minibatch, 1)
        or (n_minibatch,)
            Single time series of length mini_batch_timelength.

        Returns
        -------
        X_t : array-like, shape = (n_timelength,)
            A transformed time series of the same time length as the
            original ts_data fed into the model. Will need to maintain the
            state of the old data in order to return the transformed
            time series with the correct timelength.

        """
        if self.ts_data is None:
            raise ValueError("fit_transform_online can not be called in the\
                             very first time, should followed fit_transform.")

        adj_radius = self.adj_radius
        radius = self.radius
        exp_D = self.exp_D
        exp_D_2s = self.exp_D_2s
        ts_data = self.ts_data
        filtered_out = self.filtered_out
        sigma_i = self.sigma_i
        period_len = self.period_len
        num_period = self.num_period
        adj_win_open = self.adj_win_open
        adj_win_weight = self.adj_win_weight
        lamb = self.lamb
        div_var_toggle = self.div_var_toggle

        # check new_ts_minibatch and change format to 1d np.array
        data_validity_check(new_ts_minibatch)
        new_ts_minibatch = np.array(new_ts_minibatch).flatten()

        # update ts_data
        mini_batch_len = len(new_ts_minibatch)
        ts_data = np.concatenate([ts_data[mini_batch_len:], new_ts_minibatch])

        # calculate the eatimated noise variance
        # sigma_n = calcu_noise_sigma(ts_data)
        sigma_n = np.std(ts_data)
        # minimal_sigma_n = 1e-2
        # sigma_n = minimal_sigma_n if sigma_n < minimal_sigma_n else sigma_n
        sig_I = sigma_i * sigma_n  # note sigma_i is just a multiplicative para

        new_filtered_out = np.copy(new_ts_minibatch)
        data_len = len(ts_data)
        for idx in range((data_len - mini_batch_len), data_len):
            # # this part indicates using local neiborhood data
            data_window_combined = np.array([])
            bilateral_weights_combined = np.array([])
            if adj_win_open:
                data_window = ts_data[(idx - adj_radius):idx]
                dy = data_window - ts_data[idx]
                exp_I = gaussian_expfun(dy, sig_I)
                bilateral_weights = adj_win_weight * exp_I * exp_D
                # concatenate data and weights
                data_window_combined = np.concatenate(
                    (data_window_combined, data_window))
                bilateral_weights_combined = np.concatenate(
                    (bilateral_weights_combined, bilateral_weights))

            # this part indicates using non-local neiborhood data
            # from previous period signals
            actual_num_period = 0
            for idx_period in range(1, num_period + 1):
                if idx - radius - period_len * idx_period >= 0:
                    actual_num_period += 1
                    data_window = ts_data[(idx - radius -
                                           period_len * idx_period):(
                                               idx + radius + 1 -
                                               period_len * idx_period)]
                    dy = data_window - ts_data[idx]
                    exp_I = gaussian_expfun(dy, sig_I)
                    bilateral_weights = exp_I * exp_D_2s
                    # concatenate data and weights
                    data_window_combined = np.concatenate(
                        (data_window_combined, data_window))
                    bilateral_weights_combined = np.concatenate(
                        (bilateral_weights_combined, bilateral_weights))

            weights_sum = bilateral_weights_combined.sum()
            if weights_sum == 0:
                bilateral_weights_combined = 1.0 / len(
                    bilateral_weights_combined)
            else:
                bilateral_weights_combined /= weights_sum

            _mean = np.sum(data_window_combined * bilateral_weights_combined)
            # If div_var_toggle is True, calculate the necessary statistic
            # and divide (x - _mean) by the harmonic mean of 2 var
            if div_var_toggle:
                # adjusted local var
                _local_diff_square = np.square(data_window_combined - _mean)
                _local_var = np.sum(_local_diff_square *
                                    bilateral_weights_combined)
                _local_var = 1e-10 if _local_var < 1e-10 else _local_var
                # adjusted global var
                _data_global = ts_data[(idx - radius -
                                        period_len * actual_num_period):idx]
                _global_var = np.var(_data_global)
                _global_var = 1e-10 if _global_var < 1e-10 else _global_var
                # harmonic mean of sigma_local and sigma_global
                _harmonic_std_inv = np.sqrt(lamb / _global_var +
                                            (1 - lamb) / _local_var)
                # multipy it by _harmonic_std_inv
                new_filtered_out[idx - (data_len - mini_batch_len)] = (
                    ts_data[idx] - _mean) * _harmonic_std_inv
            # Otherwise, only use (x - _mean)
            else:
                new_filtered_out[idx - (data_len -
                                        mini_batch_len)] = ts_data[idx] - _mean

        # update filtered_out
        new_filtered_out = new_filtered_out.reshape(-1, 1)
        filtered_out = np.concatenate(
            [filtered_out[mini_batch_len:], new_filtered_out])
        filtered_out = filtered_out.reshape(-1, 1)
        self.ts_data = ts_data
        self.filtered_out = filtered_out
        return filtered_out

    def fit_transform_inverse(self, X_t):
        """Inverse transformation of data from X_t back to origin X.

        This should not change the old data maintained in the transformer.

        Parameters
        ---------
        X_t : array-like, shape = (mini_batch_timelength, n_transformed_dim)
            Transformed time series of length mini_batch_timelength.
            The single ts could be of high dimension.

        Returns
        -------
        X_o : array-like, shape = (mini_batch_timelength, n_dim)
            Origin time series of the same time length as X_t.
        """
        X_t = to_2dnp_array(X_t)
        mini_batch_size, n_dim = X_t.shape

        X_before = self.ts_data[-mini_batch_size:]
        X_after = self._get_prev_transform()[-mini_batch_size:]
        X_o = X_t - X_after + X_before
        return X_o

    def _get_prev_transform(self):
        # debug purpose
        return self.filtered_out

    def get_required_len(self):
        return self._required_len


"""
Matrix inverse is used in this version
This is an ADMM solver for the generalized lasso problem:
min_x 1/2 * ||Ax - b||_2^2 + lambda ||Fx - p||_1
Many prolems in our pipeline can be transformed into the form of the
genearlized lasso defined above.
In using this solver, the user must explicitly give the defintion of
A, b, F, p, reg_lambda (the lambda in original formulation)
=== Input ===
A: m*n matrix
b: m vector
F: l*n matrix
p: l vector
reg_lambda: a nonnegative scalar
Some other important parameters:
rho: the penalized parameter in ADMM
max_iter: the maximum number of iterations
tol_abs: the absolute tolerance parameter
tol_rel: the relative tolerance parameter
Optional parameters:
x0: the initial value of x for warm start
Quite: True/False, to enable the display or intermediate results or not
=== Output ===
We return both the optimial solution and the whole history in the solving
process (we call history)
1. The solution is returned in the vector x
2. The history is returned in the dictionary h containing the objective value,
the primal and dual residual norms, and the tolerances for the primal and dual
residual norms at each iteration.

Notes:
1. Over relaxation is supported in the API, but currently not fully
    implemented. We will implement it in the near future.
    rel_par is the over-relaxation parameter (typical values for rel_par
    are between 1.0 and 1.8).
"""

# from scipy.sparse.linalg import spsolve
# from scipy.linalg import solve_triangular


def g_lasso_admm_solver(A,
                        b,
                        F,
                        p,
                        reg_lambda,
                        x0=None,
                        rho=1.0,
                        MAX_ITER=5000,
                        rel_par=1.,
                        QUIET=True,
                        tol_abs=1e-5,
                        tol_rel=1e-2,
                        S_inv=None,
                        warm_start_toggle=False):
    # Parameter checking
    if reg_lambda < 0:
        reg_lambda = 0
    if rel_par < 1:
        rel_par = 1.0
    if rel_par > 2:
        rel_par = 2.0
    if rho < 0:
        rho = 1.0
    if MAX_ITER < 0:
        MAX_ITER = 50
    if rel_par < 1:
        rel_par = 1.0
    if QUIET is None:
        QUIET = True
    if tol_abs < 0:
        tol_abs = 1e-3
    if tol_rel < 0:
        tol_rel = 1e-2

    if not QUIET:
        tic = time.time()

    # Data preprocessing
    m, n = A.shape
    ell = F.shape[0]
    b = b.ravel()
    p = p.ravel()
    if x0 is not None:
        x0 = x0.ravel()

    # Initialize
    x = np.zeros(n)
    z = np.zeros(ell)
    u = np.zeros(ell)

    if not QUIET:
        print('\n%3s\t%10s\t%10s\t%10s\t%10s\t%10s' %
              ('iter', 'r norm', 'eps pri', 's norm', 'eps dual', 'objective'))

    # Saving state
    h = {}
    h['obj_val'] = np.zeros(MAX_ITER)
    h['r_norm'] = np.zeros(MAX_ITER)
    h['s_norm'] = np.zeros(MAX_ITER)
    h['eps_pri'] = np.zeros(MAX_ITER)
    h['eps_dual'] = np.zeros(MAX_ITER)

    # Cache some results
    Atb = A.T.dot(b)
    # Cache the Cholesky decomposition
    # L, U = Cholesky_decomp_sp(A, F, rho)
    if S_inv is None:
        S = A.T.dot(A) + rho * F.T.dot(F)
        S_inv = np.linalg.inv(S)
    # L = cholesky(S)
    # U = L.T

    for k in range(MAX_ITER):
        # x-update
        if warm_start_toggle and k == 0 and x0 is not None:
            x = x0
        else:
            # x = spsolve(U, spsolve(L, q))
            q = Atb + rho * F.T.dot(z - u + p)
            x = S_inv.dot(q)
            # solve_triangular(L, q, lower=True, overwrite_b=True)
            # solve_triangular(U, q, lower=False, overwrite_b=True)
            # x = q
        Fx = F.dot(x)

        # z-update
        zold = np.copy(z)
        v = Fx + u - p
        z = soft_thresholding(v, reg_lambda * 1. / rho)

        # u-update
        u = u + Fx - z - p

        # Diagnostics, reporting, termination checks
        h['obj_val'][k] = f_objective(A, b, reg_lambda, x, z)
        h['r_norm'][k] = norm(Fx - z - p)
        h['s_norm'][k] = norm(-rho * F.T.dot(z - zold))
        h['eps_pri'][k] = np.sqrt(ell) * tol_abs + tol_rel * np.array(
            [norm(Fx), norm(z), norm(p)]).max()
        h['eps_dual'][k] = np.sqrt(n) * tol_abs + tol_rel * norm(
            rho * F.T.dot(u))

        if not QUIET:
            # h['obj_val'][k] = f_objective(A, b, reg_lambda, x, z)
            print('%4d\t%10.4f\t%10.4f\t%10.4f\t%10.4f\t%10.2f' %
                  (k + 1, h['r_norm'][k], h['eps_pri'][k], h['s_norm'][k],
                   h['eps_dual'][k], h['obj_val'][k]))

        if (h['r_norm'][k] < h['eps_pri'][k]) and \
           (h['s_norm'][k] < h['eps_dual'][k]):
            break

    if not QUIET:
        toc = time.time() - tic
        print("\nElapsed time is %.2f seconds" % toc)

    return x.ravel(), h


# Supporting functions for the genearlized Lasso solver
def f_objective(A, b, reg_lambda, x, z):
    return 0.5 * np.square(A.dot(x) - b).sum() + reg_lambda * norm(z, 1)


def soft_thresholding(x, alpha):
    return np.maximum(0., x - alpha) - np.maximum(0., -x - alpha)


def Cholesky_decomp_sp(A, F, rho):
    S = A.T.dot(A) + rho * F.T.dot(F)
    L = cholesky(S)
    L = sparse.csc_matrix(L)
    U = sparse.csc_matrix(L.T)
    return L, U


# def lad_trend(denoised_data, data_T, vlambda, vlambda_diff, solver_method,
#               trend_solver_admm_rho, maxiters, show_progress, S_inv,
#               warm_start_toggle, warm_start_iniValue):

#     # normalize input data
#     scaler = StandardScaler()
#     denoised_data_scaled = scaler.fit_transform(denoised_data)

#     T = data_T
#     N = len(denoised_data_scaled)
#     # season diff
#     pd_denoised_data = pd.DataFrame(denoised_data_scaled)
#     g_t = pd_denoised_data.diff(periods=T)[T:].values.flatten().reshape(-1, 1)

#     # reformulate 3 L1 to 1 L1 (extended L1)
#     extended_g_t = np.vstack((g_t, np.zeros((N - 1, 1)), np.zeros((N - 2, 1))))
#     row = np.hstack((np.ones((1, T)), np.zeros((1, N - 1 - T))))
#     mat_delta_t = circulant(row).T[:(N - T), :]
#     eye_tmp = vlambda * np.eye(N - 1)
#     row_tmp = np.hstack(
#         (np.array([1, -1]).reshape(1, 2), np.zeros((1, N - 2 - 1))))
#     Diff_1st_tmp = vlambda_diff * circulant(row_tmp).T[:(N - 2), :]
#     extended_mat = np.vstack((mat_delta_t, eye_tmp, Diff_1st_tmp))

#     # method based on LP in CVXOPT
#     if solver_method == 'cvxopt_default':
#         from .L1_LPsolver import l1blas
#         P = matrix(extended_mat)
#         q = matrix(extended_g_t)
#         # delta_t = l1(P, q)
#         delta_t = l1blas(P, q, maxiters, show_progress)  # faster
#         next_warm_start_value = np.asarray(delta_t).reshape(-1, )
#     elif solver_method == 'cvxopt_mosek':
#         from .L1_LPsolver import l1mosek2
#         P = matrix(extended_mat)
#         q = matrix(extended_g_t)
#         # delta_t = l1mosek(P,q)
#         delta_t = l1mosek2(P, q, maxiters, show_progress)  # faster
#         next_warm_start_value = np.asarray(delta_t).reshape(-1, )
#     elif solver_method == 'g_lasso_admm':
#         l_admm, n_admm = extended_mat.shape
#         m_admm = 1
#         A_admm = np.zeros((m_admm, n_admm))
#         b_admm = np.zeros(m_admm)
#         F_admm = extended_mat
#         p_admm = extended_g_t
#         p_admm = p_admm.ravel()
#         reg_lambda = 1.0

#         R_admm = g_lasso_admm_solver(A_admm,
#                                      b_admm,
#                                      F_admm,
#                                      p_admm,
#                                      reg_lambda,
#                                      x0=warm_start_iniValue,
#                                      rho=trend_solver_admm_rho,
#                                      MAX_ITER=maxiters,
#                                      rel_par=1.,
#                                      QUIET=(not show_progress),
#                                      tol_abs=1e-4,
#                                      S_inv=S_inv,
#                                      warm_start_toggle=warm_start_toggle)
#         delta_t = R_admm[0].reshape(-1, 1)
#         next_warm_start_value = R_admm[0]

#     # add [0] as the first element to keep data length
#     delta_t_out = np.vstack(([0], np.asarray(delta_t)))
#     ini_trend_data_scaled = np.cumsum(delta_t_out).reshape(-1, 1)
#     # de-normalize data
#     ini_trend_data = scaler.inverse_transform(ini_trend_data_scaled) - \
#         float(scaler.mean_)
#     return ini_trend_data, next_warm_start_value



def lad_trend(denoised_data, data_T, vlambda, vlambda_diff, solver_method,
              trend_solver_admm_rho, trend_down_sample, maxiters,
              show_progress, S_inv, warm_start_toggle, warm_start_iniValue,
              update_fast_trend, fast_trend_record):

    # trend down_sample
    d_a = trend_down_sample
    ori_len = len(denoised_data)
    denoised_data = denoised_data.copy()[::d_a]
    T = int(data_T/d_a)

    # normalize input data
    scaler = StandardScaler()
    denoised_data_scaled = scaler.fit_transform(denoised_data)

    # T = data_T
    N = len(denoised_data_scaled)
    # season diff
    pd_denoised_data = pd.DataFrame(denoised_data_scaled)
    g_t = pd_denoised_data.diff(periods=T)[T:].values.flatten().reshape(-1, 1)

    # reformulate 3 L1 to 1 L1 (extended L1)
    extended_g_t = np.vstack((g_t, np.zeros((N - 1, 1)), np.zeros((N - 2, 1))))

    if solver_method == 'GADMM' or solver_method == 'PDHG':
        pass
    else:
        row = np.hstack((np.ones((1, T)), np.zeros((1, N - 1 - T))))
        mat_delta_t = circulant(row).T[:(N - T), :]
        eye_tmp = vlambda * np.eye(N - 1)
        row_tmp = np.hstack((np.array([1, -1]).reshape(1, 2),
                             np.zeros((1, N - 2 - 1))))
        Diff_1st_tmp = vlambda_diff * circulant(row_tmp).T[:(N - 2), :]
        extended_mat = np.vstack((mat_delta_t, eye_tmp, Diff_1st_tmp))

    # method based on LP in CVXOPT
    gadmm_pdhg_solver = None
    if solver_method == 'cvxopt_default':
        from .L1_LPsolver import l1blas
        P = matrix(extended_mat)
        q = matrix(extended_g_t)
        # delta_t = l1(P, q)
        delta_t = l1blas(P, q, maxiters, show_progress)  # faster
        next_warm_start_value = np.asarray(delta_t).reshape(-1,)
    elif solver_method == 'cvxopt_mosek':
        from .L1_LPsolver import l1mosek2
        P = matrix(extended_mat)
        q = matrix(extended_g_t)
        # delta_t = l1mosek(P,q)
        delta_t = l1mosek2(P, q, maxiters, show_progress)  # faster
        next_warm_start_value = np.asarray(delta_t).reshape(-1,)
    elif solver_method == 'g_lasso_admm':
        l_admm, n_admm = extended_mat.shape
        m_admm = 1
        A_admm = np.zeros((m_admm, n_admm))
        b_admm = np.zeros(m_admm)
        F_admm = extended_mat
        p_admm = extended_g_t
        p_admm = p_admm.ravel()
        reg_lambda = 1.0

        R_admm = g_lasso_admm_solver(A_admm, b_admm, F_admm, p_admm,
                                     reg_lambda, x0=warm_start_iniValue,
                                     rho=trend_solver_admm_rho,
                                     MAX_ITER=maxiters, rel_par=1.,
                                     QUIET=(not show_progress),
                                     tol_abs=1e-4, S_inv=S_inv,
                                     warm_start_toggle=warm_start_toggle)
        delta_t = R_admm[0].reshape(-1, 1)
        next_warm_start_value = R_admm[0]

    elif solver_method == 'GADMM' or solver_method == 'PDHG':
        if solver_method == 'GADMM':
            solver_cls = gADMMSolver
        else:
            solver_cls = PDHG

        b_vec = extended_g_t
        if update_fast_trend:
            # A_mat = extended_mat
            A_operator = DetailA(N, T, vlambda, vlambda_diff)
            A_norm = max(T, vlambda, vlambda_diff)
            gadmm_pdhg_solver = \
                solver_cls(A_operator=A_operator, A_norm=A_norm)
            gadmm_pdhg_solver.paramTuning(b_vec=b_vec, maxIter=40)
            # gc.collect()
            # time.sleep(.1)
        else:
            gadmm_pdhg_solver = fast_trend_record

        (g_soln, g_obj, g_obj_history, g_time_history) = \
            gadmm_pdhg_solver.solve(b_vec=b_vec, max_iter=maxiters)
        delta_t = g_soln
        next_warm_start_value = np.asarray(delta_t).reshape(-1,)

    # add [0] as the first element to keep data length
    delta_t_out = np.vstack(([0], np.asarray(delta_t)))
    ini_trend_data_scaled = np.cumsum(delta_t_out).reshape(-1, 1)
    # de-normalize data
    ini_trend_data = scaler.inverse_transform(ini_trend_data_scaled) - \
        float(scaler.mean_)

    # trend up_sample
    ini_trend_data = np.repeat(ini_trend_data, d_a)[:ori_len].reshape(-1, 1)
    next_warm_start_value = np.repeat(next_warm_start_value, d_a)[:ori_len]

    return ini_trend_data, next_warm_start_value, gadmm_pdhg_solver


def bilateral_season(coarse_season,
                     data_T,
                     bilateral_period_num,
                     neighbour_wdw_size,
                     sigma_i,
                     sigma_d,
                     fit_transform_len_spec=None):

    # ref: coarse_season = denoised_data - ini_trend_data
    b_season_tstat = BilateralSeasonTstat(
        num_period=bilateral_period_num,
        period_len=data_T,
        neighbour_wdw_size=neighbour_wdw_size,
        sigma_i=sigma_i,
        sigma_d=sigma_d,
        fit_transform_len_spec=fit_transform_len_spec)

    irregular_season = b_season_tstat.fit_transform(coarse_season)
    ini_season_data = coarse_season - irregular_season

    return ini_season_data, irregular_season


# class RobustSTL():
#     """Robust STL for decomposing time series

#     Parameters
#     ----------
#     data_T : int
#         The length of one period in terms of data points.
#         The value must be larger than 1
#     noise_toggle: boolean
#         [default=True]
#         The toggle that determines whether to denoise signal.
#     noise_sigma_i : double
#         [default=2.0]
#         The standard deviation for intensity used in bilateral gaussian kernel
#     noise_sigma_d : double
#         [default=2.5]
#         The standard deviation for distance used in bilateral gaussian kernel
#     truncate: double
#         [default=8.0]
#         Used for the radius of the filter, which equals to truncate times
#         sigma_d
#     trend_toggle: boolean
#         [default=True]
#         The toggle that determines whether to detrend signal.
#     trend_vlambda : double
#         [default=40]
#         The lambda to control first difference of trend in L1 reg
#     trend_vlambda_diff : double
#         [default=20]
#         The lambda to control twice difference of trend in L1 reg
#     trend_solver_method: str, 'g_lasso_admm','cvxopt_default', 'cvxopt_mosek'
#         [default='g_lasso_admm']
#         The solver to solve the L1 problem. cvxopt_mosek is much
#         faster but need license.
#     trend_solver_maxiters : int
#         [default=15]
#         Set the iteration number in  the optimization progress in trend filter
#     trend_solver_show_progress: boolean
#         [default=False]
#         Show the optimization progress in trend filter
#     trend_solver_admm_rho: float
#         The para rho used in the admm solver in trend filter
#     trend_solver_warm_start: boolean
#         [default=True]
#         speed up in the online mode by using warm_start in the admm solver
#     season_toggle: boolean
#         [default=True]
#         The toggle that determines whether to deseasonize signal.
#     season_bilateral_period_num : int
#         The number of previous periods used in the deseason.
#         num_period can be 1,2,3...,
#     season_neighbour_wdw_size : int
#         [default=20]
#         The half length of the window used in historical period data
#     season_sigma_i : double
#         [default=2.5]
#         The standard deviation for intensity used in bilateral gaussian kernel
#     season_sigma_d : double
#         [default=2.5]
#         The standard deviation for distance used in bilateral gaussian kernel
#     online_history_length : int
#         The length of history data used in online mode for RobustSTL
#     online_trend_update_freq : int
#         [default=5]
#         speed up in the online mode by only calculating trend every
#         online_trend_update_freq times
#     online_update_mode : string
#         [default='only_mini_batch']
#         The online update mode, can be 'only_mini_batch' or
#         'entire_online_history', default is 'only_mini_batch'
#     """

#     def __init__(self,
#                  data_T=288,
#                  noise_toggle=True,
#                  noise_sigma_i=2.0,
#                  noise_sigma_d=2.5,
#                  noise_truncate=8.0,
#                  trend_toggle=True,
#                  trend_vlambda=40,
#                  trend_vlambda_diff=20,
#                  trend_solver_method='g_lasso_admm',
#                  trend_solver_maxiters=15,
#                  trend_solver_show_progress=False,
#                  trend_solver_admm_rho=1.0,
#                  trend_solver_warm_start=True,
#                  season_toggle=True,
#                  season_bilateral_period_num=2,
#                  season_neighbour_wdw_size=20,
#                  season_sigma_i=2,
#                  season_sigma_d=10,
#                  online_history_length=288 * 3,
#                  online_trend_update_freq=5,
#                  online_update_mode='only_mini_batch'):

#         self.data_T = data_T
#         if data_T <= 1:
#             raise ValueError("The data_T (period) must be larger than 1")
#         self.noise_toggle = noise_toggle
#         self.noise_sigma_i = noise_sigma_i
#         self.noise_sigma_d = noise_sigma_d
#         self.noise_truncate = noise_truncate

#         self.trend_toggle = trend_toggle
#         self.trend_vlambda = trend_vlambda
#         self.trend_vlambda_diff = trend_vlambda_diff
#         self.trend_solver_method = trend_solver_method
#         self.trend_solver_maxiters = trend_solver_maxiters
#         self.trend_solver_show_progress = trend_solver_show_progress
#         self.trend_solver_admm_rho = trend_solver_admm_rho
#         self.trend_solver_warm_start = trend_solver_warm_start

#         self.season_toggle = season_toggle
#         self.season_bilateral_period_num = season_bilateral_period_num
#         if season_bilateral_period_num < 1:
#             raise ValueError("The season_bilateral_period_num at least 1")
#         self.season_neighbour_wdw_size = season_neighbour_wdw_size
#         self.season_sigma_i = season_sigma_i
#         self.season_sigma_d = season_sigma_d

#         self.online_history_length = online_history_length
#         self.online_trend_update_freq = online_trend_update_freq
#         self.online_update_mode = online_update_mode
#         self.online_count = 0

#         # calcu S_inv for fit_transform_online only
#         N = online_history_length
#         T = data_T
#         vlambda = trend_vlambda
#         vlambda_diff = trend_vlambda_diff
#         row = np.hstack((np.ones((1, T)), np.zeros((1, N - 1 - T))))
#         mat_delta_t = circulant(row).T[:(N - T), :]
#         eye_tmp = vlambda * np.eye(N - 1)
#         row_tmp = np.hstack(
#             (np.array([1, -1]).reshape(1, 2), np.zeros((1, N - 2 - 1))))
#         Diff_1st_tmp = vlambda_diff * circulant(row_tmp).T[:(N - 2), :]
#         extended_mat = np.vstack((mat_delta_t, eye_tmp, Diff_1st_tmp))
#         F_admm = extended_mat
#         rho = trend_solver_admm_rho
#         S = rho * F_admm.T.dot(F_admm)
#         self.S_inv = np.linalg.inv(S)

#     def fit_transform(self,
#                       ts_data,
#                       online_mode=False,
#                       S_inv=None,
#                       warm_start_iniValue=None,
#                       online_trend_update_toggle=True,
#                       mini_batch_len=None,
#                       override=True):
#         """
#         This method to do robust STL filtering process for time sereis.

#         Parameters
#         ----------
#         ts_data : array-like, shape = (n_timelength, 1) or (n_timelength,)
#             The input data, 1D data: can be numpy array, list,
#             or pandas dataframe.
#         online_mode: boolean
#             [default=False]
#             In batch mode, this is False. Used in online mode to speed up.
#         S_inv : matrix
#             To be used for online case only to speed up
#         warm_start_iniValue: array-like, shape = (n_timelength, 1)
#             Used in online mode to speed up the trend solver.
#         online_trend_update_toggle: boolean
#             [default=True]
#             In batch mode, this is True, which decides when to upate trend.
#         mini_batch_len: int
#             the length of mini_batch
#         override : boolean
#             To be used for online case, ignore for now.

#         Returns
#         -------
#         decomposed_data : array-like, shape = (n_timelength, 3)
#             The output of decomposed signal: [trend, season, irregular]
#         """
#         data_T = self.data_T

#         noise_toggle = self.noise_toggle
#         noise_sigma_i = self.noise_sigma_i
#         noise_sigma_d = self.noise_sigma_d
#         noise_truncate = self.noise_truncate

#         trend_toggle = self.trend_toggle
#         trend_vlambda = self.trend_vlambda
#         trend_vlambda_diff = self.trend_vlambda_diff
#         trend_solver_method = self.trend_solver_method
#         trend_solver_maxiters = self.trend_solver_maxiters
#         trend_solver_show_progress = self.trend_solver_show_progress
#         trend_solver_admm_rho = self.trend_solver_admm_rho
#         trend_solver_warm_start = self.trend_solver_warm_start

#         season_toggle = self.season_toggle
#         season_bilateral_period_num = self.season_bilateral_period_num
#         season_neighbour_wdw_size = self.season_neighbour_wdw_size
#         season_sigma_i = self.season_sigma_i
#         season_sigma_d = self.season_sigma_d
#         online_history_length = self.online_history_length

#         data_validity_check(ts_data)
#         ts_data = np.array(ts_data).flatten().reshape(-1, 1)
#         original_data = ts_data.copy()

#         # 1st step: denoising
#         if noise_toggle:
#             if not online_mode:  # batch mode
#                 self.denoising_filter = CausalBilateralFilter(
#                     sigma_i=noise_sigma_i,
#                     sigma_d=noise_sigma_d,
#                     truncate=noise_truncate)
#                 denoised_data = self.denoising_filter.fit_transform(
#                     original_data)
#             else:  # online mode
#                 new_ts_minibatch = ts_data[-mini_batch_len:]
#                 denoised_data = self.denoising_filter.fit_transform_online(
#                     new_ts_minibatch)
#                 denoised_data = denoised_data[-online_history_length:]
#         else:
#             denoised_data = original_data  # no denoising

#         noise_data = original_data - denoised_data

#         # 2nd step: detrending
#         if trend_toggle:
#             # online_trend_update_toggle == True by default,
#             # since in batch mode we must do this
#             if online_trend_update_toggle:
#                 ini_trend_data, next_warm_start_value = lad_trend(
#                     denoised_data=denoised_data,
#                     data_T=data_T,
#                     vlambda=trend_vlambda,
#                     vlambda_diff=trend_vlambda_diff,
#                     solver_method=trend_solver_method,
#                     trend_solver_admm_rho=trend_solver_admm_rho,
#                     maxiters=trend_solver_maxiters,
#                     show_progress=trend_solver_show_progress,
#                     S_inv=S_inv,
#                     warm_start_toggle=trend_solver_warm_start,
#                     warm_start_iniValue=warm_start_iniValue)
#             else:
#                 next_warm_start_value = np.hstack(
#                     (self.next_warm_start_value[mini_batch_len:],
#                      np.tile(0.0, mini_batch_len)))
#                 # ini_trend_data, can also do interplate or prediction]
#                 ini_trend_data_tmp = self.ini_trend_data.reshape(-1, )
#                 ini_trend_data = np.hstack(
#                     (ini_trend_data_tmp[mini_batch_len:],
#                      np.tile(ini_trend_data_tmp[-1], mini_batch_len)))
#                 ini_trend_data = ini_trend_data.reshape(-1, 1)
#             # summary
#             coarse_season = denoised_data - ini_trend_data
#             self.ini_trend_data = ini_trend_data
#             self.next_warm_start_value = next_warm_start_value

#         else:
#             ini_trend_data = np.zeros_like(denoised_data)
#             coarse_season = denoised_data
#             self.next_warm_start_value = np.zeros(online_history_length)

#         # 3rd step: de-season
#         if season_toggle:
#             # # version 1 with later evel_adj = np.mean(ini_season_data)
#             # ini_season_data, irregular_season = bilateral_season(
#             #     coarse_season=coarse_season,
#             #     data_T=data_T,
#             #     bilateral_period_num=season_bilateral_period_num,
#             #     neighbour_wdw_size=season_neighbour_wdw_size,
#             #     sigma_i=season_sigma_i,
#             #     sigma_d=season_sigma_d)

#             # # version 2 is the same as above when set level_adj=0 later
#             # # much better speedup with level_adj = np.mean(coarse_season)
#             if not online_mode:  # batch mode
#                 ini_season_data, irregular_season = bilateral_season(
#                     coarse_season=coarse_season,
#                     data_T=data_T,
#                     bilateral_period_num=season_bilateral_period_num,
#                     neighbour_wdw_size=season_neighbour_wdw_size,
#                     sigma_i=season_sigma_i,
#                     sigma_d=season_sigma_d,
#                     fit_transform_len_spec=None)
#                 self.ini_season_data = ini_season_data[-online_history_length:]
#             else:  # online mode:
#                 ini_season_data, irregular_season = bilateral_season(
#                     coarse_season=coarse_season,
#                     data_T=data_T,
#                     bilateral_period_num=season_bilateral_period_num,
#                     neighbour_wdw_size=season_neighbour_wdw_size,
#                     sigma_i=season_sigma_i,
#                     sigma_d=season_sigma_d,
#                     fit_transform_len_spec=mini_batch_len)
#                 ini_season_data = np.vstack(
#                     (self.ini_season_data[mini_batch_len:],
#                      ini_season_data[-mini_batch_len:]))
#                 self.ini_season_data = ini_season_data
#         else:
#             coarse_season_mean = np.mean(coarse_season)
#             irregular_season = coarse_season - coarse_season_mean
#             ini_season_data = coarse_season_mean * \
#                 np.ones_like(coarse_season)

#         # 4nd step: adjust trend, season, irregular_season.
#         # make mean(adjusted_season) = 0
#         # level_adj = np.mean(ini_season_data)

#         # adopt mean(coarse_season) to approx mean(ini_season_data)
#         # in the version 1 in step_3
#         level_adj = np.mean(coarse_season)
#         adjusted_season = ini_season_data - level_adj
#         if trend_toggle:
#             adjusted_trend = ini_trend_data + level_adj
#             irregular_data = irregular_season + noise_data
#         else:
#             adjusted_trend = ini_trend_data
#             irregular_data = irregular_season + noise_data + level_adj

#         decomposed_data = np.hstack(
#             (adjusted_trend, adjusted_season, irregular_data))

#         self.ts_data = ts_data
#         self.adjusted_trend = adjusted_trend
#         self.adjusted_season = adjusted_season
#         self.irregular_data = irregular_data
#         self.decomposed_data = decomposed_data
#         return decomposed_data

#     def fit_transform_online(self, new_ts_minibatch):
#         """
#         The online version of RobustSTL to do the fit transform with
#         much faster speed.
#         Note:
#         1, online mode only uses the most recent online_history_length data
#         (including the newest new_ts_minibatch) to do decomposition.
#         2, the output would be the same as the output of fit_transform (i.e.,
#         the length is len(ts_data)), but only the last len(new_ts_minibatch)
#         data is computed from the fit_transform_online.

#         Parameters
#         ---------
#         new_ts_minibatch : array-like, shape = (n_minibatch, 1)
#         or (n_minibatch,)
#             Single time series of length mini_batch_timelength.

#         Returns
#         -------
#         decomposed_data : numpy array, shape = (n_timelength, 3)
#             The output of decomposed signal: [trend, season, irregular]
#         """
#         S_inv = self.S_inv
#         next_warm_start_value = self.next_warm_start_value
#         online_history_length = self.online_history_length
#         online_update_mode = self.online_update_mode
#         online_trend_update_freq = self.online_trend_update_freq

#         ts_data = self.ts_data
#         adjusted_trend = self.adjusted_trend
#         adjusted_season = self.adjusted_season
#         irregular_data = self.irregular_data
#         decomposed_data = self.decomposed_data

#         mini_batch_len = len(new_ts_minibatch)
#         if self.ts_data is None:
#             raise ValueError("fit_transform_online can not be called in the\
#                              very first time, should followed fit_transform.")
#         if mini_batch_len > online_history_length:
#             raise ValueError("for online detrend, the length of mini_batch\
#                              should not larger than the online_history_length")
#         if len(ts_data) < online_history_length:
#             raise ValueError("for online detrend, the online_history_length\
#                              should not larger than the initial len(ts_data)")

#         # check new_ts_minibatch and change format to 1d np.array
#         data_validity_check(new_ts_minibatch)
#         new_ts_minibatch = np.array(new_ts_minibatch).flatten()
#         new_ts_minibatch = new_ts_minibatch.reshape(-1, 1)

#         # update ts_data
#         ts_data = np.concatenate([ts_data[mini_batch_len:], new_ts_minibatch])
#         # detrend for ts_data_online_part
#         ts_data_online = ts_data[-online_history_length:]

#         next_warm_start_value = \
#             next_warm_start_value[-(online_history_length-1):]
#         # warm_start_ini = np.hstack(
#         #         (next_warm_start_value[mini_batch_len:],
#         #          np.tile(next_warm_start_value[-1], mini_batch_len)))
#         warm_start_ini = np.hstack((next_warm_start_value[mini_batch_len:],
#                                     np.tile(0, mini_batch_len)))

#         # count the No. of online ops to decide if do online_trend_update
#         if self.online_count == 0:
#             online_trend_update_toggle = True
#         else:
#             online_trend_update_toggle = False
#         # update self.online_count for next use
#         self.online_count = (self.online_count + 1) % online_trend_update_freq

#         # do fit_transform
#         online_mode = True
#         self.fit_transform(ts_data_online, online_mode, S_inv, warm_start_ini,
#                            online_trend_update_toggle, mini_batch_len)
#         adjusted_trend_online = self.adjusted_trend
#         adjusted_season_online = self.adjusted_season
#         irregular_data_online = self.irregular_data
#         # decomposed_data_online = self.decomposed_data

#         # # update trend_signal residual_signal
#         if online_update_mode == 'only_mini_batch':
#             adjusted_trend = np.vstack(
#                 (adjusted_trend[mini_batch_len:],
#                  adjusted_trend_online[-mini_batch_len:]))
#             adjusted_season = np.vstack(
#                 (adjusted_season[mini_batch_len:],
#                  adjusted_season_online[-mini_batch_len:]))
#             irregular_data = np.vstack(
#                 (irregular_data[mini_batch_len:],
#                  irregular_data_online[-mini_batch_len:]))
#         elif online_update_mode == 'entire_online_history':
#             update_data_len = online_history_length
#             adjusted_trend = np.vstack(
#                 (adjusted_trend[mini_batch_len:(-update_data_len +
#                                                 mini_batch_len)],
#                  adjusted_trend_online))
#             adjusted_season = np.vstack(
#                 (adjusted_season[mini_batch_len:(-update_data_len +
#                                                  mini_batch_len)],
#                  adjusted_season_online))
#             irregular_data = np.vstack(
#                 (irregular_data[mini_batch_len:(-update_data_len +
#                                                 mini_batch_len)],
#                  irregular_data_online))
#         else:
#             raise ValueError("input para of online_update_mode is not right")
#         decomposed_data = np.hstack(
#             (adjusted_trend, adjusted_season, irregular_data))

#         self.ts_data = ts_data
#         self.adjusted_trend = adjusted_trend
#         self.adjusted_season = adjusted_season
#         self.irregular_data = irregular_data
#         self.decomposed_data = decomposed_data
#         return decomposed_data

#     def _get_prev_transform(self):
#         # debug purpose  --> should be n*3 numpy.array
#         return self.decomposed_data


class RobustSTL():
    """Robust STL for decomposing time series

    Parameters
    ----------
    data_T : int
        The length of one period in terms of data points.
        The value must be larger than 1
    noise_toggle: boolean
        [default=True]
        The toggle that determines whether to denoise signal.
    noise_sigma_i : double
        [default=2.0]
        The standard deviation for intensity used in bilateral gaussian kernel
    noise_sigma_d : double
        [default=2.5]
        The standard deviation for distance used in bilateral gaussian kernel
    truncate: double
        [default=8.0]
        Used for the radius of the filter, which equals to truncate times
        sigma_d
    trend_toggle: boolean
        [default=True]
        The toggle that determines whether to detrend signal.
    trend_vlambda : double
        [default=40]
        The lambda to control first difference of trend in L1 reg
    trend_vlambda_diff : double
        [default=20]
        The lambda to control twice difference of trend in L1 reg
    trend_solver_method: str, 'g_lasso_admm','cvxopt_default', 'cvxopt_mosek'
        [default='g_lasso_admm']
        The solver to solve the L1 problem. cvxopt_mosek is much
        faster but need license.
    trend_solver_maxiters : int
        [default=15]
        Set the iteration number in  the optimization progress in trend filter
    trend_solver_show_progress: boolean
        [default=False]
        Show the optimization progress in trend filter
    trend_solver_admm_rho: float
        The para rho used in the admm solver in trend filter
    trend_solver_warm_start: boolean
        [default=True]
        speed up in the online mode by using warm_start in the admm solver
    season_toggle: boolean
        [default=True]
        The toggle that determines whether to deseasonize signal.
    season_bilateral_period_num : int
        The number of previous periods used in the deseason.
        num_period can be 1,2,3...,
    season_neighbour_wdw_size : int
        [default=20]
        The half length of the window used in historical period data
    season_sigma_i : double
        [default=2.5]
        The standard deviation for intensity used in bilateral gaussian kernel
    season_sigma_d : double
        [default=2.5]
        The standard deviation for distance used in bilateral gaussian kernel
    online_history_length : int
        The length of history data used in online mode for RobustSTL
    online_trend_update_freq : int
        [default=5]
        speed up in the online mode by only calculating trend every
        online_trend_update_freq times
    online_update_mode : string
        [default='only_mini_batch']
        The online update mode, can be 'only_mini_batch' or
        'entire_online_history', default is 'only_mini_batch'
    """
    def __init__(self,
                 data_T=288,
                 noise_toggle=True,
                 noise_sigma_i=2.0,
                 noise_sigma_d=2.5,
                 noise_truncate=8.0,
                 trend_toggle=True,
                 trend_vlambda=40,
                 trend_vlambda_diff=20,
                 trend_solver_method='g_lasso_admm',
                 trend_solver_maxiters=15,
                 trend_solver_show_progress=False,
                 trend_solver_admm_rho=1.0,
                 trend_solver_warm_start=True,
                 trend_down_sample=1,
                 season_toggle=True,
                 season_bilateral_period_num=2,
                 season_neighbour_wdw_size=20,
                 season_sigma_i=2,
                 season_sigma_d=10,
                 fastbatch_length=None,
                 online_history_length=None,
                 online_trend_update_freq=5,
                 online_update_mode='only_mini_batch'):

        self.data_T = data_T
        if data_T <= 1:
            raise ValueError("The data_T (period) must be larger than 1")
        self.noise_toggle = noise_toggle
        self.noise_sigma_i = noise_sigma_i
        self.noise_sigma_d = noise_sigma_d
        self.noise_truncate = noise_truncate

        self.trend_toggle = trend_toggle
        self.trend_vlambda = trend_vlambda
        self.trend_vlambda_diff = trend_vlambda_diff
        self.trend_solver_method = trend_solver_method
        self.trend_solver_maxiters = trend_solver_maxiters
        self.trend_solver_show_progress = trend_solver_show_progress
        self.trend_solver_admm_rho = trend_solver_admm_rho
        self.trend_solver_warm_start = trend_solver_warm_start
        self.trend_down_sample = trend_down_sample

        self.season_toggle = season_toggle
        self.season_bilateral_period_num = season_bilateral_period_num
        if season_bilateral_period_num < 1:
            raise ValueError("The season_bilateral_period_num at least 1")
        self.season_neighbour_wdw_size = season_neighbour_wdw_size
        self.season_sigma_i = season_sigma_i
        self.season_sigma_d = season_sigma_d

        self.fastbatch_length = fastbatch_length
        if online_history_length is None:
            self.online_history_length = 3 * data_T
        else:
            self.online_history_length = online_history_length
        self.online_trend_update_freq = online_trend_update_freq
        self.online_update_mode = online_update_mode
        self.online_count = 0

        # calcu S_inv for fit_transform_online only
        if trend_solver_method == 'GADMM' or trend_solver_method == 'PDHG':
            self.S_inv = None
        else:
            N = self.online_history_length
            T = self.data_T
            vlambda = trend_vlambda
            vlambda_diff = trend_vlambda_diff
            row = np.hstack((np.ones((1, T)), np.zeros((1, N - 1 - T))))
            mat_delta_t = circulant(row).T[:(N - T), :]
            eye_tmp = vlambda * np.eye(N - 1)
            row_tmp = np.hstack(
                (np.array([1, -1]).reshape(1, 2), np.zeros((1, N - 2 - 1))))
            Diff_1st_tmp = vlambda_diff * circulant(row_tmp).T[:(N - 2), :]
            extended_mat = np.vstack((mat_delta_t, eye_tmp, Diff_1st_tmp))
            F_admm = extended_mat
            rho = trend_solver_admm_rho
            S = rho * F_admm.T.dot(F_admm)
            self.S_inv = np.linalg.inv(S)

        # Fast-RobustSTL code extension: based on GADMM or PDHG
        # temp for fast-stl, if ts_data_length is changed, need update
        # for the first time we need update, so ini value=0 here
        self.ts_data_length = 0
        self.fast_trend_record = None

    def fit_transform_fastbatch(self, ts_data):
        data_T = self.data_T
        fastbatch_length = self.fastbatch_length
        if fastbatch_length is None:
            fastbatch_length = 4 * data_T

        # decide how many times to call fit_transform
        data_validity_check(ts_data)
        ts_data = np.array(ts_data).flatten().reshape(-1, 1)
        total_len = len(ts_data)
        segments_num = int(total_len / fastbatch_length)

        if segments_num <= 1:
            decomposed_data = self.fit_transform(ts_data)
            return decomposed_data
        else:
            adjusted_trend = np.array([])
            adjusted_season = np.array([])
            irregular_data = np.array([])
            # for loop and contact, the last one is longer.
            for seg_idx in range(segments_num):
                print("seg_idx")
                print(seg_idx)
                if seg_idx == segments_num - 1:
                    seg_data = ts_data[seg_idx * fastbatch_length:]
                else:
                    seg_data = ts_data[seg_idx *
                                       fastbatch_length:(seg_idx + 1) *
                                       fastbatch_length]

                self.fit_transform(seg_data)
                adjusted_trend_tmp = self.adjusted_trend
                adjusted_season_tmp = self.adjusted_season
                irregular_data_tmp = self.irregular_data

                adjusted_trend = np.append(adjusted_trend, adjusted_trend_tmp)
                adjusted_season = np.append(adjusted_season,
                                            adjusted_season_tmp)
                irregular_data = np.append(irregular_data, irregular_data_tmp)
        adjusted_trend = adjusted_trend.reshape(-1, 1)
        adjusted_season = adjusted_season.reshape(-1, 1)
        irregular_data = irregular_data.reshape(-1, 1)

        decomposed_data = np.hstack(
            (adjusted_trend, adjusted_season, irregular_data))
        self.adjusted_trend = adjusted_trend
        self.adjusted_season = adjusted_season
        self.irregular_data = irregular_data
        self.decomposed_data = decomposed_data
        return decomposed_data

    def fit_transform(self,
                      ts_data,
                      online_mode=False,
                      S_inv=None,
                      warm_start_iniValue=None,
                      online_trend_update_toggle=True,
                      mini_batch_len=None,
                      override=True):
        """
        This method to do robust STL filtering process for time sereis.

        Parameters
        ----------
        ts_data : array-like, shape = (n_timelength, 1) or (n_timelength,)
            The input data, 1D data: can be numpy array, list,
            or pandas dataframe.
        online_mode: boolean
            [default=False]
            In batch mode, this is False. Used in online mode to speed up.
        S_inv : matrix
            To be used for online case only to speed up
        warm_start_iniValue: array-like, shape = (n_timelength, 1)
            Used in online mode to speed up the trend solver.
        online_trend_update_toggle: boolean
            [default=True]
            In batch mode, this is True, which decides when to upate trend.
        mini_batch_len: int
            the length of mini_batch
        override : boolean
            To be used for online case, ignore for now.

        Returns
        -------
        decomposed_data : array-like, shape = (n_timelength, 3)
            The output of decomposed signal: [trend, season, irregular]
        """
        data_T = self.data_T

        noise_toggle = self.noise_toggle
        noise_sigma_i = self.noise_sigma_i
        noise_sigma_d = self.noise_sigma_d
        noise_truncate = self.noise_truncate

        trend_toggle = self.trend_toggle
        trend_vlambda = self.trend_vlambda
        trend_vlambda_diff = self.trend_vlambda_diff
        trend_solver_method = self.trend_solver_method
        trend_solver_maxiters = self.trend_solver_maxiters
        trend_solver_show_progress = self.trend_solver_show_progress
        trend_solver_admm_rho = self.trend_solver_admm_rho
        trend_solver_warm_start = self.trend_solver_warm_start
        trend_down_sample = self.trend_down_sample

        season_toggle = self.season_toggle
        season_bilateral_period_num = self.season_bilateral_period_num
        season_neighbour_wdw_size = self.season_neighbour_wdw_size
        season_sigma_i = self.season_sigma_i
        season_sigma_d = self.season_sigma_d
        online_history_length = self.online_history_length

        data_validity_check(ts_data)
        ts_data = np.array(ts_data).flatten().reshape(-1, 1)
        original_data = ts_data.copy()

        # 1st step: denoising
        if noise_toggle:
            if not online_mode:  # batch mode
                self.denoising_filter = CausalBilateralFilter(
                    sigma_i=noise_sigma_i,
                    sigma_d=noise_sigma_d,
                    truncate=noise_truncate)
                denoised_data = self.denoising_filter.fit_transform(
                    original_data)
            else:  # online mode
                new_ts_minibatch = ts_data[-mini_batch_len:]
                denoised_data = self.denoising_filter.fit_transform_online(
                    new_ts_minibatch)
                denoised_data = denoised_data[-online_history_length:]
        else:
            denoised_data = original_data  # no denoising

        noise_data = original_data - denoised_data

        # 2nd step: detrending
        if trend_toggle:
            # online_trend_update_toggle == True by default,
            # since in batch mode we must do this
            if online_trend_update_toggle:
                # suppose operate as (1) batch, online, online,...
                # or (2) batch, batch, batch, ....
                # if input ts_data_length, fast-stl needs update
                cur_ts_data_length = len(ts_data)
                if self.ts_data_length != cur_ts_data_length:
                    self.ts_data_length = cur_ts_data_length
                    update_fast_trend = True
                else:
                    update_fast_trend = False
                ini_trend_data, next_warm_start_value, fast_trend_solver = \
                    lad_trend(
                        denoised_data=denoised_data,
                        data_T=data_T,
                        vlambda=trend_vlambda,
                        vlambda_diff=trend_vlambda_diff,
                        solver_method=trend_solver_method,
                        trend_solver_admm_rho=trend_solver_admm_rho,
                        trend_down_sample=trend_down_sample,
                        maxiters=trend_solver_maxiters,
                        show_progress=trend_solver_show_progress,
                        S_inv=S_inv,
                        warm_start_toggle=trend_solver_warm_start,
                        warm_start_iniValue=warm_start_iniValue,
                        update_fast_trend=update_fast_trend,
                        fast_trend_record=self.fast_trend_record)
                self.fast_trend_record = fast_trend_solver
            else:
                next_warm_start_value = np.hstack(
                    (self.next_warm_start_value[mini_batch_len:],
                     np.tile(0.0, mini_batch_len)))
                # ini_trend_data, can also do interplate or prediction]
                ini_trend_data_tmp = self.ini_trend_data.reshape(-1, )
                ini_trend_data = np.hstack(
                    (ini_trend_data_tmp[mini_batch_len:],
                     np.tile(ini_trend_data_tmp[-1], mini_batch_len)))
                ini_trend_data = ini_trend_data.reshape(-1, 1)
            # summary
            coarse_season = denoised_data - ini_trend_data
            self.ini_trend_data = ini_trend_data
            self.next_warm_start_value = next_warm_start_value

        else:
            ini_trend_data = np.zeros_like(denoised_data)
            coarse_season = denoised_data
            self.next_warm_start_value = np.zeros(online_history_length)

        # 3rd step: de-season
        if season_toggle:
            # # version 1 with later evel_adj = np.mean(ini_season_data)
            # ini_season_data, irregular_season = bilateral_season(
            #     coarse_season=coarse_season,
            #     data_T=data_T,
            #     bilateral_period_num=season_bilateral_period_num,
            #     neighbour_wdw_size=season_neighbour_wdw_size,
            #     sigma_i=season_sigma_i,
            #     sigma_d=season_sigma_d)

            # # version 2 is the same as above when set level_adj=0 later
            # # much better speedup with level_adj = np.mean(coarse_season)
            if not online_mode:  # batch mode
                ini_season_data, irregular_season = bilateral_season(
                    coarse_season=coarse_season,
                    data_T=data_T,
                    bilateral_period_num=season_bilateral_period_num,
                    neighbour_wdw_size=season_neighbour_wdw_size,
                    sigma_i=season_sigma_i,
                    sigma_d=season_sigma_d,
                    fit_transform_len_spec=None)
                self.ini_season_data = ini_season_data[-online_history_length:]
            else:  # online mode:
                ini_season_data, irregular_season = bilateral_season(
                    coarse_season=coarse_season,
                    data_T=data_T,
                    bilateral_period_num=season_bilateral_period_num,
                    neighbour_wdw_size=season_neighbour_wdw_size,
                    sigma_i=season_sigma_i,
                    sigma_d=season_sigma_d,
                    fit_transform_len_spec=mini_batch_len)
                ini_season_data = np.vstack(
                    (self.ini_season_data[mini_batch_len:],
                     ini_season_data[-mini_batch_len:]))
                self.ini_season_data = ini_season_data
        else:
            coarse_season_mean = np.mean(coarse_season)
            irregular_season = coarse_season - coarse_season_mean
            ini_season_data = coarse_season_mean * \
                np.ones_like(coarse_season)

        # 4nd step: adjust trend, season, irregular_season.
        # make mean(adjusted_season) = 0
        # level_adj = np.mean(ini_season_data)

        # adopt mean(coarse_season) to approx mean(ini_season_data)
        # in the version 1 in step_3
        level_adj = np.mean(coarse_season)
        adjusted_season = ini_season_data - level_adj
        if trend_toggle:
            adjusted_trend = ini_trend_data + level_adj
            irregular_data = irregular_season + noise_data
        else:
            adjusted_trend = ini_trend_data
            irregular_data = irregular_season + noise_data + level_adj

        decomposed_data = np.hstack(
            (adjusted_trend, adjusted_season, irregular_data))

        self.ts_data = ts_data
        self.adjusted_trend = adjusted_trend
        self.adjusted_season = adjusted_season
        self.irregular_data = irregular_data
        self.decomposed_data = decomposed_data
        return decomposed_data

    def fit_transform_online(self, new_ts_minibatch):
        """
        The online version of RobustSTL to do the fit transform with
        much faster speed.
        Note:
        1, online mode only uses the most recent online_history_length data
        (including the newest new_ts_minibatch) to do decomposition.
        2, the output would be the same as the output of fit_transform (i.e.,
        the length is len(ts_data)), but only the last len(new_ts_minibatch)
        data is computed from the fit_transform_online.

        Parameters
        ---------
        new_ts_minibatch : array-like, shape = (n_minibatch, 1)
        or (n_minibatch,)
            Single time series of length mini_batch_timelength.

        Returns
        -------
        decomposed_data : numpy array, shape = (n_timelength, 3)
            The output of decomposed signal: [trend, season, irregular]
        """
        S_inv = self.S_inv
        next_warm_start_value = self.next_warm_start_value
        online_history_length = self.online_history_length
        online_update_mode = self.online_update_mode
        online_trend_update_freq = self.online_trend_update_freq

        ts_data = self.ts_data
        adjusted_trend = self.adjusted_trend
        adjusted_season = self.adjusted_season
        irregular_data = self.irregular_data
        decomposed_data = self.decomposed_data

        mini_batch_len = len(new_ts_minibatch)
        if self.ts_data is None:
            raise ValueError("fit_transform_online can not be called in the\
                             very first time, should followed fit_transform.")
        if mini_batch_len > online_history_length:
            raise ValueError("for online detrend, the length of mini_batch\
                             should not larger than the online_history_length")
        if len(ts_data) < online_history_length:
            raise ValueError("for online detrend, the online_history_length\
                             should not larger than the initial len(ts_data)")

        # check new_ts_minibatch and change format to 1d np.array
        data_validity_check(new_ts_minibatch)
        new_ts_minibatch = np.array(new_ts_minibatch).flatten()
        new_ts_minibatch = new_ts_minibatch.reshape(-1, 1)

        # update ts_data
        ts_data = np.concatenate([ts_data[mini_batch_len:], new_ts_minibatch])
        # detrend for ts_data_online_part
        ts_data_online = ts_data[-online_history_length:]

        next_warm_start_value = \
            next_warm_start_value[-(online_history_length-1):]
        # warm_start_ini = np.hstack(
        #         (next_warm_start_value[mini_batch_len:],
        #          np.tile(next_warm_start_value[-1], mini_batch_len)))
        warm_start_ini = np.hstack((next_warm_start_value[mini_batch_len:],
                                    np.tile(0, mini_batch_len)))

        # count the No. of online ops to decide if do online_trend_update
        if self.online_count == 0:
            online_trend_update_toggle = True
        else:
            online_trend_update_toggle = False
        # update self.online_count for next use
        self.online_count = (self.online_count + 1) % online_trend_update_freq

        # do fit_transform
        online_mode = True
        self.fit_transform(ts_data_online, online_mode, S_inv, warm_start_ini,
                           online_trend_update_toggle, mini_batch_len)
        adjusted_trend_online = self.adjusted_trend
        adjusted_season_online = self.adjusted_season
        irregular_data_online = self.irregular_data
        # decomposed_data_online = self.decomposed_data

        # # update trend_signal residual_signal
        if online_update_mode == 'only_mini_batch':
            adjusted_trend = np.vstack(
                (adjusted_trend[mini_batch_len:],
                 adjusted_trend_online[-mini_batch_len:]))
            adjusted_season = np.vstack(
                (adjusted_season[mini_batch_len:],
                 adjusted_season_online[-mini_batch_len:]))
            irregular_data = np.vstack(
                (irregular_data[mini_batch_len:],
                 irregular_data_online[-mini_batch_len:]))
        elif online_update_mode == 'entire_online_history':
            update_data_len = online_history_length
            adjusted_trend = np.vstack(
                (adjusted_trend[mini_batch_len:(-update_data_len +
                                                mini_batch_len)],
                 adjusted_trend_online))
            adjusted_season = np.vstack(
                (adjusted_season[mini_batch_len:(-update_data_len +
                                                 mini_batch_len)],
                 adjusted_season_online))
            irregular_data = np.vstack(
                (irregular_data[mini_batch_len:(-update_data_len +
                                                mini_batch_len)],
                 irregular_data_online))
        else:
            raise ValueError("input para of online_update_mode is not right")
        decomposed_data = np.hstack(
            (adjusted_trend, adjusted_season, irregular_data))

        self.ts_data = ts_data
        self.adjusted_trend = adjusted_trend
        self.adjusted_season = adjusted_season
        self.irregular_data = irregular_data
        self.decomposed_data = decomposed_data
        return decomposed_data

    def _get_prev_transform(self):
        # debug purpose  --> should be n*3 numpy.array
        return self.decomposed_data


class RobustSTL_partialComponent(RobustSTL):
    """Robust STL for decomposing time series, only output partial component

    Parameters
    ----------
    partial_component: str
        [default='trend']
        The partial component of the RobustSTL output, can be 'trend',
        'season', 'residual', 'deseasonalized', and 'detrended'.
        'deseasonalized' indicates 'trend' + 'residual'; while
        'detrended' indicates 'season' + 'residual'.
    data_T : int
        The length of one period in terms of data points.
        The value must be larger than 1
    noise_toggle: boolean
        [default=True]
        The toggle that determines whether to denoise signal.
    noise_sigma_i : double
        [default=2.0]
        The standard deviation for intensity used in bilateral gaussian kernel
    noise_sigma_d : double
        [default=2.5]
        The standard deviation for distance used in bilateral gaussian kernel
    truncate: double
        [default=8.0]
        Used for the radius of the filter, which equals to truncate times
        sigma_d
    trend_toggle: boolean
        [default=True]
        The toggle that determines whether to detrend signal.
    trend_vlambda : double
        [default=200]
        The lambda to control first difference of trend in L1 reg
    trend_vlambda_diff : double
        [default=200]
        The lambda to control twice difference of trend in L1 reg
    trend_solver_method: str, 'g_lasso_admm', 'cvxopt_default', 'cvxopt_mosek'
        [default='g_lasso_admm']
        The solver to solve the L1 problem. cvxopt_mosek is much
        faster but need license.
    trend_solver_maxiters : int
        [default=200]
        Set the iteration number in  the optimization progress in trend filter
    trend_solver_show_progress: boolean
        [default=False]
        Show the optimization progress in trend filter
    season_toggle: boolean
        [default=True]
        The toggle that determines whether to deseasonize signal.
    season_bilateral_period_num : int
        The number of previous periods used in the deseason.
        num_period can be 1,2,3...,
    season_neighbour_wdw_size : int
        [default=20]
        The half length of the window used in historical period data
    season_sigma_i : double
        [default=2.5]
        The standard deviation for intensity used in bilateral gaussian kernel
    season_sigma_d : double
        [default=2.5]
        The standard deviation for distance used in bilateral gaussian kernel
    online_history_length : int
        The length of history data used in online mode for RobustSTL
    online_update_mode : string
        [default='only_mini_batch']
        The online update mode, can be 'only_mini_batch' or
        'entire_online_history', default is 'only_mini_batch'
    """

    def __init__(self,
                 partial_component='trend',
                 data_T=288,
                 noise_toggle=True,
                 noise_sigma_i=2.0,
                 noise_sigma_d=2.5,
                 noise_truncate=8.0,
                 trend_toggle=True,
                 trend_vlambda=40,
                 trend_vlambda_diff=20,
                 trend_solver_method='g_lasso_admm',
                 trend_solver_maxiters=15,
                 trend_solver_show_progress=False,
                 trend_solver_admm_rho=1.0,
                 trend_solver_warm_start=True,
                 season_toggle=True,
                 season_bilateral_period_num=2,
                 season_neighbour_wdw_size=20,
                 season_sigma_i=2,
                 season_sigma_d=10,
                 online_history_length=288 * 3,
                 online_trend_update_freq=5,
                 online_update_mode='only_mini_batch'):

        super().__init__(data_T, noise_toggle, noise_sigma_i, noise_sigma_d,
                         noise_truncate, trend_toggle, trend_vlambda,
                         trend_vlambda_diff, trend_solver_method,
                         trend_solver_maxiters, trend_solver_show_progress,
                         trend_solver_admm_rho, trend_solver_warm_start,
                         season_toggle, season_bilateral_period_num,
                         season_neighbour_wdw_size, season_sigma_i,
                         season_sigma_d, online_history_length,
                         online_trend_update_freq, online_update_mode)
        self.partial_component = partial_component

    def _return_partial_component(self, partial_component, decomposed_data):
        if partial_component == 'trend':
            decomposed_out = decomposed_data[:, 0].reshape(-1, 1)
        elif partial_component == 'season':
            decomposed_out = decomposed_data[:, 1].reshape(-1, 1)
        elif partial_component == 'residual':
            decomposed_out = decomposed_data[:, 2].reshape(-1, 1)
        elif partial_component == 'deseasonalized':
            decomposed_out = decomposed_data[:, 0].reshape(-1, 1) + \
                decomposed_data[:, 2].reshape(-1, 1)
        elif partial_component == 'detrended':
            decomposed_out = decomposed_data[:, 1].reshape(-1, 1) + \
                decomposed_data[:, 2].reshape(-1, 1)
        else:
            raise ValueError("the parameter partial_component must one of the\
                'trend', 'season', 'residual', 'deseasonalized', 'detrended'")
        return decomposed_out

    def fit_transform(self,
                      ts_data,
                      online_mode=False,
                      S_inv=None,
                      warm_start_iniValue=None,
                      online_trend_update_toggle=True,
                      mini_batch_len=None,
                      override=True):
        partial_component = self.partial_component
        decomposed_data = super().fit_transform(ts_data, online_mode, S_inv,
                                                warm_start_iniValue,
                                                online_trend_update_toggle,
                                                mini_batch_len, override)
        return self._return_partial_component(partial_component,
                                              decomposed_data)

    def fit_transform_online(self, new_ts_minibatch):
        partial_component = self.partial_component
        decomposed_data = super().fit_transform_online(new_ts_minibatch)
        return self._return_partial_component(partial_component,
                                              decomposed_data)

    def _get_prev_transform(self):
        partial_component = self.partial_component
        decomposed_data = self.decomposed_data
        return self._return_partial_component(partial_component,
                                              decomposed_data)
