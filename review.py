#!/usr/bin/env python
import requests
import pandas
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

def pandas_frame_from(result):
    d = pandas.io.json.json_normalize(result, [['result', 'data']])

    # flatten 'fields' entry
    fields = d.pop("fields").apply(pandas.Series)
    return pandas.concat([d, fields], axis=1)

def query():
    params = {'queryKey':'active', 'order':'newest', 'api.token':CONDUIT_TOKEN}
    result = requests.get(ENDPOINT, params).json()

    data_frame = pandas_frame_from(result)
    data_frame.pop('attachments')
    data_frame.pop('policy')
    data_frame.pop('type')
    data_frame.pop('jira.issues')

    # Convert dates and calculate age
    dates = data_frame[['dateCreated', 'dateModified']].applymap(lambda d:
        datetime.fromtimestamp(d))
    data_frame = data_frame.join(dates, rsuffix='.converted').assign(
        age = lambda x: datetime.now() - x['dateCreated.converted'])


    age_stats = data_frame[['age']].describe(percentiles=[.25, .5, .75, .9])
    print(age_stats)


if __name__ == "__main__":
    query()
