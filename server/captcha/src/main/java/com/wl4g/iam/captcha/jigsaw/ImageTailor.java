/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.iam.captcha.jigsaw;

import javax.imageio.ImageIO;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

import static com.wl4g.infra.common.codec.Compresss.snappyCompress;
import static com.wl4g.infra.common.codec.Compresss.snappyUnCompress;
import static io.netty.util.internal.ThreadLocalRandom.current;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;

/**
 * Image tailor.
 * 
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2019-08-30
 * @since
 */
public class ImageTailor {

    /**
     * The width pixel of the area where the edge of the slider needs to be
     * Gaussian blurred.
     */
    final public static int DEFAULT_BORDER_WEIGHT = 3;

    /**
     * The distance from the center of the circle is offset in radians. </br>
     * 
     * <pre>
     * public class Test {
     * 	public static void main(String args[]) {
     * 		double degrees = 45.0;
     * 		double radians = Math.toRadians(degrees);
     * 		System.out.format("pi ????????? %.4f%n", Math.PI);
     * 		System.out.format("%.4f ?????????????????? %.4f ??? %n", Math.sin(radians), Math.toDegrees(Math.asin(Math.sin(radians))));
     * 	}
     * }
     * <hr>
     * Output:
     *   pi ????????? 3.1416
     *   0.7071 ?????????????????? 45.0000 ???
     * </pre>
     * 
     * @see {@link ImageTailor#circleOffset}
     */
    final public static double DEFAULT_CIRCLE_OFFSET_RATIO = StrictMath.sin(StrictMath.toRadians(30));

    /** Material source drawing requires maximum width(:PX) */
    final protected int sourceMaxWidth;
    /** Material source drawing requires maximum height(:PX) */
    final protected int sourceMaxHeight;
    /** Material source drawing requires minimum width(:PX) */
    final protected int sourceMinWidth;
    /** Material source drawing requires minimum height(:PX) */
    final protected int sourceMinHeight;
    /** Block width(:PX) */
    final protected int blockWidth;
    /** Block height(:PX) */
    final protected int blockHeight;
    /** The radius of the circle of the tailored ear. */
    final protected int circleR;
    /** Watermark string drawn. */
    final protected String watermark;
    /**
     * For beautification, the offset distance from the center of the circle.
     */
    final protected int circleOffset;

    // --- Block borders position. ---
    private int borderTopXMin; // Top
    private int borderTopXMax;
    private int borderTopYMin;
    private int borderTopYMax;
    private int borderRightXMin; // Right
    private int borderRightXMax;
    private int borderRightYMin;
    private int borderRightYMax;
    private int borderBottomXMin; // Bottom
    private int borderBottomXMax;
    private int borderBottomYMin;
    private int borderBottomYMax;
    private int borderLeftXMin; // Left
    private int borderLeftXMax;
    private int borderLeftYMin;
    private int borderLeftYMax;

    public ImageTailor() {
        this(46, 46, 8, "wanglsir@gmail.com");
    }

    public ImageTailor(String watermark) {
        this(46, 46, 8, watermark);
    }

    public ImageTailor(int blockWidth, int blockHeight, int circleR, String watermark) {
        this(300, 200, 200, 100, blockWidth, blockHeight, circleR, watermark);
    }

    public ImageTailor(int sourceMaxWidth, int sourceMaxHeight, int sourceMinWidth, int sourceMinHeight, int blockWidth,
            int blockHeight, int circleR, String watermark) {
        isTrue(sourceMinWidth != 0, "sourceMinWidth cannot be less than 0");
        isTrue(sourceMaxWidth != 0, "sourceMaxWith cannot be less than 0");
        isTrue(sourceMinHeight != 0, "sourceMinHeight cannot be less than 0");
        isTrue(sourceMaxHeight != 0, "sourceMaxHeight cannot be less than 0");
        isTrue(blockWidth != 0, "blockWidth cannot be less than 0");
        isTrue(blockWidth < sourceMinWidth / 2, "blockWidth cannot be less than sourceMinWidth/2");
        isTrue(blockHeight != 0, "blockHeight cannot be less than 0");
        isTrue(blockHeight < sourceMinHeight / 2, "blockWidth cannot be less than sourceMinHeight/2");
        isTrue(circleR != 0, "circleR cannot be less than 0");
        // hasText(watermark, "watermark cannot be empty");
        this.sourceMaxWidth = sourceMaxWidth;
        this.sourceMaxHeight = sourceMaxHeight;
        this.sourceMinWidth = sourceMinWidth;
        this.sourceMinHeight = sourceMinHeight;
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
        this.circleR = circleR;
        this.circleOffset = (int) (circleR * DEFAULT_CIRCLE_OFFSET_RATIO);
        this.watermark = watermark;
    }

    /**
     * Get cut image from local file
     * 
     * @param filepath
     * @return
     * @throws IOException
     */
    public TailoredImage getImageFile(String filepath) throws IOException {
        BufferedImage source = ImageIO.read(new File(filepath));
        return doProcess(source);
    }

    /**
     * Get cut image from URL
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public TailoredImage getImageUrl(String url) throws IOException {
        return doProcess(ImageIO.read(new URL(url)));
    }

    /**
     * Get cut image from input stream
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public TailoredImage getImageInputStream(InputStream in) throws IOException {
        return doProcess(ImageIO.read(in));
    }

    /**
     * Do processing cut image.
     * 
     * @param sourceImg
     * @return
     * @throws IOException
     */
    private TailoredImage doProcess(BufferedImage sourceImg) throws IOException {
        int width = sourceImg.getWidth();
        int height = sourceImg.getHeight();
        // Check maximum effective width height
        isTrue((width <= sourceMaxWidth && height <= sourceMaxHeight),
                String.format("Source image is too big, max limits: %d*%d", sourceMaxWidth, sourceMaxHeight));
        isTrue((width >= sourceMinWidth && height >= sourceMinHeight),
                String.format("Source image is too small, min limits: %d*%d", sourceMinWidth, sourceMinHeight));

        // ??????????????????TYPE_4BYTE_ABGR????????????8???RGBA?????????????????????(???????????????BufferedImage)????????????bufImg.getType()
        BufferedImage primaryImg = new BufferedImage(sourceImg.getWidth(), sourceImg.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        // ???????????????
        BufferedImage blockImg = new BufferedImage(sourceImg.getWidth(), sourceImg.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        // ?????????????????????
        int maxX0 = width - blockWidth - (circleR + circleOffset);
        int maxY0 = height - blockHeight;
        int blockX0 = current().nextInt((int) (maxX0 * 0.25), maxX0); // *0.25??????x???????????????
        int blockY0 = current().nextInt(circleR, maxY0); // ???circleR????????????????????????????????????????????????
        // Setup block borders position.
        initBorderPositions(blockX0, blockY0, blockWidth, blockHeight);

        // ??????????????????(???????????????????????????????????????)
        drawing(sourceImg, blockImg, primaryImg, blockX0, blockY0, blockWidth, blockHeight);
        // ???????????????
        int cutX0 = blockX0;
        int cutY0 = Math.max((blockY0 - circleR - circleOffset), 0);
        int cutWidth = blockWidth + circleR + circleOffset;
        int cutHeight = blockHeight + circleR + circleOffset;
        blockImg = blockImg.getSubimage(cutX0, cutY0, cutWidth, cutHeight);

        // Add watermark string.
        addWatermarkIfNecessary(primaryImg);

        // ??????????????????
        TailoredImage img = new TailoredImage();
        // Primary image.
        ByteArrayOutputStream primaryData = new ByteArrayOutputStream();
        ImageIO.write(primaryImg, "PNG", primaryData);
        img.setPrimaryImg(primaryData.toByteArray());

        // Block image.
        ByteArrayOutputStream blockData = new ByteArrayOutputStream();
        ImageIO.write(blockImg, "PNG", blockData);
        img.setBlockImg(blockData.toByteArray());

        // Position
        img.setX(blockX0);
        img.setY(blockY0 - circleR >= 0 ? blockY0 - circleR : 0);
        return img;
    }

    /**
     * Drawing images.
     * 
     * @param sourceImg
     * @param blockImg
     * @param primaryImg
     * @param blockX0
     * @param blockY0
     * @param blockWidth
     * @param blockHeight
     */
    private void drawing(
            BufferedImage sourceImg,
            BufferedImage blockImg,
            BufferedImage primaryImg,
            int blockX0,
            int blockY0,
            int blockWidth,
            int blockHeight) {
        double rr = Math.pow(circleR, 2);// r??????
        // R1??????????????????????????????
        int c1_x0 = current().nextInt(blockWidth - 2 * circleR) + (blockX0 + circleR); // x+c_r+10;//??????x???????????????(x+r,x+with-r)?????????
        int c1_y0 = blockY0 - circleOffset;
        // R2??????????????????????????????
        int c2_x0 = blockX0 + circleOffset;
        int c2_y0 = current().nextInt(blockHeight - 2 * circleR) + (blockY0 + circleR);
        // R3??????????????????????????????
        int c3_x0 = blockX0 + blockWidth + circleOffset;
        int c3_y0 = current().nextInt(blockHeight - 2 * circleR) + (blockY0 + circleR);

        for (int x = 0; x < sourceImg.getWidth(); x++) {
            for (int y = 0; y < sourceImg.getHeight(); y++) {
                int rgb = sourceImg.getRGB(x, y);
                // (x-a)??+(y-b)??=r???????????????????????a???b???r?????????????????????(a???b)?????????r???
                double rr1 = Math.pow((x - c1_x0), 2) + Math.pow((y - c1_y0), 2);
                double rr2 = Math.pow((x - c2_x0), 2) + Math.pow((y - c2_y0), 2);
                double rr3 = Math.pow((x - c3_x0), 2) + Math.pow((y - c3_y0), 2);
                // ??????????????????????
                boolean withInBlock = x >= blockX0 && x < (blockX0 + blockWidth) && y >= blockY0 && y < (blockY0 + blockHeight);

                // Primary image
                if (rr >= rr1 || rr >= rr3) { // ???R1???R3?????????
                    primaryImg.setRGB(x, y, getGrayTranslucentRGB(rgb));
                } else if (withInBlock) { // ?????????????????????
                    if (rr >= rr2) { // ???R2?????????
                        primaryImg.setRGB(x, y, rgb);
                    } else if (!handleGaussainBlurIfNecessary(primaryImg, blockX0, blockY0, blockWidth, blockHeight, x, y, rgb)) { // ??????????????????????
                        primaryImg.setRGB(x, y, getGrayTranslucentRGB(rgb));
                    }
                } else {
                    primaryImg.setRGB(x, y, rgb);
                }

                // Block image
                if (rr >= rr3) { // ????????????R3???
                    blockImg.setRGB(x, y, rgb);
                } else if (withInBlock || rr >= rr1) { // ????????????R1???
                    if (rr < rr2) { // ??????R2???
                        if (rr >= rr1) { // ???R1???
                            blockImg.setRGB(x, y, rgb);
                        } else {
                            if (!handleGaussainBlurIfNecessary(blockImg, blockX0, blockY0, blockWidth, blockHeight, x, y, rgb)) { // ??????????????????????
                                blockImg.setRGB(x, y, rgb);
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * Watermark string drawn
     * 
     * @param img
     * @return
     * @throws IOException
     */
    private BufferedImage addWatermarkIfNecessary(BufferedImage img) throws IOException {
        if (isBlank(watermark)) {
            return img;
        }
        Graphics2D graphics2D = img.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // ????????????????????????
        graphics2D.setColor(Color.WHITE);
        // ??????????????????Font
        graphics2D.setFont(new Font("????????????", Font.BOLD, 16));
        // ???????????????????????????
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f));
        // ????????????->????????????????????????????????????->?????????????????????????????????(x,y)??? ??????10?????????????????????????????????????????????????????????
        int x = (int) (img.getWidth() - watermark.getBytes().length * 10), y = (int) (img.getHeight() * 0.9);
        graphics2D.drawString(watermark, x, y);
        graphics2D.dispose(); // ??????
        return img;
    }

    /**
     * Initialize setup border positions.
     * 
     * @param blockX0
     * @param blockY0
     * @param blockWidth
     * @param blockHeight
     */
    private void initBorderPositions(int blockX0, int blockY0, int blockWidth, int blockHeight) {
        // Top
        borderTopXMin = blockX0;
        borderTopXMax = blockX0 + blockWidth;
        borderTopYMin = blockY0;
        borderTopYMax = blockY0 + DEFAULT_BORDER_WEIGHT;
        // Right
        borderRightXMin = blockX0 + blockWidth - DEFAULT_BORDER_WEIGHT;
        borderRightXMax = blockX0 + blockWidth;
        borderRightYMin = blockY0;
        borderRightYMax = blockY0 + blockHeight;
        // Bottom
        borderBottomXMin = blockX0;
        borderBottomXMax = blockX0 + blockWidth;
        borderBottomYMin = blockY0 + blockHeight - DEFAULT_BORDER_WEIGHT;
        borderBottomYMax = blockY0 + blockHeight;
        // Left
        borderLeftXMin = blockX0;
        borderLeftXMax = blockX0 + DEFAULT_BORDER_WEIGHT;
        borderLeftYMin = blockY0;
        borderLeftYMax = blockY0 + blockHeight;
    }

    /**
     * Handle gaussian blur RGB. (borders region only)
     * 
     * @param img
     * @param blockX0
     * @param blockY0
     * @param blockWidth
     * @param blockHeight
     * @param x
     * @param y
     * @param srcRgb
     * @return ?????????????????????TRUE???????????????FALSE
     */
    private boolean handleGaussainBlurIfNecessary(
            BufferedImage img,
            int blockX0,
            int blockY0,
            int blockWidth,
            int blockHeight,
            int x,
            int y,
            int srcRgb) {
        // Top
        if (x >= borderTopXMin && x <= borderTopXMax && y >= borderTopYMin && y <= borderTopYMax) {
            doGaussainBlurRGB(img, borderTopXMin, borderTopXMax, borderTopYMin, borderTopYMax, x, y, blockWidth, blockHeight,
                    srcRgb);
            return true;
        }
        // Right
        if (x >= borderRightXMin && x <= borderRightXMax && y >= borderRightYMin && y <= borderRightYMax) {
            doGaussainBlurRGB(img, borderRightXMin, borderRightXMax, borderRightYMin, borderRightYMax, x, y,
                    DEFAULT_BORDER_WEIGHT, blockHeight, srcRgb);
            return true;
        }
        // Bottom
        if (x >= borderBottomXMin && x <= borderBottomXMax && y >= borderBottomYMin && y <= borderBottomYMax) {
            doGaussainBlurRGB(img, borderBottomXMin, borderBottomXMax, borderBottomYMin, borderBottomYMax, x, y, blockWidth,
                    blockHeight, srcRgb);
            return true;
        }
        // Left
        if (x >= borderLeftXMin && x <= borderLeftXMax && y >= borderLeftYMin && y <= borderLeftYMax) {
            doGaussainBlurRGB(img, borderLeftXMin, borderLeftXMax, borderLeftYMin, borderLeftYMax, x, y, DEFAULT_BORDER_WEIGHT,
                    blockHeight, srcRgb);
            return true;
        }
        return false;
    }

    /**
     * Set Gaussian blur RGB.
     * 
     * @param img
     * @param minBlurX
     * @param maxBlurX
     * @param minBlurY
     * @param maxBlurY
     * @param x
     * @param y
     * @param blurWidth
     * @param blurHeight
     * @param srcRgb
     */
    private void doGaussainBlurRGB(
            BufferedImage img,
            int minBlurX,
            int maxBlurX,
            int minBlurY,
            int maxBlurY,
            int x,
            int y,
            int blurWidth,
            int blurHeight,
            int srcRgb) {
        img.setRGB(x, y, srcRgb); // Nothing blur

        // int[] inPixels = new int[blurWidth * blurHeight];
        // int[] outPixels = new int[blurWidth * blurHeight];
        // java.awt.image.Kernel kernel = GaussianFilter.makeKernel(0.667f);
        // GaussianFilter.convolveAndTranspose(kernel, inPixels, outPixels,
        // blurWidth, blurHeight, true, GaussianFilter.CLAMP_EDGES);
        // GaussianFilter.convolveAndTranspose(kernel, outPixels, inPixels,
        // blurHeight, blurWidth, true, GaussianFilter.CLAMP_EDGES);
        // img.setRGB(x, y, blurWidth, blurHeight, inPixels, 0, blurWidth);

        // int v = 0;
        // if ((maxBlurY - minBlurY) > (maxBlurX - minBlurX)) { // Left/Right?
        // v = (int) (Math.abs(240 - Math.abs(x - minBlurX) * 20));
        // } else {
        // v = (int) (Math.abs(240 - Math.abs(y - minBlurY) * 20));
        // }
        // img.setRGB(x, y, new Color(v, v, v).getRGB());

        // NormalDistribution nd = new NormalDistribution(0, 1.44);

        // int r = (0xff & srcRgb);
        // int g = (0xff & (srcRgb >> 8));
        // int b = (0xff & (srcRgb >> 16));
        // srcRgb = r + (g << 8) + (b << 4) + (100 << 24);
        // img.setRGB(x, y, Color.white.getRGB());
        // img.setRGB(x, y, new Color(220, 220, 220).getRGB());
    }

    /**
     * Get the gray translucent RGB value.
     * 
     * @param rgb
     * @return
     */
    private static int getGrayTranslucentRGB(int rgb) {
        int r = (0xff & rgb);
        int g = (0xff & (rgb >> 8));
        int b = (0xff & (rgb >> 16));
        rgb = r + (g << 8) + (b << 16) + (100 << 24);
        // rgb = r + (g << 8) + (b << 16); // ?????????
        return rgb;
    }

    /**
     * Output write {@link BufferedImage} to file.
     * 
     * @param img
     * @param filepath
     * @throws IOException
     */
    public static void writeImage(BufferedImage img, String filepath) throws IOException {
        hasText(filepath, "File path can't empty");
        ImageIO.write(img, "PNG", new File(filepath));
    }

    /**
     * Tailored image model.
     * 
     * @author Wangl.sir
     * @version v1.0 2019???8???30???
     * @since
     */
    public static class TailoredImage implements Serializable {
        private static final long serialVersionUID = 4975604362812626949L;

        private int x;
        private int y;
        private byte[] primaryImg; // Base64
        private byte[] blockImg;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public byte[] getPrimaryImg() {
            return primaryImg;
        }

        public void setPrimaryImg(byte[] primaryImg) {
            this.primaryImg = primaryImg;
        }

        public byte[] getBlockImg() {
            return blockImg;
        }

        public void setBlockImg(byte[] blockImg) {
            this.blockImg = blockImg;
        }

        @Override
        public String toString() {
            return "JigsawImgCode [x=" + x + ", y=" + y + ", primaryImg=" + primaryImg + ", blockImg=" + blockImg + "]";
        }

        /**
         * Compression primary and block image.
         * 
         * @return
         */
        public TailoredImage compress() {
            setPrimaryImg(snappyCompress(getPrimaryImg()));
            setBlockImg(snappyCompress(getBlockImg()));
            return this;
        }

        /**
         * Compression primary and block image.
         * 
         * @return
         */
        public TailoredImage uncompress() {
            setPrimaryImg(snappyUnCompress(getPrimaryImg()));
            setBlockImg(snappyUnCompress(getBlockImg()));
            return this;
        }

    }

}