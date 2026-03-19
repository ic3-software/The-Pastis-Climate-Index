** Some code to get the temperature from Norway stations

https://frost.met.no/index.html

sources.csv -> stations id with a bit of info (based on existing temperature files)

for each stationId that has some constraints (data available in 1970 and 2025) create a file with temperatures

Check util.py for the settings

.env

PATH_NORWAY={Your path to the output files}
NORWAY_API_CLIENT_ID={you need one, it's free}


