import math
from scipy.linalg import norm
# from scipy.sparse import csr_matrix
# from scipy.linalg import eig
import numpy as np
from scipy.fftpack import fft, ifft
# from numpy.fft import fft, ifft
import time
timer = time.process_time


class Clock:
    time_elapsed = 0
    cur_time = 0

    def start(self):
        self.cur_time = timer()

    def pause(self):
        time_delta = timer() - self.cur_time
        self.time_elapsed += time_delta
        return self.time_elapsed

    def stop(self):
        print('time elapsed is {:6.20f}'.format(self.time_elapsed))
        self.time_elapsed = 0

    def clear(self):
        self.time_elapsed = 0


class AOperator:
    N = 0
    data_T = None

    def dot(self, x):
        """retrurn Ax"""
        print('To Be Implemented')

    def transposeDot(self, u):
        """return A'u """
        print('To Be Implemented')

    def HInverse(self, h):
        """return inv(H) h """
        print('To Be Implemented')


class DetailA(AOperator):

    x1_aug = None
    u1_aug = None
    H = None
    vlambda = None
    vlambda_diff = None

    def __init__(self, N, data_T, vlambda, vlambda_diff):
        self.data_T = data_T
        self.N = N
        self.Ashape0 = 3 * N - data_T - 3
        self.Ashape1 = N - 1

        self.vlambda = vlambda
        self.vlambda_diff = vlambda_diff
        self.x1_aug = np.zeros((N, 1))
        self.u1_aug = np.zeros((N + data_T - 1, 1))
        self.u3_aug = np.zeros((N, 1))
        self.d_H_inv = np.zeros((N - 1, 1))

        N = self.N
        T = self.data_T
        # ###### old version:
        # diff_row = np.zeros((1, N - 1))
        # diff_row[0, 0:2] = [1, -1]
        # D_aug = circulant(diff_row).T * vlambda_diff
        # ones = np.zeros((1, N - 1))
        # ones[0, 0:T] = np.ones((1, T))
        # M_aug = circulant(ones).T
        # Imat = vlambda * np.eye(N - 1)
        # H = M_aug.T.dot(M_aug) + Imat.T.dot(Imat) + D_aug.T.dot(D_aug)
        # self.H = H
        # d_H = fft(H[0, :])
        # ##### updated version:
        # print("New version of d_H_inv d_H_inv d_H_inv d_H_inv.......")
        H_first_row = np.zeros((1, N - 1))
        # add M'M to H_first row
        H_first_row[0, 0:data_T] = range(T, 0, -1)
        H_first_row[0, -data_T + 1:] = range(1, T)
        # add D'D to H_first row
        diff_lam_square = vlambda_diff ** 2
        H_first_row[0, 0] += 2 * diff_lam_square
        H_first_row[0, 1] -= 1 * diff_lam_square
        H_first_row[0, -1] -= 1 * diff_lam_square
        # add I' I
        H_first_row[0, 0] += vlambda ** 2
        d_H = fft(H_first_row)

        self.d_H_inv = np.divide(np.array([1 for i in range(N - 1)]),
                                 d_H).reshape(N - 1, 1).T

    def dot(self, x):
        """break [Ax] to [Mx, Ix, Dx]"""
        self.x1_aug[1:] = x
        cs_x = self.x1_aug.cumsum()
        u1 = cs_x[self.data_T:] - cs_x[:-self.data_T]

        u2 = x * self.vlambda
        u3 = (x[:-1] - x[1:]) * self.vlambda_diff
        return np.vstack((u1.reshape(-1, 1), u2, u3))

    def transposeDot(self, u):
        """break [A^T u] to M'u1 + I u2 + D'd u3"""
        T, N = self.data_T, self.N
        self.u1_aug[T:N] = u[:N - T].reshape(-1, 1)
        x2 = u[N - T:2 * N - T - 1] * self.vlambda

        self.u3_aug[1:N - 1] = u[2 * N - T - 1:].reshape(-1, 1)
        x3 = (self.u3_aug[1:] - self.u3_aug[:-1]) * self.vlambda_diff
        u1_cs = self.u1_aug.cumsum()
        x1 = u1_cs[T:] - u1_cs[:-T]

        return (x1.reshape(-1, 1) + x2.reshape(-1, 1) + x3.reshape(-1, 1))

    def HInverse(self, b):
        # b-->v
        b_fft = fft(b.T)
        return ifft(np.multiply(self.d_H_inv, b_fft)).real.T

    def diffHDot(self, b):
        """computes H - (A.T.dot(A)) using custom matrix multiplication"""
        x = b
        N = self.N
        T = self.data_T
        x_aug = np.hstack((x[-T:, 0], x[:T - 1, 0]))
        cs = np.cumsum(x_aug)
        mx = cs[T:] - cs[:-T]

        x_out = np.zeros((N - 1, 1))
        x_out[-(T - 1):, 0] += np.cumsum(mx)
        mx_reverse = np.flip(mx)
        x_out[0:(T - 1), 0] += np.flip(np.cumsum(mx_reverse))
        # --> for M^T*M
        md = x[-1] - x[0]
        x_out[0] -= md * self.vlambda_diff**2
        x_out[-1] += md * self.vlambda_diff**2
        return x_out


class L1Solver:
    # A = None
    b = None
    A_norm = None
    sparse_A = None
    sparse_A_trans = None

    ratio_choices = [3**i for i in range(-2, 3, 1)]
    A_norm_choices = [2**i for i in range(-1, 2, 1)]
    total_choices = None
    best_choice = 7

    A_inv = None
    max_tuning_iter = 200
    max_iter = 500
    A_operator = None
    debug = False

    def __init__(self, A_operator=None, A_norm=None, debug=False):
        # TODO: the constructon of A shoul
        '''
        Implements Chambollee Pock\' primal dual algorithm
        to solve ||Ax -b ||_1, refer to attached notes
        '''
        if self.debug:
            print('initializing.....L1 PD Solver')
        # self.A = A
        # self.len_b = A.shape[0]

        self.clock = Clock()
        if A_norm is None:
            pass
            # TODO with following value without explicit A
            # self.A_norm = sum(A[0, :])  # we use this for time being
        else:
            self.A_norm = A_norm

        self.debug = debug
        self.total_choices = len(self.ratio_choices) * len(self.A_norm_choices)
        if self.debug:
            print('norm of A is {:}'.format(self.A_norm))
            print('Finished initializing PD Solver')
        if A_operator is None:
            raise ValueError("must input A_operator")
            # self.A_operator = SparseA(A)  # not use anymore
            pass
        else:
            self.A_operator = A_operator
            self.len_b = A_operator.Ashape0
            self.Ashape0 = A_operator.Ashape0
            self.Ashape1 = A_operator.Ashape0

    def paramTuning(self, b_vec=None, maxIter=None):
        # best_idx = 0
        best_loss = math.inf
        for i in range(self.total_choices):
            (x_soln, obj, obj_history,
             time_history) = self.solve(b_vec=b_vec,
                                        max_iter=self.max_tuning_iter,
                                        param_choice=i)
            if self.debug:
                print('*', end="")
                print(
                    'param_tuning {:} using X_to_dual_ratio {:.2E} \
                    norm A ratio {:.2E} with l1 loss {:.2E}'
                    .format(
                        i, self.ratio_choices[i % len(self.ratio_choices)],
                        self.A_norm_choices[int(i / len(self.ratio_choices))],
                        obj),
                    end="")

            if obj < best_loss:
                self.best_choice = i

                best_loss = obj
        if self.debug:
            print('finished param tuning ****************************')
            print()
            print()

    def getStepSize(self, param_choice):
        # return tau and eta
        x_to_box_ratio = self.ratio_choices[param_choice %
                                            len(self.ratio_choices)]
        # hard coding to validate theoretical performance
        Dx = 1
        # updated len(self.b) by using self.len_b
        Db = norm(np.ones(self.len_b))
        # Db = norm(np.ones(len(self.b)))
        # hard coding to validate theoretical performance
        A_norm = self.A_norm_choices[int(
            param_choice / len(self.ratio_choices))] * self.A_norm
        tau = A_norm * Dx / Db * x_to_box_ratio
        eta = A_norm * Db / Dx / x_to_box_ratio
        return (tau, eta)

    def solve(self,
              b_vec=None,
              param_choice=None,
              max_iter=None,
              warm_start_u=None,
              warm_start_x=None,
              return_history=False):
        # return x_soln, obj, obj_history, time_history
        # [m, n] = self.A.shape
        [m, n] = self.Ashape0, self.Ashape1
        if param_choice is None:
            param_choice = self.best_choice
            print(self.best_choice)
        if max_iter is None:
            max_iter = self.max_iter
        if warm_start_u is None:
            cur_u = np.zeros((m, 1))

        if warm_start_x is None:
            cur_x = np.zeros((n, 1))

        (tau, eta) = self.getStepSize(param_choice)
        if self.debug:
            print('tau is {:} and eta is {:}'.format(tau, eta))
        obj_history = []
        time_history = []
        best_loss = math.inf
        best_soln = None
        old_x = cur_x
        acc_x = np.zeros((n, 1))
        for iter in range(max_iter):  # to notes: equ. (3)
            if self.debug:
                self.clock.start()
            x_til = 2 * cur_x - old_x

            cur_u = cur_u + 1 / tau * (self.A_operator.dot(x_til) - b_vec)
            cur_u = np.clip(cur_u, -1, 1)
            old_x = cur_x
            cur_x = cur_x - 1 / eta * (self.A_operator.transposeDot(cur_u))
            acc_x = cur_x

            if self.debug:
                time_history.append(self.clock.pause())
            cur_loss = np.sum(abs(self.sparse_A.dot(acc_x) - b_vec))
            if cur_loss < best_loss:
                best_soln = cur_x
                best_loss = cur_loss

            best_loss = np.sum(abs(self.sparse_A.dot(acc_x) - b_vec))
            obj_history.append(cur_loss)
        if self.debug:
            self.clock.stop()
        return (best_soln, best_loss[0], obj_history, time_history)


class PDHG(L1Solver):

    pho = 1.9

    def solve(self,
              b_vec=None,
              param_choice=None,
              max_iter=None,
              warm_start_u=None,
              warm_start_x=None,
              return_history=False):
        # return x_soln, obj, obj_history, time_history
        # [m, n] = self.A.shape
        [m, n] = self.Ashape0, self.Ashape1
        if param_choice is None:
            param_choice = self.best_choice
            if self.debug:
                print(self.best_choice)
        if max_iter is None:
            max_iter = self.max_iter
        if warm_start_u is None:
            cur_u = np.zeros((m, 1))

        if warm_start_x is None:
            cur_x = np.zeros((n, 1))

        (tau, eta) = self.getStepSize(param_choice)
        if self.debug:
            print('tau is {:} and eta is {:}'.format(tau, eta), end="*")
        obj_history = []
        time_history = []
        best_loss = math.inf
        best_soln = None
        # old_x = cur_x
        # old_u = cur_u
        acc_x = np.zeros((n, 1))
        for iter in range(max_iter):
            # to paper (ergodic convergence rates of a first...) equ. (22)
            if self.debug:
                self.clock.start()
            x_temp = cur_x - 1 / eta * (self.A_operator.transposeDot(cur_u))
            x_til = 2 * x_temp - cur_x
            u_temp = cur_u + 1 / tau * (self.A_operator.dot(x_til) - b_vec)
            u_temp = np.clip(u_temp, -1, 1)

            # old_x = cur_x
            # old_u = cur_u
            cur_x = self.pho * x_temp + (1 - self.pho) * cur_x
            cur_u = self.pho * u_temp + (1 - self.pho) * cur_u

            acc_x = acc_x + 1 / (iter + 1) * (cur_x - acc_x)

            if self.debug:
                time_history.append(self.clock.pause())
            cur_loss = np.sum(abs(self.A_operator.dot(acc_x) - b_vec))
            best_loss = cur_loss
            best_soln = acc_x
            obj_history.append(cur_loss)
        if self.debug:
            self.clock.stop()
        return (best_soln, best_loss, obj_history, time_history)


class gADMMSolver(L1Solver):
    primal_radius_ratio = [3**i for i in range(-5, 5, 1)]

    def paramTuning(self, b_vec=None, maxIter=None):
        best_loss = math.inf
        for i in range(len(self.primal_radius_ratio)):
            (x_soln, obj, obj_history,
             time_history) = self.solve(b_vec=b_vec,
                                        max_iter=maxIter,
                                        param_choice=i)
            if self.debug:
                print(
                    'param_tuning {:} using primal radius estimate \
                     ratio {:.3E} with l1 loss {:.2E}'
                    .format(i, self.primal_radius_ratio[i], obj),
                    end="")
            if obj < best_loss:
                self.best_choice = i
                best_loss = obj
        if self.debug:
            print('finished param tuning ****************************')
            print()
            print()

    def getStepSize(self, param_choice):
        # return rho
        # N, T = self.A_operator.N, self.A_operator.data_T
        N = self.A_operator.N
        dual_radius_est = 3 * N
        primal_radius_est = N * self.primal_radius_ratio[
            param_choice]  # T--> N  maybe better
        # hard coding to validate theoretical performance
        rho = math.sqrt(dual_radius_est / primal_radius_est)
        return rho

    def _soft_thresholding(self, x, alpha):
        return np.maximum(0., x-alpha) - np.maximum(0., -x-alpha)

    def solve(self,
              b_vec=None,
              param_choice=None,
              max_iter=None,
              warm_start_lam=None,
              warm_start_x=None,
              warm_start_y=None,
              return_history=False):
        # return x_soln, obj, obj_history, time_history
        # [m, n] = self.A.shape
        [m, n] = self.Ashape0, self.Ashape1
        if param_choice is None:
            param_choice = self.best_choice
            if self.debug:
                print(self.best_choice)
        if max_iter is None:
            max_iter = self.max_iter
        if warm_start_lam is None:
            cur_lam = np.zeros((m, 1))

        if warm_start_x is None:
            cur_x = np.zeros((n, 1))
        if warm_start_y is None:
            # tune could also pick 0 as starting point
            cur_y = np.zeros((m, 1))

        rho = self.getStepSize(param_choice)
        if self.debug:
            print('rho is {:}'.format(rho), end="")
        obj_history = []
        time_history = []
        best_loss = math.inf
        best_soln = None
        # old_x = cur_x
        acc_x = np.zeros((n, 1))
        if self.debug:
            self.clock.clear()
        for iter in range(max_iter):
            # print('iter {:}'.format(iter))
            if self.debug:
                self.clock.start()
            w = self.A_operator.diffHDot(cur_x) + self.A_operator.transposeDot(
                cur_y + b_vec - 1 / rho * cur_lam)
            cur_x = self.A_operator.HInverse(w)
            Ax = self.A_operator.dot(cur_x)
            v = Ax + cur_lam / rho - b_vec
            cur_y = self._soft_thresholding(v, 1.0 / rho)
            cur_lam = cur_lam + rho * (Ax - cur_y - b_vec)
            # acc_x = acc_x + 1/(iter+1) * (cur_x - acc_x)
            acc_x = cur_x

            if self.debug:
                time_history.append(self.clock.pause())
            cur_loss = np.sum(abs(self.A_operator.dot(acc_x) - b_vec))
            if cur_loss < best_loss:

                best_soln = cur_x
                best_loss = cur_loss
            # best_soln = acc_x
            best_loss = np.sum(abs(self.A_operator.dot(acc_x) - b_vec))
            obj_history.append(cur_loss)
        if self.debug:
            print(
                'x residual distance is {:} while the y distance \
                 is {:}, reference A distance is {:}'
                .format(acc_x.T.dot(self.A_operator.diffHDot(acc_x)),
                        cur_y.T.dot(cur_y),
                        norm(self.A_operator.dot(acc_x))**2))
        if self.debug:
            self.clock.stop()
        return (best_soln, best_loss, obj_history, time_history)
