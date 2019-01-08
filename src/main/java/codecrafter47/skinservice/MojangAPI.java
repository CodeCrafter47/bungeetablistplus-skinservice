/*
 * BungeeTabListPlus - a BungeeCord plugin to customize the tablist
 *
 * Copyright (C) 2014 - 2015 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.skinservice;

import codecrafter47.skinservice.database.MinecraftAccount;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Singleton
public class MojangAPI {

    private static final ThreadLocal<Gson> gson = ThreadLocal.withInitial(() -> new Gson());

    public SkinInfo fetchSkin(UUID uuid) throws IOException, InterruptedException {
        return fetchSkin0(uuid, 10);

    }

    private SkinInfo fetchSkin0(UUID uuid, int retries) throws IOException, InterruptedException {
        String uuidWithoutDashes = uuid.toString().replace("-", "");
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "https://sessionserver.mojang.com/session/minecraft/profile/" + uuidWithoutDashes + "?unsigned=false").
                openConnection();
        if (connection.getResponseCode() == 429 && retries > 0) {
            connection.disconnect();
            Thread.sleep(5000);
            return fetchSkin0(uuid, retries - 1);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), Charsets.UTF_8));
        SkinProfile skin = gson.get().fromJson(reader, (Type) SkinProfile.class);
        connection.disconnect();
        if (skin != null && skin.properties != null && !skin.properties.isEmpty()) {
            String json = new String(Base64.getDecoder().decode(skin.properties.get(0).value));
            Map<String, Object> map = gson.get().fromJson(json, (Type) LinkedTreeMap.class);
            String skinURL = (String) ((Map) ((Map) map.get("textures")).get("SKIN")).get("url");
            return new SkinInfo(skin.properties.get(0).value, skin.properties.get(0).signature, skinURL);
        }
        return null;
    }

    public AuthenticateResponse authenticate(MinecraftAccount account, String clientToken) {

        CloseableHttpClient client = HttpClients.createDefault();
        try {
            RequestConfig.Builder config = RequestConfig.copy(RequestConfig.DEFAULT)
                    .setRedirectsEnabled(true)
                    .setCircularRedirectsAllowed(false)
                    .setRelativeRedirectsAllowed(true)
                    .setMaxRedirects(10);

            ////////////////////////////////////////////////////////////////////////////////////////////////////
            // authenticate
            Object payload = new AuthenticatePayload(new Agent("Minecraft"), account.getEmail(), account.getPassword(), clientToken);

            HttpPost authReq = new HttpPost("https://authserver.mojang.com/authenticate");
            authReq.setConfig(config.build());

            StringEntity stringEntity = new StringEntity(gson.get().toJson(payload));
            authReq.setEntity(stringEntity);
            authReq.setHeader("Content-type", "application/json");
            CloseableHttpResponse authResponse = client.execute(authReq);

            if (authResponse.getStatusLine().getStatusCode() != 200) {
                authReq.releaseConnection();
                log.error("/authenticate returned status code " + authResponse.getStatusLine().getStatusCode()
                        + "\n" + EntityUtils.toString(authResponse.getEntity()));
                return null;
            }
            log.info("/authenticate successful");

            AuthenticateResponse auth = gson.get().fromJson(EntityUtils.toString(authResponse.getEntity()), AuthenticateResponse.class);
            return auth;
        } catch (IOException ex) {
            log.error("Unexpected exception", ex);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Closing client failed", e);
            }
        }
        return null;
    }

    public boolean validate(String clientToken, String accessToken) {

        if (accessToken == null)
            return false;

        CloseableHttpClient client = HttpClients.createDefault();
        try {
            RequestConfig.Builder config = RequestConfig.copy(RequestConfig.DEFAULT)
                    .setRedirectsEnabled(true)
                    .setCircularRedirectsAllowed(false)
                    .setRelativeRedirectsAllowed(true)
                    .setMaxRedirects(10);

            ////////////////////////////////////////////////////////////////////////////////////////////////////
            // authenticate
            Object payload = new ValidatePayload(accessToken, clientToken);

            HttpPost authReq = new HttpPost("https://authserver.mojang.com/validate");
            authReq.setConfig(config.build());

            StringEntity stringEntity = new StringEntity(gson.get().toJson(payload));
            authReq.setEntity(stringEntity);
            authReq.setHeader("Content-type", "application/json");
            CloseableHttpResponse authResponse = client.execute(authReq);
            authReq.releaseConnection();

            if (authResponse.getStatusLine().getStatusCode() == 204) {
                log.info("/validate - valid");
                return true;
            } else if (authResponse.getStatusLine().getStatusCode() == 403) {
                log.info("/validate - invalid");
                return false;
            } else {
                authReq.releaseConnection();
                log.error("/validate returned status code " + authResponse.getStatusLine().getStatusCode()
                        + "\n" + EntityUtils.toString(authResponse.getEntity()));
                return false;
            }
        } catch (IOException ex) {
            log.error("Unexpected exception", ex);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Closing client failed", e);
            }
        }
        return false;
    }

    public AuthenticateResponse refresh(MinecraftAccount account, String clientToken) {

        if (account.getAccessToken() == null) {
            return null;
        }
        CloseableHttpClient client = HttpClients.createDefault();
        try {
            RequestConfig.Builder config = RequestConfig.copy(RequestConfig.DEFAULT)
                    .setRedirectsEnabled(true)
                    .setCircularRedirectsAllowed(false)
                    .setRelativeRedirectsAllowed(true)
                    .setMaxRedirects(10);

            ////////////////////////////////////////////////////////////////////////////////////////////////////
            // authenticate
            Object payload = new ValidatePayload(account.getAccessToken(), clientToken);

            HttpPost authReq = new HttpPost("https://authserver.mojang.com/refresh");
            authReq.setConfig(config.build());

            StringEntity stringEntity = new StringEntity(gson.get().toJson(payload));
            authReq.setEntity(stringEntity);
            authReq.setHeader("Content-type", "application/json");
            CloseableHttpResponse authResponse = client.execute(authReq);

            if (authResponse.getStatusLine().getStatusCode() != 200) {
                authReq.releaseConnection();
                log.info("/refresh returned status code " + authResponse.getStatusLine().getStatusCode()
                        + "\n" + EntityUtils.toString(authResponse.getEntity()));
                return null;
            }
            log.info("/refresh successful");

            AuthenticateResponse auth = gson.get().fromJson(EntityUtils.toString(authResponse.getEntity()), AuthenticateResponse.class);
            return auth;
        } catch (IOException ex) {
            log.error("Unexpected exception", ex);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Closing client failed", e);
            }
        }
        return null;
    }

    public boolean updateSkin(MinecraftAccount account, byte[] file) {
        CloseableHttpClient client = HttpClients.createDefault();
        try {
            RequestConfig.Builder config = RequestConfig.copy(RequestConfig.DEFAULT)
                    .setRedirectsEnabled(true)
                    .setCircularRedirectsAllowed(false)
                    .setRelativeRedirectsAllowed(true)
                    .setMaxRedirects(10);

            ////////////////////////////////////////////////////////////////////////////////////////////////////
            // upload skin
            HttpPut uploadPage = new HttpPut("https://api.mojang.com/user/profile/" + account.getUuid().toString().replace("-", "") + "/skin");

            uploadPage.setConfig(config.build());
            uploadPage.setHeader("Authorization", "Bearer " + account.getAccessToken());

            uploadPage.setEntity(MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.create("image/png"), "skin.png")
                    .addTextBody("model", "")
                    .build()
            );
            CloseableHttpResponse skinResponse = client.execute(uploadPage);

            // check success
            if (skinResponse.getStatusLine().getStatusCode() != 204) {
                uploadPage.releaseConnection();
                log.error("/skin returned status code " + skinResponse.getStatusLine().getStatusCode());
                return false;
            }
            // done
            return true;
        } catch (IOException ex) {
            log.error("Unexpected exception", ex);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log.error("Closing client failed", e);
            }
        }
        return false;
    }

    private static class SkinProfile {

        private String id;
        private String name;

        final List<Property> properties = new ArrayList<>();

        private static class Property {

            private String name, value, signature;
        }
    }

    @RequiredArgsConstructor
    private static class Agent {
        private final String name;
        private final int version = 1;
    }

    @RequiredArgsConstructor
    private static class AuthenticatePayload {
        private final Agent agent;
        private final String username;
        private final String password;
        private final String clientToken;
    }

    @RequiredArgsConstructor
    private static class ValidatePayload {
        private final String accessToken;
        private final String clientToken;
    }

    @Getter
    public static class AuthenticateResponse {
        private String accessToken;
        private String clientToken;
        private List<Profile> availableProfiles;
        private Profile selectedProfile;
    }

    private static class Profile {
        private String id;
        private String name;
        private boolean legacy;
    }
}
