package crazydev.meteo;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDateTime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MeteoTemperatures
{
    private final MeteoH in;

    private final Path ic3data;

    private final MeteoStations stations;

    private final Object obsWriterLOCK = new Object();

    @Nullable
    private CsvWriter obsWriter;

    @Nullable
    private String obsWriterPeriod;

    public MeteoTemperatures(MeteoH in, Path ic3data, MeteoStations stations)
    {
        this.in = in;
        this.ic3data = ic3data;
        this.stations = stations;
    }

    public void write(MeteoMode mode, @Nullable String periodFilter, @Nullable Integer departmentFilter) throws IOException
    {
        final long totalStartMS = System.currentTimeMillis();

        final AtomicInteger obsCountT = new AtomicInteger();
        final AtomicInteger availableTempCountT = new AtomicInteger();
        final AtomicInteger writtenTempCountT = new AtomicInteger();

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

                synchronized (obsWriterLOCK)
                {
                    if (obsWriter == null)
                    {
                        obsWriter = createCsvObsWriter(period);
                        obsWriterPeriod = period;
                    }
                    else if (!obsWriterPeriod.equals(period))
                    {
                        obsWriter.close();
                        obsWriter = createCsvObsWriter(period);
                        obsWriterPeriod = period;
                    }
                }

                final MeteoInterval p = MeteoInterval.of(period);

                final TempObsBatch batch = new TempObsBatch();

                try (final var reader = createCsvReader(path))
                {
                    MeteoLoggers.GENERAL.info(period + " @ " + department);

                    // ( NUM_POST << 32 ) + AAAAMMJJHH
                    final Set<Long> uniqueObservations = new LongOpenHashSet();

                    final MutableInt availableTempCount = new MutableInt();
                    final MutableInt writtenTempCount = new MutableInt();

                    reader.forEach(record -> {

                        final int stationId = MeteoUtils.parseInteger(record.getField(MeteoH.F_NUM_POSTE));
                        final MeteoStation station = stations.getStationEx(stationId);

                        final LocalDateTime time = MeteoUtils.parseDateTime(MeteoH.TIMESTAMP_FORMAT, record.getField(MeteoH.F_AAAAMMJJHH));
                        MeteoUtils.assertObservationTime(p, time);

                        assertUniqueObservations(uniqueObservations, stationId, time);

                        final Double temp = MeteoUtils.parseObservationTemperature(record.getField(MeteoH.F_T));

                        if (temp == null)
                        {
                            return;
                        }

                        availableTempCount.increment();

                        final Integer tempQ = MeteoUtils.parseObservationQuality(record.getField(MeteoH.F_QT));
                        MeteoUtils.assertObservationQuality(temp, tempQ);

                        // Temps. for a missing station are by definition within their opening period.
                        // Indeed, the opening period is computed from existing temps.

                        if (station == null || station.isWithinOpenedPeriod(time))
                        {
                            batch.batch.add(new TempObs(stationId, time, temp, tempQ));
                            writtenTempCount.increment();

                            if (batch.batch.size() > 10_000)
                            {
                                synchronized (obsWriterLOCK)
                                {
                                    writeObservation(obsWriter, batch.batch);
                                }
                                batch.batch.clear();
                            }
                        }
                    });

                    if (!batch.batch.isEmpty())
                    {
                        synchronized (obsWriterLOCK)
                        {
                            writeObservation(obsWriter, batch.batch);
                        }
                        batch.batch.clear();
                    }

                    MeteoLoggers.GENERAL.debug("%s @ %s in %s [ obs. count : %s ] [ available-temps : %s] [ written-temps : %s ]".formatted(
                            period,
                            department,
                            MeteoUtils.formatMillisEx(startMS),
                            MeteoUtils.formatNice(uniqueObservations.size()),
                            MeteoUtils.formatNice(availableTempCount.intValue()),
                            MeteoUtils.formatNice(writtenTempCount.intValue())
                    ));

                    obsCountT.addAndGet(uniqueObservations.size());
                    availableTempCountT.addAndGet(availableTempCount.intValue());
                    writtenTempCountT.addAndGet(writtenTempCount.intValue());

                    return true;
                }
            }
            catch (IOException ex)
            {
                throw new RuntimeException("error while processing " + path, ex);
            }
        });

        synchronized (obsWriterLOCK)
        {
            if (obsWriter != null)
            {
                obsWriter.close();
                obsWriter = null;
                obsWriterPeriod = null;
            }
        }

        MeteoLoggers.GENERAL.warn("%s [ obs. count : %s ] [ available-temps : %s] [ written-temps : %s ]".formatted(
                MeteoUtils.formatMillisEx(totalStartMS),
                MeteoUtils.formatNice(obsCountT.intValue()),
                MeteoUtils.formatNice(availableTempCountT.intValue()),
                MeteoUtils.formatNice(writtenTempCountT.intValue())
        ));
    }

    private CsvWriter createCsvObsWriter(String period) throws IOException
    {
        final CsvWriter writer = CsvWriter.builder()
                .fieldSeparator(';')
                .build(new GZIPOutputStream(Files.newOutputStream(ic3data.resolve("observations-" + period + ".csv.gz"))));

        writer.writeRecord(
                "STATION_ID",
                "TIMESTAMP",
                "TEMP",
                "Q_TEMP"
        );

        return writer;
    }

    private void writeObservation(CsvWriter writer, List<TempObs> batch)
    {
        for (TempObs obs : batch)
        {
            writer.writeRecord(obs.stationId, obs.time, obs.temp, obs.tempQ);
        }
    }

    private static CsvReader<NamedCsvRecord> createCsvReader(Path path) throws IOException
    {
        return CsvReader.builder()
                .fieldSeparator(";")
                .ofNamedCsvRecord(
                        new GZIPInputStream(Files.newInputStream(path))
                );
    }

    private static void assertUniqueObservations(Set<Long> uniques, int stationId, LocalDateTime time)
    {
        final int high = stationId;

        final int low =
                time.getHourOfDay()
                + (time.getDayOfMonth() * 100)
                + (time.getMonthOfYear() * 100 * 100)
                + (time.getYear() * 100 * 100 * 100);

        final long unique = ((long) high << 32) | (low & 0xFFFFFFFFL);

        if (uniques.contains(unique))
        {
            throw new RuntimeException("OUCH!");
        }

        uniques.add(unique);
    }

    static class TempObs
    {
        final String stationId;

        final String time;

        final String temp;

        final String tempQ;

        public TempObs(int stationId, LocalDateTime time, double temp, int tempQ)
        {
            this.stationId = String.valueOf(stationId);
            this.time = time.toString(MeteoH.TIMESTAMP_FORMAT);
            this.temp = String.valueOf(temp);
            this.tempQ = String.valueOf(tempQ);
        }
    }

    static class TempObsBatch
    {
        final List<TempObs> batch = new ArrayList<>();
    }
}
