package com.obtuse.util.gowing.p2a.holders;

import com.obtuse.util.gowing.EntityName;
import com.obtuse.util.gowing.GowingPacker;
import com.obtuse.util.gowing.p2a.GowingConstants;
import org.jetbrains.annotations.NotNull;

/*
 * Copyright © 2015 Obtuse Systems Corporation
 */

/**
 Pack a boolean value.
 */

public class GowingBooleanHolder extends GowingAbstractPackableHolder {

    public GowingBooleanHolder( @NotNull final EntityName name, final Boolean v, @SuppressWarnings("SameParameterValue") final boolean mandatory ) {

        super( name, GowingConstants.TAG_BOOLEAN, v, mandatory );

    }

    public GowingBooleanHolder( @NotNull final EntityName name, final boolean[] v, @SuppressWarnings("SameParameterValue") final boolean mandatory ) {

        super( name, GowingConstants.TAG_BOOLEAN, v, mandatory, true );

    }

    public GowingBooleanHolder( @NotNull final EntityName name, final Boolean[] v, @SuppressWarnings("SameParameterValue") final boolean mandatory ) {

        super( name, GowingConstants.TAG_BOOLEAN, v, mandatory, false );

    }

    public void emitRepresentation( final GowingPacker packer2 ) {

        Object value = getObjectValue();

        if ( isMandatory() || value != null ) {

            switch ( getKind() ) {

                case SCALAR:
                    packer2.emit( ( (Boolean)value ).booleanValue() );
                    break;

                case PRIMITIVE_ARRAY:
                    packer2.emit( ( (boolean[])value ) );
                    break;

                case CONTAINER_ARRAY:
                    packer2.emit( ( (Boolean[])value ) );
                    break;

            }

        } else {

            packer2.emitNull();

        }

    }

}
