package crazydev.meteo;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public abstract class MeteoUtils
{
    private static final String[] COMPUTER_UNITS = {

            "B",
            "KB",
            "MB",
            "GB",
            "TB",
            "PB",
            "EB"

    };

    private static final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);

    private MeteoUtils()
    {
    }

    public static String asDD(int department)
    {
        return String.format("%02d", department);
    }

    public static void assertOutputConsistency(Path ic3data) throws IOException
    {
        if (!Files.exists(ic3data))
        {
            throw new IOException(ic3data + " does not exist");
        }

        if (!Files.isDirectory(ic3data))
        {
            throw new IOException(ic3data + " is not a folder");
        }

        try (final var stream = Files.list(ic3data))
        {
            if (stream.findFirst().isPresent())
            {
                throw new IOException(ic3data + " is not empty");
            }
        }
    }

    public static void assertObservationTime(MeteoInterval period, LocalDateTime time)
    {
        if (time.getMinuteOfHour() != 0)
        {
            throw new RuntimeException("OUCH!");
        }
        if (time.getSecondOfMinute() != 0)
        {
            throw new RuntimeException("OUCH!");
        }
        if (time.getMillisOfSecond() != 0)
        {
            throw new RuntimeException("OUCH!");
        }

        if(!period.isWithin(time))
        {
            throw new RuntimeException("OUCH!");
        }
    }

    public static void assertObservationQuality(@Nullable Object data, @Nullable Integer dataQuality)
    {
        if (data == null)
        {
            if (dataQuality != null && dataQuality != 1)
            {
                throw new RuntimeException("OUCH!");
            }

            return;
        }

        if (dataQuality == null)
        {
            throw new RuntimeException("OUCH!");
        }

        if (dataQuality != 0 && dataQuality != 1 && dataQuality != 2 && dataQuality != 9)
        {
            throw new RuntimeException("OUCH!");
        }
    }

    public static boolean parseBoolean(String value)
    {
        if(value.equals("true"))
        {
            return true;
        }
        else if(value.equals("false"))
        {
            return false;
        }
        throw new RuntimeException("OUCH!");
    }

    public static int parseInteger(String value)
    {
        return Integer.parseInt(value);
    }

    public static double parseDouble(String value)
    {
        return Double.parseDouble(value);
    }

    public static String parseString(String value)
    {
        if(isNullOrBlank(value))
        {
            throw new RuntimeException("OUCH!");
        }
        return value;
    }

    @Nullable
    public static String parseStringOpt(String value)
    {
        if(isNullOrBlank(value))
        {
            return null;
        }
        return value;
    }

    public static LocalDate parseDate(DateTimeFormatter df, String value)
    {
        return LocalDate.parse(value, df);
    }

    public static LocalDate parseDateOpt(DateTimeFormatter df, String value, LocalDate defaultValue)
    {
        if(isNullOrBlank(value))
        {
            return defaultValue;
        }
        return LocalDate.parse(value, df);
    }

    public static LocalDateTime parseDateTime(DateTimeFormatter df, String value)
    {
        return LocalDateTime.parse(value, df);
    }

    @Nullable
    public static Double parseObservationTemperature(String value)
    {
        if (isNullOrBlank(value))
        {
            return null;
        }
        return Double.parseDouble(value);
    }

    @Nullable
    public static Integer parseObservationQuality(String value)
    {
        if (isNullOrBlank(value))
        {
            return null;
        }

        return Integer.parseInt(value);
    }

    @Contract("null -> true")
    public static boolean isNullOrBlank(@Nullable String string)
    {
        return string == null || string.isBlank();
    }

    public static String formatSize(double value)
    {
        return formatSize(Locale.ENGLISH, value);
    }

    public static String formatSize(Locale locale, double value)
    {
        int curUnit = 0;

        while (value >= 1024.0)
        {
            value /= 1024.0;
            curUnit++;
        }

        if (curUnit > COMPUTER_UNITS.length)
        {
            throw new RuntimeException("Cannot convert (unit=2^3*" + curUnit + ")");
        }
        if (value == Math.round(value))
        {
            return String.format(locale, "%d%s", Math.round(value), COMPUTER_UNITS[curUnit]);
        }
        else if (value < 10.0)
        {
            return String.format(locale, "%.2f%s", value, COMPUTER_UNITS[curUnit]);
        }
        else if (value + 0.05 < 100.0)
        {
            return String.format(locale, "%.1f%s", value, COMPUTER_UNITS[curUnit]);
        }
        else
        {
            return String.format(locale, "%d%s", Math.round(value), COMPUTER_UNITS[curUnit]);
        }
    }

    public static String formatMillisEx(long startMillis)
    {
        return formatMillis(System.currentTimeMillis() - startMillis);
    }

    public static String formatMillis(long millis)
    {
        return formatMillis(null, millis);
    }

    public static String formatMillis(@Nullable Locale locale, long millis)
    {
        return formatMillis(locale, "", millis);
    }

    public static String formatMillis(@Nullable Locale locale, String separator, long millis)
    {
        // < 10 seconds
        if (millis < 10 * 1000)
        {
            return String.format(locale, "%,dms", millis);
        }
        // < 1 minute
        if (millis < 60 * 1000)
        {
            return String.format(locale, "%.2fs", millis / 1000.0);
        }

        // guess a little more nanos it's fine if it takes more than 1 minute
        final long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        // >= 24 hours
        if (days != 0)
        {
            return String.format(locale, "%dd" + separator + "%02dh" + separator + "%02dm" + separator + "%02ds", days, hours, minutes, seconds);
        }
        // >= 1 hour
        if (hours != 0)
        {
            return String.format(locale, "%dh" + separator + "%02dm" + separator + "%02ds", hours, minutes, seconds);
        }
        // < 1 hour && > 1minute
        else
        {
            return String.format(locale, "%dm" + separator + "%02ds", minutes, seconds);
        }
    }

    public static String formatNice(long value)
    {
        final DecimalFormat df = new DecimalFormat("0", formatSymbols);

        df.setGroupingUsed(true);
        df.setGroupingSize(3);

        return df.format(value);
    }
}
