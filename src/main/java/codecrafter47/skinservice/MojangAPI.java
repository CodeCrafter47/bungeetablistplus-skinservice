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

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Singleton
public class MojangAPI {

    private static final ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new Gson();
        }
    };

    @Nullable
    public Skin fetchSkin(UUID uuid) {
        try {
            Skin skin = fetchSkin0(uuid, 10);
            if (skin != null) return skin;
        } catch (Throwable e) {
            log.error("Unexpected exception", e);
        }
        return null;

    }

    private Skin fetchSkin0(UUID uuid, int retries) throws IOException, InterruptedException {
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
            return new Skin(skin.properties.get(0).value, skin.properties.get(0).signature);
        }
        return null;
    }

    private static class Profile {

        private String id;
        private String name;
    }

    private static class SkinProfile {

        private String id;
        private String name;

        final List<Property> properties = new ArrayList<>();

        private static class Property {

            private String name, value, signature;
        }
    }
}
