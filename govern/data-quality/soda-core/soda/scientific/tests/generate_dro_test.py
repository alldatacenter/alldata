import numpy as np
import pytest
from numpy.random import default_rng
from numpy.testing import assert_almost_equal

from soda.scientific.distribution.utils import RefDataCfg

rng = default_rng(1234)


@pytest.mark.parametrize(
    "cfg, data, expected_weights, expected_bins",
    [
        pytest.param(
            RefDataCfg(distribution_type="continuous"),
            list(rng.normal(loc=2, scale=1.0, size=1000)),
            np.array(
                [0, 2, 4, 12, 15, 40, 49, 57, 58, 87, 112, 112, 98, 88, 74, 67, 40, 29, 27, 13, 6, 1, 7, 0, 0, 1, 1]
            ),
            np.array(
                [
                    -0.76828668,
                    -0.50476956,
                    -0.24125244,
                    0.02226468,
                    0.2857818,
                    0.54929893,
                    0.81281605,
                    1.07633317,
                    1.33985029,
                    1.60336741,
                    1.86688453,
                    2.13040166,
                    2.39391878,
                    2.6574359,
                    2.92095302,
                    3.18447014,
                    3.44798726,
                    3.71150439,
                    3.97502151,
                    4.23853863,
                    4.50205575,
                    4.76557287,
                    5.02909,
                    5.29260712,
                    5.55612424,
                    5.81964136,
                    6.08315848,
                ]
            ),
            id="continuous data",
        ),
    ],
)
def test_generate_dro_continuous(cfg, data, expected_bins, expected_weights):
    from soda.scientific.distribution.generate_dro import DROGenerator

    dro_generator = DROGenerator(cfg, data)
    dro = dro_generator.generate()

    assert_almost_equal(dro.weights, expected_weights / np.sum(expected_weights))
    assert_almost_equal(dro.bins, expected_bins)


def test_generate_dro_continuous_with_outlier():
    from soda.scientific.distribution.generate_dro import DROGenerator

    cfg = RefDataCfg(distribution_type="continuous")
    np.random.seed(61)
    data = np.random.rand(7000)
    data[1000] = 1000000000000000
    data[0] = -1000000000000000
    dro_generator = DROGenerator(cfg, data)
    dro = dro_generator.generate()

    assert_almost_equal(
        dro.weights,
        [
            0.0,
            0.0543012289225493,
            0.04844241211774793,
            0.05101457559302658,
            0.04901400400114318,
            0.0481566161760503,
            0.05487282080594456,
            0.05130037153472421,
            0.053015147184909975,
            0.05272935124321235,
            0.05215775935981709,
            0.05144326950557302,
            0.05001428979708488,
            0.047299228350957415,
            0.05001428979708488,
            0.04787082023435267,
            0.051586167476421835,
            0.045870248642469275,
            0.04844241211774793,
            0.045155758788225205,
            0.047299228350957415,
        ],
    )
    assert_almost_equal(
        dro.bins,
        [
            3.2384564753185074e-05,
            0.050028300625252625,
            0.10002421668575207,
            0.1500201327462515,
            0.20001604880675095,
            0.2500119648672504,
            0.3000078809277498,
            0.35000379698824924,
            0.3999997130487487,
            0.4499956291092482,
            0.4999915451697476,
            0.549987461230247,
            0.5999833772907465,
            0.6499792933512459,
            0.6999752094117453,
            0.7499711254722448,
            0.7999670415327442,
            0.8499629575932437,
            0.8999588736537432,
            0.9499547897142425,
            0.999950705774742,
        ],
    )


def test_generate_dro_continuous_with_sqrt_bins():
    from soda.scientific.distribution.generate_dro import DROGenerator

    cfg = RefDataCfg(distribution_type="continuous")
    np.random.seed(61)
    data = np.random.rand(20)
    data[0] = 1000000000000000
    data[3] = 1000000000000000
    data[6] = 1000000000000000
    data[1] = 1000000000
    data[2] = 10000
    data2 = np.random.rand(3) * 1000000000000000
    data = np.concatenate((data, data2))
    dro_generator = DROGenerator(cfg, data)
    dro = dro_generator.generate()
    assert_almost_equal(dro.weights, [0.0, 0.9444444444444444, 0.0, 0.0, 0.0, 0.05555555555555555])
    assert_almost_equal(
        dro.bins,
        [
            0.0035455468097308485,
            7974634803449.869,
            15949269606899.734,
            23923904410349.598,
            31898539213799.465,
            39873174017249.33,
        ],
    )


def test_generate_dro_continuous_exceeding_max_allowed_bin_size():
    from soda.scientific.distribution.generate_dro import DROGenerator

    cfg = RefDataCfg(distribution_type="continuous")
    np.random.seed(61)
    data = np.random.rand(20)
    data[0] = 1000000000000000
    data[3] = 1000000000000000
    data[6] = 1000000000000000
    data[1] = 1000000000
    data[2] = 10000
    data2 = np.random.rand(3) * 1000000000000000
    data = np.concatenate((data, data2))
    dro_generator = DROGenerator(cfg, data)
    dro_generator.maximum_allowed_bin_size = 3
    dro = dro_generator.generate()
    assert_almost_equal(dro.weights, [0.0, 0.9444444444444444, 0.0, 0.05555555555555555])
    assert_almost_equal(dro.bins, [0.0035455468097308485, 13291058005749.78, 26582116011499.555, 39873174017249.33])


def test_generate_dro_continuous_all_same_values():
    from soda.scientific.distribution.generate_dro import DROGenerator

    cfg = RefDataCfg(distribution_type="continuous")
    data = np.ones(20)
    dro_generator = DROGenerator(cfg, data)
    dro = dro_generator.generate()
    assert_almost_equal(dro.weights, [0.0, 1.0])
    assert_almost_equal(dro.bins, [0.5, 1.5])


@pytest.mark.parametrize(
    "cfg, data, expected_weights, expected_bins",
    [
        pytest.param(
            RefDataCfg(distribution_type="categorical"),
            list(rng.choice(["hello", "world", "foo"], p=[0.1, 0.4, 0.5], size=1000)),  # type: ignore
            np.array([0.525, 0.392, 0.083]),
            np.array(["foo", "world", "hello"]),
            id="categorical data",
        ),
    ],
)
def test_generate_dro_categorical(cfg, data, expected_weights, expected_bins):
    from soda.scientific.distribution.generate_dro import DROGenerator

    dro_generator = DROGenerator(cfg, data)
    dro = dro_generator.generate()

    assert_almost_equal(dro.weights, expected_weights)
    assert dro.bins == expected_bins.tolist()
