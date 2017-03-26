package codecrafter47.skinservice;

import codecrafter47.skinservice.database.Database;
import codecrafter47.skinservice.response.ResponseError;
import codecrafter47.skinservice.response.ResponseQueued;
import codecrafter47.skinservice.response.ResponseSuccess;
import codecrafter47.skinservice.response.ResponseSuccessOld;
import codecrafter47.skinservice.routes.AbstractRegisterSkinRoute;
import codecrafter47.skinservice.util.ImageWrapper;
import codecrafter47.skinservice.util.InternalServerException;
import codecrafter47.skinservice.util.JsonResponseTransformer;
import codecrafter47.skinservice.util.RequestException;
import com.beust.jcommander.JCommander;
import com.google.common.base.Preconditions;
import com.google.gson.JsonParseException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerEngine;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.ipAddress;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.staticFileLocation;

@Singleton
@Slf4j
public class SkinService {

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
            attributes.put("mojangRequests5", statsTracker.getMojangRequests(5));

            attributes.put("avrgQueueSize60", statsTracker.getAvrgQueueSize(60));
            attributes.put("maxQueueSize60", statsTracker.getMaxQueueSize(60));
            attributes.put("mojangRequests60", statsTracker.getMojangRequests(60));

            attributes.put("avrgQueueSize1440", statsTracker.getAvrgQueueSize(1440));
            attributes.put("maxQueueSize1440", statsTracker.getMaxQueueSize(1440));
            attributes.put("mojangRequests1440", statsTracker.getMojangRequests(1440));

            attributes.put("avrgQueueSize10080", statsTracker.getAvrgQueueSize(10080));
            attributes.put("maxQueueSize10080", statsTracker.getMaxQueueSize(10080));
            attributes.put("mojangRequests10080", statsTracker.getMojangRequests(10080));

            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());

        get("/", "text/html", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        post("/api/customhead", (req, res) -> {
            String base64 = req.body();
            byte[] decode = Base64.getDecoder().decode(base64);
            Preconditions.checkArgument(decode.length == 256);

            BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            int[] rgb = new int[64];
            ByteBuffer.wrap(decode).asIntBuffer().get(rgb);
            image.setRGB(0, 0, 8, 8, rgb, 0, 8);

            ImageWrapper headImage = new ImageWrapper(image);
            SkinInfo head = database.getSkinWithFace(headImage);
            if (head != null) {
                return new ResponseSuccessOld(head);
            }

            ImageWrapper skin = ImageWrapper.createTransparentImage(64, 64);
            skin.drawImage(headImage, 8, 8);

            SkinRequest skinRequest = skinManager.requestSkin(skin, req.ip());
            if (skinRequest.isError()) {
                return new ResponseError("Server error.");
            }
            if (!skinRequest.isFinished()) {
                return new ResponseQueued(skinRequest.getTimeLeft());
            }

            SkinInfo skinInfo = skinRequest.getResult();
            if (skinInfo != null) {
                new ResponseSuccessOld(skinInfo);
            }

            log.error("Skin neither in database nor requested");
            return new ResponseError("Internal server error.");
        }, JsonResponseTransformer.getInstance());

        put("/api/v2/register_face", "application/json",
                new AbstractRegisterSkinRoute(skinManager) {
                    @Override
                    protected Object handle(Request request, Response response, ImageWrapper image) throws Exception {
                        if (image.getWidth() != 8 || image.getHeight() != 8) {
                            throw new RequestException("Image must be 8x8 px.");
                        }

                        SkinInfo skinInfo = database.getSkinWithFace(image);
                        if (skinInfo != null) {
                            return new ResponseSuccess(skinInfo);
                        }

                        ImageWrapper skin = ImageWrapper.createTransparentImage(64, 64);
                        skin.drawImage(image, 8, 8);

                        return handleRegisterSkin(request, response, skin);
                    }
                },
                JsonResponseTransformer.getInstance());

        put("/api/v2/register_head", "application/json",
                new AbstractRegisterSkinRoute(skinManager) {
                    @Override
                    protected Object handle(Request request, Response response, ImageWrapper image) throws Exception {
                        if ((image.getWidth() != 32 && image.getWidth() != 64) || image.getHeight() != 16) {
                            throw new RequestException("Image must be 32x16 or 64x16 px.");
                        }

                        // extend to 64x16 px if necessary
                        if (image.getWidth() == 32) {
                            ImageWrapper newImage = ImageWrapper.createTransparentImage(64, 16);
                            newImage.drawImage(image, 0, 0);
                            image = newImage;
                        }

                        // ignore unused parts
                        image.setAreaTransparent(0, 0, 8, 8);
                        image.setAreaTransparent(24, 0, 16, 8);
                        image.setAreaTransparent(56, 0, 8, 8);

                        SkinInfo skinInfo = database.getSkinWithHead(image);
                        if (skinInfo != null) {
                            return new ResponseSuccess(skinInfo);
                        }

                        ImageWrapper skin = ImageWrapper.createTransparentImage(64, 64);
                        skin.drawImage(image, 0, 0);

                        return handleRegisterSkin(request, response, skin);
                    }
                },
                JsonResponseTransformer.getInstance());

        put("/api/v2/register_skin", "application/json",
                new AbstractRegisterSkinRoute(skinManager) {
                    @Override
                    protected Object handle(Request request, Response response, ImageWrapper image) throws Exception {
                        if (image.getWidth() != 64 || image.getHeight() != 64) {
                            throw new RequestException("Image must be 64x64 px.");
                        }

                        // ignore unused parts
                        image.setAreaTransparent(0, 0, 8, 8);
                        image.setAreaTransparent(24, 0, 16, 8);
                        image.setAreaTransparent(56, 0, 8, 8);

                        image.setAreaTransparent(0, 16, 4, 4);
                        image.setAreaTransparent(12, 16, 8, 4);
                        image.setAreaTransparent(36, 16, 8, 4);
                        image.setAreaTransparent(52, 16, 4, 4);

                        image.setAreaTransparent(0, 32, 4, 4);
                        image.setAreaTransparent(12, 32, 8, 4);
                        image.setAreaTransparent(36, 32, 8, 4);
                        image.setAreaTransparent(52, 32, 4, 4);

                        image.setAreaTransparent(0, 48, 4, 4);
                        image.setAreaTransparent(12, 48, 8, 4);
                        image.setAreaTransparent(28, 48, 8, 4);
                        image.setAreaTransparent(44, 48, 8, 4);
                        image.setAreaTransparent(60, 48, 4, 4);

                        image.setAreaTransparent(56, 16, 8, 32);

                        SkinInfo skinInfo = database.getSkin(image);
                        if (skinInfo != null) {
                            return new ResponseSuccess(skinInfo);
                        }

                        return handleRegisterSkin(request, response, image);
                    }
                },
                JsonResponseTransformer.getInstance());

        exception(RequestException.class, (exception, request, response) -> {
            response.body(JsonResponseTransformer.getInstance().render(new ResponseError(exception.getMessage())));
            response.status(400);
        });

        exception(JsonParseException.class, (exception, request, response) -> {
            response.body(JsonResponseTransformer.getInstance().render(new ResponseError(exception.getMessage())));
            response.status(400);
        });

        exception(InternalServerException.class, (exception, request, response) -> {
            log.error("Unknown error", exception);
            halt(500);
        });

        exception(Exception.class, (e, request, response) -> {
            log.error("Unknown error", e);
            halt(500);
        });
    }

}
