from pathlib import Path
import os
from dotenv import load_dotenv

load_dotenv()

OUTPUT_DIR = Path(os.getenv('PATH_NORWAY'))
OUTPUT_DIR.mkdir(exist_ok=True, parents=True)

norwayClientId = os.getenv('NORWAY_API_CLIENT_ID')
norwayAPI = 'https://frost.met.no/sources/v0.jsonld'


def toZip(stationId) -> Path:
    return OUTPUT_DIR / f"{stationId}.zip"


def toCSV(stationId) -> Path:
    return OUTPUT_DIR / f"{stationId}.csv"
