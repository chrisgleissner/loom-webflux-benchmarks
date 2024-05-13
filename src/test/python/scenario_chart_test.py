import imghdr
import os
import pytest
import shutil
import sys
from unittest.mock import patch

sys.path.append("src/main/python")
sys.path.append("../../main/python")

import scenario_chart

PROJECT_ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../.."))
TEST_RELATIVE_DIR = "scenario_chart_test_py"
RESOURCES_DIR = PROJECT_ROOT_DIR + "/src/test/resources/" + TEST_RELATIVE_DIR + "/"
TEST_OUTPUT_DIR = PROJECT_ROOT_DIR + "/build/test-output/" + TEST_RELATIVE_DIR + "/"


@pytest.fixture(autouse=True)
def clear_test_output_dir():
    shutil.rmtree(TEST_OUTPUT_DIR, ignore_errors=True)
    os.makedirs(TEST_OUTPUT_DIR, exist_ok=True)


parameterization = [
    ("loom-netty-latency.csv",
     "loom-netty-system.csv",
     "loom-netty-jvm.csv",
     "loom-netty.png",
     "results.csv",
     ),
    ("missing-latencies/webflux-netty-latency.csv",
     "missing-latencies/webflux-netty-system.csv",
     "missing-latencies/webflux-netty-jvm.csv",
     "missing-latencies/webflux-netty.png",
     "missing-latencies/results.csv",
     ),
]


@pytest.mark.parametrize("latency_csv_filename, system_csv_filename, jvm_csv_filename, png_filename, results_csv_filename", parameterization)
def test_main(latency_csv_filename, system_csv_filename, jvm_csv_filename, png_filename, results_csv_filename):
    expected_png_file = TEST_OUTPUT_DIR + png_filename

    results_csv_file = TEST_OUTPUT_DIR + results_csv_filename
    expected_results_csv_file = RESOURCES_DIR + results_csv_filename

    argv = [
        "scenario_chart.py",
        "smoketest",
        "loom-netty",
        RESOURCES_DIR + latency_csv_filename,
        RESOURCES_DIR + system_csv_filename,
        RESOURCES_DIR + jvm_csv_filename,
        expected_png_file,
        results_csv_file
    ]
    with patch('sys.argv', argv):
        scenario_chart.main()

        assert_file_exists(expected_results_csv_file)
        assert_files_match(results_csv_file, expected_results_csv_file)

        assert_file_exists(expected_png_file)
        assert imghdr.what(expected_png_file) == "png", f"Output file '{expected_png_file}' is not a valid PNG file"


def assert_file_exists(file):
    assert os.path.exists(file), f"File '{file}' does not exist"


def normalize_newlines(content):
    return content.replace(b'\r\n', b'\n').replace(b'\r', b'\n')


def assert_files_match(actual_file, expected_file):
    assert os.path.exists(actual_file), f"File '{actual_file}' does not exist"
    assert os.path.exists(expected_file), f"File '{expected_file}' does not exist"
    with open(actual_file, 'rb') as f, open(expected_file, 'rb') as expected_f:
        actual_content = normalize_newlines(f.read())
        expected_content = normalize_newlines(expected_f.read())
        assert actual_content == expected_content, f"Contents of '{actual_file}' do not match '{expected_file}'"
