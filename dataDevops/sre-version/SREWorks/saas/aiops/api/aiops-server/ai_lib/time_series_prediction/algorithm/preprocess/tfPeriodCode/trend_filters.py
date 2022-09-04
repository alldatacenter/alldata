import numpy as np
import pandas as pd
import numbers
from numbers import Number
from statsmodels.tsa.filters.hp_filter import hpfilter
from itertools import chain
from cvxopt import matrix, solvers, spmatrix

solvers.options['show_progress'] = False


class HPTrendFilter():
    """(Robust) HP Trend Filter( L2 Trend Filter) for detrending time series

    Parameters
    ----------
    lambda_reg : float
        The regularization term lambda used in L2 Trend Objective Function
    online_history_length : int
        The length of history data used in online mode for detrending
    online_update_mode : string
        The online update mode, can be 'only_mini_batch' or
        'entire_online_history', default is 'only_mini_batch'
    quasi_robust : Boolean, [default=False]
        A toggle to control if we will use quasi_robust method to clip the
        time signal before entering trend filter
    fit_model_mode : string
        The fit_model mode, can be 'batch' or 'sliding_wdw', default is 'batch'
        'batch': run HP on the whole data
        'sliding_wdw': calculate HP on a sliding window of size
                       online_history_length
    upper_percent: float in [0., 1], [default=0.95]
        The upper percentile to decide whether the time signal before
        entering trend filter should be clipped
    lower_percent: float in [0., 1], [default=0.05]
        The lower percentile to decide whether the time signal before
        entering trend filter should be clipped
    """

    def __init__(self,
                 lambda_reg=1e4,
                 lambda_reg_batch=None,
                 fit_model_mode='batch',
                 online_history_length=100,
                 online_update_mode='only_mini_batch',
                 quasi_robust=False,
                 percentile_context_wdw_size=800,
                 upper_percent=0.95,
                 lower_percent=0.05):

        self.lambda_reg = lambda_reg
        self.lambda_reg_batch = lambda_reg_batch
        self.online_history_length = online_history_length
        self.ts_data = None
        self.trend_signal = None
        self.residual_signal = None
        self.detrended_out = None
        self.online_update_mode = online_update_mode
        self.quasi_robust = quasi_robust
        self.fit_model_mode = fit_model_mode
        self.upper_percent = upper_percent
        self.lower_percent = lower_percent
        self.percentile_context_wdw_size = percentile_context_wdw_size

        self.upper_percent = upper_percent
        if not isinstance(self.upper_percent, Number):
            raise ValueError("upper_percent must be a number!")
        elif self.upper_percent < 0 or self.upper_percent > 1:
            raise ValueError("upper_percent must be 0 ~ 1 !")

        if self.lower_percent > self.upper_percent:
            raise ValueError("lower_percent must lower than upper_percent!")

    def fit_transform(self, ts_data, override=True):
        """
        This method to do detrending filtering process for time sereis.

        Parameters
        ----------
        ts_data : array-like, shape = (n_timelength, 1) or (n_timelength,)
            The input data, 1D data: can be numpy array, list,
            or pandas dataframe.
        override : boolean
            To be used for online case, ignore for now.

        Returns
        -------
        detrended_out : numpy array, shape = (n_timelength, 2)
            The output of detrended time series: the first column is trend
            signal, the second signal is the residual signal
        """
        lambda_reg = self.lambda_reg

        data_validity_check(ts_data)
        ts_data = np.array(ts_data).flatten().reshape(-1, 1)

        n_samples, n_dim = ts_data.shape

        if self.fit_model_mode == "batch":
            if self.lambda_reg_batch is not None:
                lambda_reg = self.lambda_reg_batch
            trend_signal = self._get_trend(ts_data, lambda_reg)

        elif self.fit_model_mode == "sliding_wdw":
            # left-most few points with length of online_history_length
            inital_ts_data = ts_data[:self.online_history_length]
            trend_signal = self._get_trend(inital_ts_data, lambda_reg)

            # sliding window of size online_history_length
            # from left to right
            for i in range(self.online_history_length, n_samples):
                temp_ts = ts_data[(i - self.online_history_length + 1):i + 1]
                temp_trend = self._get_trend(
                    temp_ts,
                    lambda_reg,
                    percentile_context_wdw_sz=self.percentile_context_wdw_size)

                trend_signal = np.vstack((trend_signal, temp_trend[-1:]))

        residual_signal = ts_data - trend_signal
        detrended_out = np.hstack((trend_signal, residual_signal))

        self.ts_data = ts_data
        self.trend_signal = trend_signal
        self.residual_signal = residual_signal
        self.detrended_out = detrended_out

        return detrended_out

    def _get_trend(self, ts_data, lambda_reg, percentile_context_wdw_sz=None):

        if self.quasi_robust:
            percentile_context_wdw_sz = percentile_context_wdw_sz or 0
            ts_data = ts_data[-percentile_context_wdw_sz:]
            _lower = np.percentile(ts_data, self.lower_percent * 100)
            _upper = np.percentile(ts_data, self.upper_percent * 100)
            quasi_robust_ts_data = np.clip(ts_data, _lower, _upper)
            self.model = (_lower, _upper)
        else:
            quasi_robust_ts_data = ts_data

        _residual_signal, trend_signal = hpfilter(quasi_robust_ts_data,
                                                  lambda_reg)
        trend_signal = trend_signal.reshape(-1, 1)
        return trend_signal


class HPTrendFilter_trend(HPTrendFilter):
    def __init__(self,
                 lambda_reg=1e4,
                 lambda_reg_batch=None,
                 fit_model_mode='batch',
                 online_history_length=100,
                 online_update_mode='only_mini_batch',
                 quasi_robust=False,
                 percentile_context_wdw_size=800,
                 upper_percent=0.95,
                 lower_percent=0.05):
        super().__init__(lambda_reg, lambda_reg_batch, fit_model_mode,
                         online_history_length, online_update_mode,
                         quasi_robust, percentile_context_wdw_size,
                         upper_percent, lower_percent)

    def fit_transform(self, ts_data, override=True):
        detrended_out = super().fit_transform(ts_data, override)
        return detrended_out[:, 0].reshape(-1, 1)


class L1TrendFilter():
    """L1 Trend Filter for detrending time series

    Parameters
    ----------
    lambda_reg : float
        The regularization term lambda used in L1 Trend Objective Function
    online_history_length : int
        The length of history data used in online mode for detrending
    piece_wise_fitting_method : string
        The fitting method corresponding to reg term
        can be 'linear' or 'constant', default is 'linear'
    online_update_mode : string
        The online update mode, can be 'only_mini_batch' or
        'entire_online_history', default is 'only_mini_batch'
    quasi_robust : Boolean, [default=False]
        A toggle to control if we will use quasi_robust method to clip the
        time signal before entering trend filter
    upper_percent: float in [0., 1], [default=0.95]
        The upper percentile to decide whether the time signal before
        entering trend filter should be clipped
    lower_percent: float in [0., 1], [default=0.05]
        The lower percentile to decide whether the time signal before
        entering trend filter should be clipped
    """

    def __init__(self,
                 piece_wise_fitting_method='linear',
                 lambda_reg=1.5,
                 lambda_reg_batch=None,
                 online_history_length=200,
                 online_update_mode='only_mini_batch',
                 quasi_robust=False,
                 percentile_context_wdw_size=800,
                 upper_percent=0.95,
                 lower_percent=0.05):

        self.lambda_reg = lambda_reg
        self.lambda_reg_batch = lambda_reg_batch
        self.online_history_length = online_history_length
        self.piece_wise_fitting_method = piece_wise_fitting_method
        self.ts_data = None
        self.trend_signal = None
        self.residual_signal = None
        self.detrended_out = None
        self.online_update_mode = online_update_mode
        self.quasi_robust = quasi_robust
        self.upper_percent = upper_percent
        self.lower_percent = lower_percent
        self.percentile_context_wdw_size = percentile_context_wdw_size

        self.upper_percent = upper_percent
        if not isinstance(self.upper_percent, Number):
            raise ValueError("upper_percent must be a number!")
        elif self.upper_percent < 0 or self.upper_percent > 1:
            raise ValueError("upper_percent must be 0 ~ 1 !")

        if self.lower_percent > self.upper_percent:
            raise ValueError("lower_percent must lower than upper_percent!")

    def fit_transform(self, ts_data, override=True):
        """
        This method to do detrending filtering process for time sereis.

        Parameters
        ----------
        ts_data : array-like, shape = (n_timelength, 1) or (n_timelength,)
            The input data, 1D data: can be numpy array, list,
            or pandas dataframe.
        override : boolean
            To be used for online case, ignore for now.

        Returns
        -------
        detrended_out : numpy array, shape = (n_timelength, 2)
            The output of detrended time series: the first column is trend
            signal, the second signal is the residual signal
        """
        lambda_reg = self.lambda_reg
        piece_wise_fitting_method = self.piece_wise_fitting_method

        data_validity_check(ts_data)
        ts_data = np.array(ts_data).flatten().reshape(-1, 1)

        _lower = np.percentile(ts_data, self.lower_percent * 100)
        _upper = np.percentile(ts_data, self.upper_percent * 100)
        self.model = (_lower, _upper)
        if self.quasi_robust:
            quasi_robust_ts_data = np.clip(ts_data, _lower, _upper)
        else:
            quasi_robust_ts_data = ts_data
        if self.lambda_reg_batch is not None:
            lambda_reg = self.lambda_reg_batch
        trend_signal = l1_trend_filter(quasi_robust_ts_data, lambda_reg,
                                       piece_wise_fitting_method)

        trend_signal = trend_signal.reshape(-1, 1)
        residual_signal = ts_data - trend_signal
        detrended_out = np.hstack((trend_signal, residual_signal))

        self.ts_data = ts_data
        self.trend_signal = trend_signal
        self.residual_signal = residual_signal
        self.detrended_out = detrended_out

        return detrended_out


class L1TrendFilter_trend(L1TrendFilter):
    def __init__(self,
                 piece_wise_fitting_method='linear',
                 lambda_reg=1.5,
                 lambda_reg_batch=None,
                 online_history_length=200,
                 online_update_mode='only_mini_batch',
                 quasi_robust=False,
                 percentile_context_wdw_size=800,
                 upper_percent=0.95,
                 lower_percent=0.05):
        super().__init__(
            piece_wise_fitting_method, lambda_reg, lambda_reg_batch,
            online_history_length, online_update_mode, quasi_robust,
            percentile_context_wdw_size, upper_percent, lower_percent)

    def fit_transform(self, ts_data, override=True):
        detrended_out = super().fit_transform(ts_data, override)
        return detrended_out[:, 0].reshape(-1, 1)


def _second_order_derivative_matrix(size_of_matrix, piece_wise_fitting_method):
    """ Return a second order derivative matrix
    for a given signal size

    Parameters:
        size_of_matrix(int): Size of matrix
    Returns:
        second_order(cvxopt.spmatrix): Sparse matrix
        that has the second order derivative matrix
    """
    temp = size_of_matrix - 2
    if piece_wise_fitting_method == 'linear':
        first = [1, -2, 1] * temp
    elif piece_wise_fitting_method == 'constant':
        first = [1, -1, 0] * temp
        # first = [0, 1, -1] * temp
    second = list(chain.from_iterable([[ii] * 3 for ii in range(temp)]))
    third = list(
        chain.from_iterable([[ii, ii + 1, ii + 2] for ii in range(temp)]))
    second_order = spmatrix(first, second, third)

    return second_order


def _l1(signal, regularizer, piece_wise_fitting_method):
    """
    Parameters:
        signal(np.ndarray): Original, volatile signal
        regularizer(float): regularizer to keep the balance between smoothing
            and 'truthfulness' of the signal
    Returns:
        trend(np.ndarray): Trend of the signal extracted from l1 regularization
    Problem Formulation:
        minimize    (1/2) * ||x - signal||_2^2 + regularizer * sum(y)
        subject to  | D*x | <= y
    """

    signal_size = signal.size[0]
    temp = signal_size - 2
    temp_ls = range(temp)

    D = _second_order_derivative_matrix(signal_size, piece_wise_fitting_method)
    P = D * D.T
    q = -D * signal

    G = spmatrix([], [], [], (2 * temp, temp))
    G[:temp, :temp] = spmatrix(1.0, temp_ls, temp_ls)
    G[temp:, :temp] = -spmatrix(1.0, temp_ls, temp_ls)
    h = matrix(regularizer, (2 * temp, 1), tc='d')
    residual = solvers.qp(P, q, G, h)
    trend = signal - D.T * residual['x']

    return trend


def l1_trend_filter(signal, regularizer, piece_wise_fitting_method):
    """
    Fits the l1 trend on top of the `signal` with a particular
    `regularizer`
    Parameters:
            signal(np.ndarray): Original Signal that we want to fit l1
                trend
            regularizer(float): Regularizer which provides a balance between
                smoothing of a signal and truthfulness of signal
    Returns:
        values(np.array): L1 Trend of a signal that is extracted from
        the signal
    """

    if not isinstance(signal, np.ndarray):
        raise TypeError("Signal Needs to be a numpy array")

    m = float(signal.min())
    M = float(signal.max())
    difference = M - m
    if not difference:  # If signal is constant
        difference = 1
    t = (signal - m) / difference

    values = matrix(t)
    values = _l1(values, regularizer, piece_wise_fitting_method)
    values = values * difference + m
    values = np.asarray(values).squeeze()

    # TEMP special processing for L1 constant fitting:
    # 1, this can deal the last element of batch processing
    # 2, this is also make online mode more smoothing
    if piece_wise_fitting_method == 'constant':
        values[-1] = values[-2]

    return values


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
