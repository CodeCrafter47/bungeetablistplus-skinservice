package codecrafter47.skinservice;

import codecrafter47.skinservice.database.Model;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.converters.UUIDConverter;
import org.sql2o.quirks.NoQuirks;

import java.util.HashMap;
import java.util.UUID;

public class SkinServiceModule extends AbstractModule {
    private final CommandLineOptions commandLineOptions;

    public SkinServiceModule(CommandLineOptions commandLineOptions) {
        this.commandLineOptions = commandLineOptions;
    }

    @Override
    protected void configure() {
        bind(CommandLineOptions.class).toInstance(commandLineOptions);
        bind(Model.class).to(Database.class);
    }

    @Provides
    protected Sql2o createSQL() {
        return new Sql2o("jdbc:mysql://" + commandLineOptions.dbHost + ":" + commandLineOptions.dbPort + "/" + commandLineOptions.database,
                commandLineOptions.dbUsername, commandLineOptions.dbPassword, new NoQuirks(new HashMap<Class, Converter>() {{
            put(UUID.class, new UUIDConverter());
        }}));
    }
}
