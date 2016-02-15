package codecrafter47.skinservice.database;

import lombok.Data;

import java.util.UUID;

@Data
public class MinecraftAccount {
    private String email;
    private String password;
    private UUID uuid;
}
