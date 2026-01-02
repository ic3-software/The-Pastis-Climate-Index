package crazydev.meteo;

import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public class MeteoShell
{
    /**
     * The folder containing the icCube input files (as generated from the Météo-France data files) :
     * <pre>
     *      /temperatures
     *          /observations-1780-1789.csv.gz
     *          /...
     *          /observations-2024-2025.csv.gz
     *      /stations.csv.gz
     *      /stations-missings.csv.gz
     * </pre>
     *
     * Adjust to your own file system and update accordingly the definitions of the file data sources
     * in the Meteo.icc-schema file.
     */
    static final Path DATA_FOLDER = Path.of("/home/mpo/icCube/meteo/ic3/data");

    static final Path DATA_TEMPERATURES_FOLDER = DATA_FOLDER.resolve("temperatures");

    /**
     * The folder containing the Météo-France data files.
     * <pre>
     *     /H
     *          /historic
     *          /previous
     *          /latest
     *     /stations
     *          /stations-meteo-france.csv
     * </pre>
     *
     * Adjust to your own file system.
     */
    static final Path FILE_METEO_FRANCE = Path.of("/home/mpo/icCube/meteo/meteo-france");

    /**
     * As downloaded from:
     * <pre>
     *      https://www.data.gouv.fr/datasets/informations-sur-les-stations-meteo-france-metadonnees/informations
     * </pre>
     */
    static final Path FILE_STATIONS = FILE_METEO_FRANCE.resolve("stations/stations-meteo-france.csv");

    /**
     * Will contain the downloaded Météo-France data.
     */
    static final MeteoH H_FILES = new MeteoH("/home/mpo/icCube/meteo/meteo-france/H");

    static void main() throws Exception
    {
        MeteoLog4jUtils.configure(Level.DEBUG);

        // 2025-2026
        downloadLatest();

        // 2020-2024
        downloadPrevious();

        // Before 2020
        // downloadHistoric();

        // generateIcCubeData(MeteoMode.STATIONS);
        // generateIcCubeData(MeteoMode.MISSING_STATIONS);

        generateIcCubeData(MeteoMode.TEMPERATURES, ">2020");
    }

    private static void downloadLatest()
    {
        final MeteoDownloader downloader = new MeteoDownloader(H_FILES);
        downloader.downloadLatestH(null);
    }

    private static void downloadPrevious()
    {
        final MeteoDownloader downloader = new MeteoDownloader(H_FILES);
        downloader.downloadPreviousH(null);
    }

    private static void downloadHistoric()
    {
        final MeteoDownloader downloader = new MeteoDownloader(H_FILES);
        downloader.downloadHistoricH(null);
    }

    private static void generateIcCubeData(MeteoMode mode, @Nullable String periodFilter) throws IOException
    {
        // -------------------------------------------------------------------------------------------------------------
        // It seems all the stations are public. Without the oversea territory.
        // -------------------------------------------------------------------------------------------------------------

        if (mode == MeteoMode.STATIONS)
        {
            final MeteoStations stations = new MeteoStations(
                    FILE_STATIONS,
                    DATA_FOLDER
            );

            stations.build();
            stations.write();
        }

        // -------------------------------------------------------------------------------------------------------------
        // Extract stations information from the observations directly.
        // The stations file (see above) does not contain all of them.
        // -------------------------------------------------------------------------------------------------------------

        if (mode == MeteoMode.MISSING_STATIONS)
        {
            final MeteoStations stations = new MeteoStations(
                    FILE_STATIONS,
                    DATA_FOLDER
            );

            stations.build();

            final MeteoMissingStations missingStations = new MeteoMissingStations(H_FILES, DATA_FOLDER, stations);

            missingStations.write(null, null);
        }

        // -------------------------------------------------------------------------------------------------------------
        // Extract temperatures information from the observations.
        // -------------------------------------------------------------------------------------------------------------

        if (mode == MeteoMode.TEMPERATURES)
        {
            if (periodFilter == null)
            {
                MeteoUtils.assertOutputConsistency(DATA_TEMPERATURES_FOLDER);
            }

            final MeteoStations stations = new MeteoStations(
                    FILE_STATIONS,
                    DATA_FOLDER
            );

            stations.build();

            final MeteoTemperatures temps = new MeteoTemperatures(H_FILES, DATA_TEMPERATURES_FOLDER, stations);

            temps.write(mode, periodFilter, null);
        }

    }

}
