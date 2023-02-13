from anomaly_detection import AnomalyDetection

if __name__ == '__main__':
    bentoml = AnomalyDetection()

    bentoml.pack('model', None)

    bentoml.save_to_dir("/Users/congrong/BentoProjects/SREWorksAnomalyDetection/package")