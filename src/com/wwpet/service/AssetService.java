package com.wwpet.service;

import com.wwpet.model.PetState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AssetService {
    private static final List<String> EXTENSIONS = List.of("png", "gif", "jpg", "jpeg");

    private final Path assetDirectory;
    private final Map<PetState, CachedAsset> cache = new EnumMap<>(PetState.class);

    public AssetService(Path assetDirectory) {
        this.assetDirectory = assetDirectory;
    }

    public BufferedImage loadPetImage(PetState state) {
        String baseName = state == PetState.FOCUS ? "focus" : "rest";
        return loadImage(baseName, state);
    }

    public BufferedImage loadAppIcon() {
        return loadImage("icon", null);
    }

    private BufferedImage loadImage(String baseName, PetState state) {
        for (String extension : EXTENSIONS) {
            Path candidate = assetDirectory.resolve(baseName + "." + extension);
            if (!Files.exists(candidate)) {
                continue;
            }
            try {
                long size = Files.size(candidate);
                if (size == 0L) {
                    continue;
                }
                long modifiedAt = Files.getLastModifiedTime(candidate).toMillis();
                CachedAsset cachedAsset = state == null ? null : cache.get(state);
                if (cachedAsset != null && candidate.equals(cachedAsset.path()) && cachedAsset.modifiedAt() == modifiedAt) {
                    return cachedAsset.image();
                }
                BufferedImage image = ImageIO.read(candidate.toFile());
                if (image != null && state != null) {
                    cache.put(state, new CachedAsset(candidate, modifiedAt, image));
                }
                return image;
            } catch (IOException ignored) {
            }
        }
        if (state != null) {
            cache.remove(state);
        }
        return null;
    }

    private record CachedAsset(Path path, long modifiedAt, BufferedImage image) {
    }
}
