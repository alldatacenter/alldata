import pandas as pd
import json
import time

from bentoml import env, artifacts, api, BentoService
from bentoml.adapters import DataframeInput, JsonInput, StringInput
from bentoml.frameworks.sklearn import SklearnModelArtifact


@env(infer_pip_packages=True)
@artifacts([SklearnModelArtifact('model')])
class AnomalyDetection(BentoService):
    """
    A minimum prediction service exposing a Scikit-learn model
    """

    @api(input=JsonInput())
    def analyse(self, param: json):
        """
        An inference API named `analyse` with Dataframe input adapter, which codifies
        how HTTP requests or CSV files are converted to a pandas Dataframe object as the
        inference API function iwnput
        """
        dic = {}
        if param['taskType']=='async':
            time.sleep(30)

        try:
            if len(param['seriesList'])<2:
                raise Exception()
            else:
                series = []
                series.append([1635216096000, 23.541])
                dic['predictSeriesList'] = series

        except Exception as ex:
            dic['code'] = 'detectorError'
            dic['message'] = 'some error in detector internal!'
        return dic


    @api(input=DataframeInput(), batch=True)
    def predict(self, df: pd.DataFrame):
        """
        An inference API named `predict` with Dataframe input adapter, which codifies
        how HTTP requests or CSV files are converted to a pandas Dataframe object as the
        inference API function input
        """
        return self.artifacts.model.predict(df)

    @api(input=JsonInput())
    def analyze(self, param: json):
        """
        An inference API named `predict` with Dataframe input adapter, which codifies
        how HTTP requests or CSV files are converted to a pandas Dataframe object as the
        inference API function input
        """
        return "good"



    @api(input=StringInput())
    def doc(self, message: str):
        """
        get README.md
        """
        f = open("README.md")
        doc = f.read()
        f.close()
        return doc


