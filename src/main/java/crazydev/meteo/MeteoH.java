package crazydev.meteo;

import org.jetbrains.annotations.Nullable;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MeteoH
{
    // -----------------------------------------------------------------------------------------------------------------
    // https://www.data.gouv.fr/datasets/donnees-climatologiques-de-base-horaires/
    //
    // La mise à jour des fichiers est annuelle pour les fichiers par décennie,
    // mensuelles pour les fichiers de la décennie en cours jusqu'à l'année -2
    // et quotidiennes pour les deux dernières années.
    //
    // -----------------------------------------------------------------------------------------------------------------

    public static final String URL_LATEST = "https://object.files.data.gouv.fr/meteofrance/data/synchro_ftp/BASE/HOR/H_$DEPT$_latest-2025-2026.csv.gz";

    public static final String URL_PREVIOUS = "https://object.files.data.gouv.fr/meteofrance/data/synchro_ftp/BASE/HOR/H_$DEPT$_previous-2020-2024.csv.gz";

    public static final String URL_HISTORIC = "https://object.files.data.gouv.fr/meteofrance/data/synchro_ftp/BASE/HOR/H_$DEPT$_$PERIOD$.csv.gz";

    // numéro Météo-France du poste sur 8 chiffres
    public static final int F_NUM_POSTE = 0;

    public static final int F_NOM_USUEL = 1;

    // latitude, négative au sud (en degrés et millionièmes de degré)
    public static final int F_LAT = 2;

    // longitude, négative à l’ouest de GREENWICH (en degrés et millionièmes de degré)
    public static final int F_LON = 3;

    // altitude du pied de l'abri ou du pluviomètre si pas d'abri (en m)
    public static final int F_ALTI = 4;

    // date de la mesure (année mois jour heure)
    public static final int F_AAAAMMJJHH = 5;

    // température sous abri instantanée (en °C et 1/10)
    public static final int F_T = 42;

    // A chaque donnée est associé un code qualité (ex: T;QT) :
    //      9 : donnée filtrée (la donnée a passé les filtres/contrôles de premiers niveaux)
    //      0 : donnée protégée (la donnée a été validée définitivement par le climatologue)
    //      1 : donnée validée (la donnée a été validée par contrôle automatique ou par le climatologue)
    //      2 : donnée douteuse en cours de vérification (la donnée a été mise en doute par contrôle automatique)
    public static final int F_QT = 43;

    // -----------------------------------------------------------------------------------------------------------------

    static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormat.forPattern("yyyyMMddHH");

    // -----------------------------------------------------------------------------------------------------------------

    private static final String[] PERIODS = new String[]{

            "1780-1789", "1790-1799",
            "1800-1809", "1810-1819", "1820-1829", "1830-1839", "1840-1849", "1850-1859", "1860-1869", "1870-1879", "1880-1889", "1890-1899",
            "1900-1909", "1910-1919", "1920-1929", "1930-1939", "1940-1949", "1950-1959", "1960-1969", "1970-1979", "1980-1989", "1990-1999",
            "2000-2009", "2010-2019",
            "previous-2020-2024",
            "latest-2025-2026",

            };

    private static final int[] DEPARTMENTS = new int[]{

            1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
            60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
            70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
            80, 81, 82, 83, 84, 85, 86, 87, 88, 89,
            90, 91, 92, 93, 94, 95,

            };
    // -----------------------------------------------------------------------------------------------------------------

    private final String folder;

    public MeteoH(String folder)
    {
        this.folder = folder;
    }

    public Path latest(int department)
    {
        return Path.of(
                folder, "latest", MeteoUtils.asDD(department),
                "H_" + MeteoUtils.asDD(department) + "_latest-2025-2026.csv.gz"
        );
    }

    public Path previous(int department)
    {
        return Path.of(
                folder, "previous", MeteoUtils.asDD(department),
                "H_" + MeteoUtils.asDD(department) + "_previous-2020-2024.csv.gz"
        );
    }

    public Path historic(int department, String period)
    {
        return Path.of(
                folder, "historic", MeteoUtils.asDD(department),
                "H_" + MeteoUtils.asDD(department) + "_" + period + ".csv.gz"
        );
    }

    public void forEach(@Nullable String periodFilter, @Nullable Integer departmentFilter, Action cb)
    {
        for (final String period : PERIODS)
        {
            if (periodFilter != null && !isAcceptedPeriod(periodFilter, period))
            {
                continue;
            }

            for (final int department : DEPARTMENTS)
            {
                if (departmentFilter != null && !isAcceptedDepartment(departmentFilter, department))
                {
                    continue;
                }

                final String category = period.contains("previous-")
                                        ? "previous"
                                        : period.contains("latest-")
                                          ? "latest"
                                          : "historic";

                final String dept = MeteoUtils.asDD(department);
                final String filename = "H_" + dept + "_" + period + ".csv.gz";
                final Path path = Path.of(folder, category, dept, filename);

                final String periodF = period
                        .replace("previous-", "")
                        .replace("latest-", "");

                cb.cb(department, periodF, path);
            }
        }
    }

    public void forEachPD(@Nullable String periodFilter, @Nullable Integer departmentFilter, Action cb)
    {
        for (final String period : PERIODS)
        {
            if (periodFilter != null && !isAcceptedPeriod(periodFilter, period))
            {
                continue;
            }

            try (final ExecutorService pool = Executors.newFixedThreadPool(8))
            {
                final Future<DepartmentTaskResult>[] futures = new Future[DEPARTMENTS.length];

                for (int dd = 0; dd < DEPARTMENTS.length; dd++)
                {
                    final int department = DEPARTMENTS[dd];

                    futures[dd] = pool.submit(() -> processDepartment(period, departmentFilter, department, cb));
                }

                // ---------------------------------------------------------------------------------------------------------
                // *** Blocking ***  call waiting for all futures to complete.
                // ---------------------------------------------------------------------------------------------------------

                try
                {
                    for (int dd = 0; dd < DEPARTMENTS.length; dd++)
                    {
                        final Future<DepartmentTaskResult> future = futures[dd];

                        if (future != null)
                        {
                            final DepartmentTaskResult processed = future.get();

                        }
                    }

                }
                catch (ExecutionException | InterruptedException ex)
                {
                    throw new RuntimeException("OUCH!", ex);
                }
            }
        }
    }

    private DepartmentTaskResult processDepartment(String period, @Nullable Integer departmentFilter, int department, Action cb)
    {
        if (departmentFilter != null && !isAcceptedDepartment(departmentFilter, department))
        {
            return new DepartmentTaskResult();
        }

        final String category = period.contains("previous-")
                                ? "previous"
                                : period.contains("latest-")
                                  ? "latest"
                                  : "historic";

        final String dept = MeteoUtils.asDD(department);
        final String filename = "H_" + dept + "_" + period + ".csv.gz";
        final Path path = Path.of(folder, category, dept, filename);

        final String periodF = period
                .replace("previous-", "")
                .replace("latest-", "");

        cb.cb(department, periodF, path);

        return new DepartmentTaskResult();
    }

    private boolean isAcceptedPeriod(String filter, String period)
    {
        if (filter.equals("latest"))
        {
            return period.contains("latest-");
        }

        if (filter.equals("previous"))
        {
            return period.contains("previous-");
        }

        if (filter.equals("historic"))
        {
            return !period.contains("latest-") && !period.contains("previous-");
        }

        if (filter.startsWith(">"))
        {
            period = period.replace("previous-", "").replace("latest-", "");

            final int filterI = Integer.parseInt(filter.substring(1));
            final int periodI = Integer.parseInt(period.substring(0, 4));

            return periodI >= filterI;
        }

        return period.contains(filter);
    }

    private boolean isAcceptedDepartment(int filter, int department)
    {
        return department == filter;
    }

    @FunctionalInterface
    interface Action
    {
        boolean cb(int department, String period, Path path);
    }

    static class DepartmentTaskResult
    {
    }
}
