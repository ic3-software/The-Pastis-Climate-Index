package crazydev.meteo;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

public class MeteoInterval
{
    // Inclusive
    public final LocalDate from;

    // Exclusive
    public final LocalDate to;

    public MeteoInterval(LocalDate from, LocalDate to)
    {
        this.from = from;
        this.to = to;
    }

    public static MeteoInterval of(String period)
    {
        // e.g., 2000-2009
        final String[] parts = period.split("-");

        final int fromY = Integer.parseInt(parts[0]);
        final int toY = Integer.parseInt(parts[1]);

        return new MeteoInterval(
                new LocalDate(fromY, 1, 1),
                new LocalDate(toY + 1, 1, 1)
        );
    }

    public boolean isWithin(LocalDateTime timestamp )
    {
        final LocalDate date = timestamp.toLocalDate();
        return date.compareTo(from) >= 0 && date.compareTo(to) < 0;
    }
}
