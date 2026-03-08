package com.wwpet.service;

import com.wwpet.model.PetState;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AssetService {
    private static final List<String> EXTENSIONS = List.of("png", "gif", "jpg", "jpeg");

    private final Path assetDirectory;
    private final Map<String, CachedAsset> cache = new HashMap<>();

    public AssetService(Path assetDirectory) {
        this.assetDirectory = assetDirectory;
    }

    public BufferedImage loadPetImage(PetState state) {
        return loadPetImage(state, -1, -1);
    }

    public BufferedImage loadPetImage(PetState state, int targetWidth, int targetHeight) {
        String baseName = state == PetState.FOCUS ? "focus" : "rest";
        return loadImage(baseName, targetWidth, targetHeight);
    }

    public BufferedImage loadAppIcon() {
        return loadAppIcon(-1, -1);
    }

    public BufferedImage loadAppIcon(int targetWidth, int targetHeight) {
        return loadImage("icon", targetWidth, targetHeight);
    }

    private BufferedImage loadImage(String baseName, int targetWidth, int targetHeight) {
        String cacheKey = baseName + ":" + targetWidth + "x" + targetHeight;
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
                CachedAsset cachedAsset = cache.get(cacheKey);
                if (cachedAsset != null && candidate.equals(cachedAsset.path()) && cachedAsset.modifiedAt() == modifiedAt) {
                    return cachedAsset.image();
                }

                BufferedImage sourceImage = ImageIO.read(candidate.toFile());
                if (sourceImage != null) {
                    BufferedImage image = scaleImage(sourceImage, targetWidth, targetHeight);
                    cache.put(cacheKey, new CachedAsset(candidate, modifiedAt, image));
                    return image;
                }
            } catch (IOException ignored) {
            }
        }

        cache.remove(cacheKey);
        return null;
    }

    private BufferedImage scaleImage(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return sourceImage;
        }
        if (sourceImage.getWidth() == targetWidth && sourceImage.getHeight() == targetHeight) {
            return sourceImage;
        }

        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaledImage;
    }

    private record CachedAsset(Path path, long modifiedAt, BufferedImage image) {
    }
}
