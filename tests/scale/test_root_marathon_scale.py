from utils import *
from common import *

import pytest

import csv
import time
import sys
import os
"""
    assumptions:
        1) written in progressively higher scale
        2) mom1 and mom2 are group such that we are not switching each test
        3) the tests are run in the order they are written
"""


type_test_failed = {}

test_log = []
##############
# Test Section
##############


@pytest.mark.parametrize("scale_type", [
    'test_root_apps_instances_1_1',
    'test_root_apps_instances_1_10',
    'test_root_apps_instances_1_100',
    'test_root_apps_instances_1_500',
    'test_root_apps_instances_1_1000',
    'test_root_apps_instances_1_5000',
    'test_root_apps_instances_1_10000',
    'test_root_apps_instances_1_25000'
])
def test_instance_scale(scale_type):
    run_test(scale_type)


@pytest.mark.parametrize("scale_type", [
    'test_root_apps_count_1_1',
    'test_root_apps_count_10_1'
    'test_root_apps_count_100_1',
    'test_root_apps_count_500_1',
    'test_root_apps_count_1000_1',
    'test_root_apps_count_5000_1',
    'test_root_apps_count_10000_1',
    'test_root_apps_count_25000_1'
])
def test_count_scale(scale_type):
    run_test(scale_type)


@pytest.mark.parametrize("scale_type", [
    'test_root_apps_group_1_1',
    'test_root_apps_group_10_1'
    'test_root_apps_group_100_1',
    'test_root_apps_group_1000_1'
])
def test_group_scale(scale_type):
    run_test(scale_type)


##############
# End Test Section
##############


def run_test(name):
    current_test = start_test(name)
    test_log.append(current_test)
    need = scaletest_resources(current_test)
    # TODO: why marathon stops at 80%
    if need > (available_resources() * 0.8):
        current_test.skip('insufficient resources')
        return
    if previous_style_test_failed(current_test):
        current_test.skip('smaller scale failed')
        return

    # wait for max
    # respond to timeouts
    time = scale_test_apps(current_test)

    if "failed" in current_test.status:
        type_test_failed[get_style_key(current_test)] = True


def get_style_key(current_test):
    return '{}_{}'.format(current_test.mom, current_test.style)


def previous_style_test_failed(test_obj):
    failed = False
    try:
        failed = type_test_failed.get(get_style_key(current_test))
    except:
        failed = False
        pass

    return failed


def setup_module(module):
    delete_all_apps_wait()
    cluster_info()
    print('testing root marathon')
    print(available_resources())


def teardown_module(module):
    stats = collect_stats()
    write_csv(stats)
    read_csv()
    write_meta_data(get_metadata())
    delete_all_apps_wait()


def get_metadata():
    metadata = {
        'marathon': 'root'
    }
    return metadata


def collect_stats():
    stats = {
        'root_instances': [],
        'root_instances_target': [],
        'root_instances_max': [],
        'root_count': [],
        'root_count_target': [],
        'root_count_max': [],
        'root_group': [],
        'root_group_target': []
    }

    for scale_test in test_log:
        print(scale_test)
        scale_test.log_events()
        scale_test.log_stats()
        print('')
        stats.get(get_style_key(scale_test)).append(scale_test.deploy_time)
        target_key = '{}_target'.format(get_style_key(scale_test))
        if 'instances' in target_key:
            stats.get(target_key).append(scale_test.instance)
        else:
            stats.get(target_key).append(scale_test.count)

    return stats


def read_csv(filename='scale-test.csv'):
    with open(filename, 'r') as fin:
        print(fin.read())


def write_csv(stats, filename='scale-test.csv'):
    with open(filename, 'w') as f:
        w = csv.writer(f, quoting=csv.QUOTE_NONNUMERIC)
        write_stat_lines(f, w, stats, 'root', 'instances')
        write_stat_lines(f, w, stats, 'root', 'count')
        write_stat_lines(f, w, stats, 'root', 'group')


def write_stat_lines(f, w, stats, marathon, test_type):
        f.write('Marathon: {}, {}'.format('root', test_type))
        f.write('\n')
        w.writerow(stats['{}_{}_target'.format(marathon, test_type)])
        w.writerow(stats['{}_{}'.format(marathon, test_type)])
        f.write('\n')


def get_current_test():
    return test_log[-1]
