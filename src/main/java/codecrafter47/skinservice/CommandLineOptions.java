package codecrafter47.skinservice;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import lombok.Getter;

@Getter
public class CommandLineOptions {

    @Parameter(names = "--port", validateValueWith = Port.class, required = true)
    Integer port = 4567;

    @Parameter(names = "--host")
    String host = "0.0.0.0";

    @Parameter(names = "--database")
    String database = "skinservice";

    @Parameter(names = "--db-host")
    String dbHost = "localhost";

    @Parameter(names = "--db-username", required = true)
    String dbUsername = "user";

    @Parameter(names = "--db-password", required = true)
    String dbPassword = "secret";

    @Parameter(names = "--db-port")
    Integer dbPort = 3306;

    public static class Port implements IValueValidator<Integer> {
        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value == null || value < 0 || value > 25565) {
                throw new ParameterException("Parameter " + name + " must be a valid port number.");
            }
        }
    }
}
