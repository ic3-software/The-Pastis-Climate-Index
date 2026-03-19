import csv
import logging
from datetime import datetime
from pathlib import Path

import requests
from dateutil.relativedelta import relativedelta
from dateutil import parser
import zipfile
import os

from util import  toZip
from util import  toCSV
from util import norwayClientId
from util import norwayAPI

logger = logging.getLogger(__name__)


def processTempAndSave(stationId, startDate: datetime, endDate: datetime):
    start = startDate.strftime("%Y-%m-%d")
    end = endDate.strftime("%Y-%m-%d")
    params = {
        'sources': stationId,
        'elements': 'air_temperature',
        'referencetime': f"{start}/{end}",  # Datos horarios de este rango
    }

    r = requests.get(e, params, auth=(norwayClientId, ''))
    if r.status_code == 412:
        return []
    if r.status_code == 403:
        return []

    print("status : ", r.status_code )

    rows = []
    data = r.json()

    qualityCode = 0
    prevDate = None
    temperatures = []
    if not 'data' in data:
        return rows

    for row in data['data']:
        date = parser.isoparse(row['referenceTime']).replace(minute=0, second=0, microsecond=0)
        firstObservation = row['observations'][0]
        temp = float(firstObservation['value'])
        qualityCode = int(firstObservation['qualityCode'] if 'qualityCode' in firstObservation else -1)
        temperatures.append(temp)
        if prevDate != date and prevDate != None:
            rows.append([date,sum(temperatures) / len(temperatures), qualityCode] )
            temperatures = []
        prevDate = date

    if prevDate != None and len(temperatures) > 0:
        rows.append([prevDate,sum(temperatures) / len(temperatures), qualityCode] )

    return rows

def checkHasData(stationId):
    START_DATE = datetime(1970, 1, 1)
    END_DATE = datetime(1970, 1, 7)
    rows = processTempAndSave(stationId,START_DATE,END_DATE)
    if len(rows) == 0:
        return False

    START_DATE = datetime(2026, 1, 1)
    END_DATE = datetime(2026, 1, 7)
    rows = processTempAndSave(stationId,START_DATE,END_DATE)
    return len(rows) > 0


def processStation(stationId):

    START_DATE = datetime(1900, 1, 1)
    END_DATE = datetime.today()
    csvFileName = toCSV(stationId)
    zipFileName = toZip(stationId)

    with open(csvFileName, 'w', newline='', encoding='utf-8') as csvfile:

        fieldnames = ['date', 'temperature', 'quality code']
        writer = csv.writer(csvfile,delimiter=',')
        writer.writerow(fieldnames)
        current = START_DATE

        print("processing station : ", stationId)

        while current <= END_DATE:
            delta = 10 if current.year < 1960 else 1 if current.year < 2025 else 0
            endDate = current + relativedelta(years=delta, months=0 if delta != 0 else 6)
            data = processTempAndSave(stationId, current, endDate)
            writer.writerows(data)
            #    time.sleep(8)  # ≈7–8 peticiones/minuto → más seguro que 50/min
            current = endDate

    with zipfile.ZipFile(zipFileName, 'w', compression=zipfile.ZIP_DEFLATED) as zipf:
        zipf.write(csvFileName, arcname=f"{stationId}.csv")   # arcname = name inside zip

    os.remove(csvFileName)


# MAIN
r = requests.get(norwayAPI, {}, auth=(norwayClientId, ''))
data = r.json()

for row in data['data']:
    if 'geometry' in row and row['countryCode'] == "NO":
        stationId = row['id']
        if 'geometry' in row and not Path(toZip(stationId)).exists():
            if checkHasData(stationId):
                processStation(stationId)



