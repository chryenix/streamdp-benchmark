#!/usr/bin/env python3

import timeeval
assert timeeval.__version__ == "1.2.4", "TimeEval version not supported. This script is for TimeEval version 1.2.4!"


from pathlib import Path

from timeeval import TimeEval, MultiDatasetManager, Metric, Algorithm, TrainingType, InputDimensionality, ResourceConstraints
from timeeval.adapters import DockerAdapter
from timeeval.params import FixedParameters
from timeeval.resource_constraints import GB

import numpy as np
from timeeval.utils.window import ReverseWindowing

def post_sLOF(scores: np.ndarray, args: dict) -> np.ndarray:
    window_size = args.get("hyper_params", {}).get("window_size", 100)
    return ReverseWindowing(window_size=window_size).fit_transform(scores)

def main():
    # load datasets and select them
    dm = MultiDatasetManager([Path("/root/th/dpbench/data")])  # or the path to your datasets (requires a datasets.csv-file in the folder)
    datasets = dm.select()  # selects ALL available datasets
    print("#Datasets: " + str(len(datasets)))
    # datasets = dm.select(min_anomalies=2)  # select all datasets with at least 2 anomalies

    # add and configure your algorithms
    algorithms = [Algorithm(
        name="knn",
        # set skip_pull=True because the image is already present locally:
        main=DockerAdapter(image_name="ghcr.io/timeeval/knn", tag="latest", skip_pull=True),
        # the hyper parameters of your algorithm:
        param_config=FixedParameters({
            "n_neighbors": 10
        #    "random_state": 42
        }),
        # required by DockerAdapter
        data_as_file=True,
        # UNSUPERVISED --> no training, SEMI_SUPERVISED --> training on normal data, SUPERVISED --> training on anomalies
        # if SEMI_SUPERVISED or SUPERVISED, the datasets must have a corresponding training time series
        training_type=TrainingType.UNSUPERVISED,
        input_dimensionality=InputDimensionality.UNIVARIATE
    ),
    Algorithm(
        name="lof",
        # set skip_pull=True because the image is already present locally:
        main=DockerAdapter(image_name="ghcr.io/timeeval/lof", tag="latest", skip_pull=True),
        # the hyper parameters of your algorithm:
        #param_config=FixedParameters({
        #    "window_size": 20,
        #    "random_state": 42
        #}),
        # required by DockerAdapter
        data_as_file=True,
        # UNSUPERVISED --> no training, SEMI_SUPERVISED --> training on normal data, SUPERVISED --> training on anomalies
        # if SEMI_SUPERVISED or SUPERVISED, the datasets must have a corresponding training time series
        training_type=TrainingType.UNSUPERVISED,
        input_dimensionality=InputDimensionality.UNIVARIATE
    ),Algorithm(
        name="dwt_mlead",
        # set skip_pull=True because the image is already present locally:
        main=DockerAdapter(image_name="ghcr.io/timeeval/dwt_mlead", tag="latest", skip_pull=True),
        # the hyper parameters of your algorithm:
        #param_config=FixedParameters({
        #    "window_size": 20,
        #    "random_state": 42
        #}),
        # required by DockerAdapter
        data_as_file=True,
        # UNSUPERVISED --> no training, SEMI_SUPERVISED --> training on normal data, SUPERVISED --> training on anomalies
        # if SEMI_SUPERVISED or SUPERVISED, the datasets must have a corresponding training time series
        training_type=TrainingType.UNSUPERVISED,
        input_dimensionality=InputDimensionality.UNIVARIATE
    )]

    # set the number of repetitions of each algorithm-dataset combination:
    repetitions = 1
    # set resource constraints
    rcs = ResourceConstraints.default_constraints()
    # if you want to limit the CPU or memory per algorithm, you can use:
    # rcs = ResourceConstraints(
    #     task_memory_limit = 2 * GB,
    #     task_cpu_limit = 1.0,
    # )
    timeeval = TimeEval(dm, datasets, algorithms,
        repetitions=repetitions,
        metrics=[Metric.ROC_AUC],
        resource_constraints=rcs
    )

    timeeval.run()
    # aggregated=True gives the mean and stddev of each algorithm-dataset combination
    results = timeeval.get_results(aggregated=True)
    print(results.to_string())

    # detailled results are automatically stored in your current working directory at ./results/<datestring>


if __name__ == "__main__":
    main()
