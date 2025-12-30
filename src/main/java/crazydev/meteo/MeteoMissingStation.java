package crazydev.meteo;

import de.siegmar.fastcsv.reader.NamedCsvRecord;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.Objects;

public class MeteoMissingStation
{
    public final int id;

    public final String name;

    public final int dept;

    public final String deptName;

    public final double lat;

    public final double lon;

    public final int alt;

    public LocalDate startDate;

    public LocalDate endDate;

    public MeteoMissingStation(int id, String name, int dept, double lat, double lon, int alt, LocalDate date)
    {
        this.id = id;
        this.name = name;

        this.dept = dept;
        this.deptName = MeteoDepartments.getName(dept);
        ;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;

        this.startDate = date;
        this.endDate = date;
    }

    public static MeteoMissingStation of(int department, NamedCsvRecord record)
    {
        final int id = MeteoUtils.parseInteger(record.getField(MeteoH.F_NUM_POSTE));
        final String name = MeteoUtils.parseString(record.getField(MeteoH.F_NOM_USUEL));

        final double lat = MeteoUtils.parseDouble(record.getField(MeteoH.F_LAT));
        final double lon = MeteoUtils.parseDouble(record.getField(MeteoH.F_LON));
        final int alt = MeteoUtils.parseInteger(record.getField(MeteoH.F_ALTI));

        final LocalDateTime time = MeteoUtils.parseDateTime(MeteoH.TIMESTAMP_FORMAT, record.getField(MeteoH.F_AAAAMMJJHH));

        return new MeteoMissingStation(
                id, name, department, lat, lon, alt, time.toLocalDate()
        );
    }

    public void updatePeriod(MeteoMissingStation current)
    {
        if(current.startDate.compareTo(startDate) < 0)
        {
            startDate = current.startDate;
        }
        if(current.endDate.compareTo(endDate) > 0)
        {
            endDate = current.endDate;
        }
    }

    public void assertConsistency(MeteoMissingStation other)
    {
        if (
                id != other.id
                || dept != other.dept
                || Double.compare(lat, other.lat) != 0
                || Double.compare(lon, other.lon) != 0
                || alt != other.alt
                || !Objects.equals(name, other.name)
                || !Objects.equals(deptName, other.deptName)
        )
        {
            throw new RuntimeException("OUCH!");
        }
    }

    @Override
    public String toString()
    {
        return name + " [ " + startDate + " : " + endDate + " ]";
    }
}
