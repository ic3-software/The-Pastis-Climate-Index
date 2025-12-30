package crazydev.meteo;

import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

public class MeteoStation
{
    public static final LocalDate BOT = new LocalDate(1800, 1, 1);

    public static final LocalDate EOT = new LocalDate(2100, 1, 1);

    public final boolean isMissing;

    public final int id;

    public final String name;

    @Nullable
    public final String longName;

    @Nullable
    public final String namedPlace;

    public final int dept;

    public final String deptName;

    public final double lat;

    public final double lon;

    public final int alt;

    public final LocalDate startDate;

    public final LocalDate endDate;

    public final boolean isCurrent;

    public final boolean isOpen;

    public final boolean isPublic;

    public MeteoStation(boolean isMissing, int id, String name, @Nullable String longName, @Nullable String namedPlace, int dept, double lat, double lon, int alt, LocalDate startDate, LocalDate endDate, boolean isOpen, boolean isPublic)
    {
        this.isMissing = isMissing;

        this.id = id;

        this.name = name;
        this.longName = longName;
        this.namedPlace = namedPlace;

        this.dept = dept;
        this.deptName = MeteoDepartments.getName(dept);
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;

        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = endDate.equals(EOT);
        this.isOpen = isOpen /* dunno but open even if ednDate is in the past */;

        this.isPublic = isPublic;
    }

    public boolean isWithinOpenedPeriod(LocalDateTime time)
    {
        final LocalDate ld = time.toLocalDate();
        return ld.compareTo(startDate) >= 0 && ld.compareTo(endDate) < 0;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
