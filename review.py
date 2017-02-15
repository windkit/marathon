#!/usr/bin/env python
import requests
from datetime import datetime
from tabulate import tabulate

ENDPOINT="https://phabricator.mesosphere.com/api/differential.revision.search"
CONDUIT_TOKEN="cli-tthzpx6b7b3s6dtca6n7yryxenk2"

def percentile(p, revisions):
    """
    Returns the p th percentile of revisions. It assumes revisions is sorted.
    """
    p_index = int(len(revisions) / 100 * p)
    return revisions[p_index]


def stats(revisions, percentiles = [20, 50, 90]):
    """
    Calculates percentiles of age of active revisions.

    :param revisions The revisions are assumed to be ordered
    :param perenctiles The perenctiles to calculate
    :return data of percentile to data: [p, title, dataCreated, age]
    """
    for p in percentiles:
        value = percentile(p, revisions)
        created = datetime.fromtimestamp(value['fields']['dateCreated'])
        delta = datetime.now() - created
        yield [p, "D{}".format(value['id']), value['fields']['title'], created, delta]


def show(stats):
    """
    Print stats in table.
    """
    headers = ["Percentile", "ID", "Title", "Created", "Age"]
    print(tabulate(stats, headers=headers))

def query():
    params = {'queryKey':'active', 'order':'newest', 'api.token':CONDUIT_TOKEN}
    result = requests.get(ENDPOINT, params).json()
    data = stats(result['result']['data'])
    show(data)


if __name__ == "__main__":
    query()
