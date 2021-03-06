/*
 *  Copyright (c) 2016, Glib Briia  <a href="mailto:glib.briia@assertthat.com">Glib Briia</a>
 *  Distributed under the terms of the MIT License
 */

package com.assertthat.selenium_shutterbug.utils.image;

import com.assertthat.selenium_shutterbug.utils.web.Coordinates;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 * Created by Glib_Briia on 17/06/2016.
 */
public class ImageProcessor {

    private static final int ARCH_SIZE = 10;
    private static float[] matrix = new float[49];

    static {
        for (int i = 0; i < 49; i++)
            matrix[i] = 1.0f / 49.0f;
    }

    public static BufferedImage blur(BufferedImage sourceImage) {
        BufferedImageOp options = new ConvolveOp(new Kernel(7, 7, matrix), ConvolveOp.EDGE_NO_OP, null);
        return options.filter(sourceImage, null);
    }

    public static BufferedImage highlight(BufferedImage sourceImage, Coordinates coords, Color color, int lineWidth) {
        byte defaultLineWidth = 3;
        Graphics2D g = sourceImage.createGraphics();
        g.setPaint(color);
        g.setStroke(new BasicStroke(lineWidth == 0 ? defaultLineWidth : lineWidth));
        g.drawRoundRect(coords.getX(), coords.getY(), coords.getWidth(), coords.getHeight(), ARCH_SIZE, ARCH_SIZE);
        g.dispose();
        return sourceImage;
    }

    public static BufferedImage addText(BufferedImage sourceImage, int x, int y, String text, Color color, Font font) {
        Graphics2D g = sourceImage.createGraphics();
        g.setPaint(color);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x, y);
        g.dispose();
        return sourceImage;
    }

    public static BufferedImage getElement(BufferedImage sourceImage, Coordinates coords) {
        return sourceImage.getSubimage(coords.getX(), coords.getY(), coords.getWidth(), coords.getHeight());
    }

    public static BufferedImage blurArea(BufferedImage sourceImage, Coordinates coords) {
        BufferedImage blurredImage = blur(sourceImage.getSubimage(coords.getX(), coords.getY(), coords.getWidth(), coords.getHeight()));
        BufferedImage combined = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(sourceImage, 0, 0, null);
        g.drawImage(blurredImage, coords.getX(), coords.getY(), null);
        g.dispose();
        return combined;
    }

    public static BufferedImage monochromeArea(BufferedImage sourceImage, Coordinates coords) {
        BufferedImage monochromedImage = convertToGrayAndWhite(sourceImage.getSubimage(coords.getX(), coords.getY(), coords.getWidth(), coords.getHeight()));
        BufferedImage combined = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(sourceImage, 0, 0, null);
        g.drawImage(monochromedImage, coords.getX(), coords.getY(), null);
        g.dispose();
        return combined;
    }

    public static BufferedImage blurExceptArea(BufferedImage sourceImage, Coordinates coords) {
        BufferedImage subImage = sourceImage.getSubimage(coords.getX(), coords.getY(), coords.getWidth(), coords.getHeight());
        BufferedImage blurredImage = blur(sourceImage);
        BufferedImage combined = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(blurredImage, 0, 0, null);
        g.drawImage(subImage, coords.getX(), coords.getY(), null);
        g.dispose();
        return combined;
    }

    public static BufferedImage addTitle(BufferedImage sourceImage, String title, Color color, Font textFont) {
        int textOffset = 5;
        BufferedImage combined = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight() + textFont.getSize(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(sourceImage, 0, textFont.getSize() + textOffset, null);
        addText(combined, 0, textFont.getSize(), title, color, textFont);
        g.dispose();
        return combined;
    }

    public static BufferedImage convertToGrayAndWhite(BufferedImage sourceImage) {
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(sourceImage, sourceImage);
        return sourceImage;
    }

    public static boolean imagesAreEquals(BufferedImage image1, BufferedImage image2, double deviation) {
        int width1 = image1.getWidth(null);
        int width2 = image2.getWidth(null);
        int height1 = image1.getHeight(null);
        int height2 = image2.getHeight(null);
        if ((width1 != width2) || (height1 != height2)) {
            throw new UnableToCompareImagesException("Images dimensions mismatch: image1 - " + width1 + "x" + height1 + "; image2 - " + width2 + "x" + height2);
        }
        long diff = 0;
        for (int y = 0; y < height1; y++) {
            for (int x = 0; x < width1; x++) {
                int rgb1 = image1.getRGB(x, y);
                int rgb2 = image2.getRGB(x, y);
                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = (rgb1) & 0xff;
                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = (rgb2) & 0xff;
                diff += Math.abs(r1 - r2);
                diff += Math.abs(g1 - g2);
                diff += Math.abs(b1 - b2);
            }
        }
        double n = width1 * height1 * 3;
        double p = diff / n / 255.0;
        return p == 0 || p <= deviation;
    }

    public static BufferedImage scale(BufferedImage source, double ratio) {
        int w = (int) (source.getWidth() * ratio);
        int h = (int) (source.getHeight() * ratio);
        BufferedImage scaledImage = getCompatibleImage(w, h);
        Graphics2D resultGraphics = scaledImage.createGraphics();
        resultGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        resultGraphics.drawImage(source, 0, 0, w, h, null);
        resultGraphics.dispose();
        return scaledImage;
    }

    private static BufferedImage getCompatibleImage(int w, int h) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(w, h);
        return image;
    }
}
