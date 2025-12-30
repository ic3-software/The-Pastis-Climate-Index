package crazydev.meteo;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class MeteoStations
{
    // -----------------------------------------------------------------------------------------------------------------
    // https://www.data.gouv.fr/datasets/informations-sur-les-stations-meteo-france-metadonnees/informations
    //
    // Ce jeu de données comprend l'ensemble des stations météorologiques de métropole et d'outre-mer en service.
    // Au total, 14 729 stations dont 13 362 stations ouvertes.
    // -----------------------------------------------------------------------------------------------------------------

    // Identifiant de la station en 8 chiffres DDCCCNNN (INSEE de la commune) ; DD n° département, CCC n° commune
    // dans le département DD et NNN n° de la station dans la commune CCC ; i.e. 31069001 (31 069 001) pour
    // TOULOUSE-BLAGNAC ; l'identifiant est unique et non-nul.
    public static final int F_ID = 0;

    // Nom de la station ; le nom n'est pas unique, il est dupliqué 674 ; i.e. TOURNON est dupliqué 4 fois ;
    // le nom est non-nul.
    public static final int F_NAME = 1;

    // Nom long ; Peut-être nul ; i.e. BLAGNAC (TOULOUSE-BLAGNAC) pour TOULOUSE-BLAGNAC
    public static final int F_LONG_NAME = 2;

    // Lieu-dit ; Peut-être nul ; i.e. AEROP. TOULOUSE-BLAGNAC pour TOULOUSE-BLAGNAC
    public static final int F_NAMED_PLACE = 3;

    // Type de la station (0, 1, 2, 3, 4 et 5) non-nul ;
    //  0 : Station synoptique, acquisition temps réel, expertisé à J+1 ;
    //  1 : Station automatique Radome-Resome, acquisition temps réel, expertisé à J+1 ;
    //  2 : Station automatique NON Radome-Resome, acquisition temps réel, expertisé à J+1 ;
    //  3 : Station automatique, acquisition temps réel, expertisé en temps différé (à M+21 jours maxi) ;
    //  4 : Poste climatologique manuel ou station automatique, acquisition temps différé, expertisé en temps différé
    //      (à M+21 jours maxi) ;
    //  5 : Station avec acquisition temps réel ou différé, non expertisée ou expertise des données non garantie
    public static final int F_TYPE = 4;

    // Bassin ; Peut-être nul ; i.e. O209 pour TOULOUSE-BLAGNAC
    public static final int F_BASSIN = 5;

    // Longitude
    public static final int F_LON = 6;

    // Latitude
    public static final int F_LAT = 7;

    // Altitude
    public static final int F_ALT = 8;

    // Date de début de la station
    public static final int F_START_DATE = 9;

    // Date de fin de la station
    public static final int F_END_DATE = 10;

    // Est-ce que la station est ouverte ?
    public static final int F_IS_OPEN = 11;

    // Est-ce que la station est public ?
    public static final int F_IS_PUBLIC = 12;

    // Numéro du département
    public static final int F_DEPT = 13;

    // Produit des mesures quotidiennes
    public static final int F_IS_DAILY = 14;

    // Produit des mesures horaires
    public static final int F_IS_HOURLY = 15;

    // Produit des mesures infra-horaires (6 minutes)
    public static final int F_IS_MINUTELY = 16;

    public static final DateTimeFormatter STATION_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");

    private final Path in;

    private final Path ic3data;

    // ID -> station
    private final Map<Integer, MeteoStation> stations = new HashMap<>();

    public MeteoStations(Path in, Path ic3data)
    {
        this.in = in;
        this.ic3data = ic3data;
    }

    public MeteoStation getStation(int id)
    {
        final MeteoStation station = getStationEx(id);

        if(station == null)
        {
            throw new RuntimeException("OUCH!");
        }

        return station;
    }

    @Nullable
    public MeteoStation getStationEx(int id)
    {
        return stations.get(id);
    }

    public void build() throws IOException
    {
        // (dept + name) -> stations
        final Map<String, List<MeteoStation>> stationsDN = new HashMap<>();

        try (final var reader = createCsvReader())
        {
            reader.forEach(record -> {

                final int id = MeteoUtils.parseInteger(record.getField(F_ID));

                if (stations.containsKey(id))
                {
                    throw new RuntimeException("OUCH!");
                }

                final String name = MeteoUtils.parseString(record.getField(F_NAME));
                final String longName = MeteoUtils.parseStringOpt(record.getField(F_LONG_NAME));
                final String namedPlace = MeteoUtils.parseStringOpt(record.getField(F_NAMED_PLACE));

                final int dept = MeteoUtils.parseInteger(record.getField(F_DEPT));
                final double lon = MeteoUtils.parseDouble(record.getField(F_LON));
                final double lat = MeteoUtils.parseDouble(record.getField(F_LAT));
                final int alt = MeteoUtils.parseInteger(record.getField(F_ALT));

                final LocalDate startDate = MeteoUtils.parseDateOpt(STATION_DATE_FORMAT, record.getField(F_START_DATE), MeteoStation.BOT);
                final LocalDate endDate = MeteoUtils.parseDateOpt(STATION_DATE_FORMAT, record.getField(F_END_DATE), MeteoStation.EOT);
                final boolean isOpen = MeteoUtils.parseBoolean(record.getField(F_IS_OPEN));

                final boolean isPub = MeteoUtils.parseBoolean(record.getField(F_IS_PUBLIC));

                if (dept >= 1 && dept <= 95)
                {
                    final MeteoStation station = new MeteoStation(
                            false,
                            id,
                            name, longName, namedPlace,
                            dept, lat, lon, alt,
                            startDate, endDate, isOpen,
                            isPub
                    );

                    stations.put(id, station);
                    stationsDN.computeIfAbsent(dept + ":" + name, k -> new ArrayList<>()).add(station);
                }
            });
        }

        // Ensure a bit of consistency...

        for (List<MeteoStation> stations : stationsDN.values())
        {
            stations.sort(Comparator.comparing(o -> o.startDate));

            for (final MeteoStation station : stations)
            {
                if (station.startDate.compareTo(station.endDate) >= 0)
                {
                    throw new RuntimeException("OUCH!");
                }
            }

            // There can be some holes in the sequence and periods can overlap.
            //
            // if(stations.size() > 1)
            // {
            //     for (int ii = 1; ii < stations.size(); ii++)
            //     {
            //         final MeteoStation prevStation = stations.get(ii -1);
            //         final MeteoStation station = stations.get(ii);
            //
            //         if(prevStation.endDate.compareTo(station.startDate) >= 0)
            //         {
            //         }
            //     }
            // }
        }

        int opened = 0;

        for (MeteoStation station : stations.values())
        {
            if (station.isOpen)
            {
                opened++;
            }
        }

        MeteoLoggers.GENERAL.info("stations : %s [ opened : %s ]".formatted(
                stations.size(), opened
        ));
    }

    private CsvReader<NamedCsvRecord> createCsvReader() throws IOException
    {
        return CsvReader.builder()
                .fieldSeparator(",")
                .ofNamedCsvRecord(
                        Files.newInputStream(in)
                );
    }

    public void write() throws IOException
    {
        final Path file = ic3data.resolve("stations.csv.gz");

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

            for (MeteoStation station : stations.values())
            {
                writer.writeRecord(
                        String.valueOf(station.isMissing),
                        String.valueOf(station.id),
                        station.name,
                        station.longName,
                        station.namedPlace,
                        String.valueOf(station.dept),
                        station.deptName,
                        String.valueOf(station.lat),
                        String.valueOf(station.lon),
                        String.valueOf(station.alt),
                        station.startDate.toString(STATION_DATE_FORMAT),
                        station.endDate.toString(STATION_DATE_FORMAT),
                        String.valueOf(station.isCurrent)
                );
            }
        }
        catch (IOException ex)
        {
            throw new IOException("IO error while writing " + file, ex);
        }
    }
}
