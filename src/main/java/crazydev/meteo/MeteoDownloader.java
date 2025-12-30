package crazydev.meteo;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public class MeteoDownloader
{
    private final MeteoH in;

    public MeteoDownloader(MeteoH in)
    {
        this.in = in;
    }

    public void downloadLatestH(@Nullable Integer departmentFilter)
    {
        in.forEach("latest", departmentFilter, (department, period, path) -> {

            final Path destination = in.latest(department);
            final String url = MeteoH.URL_LATEST.replace("$DEPT$", MeteoUtils.asDD(department));

            MeteoLoggers.GENERAL.debug(period + " @ " + department);

            download(destination, url);

            return true;

        });
    }

    public void downloadPreviousH(@Nullable Integer departmentFilter)
    {
        in.forEach("previous", departmentFilter, (department, period, path) -> {

            final String url = MeteoH.URL_PREVIOUS.replace("$DEPT$", MeteoUtils.asDD(department));
            final Path destination = in.previous(department);

            MeteoLoggers.GENERAL.debug(period + " @ " + department);

            download(destination, url);

            return true;

        });
    }

    public void downloadHistoricH(@Nullable Integer departmentFilter)
    {
        in.forEach("historic", departmentFilter, (department, period, path) -> {

            final String url = MeteoH.URL_HISTORIC
                    .replace("$DEPT$", MeteoUtils.asDD(department))
                    .replace("$PERIOD$", period);

            final Path destination = in.historic(department, period);

            MeteoLoggers.GENERAL.debug(period + " @ " + department);

            download(destination, url);

            return true;

        });
    }

    private void download(Path destination, String url)
    {
        if (!destination.toFile().getParentFile().exists())
        {
            if (!destination.toFile().getParentFile().mkdir())
            {
                throw new RuntimeException("OUCH!");
            }
        }

        if (destination.toFile().exists())
        {
            if (!destination.toFile().delete())
            {
                throw new RuntimeException("OUCH!");
            }
        }

        try (final HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build())
        {
            try
            {
                final HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(url))
                        .build();

                final HttpResponse<Path> response = client.send(
                        request, HttpResponse.BodyHandlers.ofFile(destination)
                );

                final int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300)
                {
                    MeteoLoggers.GENERAL.info("File size : " + MeteoUtils.formatSize(destination.toFile().length()));
                }
                else
                {
                    MeteoLoggers.GENERAL.error("HTTP error " + statusCode);
                    destination.toFile().delete();
                }
            }
            catch (Exception ex)
            {
                throw new RuntimeException("HTTP error while processing " + url, ex);
            }
        }
    }

}
