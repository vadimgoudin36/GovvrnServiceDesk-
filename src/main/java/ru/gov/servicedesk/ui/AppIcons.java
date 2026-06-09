package ru.gov.servicedesk.ui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Загружает герб и иконки окон из ресурсов приложения.
 */
public final class AppIcons {
    private static final String COAT_OF_ARMS = "/icons/coat_of_arms.png";

    private AppIcons() {
    }

    /**
     * Возвращает иконки окна в нескольких размерах.
     *
     * @return список изображений для окна Swing
     */
    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : List.of(16, 24, 32, 48, 64, 128, 256)) {
            Image image = scaledImage(size);
            if (image != null) {
                icons.add(image);
            }
        }
        return icons;
    }

    /**
     * Создает компонент с масштабированным гербом.
     *
     * @param size размер изображения в пикселях
     * @return метка с логотипом
     */
    public static JLabel logoLabel(int size) {
        Image image = scaledImage(size);
        JLabel label = image == null ? new JLabel() : new JLabel(new ImageIcon(image));
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 18));
        return label;
    }

    private static Image scaledImage(int size) {
        try (InputStream stream = AppIcons.class.getResourceAsStream(COAT_OF_ARMS)) {
            if (stream == null) {
                return null;
            }
            BufferedImage source = ImageIO.read(stream);
            BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            BufferedImage scaled = progressiveScale(source, size, size);
            var graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = scaled.getWidth();
                int height = scaled.getHeight();
                int x = (size - width) / 2;
                int y = (size - height) / 2;
                graphics.drawImage(scaled, x, y, null);
            } finally {
                graphics.dispose();
            }
            return target;
        } catch (IOException ex) {
            return null;
        }
    }

    private static BufferedImage progressiveScale(BufferedImage source, int maxWidth, int maxHeight) {
        double scale = Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight());
        int targetWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage current = source;
        int width = source.getWidth();
        int height = source.getHeight();

        while (width / 2 >= targetWidth && height / 2 >= targetHeight) {
            width /= 2;
            height /= 2;
            current = drawScaled(current, width, height);
        }
        if (width != targetWidth || height != targetHeight) {
            current = drawScaled(current, targetWidth, targetHeight);
        }
        return current;
    }

    private static BufferedImage drawScaled(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, Transparency.TRANSLUCENT);
        var graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }
}
