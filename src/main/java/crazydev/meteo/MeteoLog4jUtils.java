package crazydev.meteo;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public abstract class MeteoLog4jUtils
{
    private MeteoLog4jUtils()
    {
    }

    public static void configure(Level level)
    {
        final BuiltConfiguration configuration = createConfiguration(level);
        Configurator.reconfigure(configuration);
    }

    public static BuiltConfiguration createConfiguration(Level threshold)
    {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setConfigurationName("ic3-console");

        final AppenderComponentBuilder console = builder.newAppender("Console", "Console")
                .addAttribute("target", "SYSTEM_OUT")
                .add(
                        builder.newLayout("PatternLayout")
                                .addAttribute("pattern", "[%20.20t] [%5.5p] (%d{HH:mm:ss.SSS z}) %m%n")
                );

        final RootLoggerComponentBuilder root = builder.newRootLogger(threshold)
                .add(builder.newAppenderRef(console.getName()));

        builder.add(console);
        builder.add(root);

        final BuiltConfiguration configuration = builder.build();
        return configuration;
    }

}
