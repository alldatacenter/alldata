import time
import numpy as np
import scipy.sparse as sparse
# from scipy.sparse.linalg import spsolve
# from scipy.linalg import solve_triangular
from numpy.linalg import norm, cholesky


def huverL2_admm_solver(A,
                        b,
                        F,
                        p,
                        t,
                        reg_lambda,
                        x0=None,
                        rho=1.0,
                        MAX_ITER=5000,
                        rel_par=1.,
                        QUIET=True,
                        tol_abs=1e-5,
                        tol_rel=1e-2):
    """
    This is an ADMM solver for the huber loss + L2 norm problem
    min_x g_t(Ax-b) + lambda ||Fx - p||_2^2,
    where g_t is the Huber loss with threshold t.
    In using this solver, the user must explicitly give the definitions of
    A, b, F, p, t, and reg_lambda (the lambda in original formulation)
    === Input ===
    A: m*n matrix
    b: m vector
    F: l*n matrix
    p: l vector
    t: a positive scalar, which is the threshold used in Huber loss
    reg_lambda: a non-negative scalar
    Some other important parameters:
    rho: the penalized parameter in ADMM
    max_iter: the maximum number of iterations
    tol_abs: the absolute tolerance parameter
    tol_rel: the relative tolerance parameter
    Optional parameters:
    x0: the initial value of x for warm start
    Quite: True/False, to enable the display or intermediate results or not
    === Output ===
    We return both the optimal solution and the whole history in the solving
    process (we call history)
    1. The solution is returned in the vector x
    2. The history is returned in dictionary h containing the objective value,
    the primal dual residual norms, and the tolerances for primal and dual
    residual norms at each iteration.
    """
    # Parameter checking
    if reg_lambda < 0:
        reg_lambda = 0
    if t <= 0:
        t = 1.0
    if rel_par < 1:
        rel_par = 1.0
    if rel_par > 2:
        rel_par = 2.0
    if rho < 0:
        rho = 1.0
    if MAX_ITER < 0:
        MAX_ITER = 100
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
    # ell = F.shape[0]

    b = b.flatten()
    p = p.flatten()
    if x0 is not None:
        x0 = x0.flatten()

    # Initialize
    x = np.zeros(n)
    z = np.zeros(m)
    u = np.zeros(m)

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
    Ftp = F.T.dot(p)
    norm_b = norm(b)
    # Cache the Cholesky decomposition
    # L, U = Cholesky_decomp_sp(A, F, rho)
    S = rho * A.T.dot(A) + 2 * F.T.dot(F) * reg_lambda
    S_inv = np.linalg.inv(S)

    for k in range(MAX_ITER):
        # x-update
        q = 2 * Ftp * reg_lambda + rho * A.T.dot(z + b - u)
        if k == 0 and x0 is not None:
            x = x0
        else:
            # x = spsolve(U, spsolve(L, q))
            x = S_inv.dot(q)
        Ax = A.dot(x)

        # z-update
        zold = np.copy(z)
        w = Ax + u - b
        z = huber_thresholding(w, t, rho)

        # u-update
        u = u + Ax - b - z

        # Diagnostics, reporting, termination checks
        h['obj_val'][k] = f_objective_huber_L2(F, p, reg_lambda, t, x, z)
        h['r_norm'][k] = norm(Ax - z - b)
        h['s_norm'][k] = rho * norm(A.T.dot(z - zold))
        h['eps_pri'][k] = np.sqrt(m) * tol_abs + tol_rel * np.array(
            [norm(Ax), norm(z), norm_b]).max()
        h['eps_dual'][
            k] = np.sqrt(n) * tol_abs + tol_rel * norm(rho * A.T.dot(u))

        if not QUIET:
            print('%4d\t%10.4f\t%10.4f\t%10.4f\t%10.4f\t%10.2f' %
                  (k + 1, h['r_norm'][k], h['eps_pri'][k], h['s_norm'][k],
                   h['eps_dual'][k], h['obj_val'][k]))

        if (h['r_norm'][k] < h['eps_pri'][k]) and \
                (h['s_norm'][k] < h['eps_dual'][k]):
            break

    if not QUIET:
        toc = time.time() - tic
        print("\nElapsed time is %.2f seconds" % toc)

    return x.flatten(), h


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
    """
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
    2. The history is returned in dictionary h containing objective value,
    the primal and dual residual norms, and tolerances for primal and dual
    residual norms at each iteration.
    """
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
        h['eps_dual'][
            k] = np.sqrt(n) * tol_abs + tol_rel * norm(rho * F.T.dot(u))

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


def f_huber(x, t):
    x = np.array(x).flatten()
    abs_b = (np.abs(x) <= t)
    x0 = x[abs_b]
    s0 = np.square(x0).sum() / 2.0
    # x1 = x[abs_b == False]
    x1 = x[~abs_b]
    s1 = (np.abs(x1).sum() - t / 2.0 * x1.size) * t
    return s0 + s1


def f_objective_huber_L2(F, p, reg_lambda, t, x, z):
    return reg_lambda * np.square(F.dot(x) - p).sum() + f_huber(z, t)


def huber_thresholding(w, t, rho):
    return rho/(1.0 + rho) * w + \
        soft_thresholding(w, t*(1.0+rho)/rho)/(1.0 + rho)
