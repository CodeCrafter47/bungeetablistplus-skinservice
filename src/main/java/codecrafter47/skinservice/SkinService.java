package codecrafter47.skinservice;

import codecrafter47.skinservice.database.Head;
import com.beust.jcommander.JCommander;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import freemarker.template.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spark.ModelAndView;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.awt.Color.red;
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

        staticFileLocation("/static");

        get("/index.html", "text/html", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();

            attributes.put("avrgQueueSize5", statsTracker.getAvrgQueueSize(5));
            attributes.put("maxQueueSize5", statsTracker.getMaxQueueSize(5));
            attributes.put("requests5", statsTracker.getRequestsServed(5));
            attributes.put("cachedRequests5", statsTracker.getCachedRequests(5));
            attributes.put("mojangRequests5", statsTracker.getMojangRequests(5));

            attributes.put("avrgQueueSize60", statsTracker.getAvrgQueueSize(60));
            attributes.put("maxQueueSize60", statsTracker.getMaxQueueSize(60));
            attributes.put("requests60", statsTracker.getRequestsServed(60));
            attributes.put("cachedRequests60", statsTracker.getCachedRequests(60));
            attributes.put("mojangRequests60", statsTracker.getMojangRequests(60));

            attributes.put("avrgQueueSize1440", statsTracker.getAvrgQueueSize(1440));
            attributes.put("maxQueueSize1440", statsTracker.getMaxQueueSize(1440));
            attributes.put("requests1440", statsTracker.getRequestsServed(1440));
            attributes.put("cachedRequests1440", statsTracker.getCachedRequests(1440));
            attributes.put("mojangRequests1440", statsTracker.getMojangRequests(1440));

            attributes.put("avrgQueueSize10080", statsTracker.getAvrgQueueSize(10080));
            attributes.put("maxQueueSize10080", statsTracker.getMaxQueueSize(10080));
            attributes.put("requests10080", statsTracker.getRequestsServed(10080));
            attributes.put("cachedRequests10080", statsTracker.getCachedRequests(10080));
            attributes.put("mojangRequests10080", statsTracker.getMojangRequests(10080));

            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());

        get("/", "text/html", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

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

        exception(Exception.class, (e, request, response) -> {
            log.error("Unknown error", e);
            halt(500);
        });
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
