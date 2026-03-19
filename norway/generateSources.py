import csv
import logging
from pathlib import Path

import requests

from util import toCSV
from util import toZip
from util import norwayClientId

logger = logging.getLogger(__name__)


from util import norwayAPI

def process():
    r = requests.get(norwayAPI, {}, auth=(norwayClientId, ''))
    if r.status_code == 412:
        return []
    if r.status_code == 403:
        return []
    rows = []
    data = r.json()
    for row in data['data']:
        if 'geometry' in row and Path(toZip(row['id'])).exists():
            rows.append([
                row['id'],
                row['name'],
                row['shortName'] if 'shortName' in row else '',
                row['countryCode'],
                row.get('county') or "Without County",
                row.get('municipality') or "Without Municipality",
                row['geometry']['coordinates'][1],
                row['geometry']['coordinates'][0],
            ])
    return rows


with open(toCSV("sources"), 'w', newline='', encoding='utf-8') as csvfile:
    fieldnames = ['id', 'name', 'shortName', 'countryCode', 'county', 'municipality', 'latitude', 'longitude']
    writer = csv.writer(csvfile, delimiter=',')
    writer.writerow(fieldnames)
    data = process()
    writer.writerows(data)
