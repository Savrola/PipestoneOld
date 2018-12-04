/*
 Copyright © 2014 Daniel Boulet
 */

package com.obtuse.util;

import com.obtuse.exceptions.HowDidWeGetHereError;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.RescaleOp;
import java.net.URL;

/**
 * Utility methods for creating icons from images stored in our resources package.
 */

@SuppressWarnings({ "UnusedDeclaration" })
public class ImageIconUtils {

    private static String s_resourcesBaseDirectory = ".";

    private ImageIconUtils() {

        super();

    }

    public static ImageIcon fetchIconImage( final String fileName ) {

        return ImageIconUtils.fetchIconImage( fileName, 0 );

    }

    public static void setDefaultResourcesDirectory( final String resourcesBaseDirectory ) {

        ImageIconUtils.s_resourcesBaseDirectory = resourcesBaseDirectory;

    }

    public static String getDefaultResourceBaseDirectory() {

        return ImageIconUtils.s_resourcesBaseDirectory;

    }

    public static ImageIcon fetchIconImage( final String fileName, final int size ) {

        return ImageIconUtils.fetchIconImage( fileName, size, ImageIconUtils.s_resourcesBaseDirectory );

    }

    public static ImageIcon fetchIconImage(
            @NotNull final String fileName,
            final int size,
            @NotNull final String resourceBaseDirectory
    ) {

        return fetchIconImage(
                fileName,
                size,
                ImageIconUtils.class.getClassLoader(),
                resourceBaseDirectory
        );

    }

    public static ImageIcon fetchIconImage(
            @NotNull final String fileName,
            final int size,
            @NotNull ClassLoader classLoader,
            @NotNull final String resourceBaseDirectory
    ) {

        URL url = null;
        String resourcePath = resourceBaseDirectory + '/' + fileName;
        try {

            url = classLoader.getResource( resourcePath );

        } catch ( Throwable e ) {

            Logger.logErr( "Unable to load resource from " + ObtuseUtil.enquoteToJavaString( resourcePath ), e );

            ObtuseUtil.doNothing();

            // just ignore whatever went wrong

        }

        ImageIcon rval;
        if ( url == null ) {

            return null;

        } else {

            rval = new ImageIcon( url );

        }

        if ( size == 0 ) {

            return rval;

        } else {

            int rvalWidth = rval.getIconWidth();
            int rvalHeight = rval.getIconHeight();

            int scaledWidth = rvalWidth >= rvalHeight ? size : -1;
            int scaledHeight = rvalWidth <= rvalHeight ? size : -1;
            if ( scaledWidth == -1 && scaledHeight == -1 ) {

                throw new HowDidWeGetHereError(
                        "ImageIconUtils.fetchIconImage:  " +
                        "rvW=" + rvalWidth + ", rvH=" + rvalHeight + ", s=" + size +
                        " yielded both sW and sH equal to " + -1
                );

            }

            return new ImageIcon(
                    rval.getImage().getScaledInstance(
                            scaledWidth,
                            scaledHeight,
                            Image.SCALE_SMOOTH
                    )
            );

        }

    }

    /**
     * Get a {@link java.awt.image.BufferedImage} version of an {@link java.awt.Image}.
     * Identical to {@link #copyToBufferedImage(java.awt.Image)} except that the original image is returned if it is a {@link java.awt.image
     * .BufferedImage}.
     * <p/>
     * This method came from
     * <blockquote>
     * http://www.exampledepot.com/egs/java.awt.image/Image2Buf.html
     * </blockquote>
     * I (danny) don't know the terms of use as their "Terms of Use" link didn't do anything in either Safari or Firefox on my Mac OS X Snow
     * Leopard system.
     *
     * @param xImage the image to be converted.
     * @return the original image if it is a {@link java.awt.image.BufferedImage}; otherwise, the original image converted to a {@link java.awt
     * .image.BufferedImage}.
     */

    public static BufferedImage toBufferedImage( final Image xImage ) {

        if ( xImage instanceof BufferedImage ) {

            return (BufferedImage)xImage;

        }
        return ImageIconUtils.copyToBufferedImage( xImage );

    }

    /**
     * Make a {@link java.awt.image.BufferedImage} copy of an {@link java.awt.Image}.
     * Identical to {@link #toBufferedImage(java.awt.Image)} except that a new image is returned even if the original image is a {@link java.awt
     * .image.BufferedImage}.
     * <p/>
     * This method came from
     * <blockquote>
     * http://www.exampledepot.com/egs/java.awt.image/Image2Buf.html
     * </blockquote>
     * I (danny) don't know the terms of use as their "Terms of Use" link didn't do anything in either Safari or Firefox on my Mac OS X Snow
     * Leopard system.
     *
     * @param xImage the image to be converted.
     * @return a copy of the original image.
     */

    public static BufferedImage copyToBufferedImage( final Image xImage ) {

        // This code ensures that all the pixels in the image are loaded

        Image image = new ImageIcon( xImage ).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see Determining If an Image Has Transparent Pixels

        boolean hasAlpha = ImageIconUtils.hasAlpha( image );

        // Create a buffered image with a format that's compatible with the screen

        BufferedImage bImage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {

            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if ( hasAlpha ) {

                transparency = Transparency.BITMASK;

            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bImage = gc.createCompatibleImage(
                    image.getWidth( null ), image.getHeight( null ), transparency
            );

        } catch ( HeadlessException e ) {

            // The system does not have a screen

        }

        if ( bImage == null ) {

            // Create a buffered image using the default color model

            int type = BufferedImage.TYPE_INT_RGB;
            if ( hasAlpha ) {

                type = BufferedImage.TYPE_INT_ARGB;

            }

            bImage = new BufferedImage( image.getWidth( null ), image.getHeight( null ), type );

        }

        // Copy image to buffered image

        Graphics g = bImage.createGraphics();

        // Paint the image onto the buffered image

        g.drawImage( image, 0, 0, null );
        g.dispose();

        return bImage;

    }

    /**
     * This method returns true if the specified image has transparent pixels.
     * <p/>
     * This method came from
     * <blockquote>
     * http://www.exampledepot.com/egs/java.awt.image/HasAlpha.html
     * </blockquote>
     * I (danny) don't know the terms of use as their "Terms of Use" link didn't do anything in either Safari or Firefox on my Mac OS X Snow
     * Leopard system.
     *
     * @param image the image to be inspected.
     * @return true if the image has transparent pixels; false otherwise.
     */

    public static boolean hasAlpha( final Image image ) {

        // If buffered image, the color model is readily available
        if ( image instanceof BufferedImage ) {

            BufferedImage bImage = (BufferedImage)image;
            return bImage.getColorModel().hasAlpha();

        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber( image, 0, 0, 1, 1, false );
        //noinspection EmptyCatchBlock
        try {

            pg.grabPixels();

        } catch ( InterruptedException e ) {

        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();

    }

    /**
     * Create a new {@link java.awt.image.BufferedImage} which is brighter or darker than the specified {@link java.awt.Image}.
     *
     * @param image  the image to be brightened or darkened.
     * @param factor how much the image is to be brightened (if greater than 1) or darkened (if less than 1).
     *               For example, 1.2 makes the image 20% brighter whereas 0.8 makes the image 20% darker.
     * @return the brighter or darker image (always a new image even if the scaling factor is 1.0).
     */

    public static BufferedImage changeImageBrightness( final Image image, final float factor ) {

        BufferedImage bufferedVersion = ImageIconUtils.copyToBufferedImage( image );

        RescaleOp op = new RescaleOp( factor, 0, null );
        op.filter( bufferedVersion, bufferedVersion );

        return bufferedVersion;

    }

    /**
     * Create a new {@link javax.swing.ImageIcon} which is brighter or darker than the specified {@link javax.swing.ImageIcon}.
     *
     * @param imageIcon   the {@link javax.swing.ImageIcon} to be brightened or darkened.
     * @param scaleFactor how much the {@link javax.swing.ImageIcon} is to be brightened (if greater than 1) or darkened (if less than 1).
     *                    For example, 1.2 makes the image 20% brighter whereas 0.8 makes the image 20% darker.
     * @return the brighter or darker image (always a new {@link javax.swing.ImageIcon} even if the scaling factor is 1.0).
     */

    public static ImageIcon changeImageIconBrightness( final ImageIcon imageIcon, final float scaleFactor ) {

        return new ImageIcon( ImageIconUtils.changeImageBrightness( imageIcon.getImage(), scaleFactor ) );

    }

}
