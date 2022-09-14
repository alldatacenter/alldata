"""two functions to get trend and season:
Sparse and robust method to get trend
Bilateral season adjust to get season

Author: Qingsong <qingsong.wen@alibaba-inc.com>
"""

from numpy.linalg import norm, cholesky
import scipy.sparse as sparse
import time
import numpy as np
import pandas as pd
from cvxopt import matrix
from scipy.linalg import circulant
from sklearn.preprocessing import StandardScaler

# import logging
# from scipy.sparse import csc_matrix
# from scipy.sparse.linalg import lsqr
# import scipy.sparse as sp
from numbers import Number
import numbers


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


def lad_trend(denoised_data, data_T, vlambda, vlambda_diff, solver_method,
              trend_solver_admm_rho, maxiters, show_progress, S_inv,
              warm_start_toggle, warm_start_iniValue):

    # normalize input data
    scaler = StandardScaler()
    denoised_data_scaled = scaler.fit_transform(denoised_data)

    T = data_T
    N = len(denoised_data_scaled)
    # season diff
    pd_denoised_data = pd.DataFrame(denoised_data_scaled)
    g_t = pd_denoised_data.diff(periods=T)[T:].values.flatten().reshape(-1, 1)

    # reformulate 3 L1 to 1 L1 (extended L1)
    extended_g_t = np.vstack((g_t, np.zeros((N - 1, 1)), np.zeros((N - 2, 1))))
    row = np.hstack((np.ones((1, T)), np.zeros((1, N - 1 - T))))
    mat_delta_t = circulant(row).T[:(N - T), :]
    eye_tmp = vlambda * np.eye(N - 1)
    row_tmp = np.hstack(
        (np.array([1, -1]).reshape(1, 2), np.zeros((1, N - 2 - 1))))
    Diff_1st_tmp = vlambda_diff * circulant(row_tmp).T[:(N - 2), :]
    extended_mat = np.vstack((mat_delta_t, eye_tmp, Diff_1st_tmp))

    # method based on LP in CVXOPT
    if solver_method == 'cvxopt_default':
        from .L1_LPsolver import l1blas
        P = matrix(extended_mat)
        q = matrix(extended_g_t)
        # delta_t = l1(P, q)
        delta_t = l1blas(P, q, maxiters, show_progress)  # faster
        next_warm_start_value = np.asarray(delta_t).reshape(-1, )
    elif solver_method == 'cvxopt_mosek':
        from .L1_LPsolver import l1mosek2
        P = matrix(extended_mat)
        q = matrix(extended_g_t)
        # delta_t = l1mosek(P,q)
        delta_t = l1mosek2(P, q, maxiters, show_progress)  # faster
        next_warm_start_value = np.asarray(delta_t).reshape(-1, )
    elif solver_method == 'g_lasso_admm':
        l_admm, n_admm = extended_mat.shape
        m_admm = 1
        A_admm = np.zeros((m_admm, n_admm))
        b_admm = np.zeros(m_admm)
        F_admm = extended_mat
        p_admm = extended_g_t
        p_admm = p_admm.ravel()
        reg_lambda = 1.0

        R_admm = g_lasso_admm_solver(A_admm,
                                     b_admm,
                                     F_admm,
                                     p_admm,
                                     reg_lambda,
                                     x0=warm_start_iniValue,
                                     rho=trend_solver_admm_rho,
                                     MAX_ITER=maxiters,
                                     rel_par=1.,
                                     QUIET=(not show_progress),
                                     tol_abs=1e-4,
                                     S_inv=S_inv,
                                     warm_start_toggle=warm_start_toggle)
        delta_t = R_admm[0].reshape(-1, 1)
        next_warm_start_value = R_admm[0]

    # add [0] as the first element to keep data length
    delta_t_out = np.vstack(([0], np.asarray(delta_t)))
    ini_trend_data_scaled = np.cumsum(delta_t_out).reshape(-1, 1)
    # de-normalize data
    ini_trend_data = scaler.inverse_transform(ini_trend_data_scaled) - \
        float(scaler.mean_)
    return ini_trend_data, next_warm_start_value


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
