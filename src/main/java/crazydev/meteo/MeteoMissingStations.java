package crazydev.meteo;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MeteoMissingStations
{
    private final MeteoH in;

    private final Path ic3data;

    private final MeteoStations stations;

    private final Map<Integer, MeteoMissingStation> missingStations = new HashMap<>();

    public MeteoMissingStations(MeteoH in, Path ic3data, MeteoStations stations)
    {
        this.in = in;
        this.ic3data = ic3data;
        this.stations = stations;
    }

    public void write(@Nullable String periodFilter, @Nullable Integer departmentFilter) throws IOException
    {
        final long totalStartMS = System.currentTimeMillis();

        in.forEachPD(periodFilter, departmentFilter, (department, period, path) -> {

            // ---------------------------------------------------------------------------------------------------------
            // MT callback.
            // ---------------------------------------------------------------------------------------------------------

            try
            {
                if (!Files.exists(path))
                {
                    return true;
                }

                final long startMS = System.currentTimeMillis();

                try (final var reader = createCsvReader(path))
                {
                    MeteoLoggers.GENERAL.info(period + " @ " + department);

                    reader.forEach(record -> {

                        final int stationId = MeteoUtils.parseInteger(record.getField(MeteoH.F_NUM_POSTE));
                        final MeteoStation station = stations.getStationEx(stationId);

                        if (station == null)
                        {
                            reportMissingStation(department, stationId, record);
                        }

                    });

                    MeteoLoggers.GENERAL.debug("%s @ %s in %s".formatted(
                            period,
                            department,
                            MeteoUtils.formatMillisEx(startMS)
                    ));

                    return true;
                }
            }
            catch (IOException ex)
            {
                throw new RuntimeException("error while processing " + path, ex);
            }
        });

        write();

        MeteoLoggers.GENERAL.warn("%s [ count : %s ]".formatted(
                MeteoUtils.formatMillisEx(totalStartMS),
                MeteoUtils.formatNice(missingStations.size())
        ));
    }

    private static CsvReader<NamedCsvRecord> createCsvReader(Path path) throws IOException
    {
        return CsvReader.builder()
                .fieldSeparator(";")
                .ofNamedCsvRecord(
                        new GZIPInputStream(Files.newInputStream(path))
                );
    }

    private void reportMissingStation(int department, int stationId, NamedCsvRecord record)
    {
        synchronized (missingStations)
        {
            MeteoMissingStation missing = missingStations.get(stationId);

            if (missing == null)
            {
                missing = MeteoMissingStation.of(department, record);
                missingStations.put(stationId, missing);
            }
            else
            {
                final MeteoMissingStation current = MeteoMissingStation.of(department, record);

                current.assertConsistency(missing);

                missing.updatePeriod(current);

            }
        }
    }

    private void write() throws IOException
    {
        final Path file = ic3data.resolve("stations-missings.csv.gz");

        try (final var writer = CsvWriter.builder().fieldSeparator(';').build(new GZIPOutputStream(Files.newOutputStream(file))))
        {
            writer.writeRecord(
                    "STATION_IS_MISSING",
                    "STATION_ID",
                    "STATION_NAME",
                    "STATION_LONG_NAME",
                    "STATION_NAMED_PLACE",
                    "STATION_DEPARTMENT_ID",
                    "STATION_DEPARTMENT_NAME",
                    "STATION_LAT",
                    "STATION_LON",
                    "STATION_ALT",
                    "STATION_START_DATE",
                    "STATION_END_DATE",
                    "STATION_IS_CURRENT"
            );

            for (MeteoMissingStation station : missingStations.values())
            {
                writer.writeRecord(
                        String.valueOf(true),
                        String.valueOf(station.id),
                        station.name,
                        null,
                        null,
                        String.valueOf(station.dept),
                        station.deptName,
                        String.valueOf(station.lat),
                        String.valueOf(station.lon),
                        String.valueOf(station.alt),
                        station.startDate.toString(MeteoStations.STATION_DATE_FORMAT),
                        station.endDate.toString(MeteoStations.STATION_DATE_FORMAT),
                        String.valueOf(station.endDate.equals(MeteoStation.EOT))
                );
            }
        }
        catch (IOException ex)
        {
            throw new IOException("IO error while writing " + file, ex);
        }
    }


}
