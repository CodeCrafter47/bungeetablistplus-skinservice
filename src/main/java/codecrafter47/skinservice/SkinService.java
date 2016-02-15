package codecrafter47.skinservice;

import codecrafter47.skinservice.database.Head;
import com.beust.jcommander.JCommander;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spark.Spark;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Base64;

import static spark.Spark.*;

@Singleton
@Slf4j
public class SkinService {

    private final ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new Gson();
        }
    };
    private final SkinManager skinManager;
    private final Database database;
    private final CommandLineOptions options;
    private final StatsTracker statsTracker;

    @Inject
    public SkinService(SkinManager skinManager, Database database, CommandLineOptions options, StatsTracker statsTracker) {
        this.skinManager = skinManager;
        this.database = database;
        this.options = options;
        this.statsTracker = statsTracker;
    }

    public static void main(String[] args) {
        CommandLineOptions commandLineOptions = new CommandLineOptions();
        new JCommander(commandLineOptions, args);

        Injector injector = Guice.createInjector(new SkinServiceModule(commandLineOptions));
        SkinService skinService = injector.getProvider(SkinService.class).get();
        skinService.start();
    }

    private void start() {

        ipAddress(options.getHost());

        port(options.getPort());

        post("/api/customhead", (req, res) -> {
            statsTracker.onRequest();
            String base64 = req.body();
            byte[] decode = Base64.getDecoder().decode(base64);
            Preconditions.checkArgument(decode.length == 256);

            Head head = database.getHead(decode);
            if (head != null) {
                // todo validate that image still exists?
                statsTracker.onCachedRequest();
                return new ResponseSuccess("SUCCESS", head.getSkin(), head.getSignature());
            }

            SkinManager.SkinRequest skinRequest = skinManager.requestSkin(decode, req.ip());
            if (skinRequest.isError()) {
                return new ResponseError("ERROR");
            }
            if (!skinRequest.isFinished()) {
                return new ResponseQueued("QUEUED", skinRequest.getTimeLeft());
            }


            Skin skin = skinRequest.getResult();
            if (skin != null) {
                new ResponseSuccess("SUCCESS", skin.getSkin(), skin.getSignature());
            }

            log.error("Skin neither in database nor requested");
            return new ResponseError("ERROR");
        }, o -> gson.get().toJson(o));

        exception(Exception.class, (e, request, response) -> response.body("{\"state\": \"ERROR\"}"));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ResponseSuccess{
        private String state;
        private String skin;
        private String signature;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ResponseQueued{
        private String state;
        private int timeLeft;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ResponseError{
        private String state;
    }
}
