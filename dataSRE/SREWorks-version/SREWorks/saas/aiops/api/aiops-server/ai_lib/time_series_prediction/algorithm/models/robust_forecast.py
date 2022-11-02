import numpy as np
from statsmodels.tsa.api import Holt

from autoscaling.algorithm.preprocess.tfPeriodCode.generalPeriodicity import GeneralPeriodicityDetector
from autoscaling.algorithm.preprocess.simpleRobustSTL.generalSTL import GeneralDecompEstimator


def robust_forecast(ori_ts_data,
                    forecast_horizon,
                    pd_detector,
                    pd_params,
                    stl_detector,
                    stl_params,
                    forecast_truth=None,
                    debug=False):

    period_det = GeneralPeriodicityDetector(pd_params, pd_detector)
    passed_check_acf, period_output = period_det.fit_transform(ori_ts_data)
    if passed_check_acf:
        stl_params["data_T"] = period_output[0]
        stl_params["latest_decomp_length"] = forecast_horizon

        decomp = GeneralDecompEstimator(stl_params, stl_detector)
        decomp_result, latest_decomp_result = decomp.fit_transform(ori_ts_data)
        future_predict = np.array(latest_decomp_result["season_plus_trend"])
    else:

        non_period_model = Holt(list(ori_ts_data),
                                damped=True).fit(smoothing_level=0.5,
                                                 smoothing_slope=0.3)
        future_predict = non_period_model.forecast(forecast_horizon)

    if debug:
        plt.figure(figsize=(10, 3))
        if forecast_truth is not None:
            ori_signal = np.concatenate((ori_ts_data, forecast_truth), axis=0)
        else:
            ori_signal = ori_ts_data

        pred_signal = np.concatenate(
            (np.zeros_like(ori_ts_data), future_predict), axis=0)

        plt.plot(ori_signal, 'k-',
                 label='original signal')  # other: plt.semilogy(...)
        plt.plot(pred_signal, 'r-',
                 label='next forecasting')  # other: plt.semilogy(...)
        plt.axvline(x=len(ori_ts_data), color='y', linestyle='--')
        
        print("period_output")
        print(period_output)
        plt.title("training data length is %d; periodicity is %s" %
                  (len(ori_ts_data), period_output))
        plt.legend(loc='upper left')
        plt.show()
    return future_predict
