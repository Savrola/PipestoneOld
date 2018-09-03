/*
 Copyright © 2014 Daniel Boulet
 */

package com.obtuse.util;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Manage the pressed and unpressed versions of an icon/image used as a button.
 * <p/>
 */

@SuppressWarnings( { "UnusedDeclaration" } )
public class ImageButton {

    private final JLabel _button;
    private final ImageIcon _pressedIcon;
    private final ImageIcon _unpressedIcon;
//    private final ImageIcon _disabledIcon;
    private final Runnable _action;
    private final String _purpose;

    private static final float DEFAULT_DARKENING_FACTOR = 0.8f;
    private static float s_defaultDarkeningFactor = ImageButton.DEFAULT_DARKENING_FACTOR;

    private ImageButton(
            final @NotNull JLabel button,
            final @NotNull String purpose,
            final @NotNull ImageIcon pressedIcon,
            final @NotNull ImageIcon unpressedIcon,
//            final @NotNull ImageIcon disabledIcon,
            final @NotNull Runnable action
    ) {
        super();

        _button = button;
        _purpose = purpose;
        _pressedIcon = pressedIcon;
        _unpressedIcon = unpressedIcon;
//        _disabledIcon = disabledIcon;
        _action = action;

    }

    public static void setDefaultDarkeningFactor( final float factor ) {

        ImageButton.s_defaultDarkeningFactor = factor;

    }

    public static float getDefaultDarkeningFactor() {

        return ImageButton.s_defaultDarkeningFactor;

    }

    @NotNull
    public Runnable getAction() {

        return _action;

    }

    @NotNull
    public ImageIcon getPressedIcon() {

        return _pressedIcon;

    }

    @NotNull
    public ImageIcon getUnpressedIcon() {

        return _unpressedIcon;

    }

//    @NotNull
//    public ImageIcon getDisabledIcon() {
//
//        return _disabledIcon;
//
//    }

    @NotNull
    public String getPurpose() {

        return _purpose;

    }

    public void setEnabled( final boolean isEnabled ) {

        _button.setEnabled( isEnabled );

    }

    public boolean isEnabled() {

        return _button.isEnabled();

    }

    @NotNull
    public JLabel getButton() {

        return _button;

    }

    @NotNull
    public static ImageButton makeImageButton(
            final @NotNull ImageButtonOwner imageButtonOwner,
            final @NotNull String purpose,
            final @NotNull JLabel button,
            final @NotNull Runnable action,
            final @NotNull String buttonFileName
    ) {

        return ImageButton.makeImageButton(
                imageButtonOwner,
                purpose,
                button,
                action,
                buttonFileName,
                ImageIconUtils.getDefaultResourceBaseDirectory(),
                ImageButton.s_defaultDarkeningFactor
        );

    }

    public static ImageButton makeImageButton(
            final @NotNull ImageButtonOwner imageButtonOwner,
            final @NotNull String purpose,
            final @NotNull JLabel button,
            final @NotNull Runnable action,
            final @NotNull String buttonFileName,
            final @NotNull String resourceBaseDirectory,
            final float darkeningFactor
    ) {

        ImageIcon unpressedIcon = ImageIconUtils.fetchIconImage(
                "button-" + buttonFileName + ".png",
                0,
                resourceBaseDirectory
        );

        // Create a somewhat darker icon for the pressed version.

        ImageIcon pressedIcon = new ImageIcon(
                ImageIconUtils.changeImageBrightness( unpressedIcon.getImage(), darkeningFactor )
        );

//        // Create a somewhat brighter icon for the disabled version.
//
//
//        ImageIcon disabledIcon = new ImageIcon(
//                ImageIconUtils.changeImageBrightness( unpressedIcon.getImage(), 1F / darkeningFactor )
//        );

        return ImageButton.makeImageButton( imageButtonOwner, purpose, button, action, unpressedIcon, pressedIcon );

    }

    public static ImageButton makeImageButton(
            final ImageButtonOwner imageButtonOwner,
            final String purpose,
            final JLabel button,
            final Runnable action,
            final @NotNull ImageIcon unpressedIcon,
            final @NotNull ImageIcon pressedIcon
    ) {

        int width = Math.max( pressedIcon.getIconWidth(), unpressedIcon.getIconWidth() );
        int height = Math.max( pressedIcon.getIconHeight(), unpressedIcon.getIconHeight() );

        final ImageButton bi = new ImageButton( button, purpose, pressedIcon, unpressedIcon, action );

        button.addMouseListener(
                new MouseListener() {
                    public void mouseClicked( final MouseEvent mouseEvent ) {

                        if ( bi.getButton().isEnabled() ) {

                            bi.getAction().run();
                            imageButtonOwner.setButtonStates();

                        }

                    }

                    public void mousePressed( final MouseEvent mouseEvent ) {

                        if ( bi.getButton().isEnabled() ) {

                            bi.getButton().setIcon( bi.getPressedIcon() );

                        }

                    }

                    public void mouseReleased( final MouseEvent mouseEvent ) {

                        if ( bi.getButton().isEnabled() ) {

                            bi.getButton().setIcon( bi.getUnpressedIcon() );

                        }

                    }

                    public void mouseEntered( final MouseEvent mouseEvent ) {

                        if ( bi.getButton().isEnabled() ) {

                            imageButtonOwner.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );

                        }

                    }

                    public void mouseExited( final MouseEvent mouseEvent ) {

                        if ( bi.getButton().isEnabled() ) {

                            bi.getButton().setIcon( bi.getUnpressedIcon() );
                            imageButtonOwner.setCursor( Cursor.getDefaultCursor() );

                        }

                    }

                }
        );

        button.setIcon( bi.getUnpressedIcon() );
        button.setText( null );
        button.setMinimumSize( new Dimension( width, height ) );
        button.setMaximumSize( new Dimension( width, height ) );

        return bi;

    }

    public String toString() {

        return "ImageIcon( " + _purpose + " )";

    }

}