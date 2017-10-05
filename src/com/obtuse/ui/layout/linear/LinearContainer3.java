/*
 * Copyright © 2016 Daniel Boulet
 * All rights reserved.
 */

package com.obtuse.ui.layout.linear;

import com.obtuse.ui.layout.ConstraintTuple;
import com.obtuse.ui.layout.LinearOrientation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedList;

/**
 A container which is intended to be managed by a {@link LinearLayoutManager3} instance.
 */

public class LinearContainer3 extends JPanel implements LinearContainer {

    private static final java.util.List<LinearContainer3> _watchedContainers = new LinkedList<>();

//    private ConstraintTuple _breadthConstraints;
//    private ConstraintTuple _lengthConstraints;

    private ContainerConstraints _containerConstraints;
    private ComponentConstraints _componentConstraints;

    private ConstraintTuple _lengthConstraints;

    private ConstraintTuple _breadthConstraints;

    private boolean _trackParentBreadth;

    public LinearContainer3( @NotNull String name, LinearOrientation orientation ) {

        this( name, orientation, null, null );

    }

    LinearContainer3(
            @NotNull String name,
            LinearOrientation orientation,
            @SuppressWarnings("SameParameterValue") ContainerConstraints containerConstraints,
            @SuppressWarnings("SameParameterValue") ComponentConstraints componentConstraints
    ) {

        super();

        setName( name );

//	LinearLayoutUtil.addLocationTracer( this );

        setLayout( new LinearLayoutManager3( orientation, this ) );

        _containerConstraints = containerConstraints;
        _componentConstraints = componentConstraints;

//	getLayout();
//	setBreadth( breadth );

    }

    public static void watch( @NotNull LinearContainer3 watched ) {

        if ( !_watchedContainers.contains( watched ) ) {

            _watchedContainers.add( watched );

        }

//        if ( _watched != watched ) {
//
//            throw new IllegalArgumentException( "LinearCache3.watch:  already watching \"" + _watched.getName() + "\" when asked to watch \"" + watched.getName() + "\"" );
//
//	}
//
//        _watched = watched;

    }

    public static void unWatch( @NotNull LinearContainer3 watched ) {

        _watchedContainers.remove( watched );

    }

    public static java.util.List<LinearContainer3> getWatched() {

        return Collections.unmodifiableList( _watchedContainers );

    }

    public boolean isWatched() {

        return _watchedContainers.contains( this );

    }

    /**
     A hook for debuggin'.
     */

    public void doingLayout() {

    }

    /**
     Another hook for debuggin'.
     */

    public void doneLayout() {

    }

    public LinearOrientation getOrientation() {

        LayoutManager lm = getLayout();
        if ( lm instanceof LinearLayoutManager3 ) {

            return ( (LinearLayoutManager3)lm ).getOrientation();

        } else if ( lm == null ) {

            throw new IllegalArgumentException( "LinearContainer3:  no layout manager" );

        } else {

            throw new IllegalArgumentException( "LinearContainer3:  we need to be managed by a LinearLayoutManager3 (we are being managed by a " +
                                                lm.getClass().getName() +
                                                ")" );

        }

    }

    @Override
    public boolean isVertical() {

        return getOrientation() == LinearOrientation.VERTICAL;

    }

    @Override
    public boolean isHorizontal() {

        return getOrientation() == LinearOrientation.HORIZONTAL;

    }

    @Override
    public void setLengthConstraints( int minLength, int maxLength ) {

        setLengthConstraints( new ConstraintTuple( minLength, maxLength ) );
        revalidate();

    }

    @Override
    public void setLengthConstraints( ConstraintTuple lengthConstraints ) {

        _lengthConstraints = lengthConstraints;

    }

    @Override
    public void setBreadthConstraints( int minBreadth, int maxBreadth ) {

        setBreadthConstraints( new ConstraintTuple( minBreadth, maxBreadth ) );
        revalidate();

    }

    @Override
    public void setBreadthConstraints( ConstraintTuple breadthConstraints ) {

        _breadthConstraints = breadthConstraints;

    }

//    public void setTrackParentBreadth( boolean trackParentBreadth ) {
//
//	_trackParentBreadth = trackParentBreadth;
//
//    }
//
//    public boolean trackParentBreadth() {
//
//        return _trackParentBreadth;
//
//    }

    @Override
    public ConstraintTuple getLengthConstraints() {

        return _lengthConstraints;

    }

    @Override
    public ConstraintTuple getBreadthConstraints() {

        return _breadthConstraints;

    }

    public void setConstraints(
            int minBreadth, int maxBreadth,
            int minLength, int maxLength
    ) {

        setBreadthConstraints( minBreadth, maxBreadth );
        setLengthConstraints( minLength, maxLength );

    }

//    public int applyBreadthConstraints( int value ) {
//
//	int newValue = applyConstraints( _breadthConstraints, value );
//
//	return newValue;
//
//    }
//
//    public int applyLengthConstraints( int value ) {
//
//	int newValue = applyConstraints( _lengthConstraints, value );
//
//	return newValue;
//
//    }
//
//    public int applyConstraints( ConstraintTuple constraints, int value ) {
//
//	if ( constraints == null ) {
//
//	    return value;
//
//	} else {
//
//	    int newValue = Math.min( Math.max( value, constraints.getMinimum() ), constraints.getMaximum() );
//
//	    return newValue;
//
//	}
//
//    }

//    public void setBreadth( Integer breadth ) {
//
//	if ( breadth == null ) {
//
//	    _breadthConstraints = null;
//
//	} else {
//
//	    _breadthConstraints = new ConstraintTuple( breadth.intValue(), breadth.intValue(), breadth.intValue() );
//
//	}
//
//    }

//    public void setBreadthConstraints( int minBreadth, int prefBreadth, int maxBreadth ) {
//
//	_breadthConstraints = new ConstraintTuple( minBreadth, prefBreadth, maxBreadth );
//
//    }
//
//    public void setLengthConstraints( int minLength, int prefLength, int maxLength ) {
//
//	_lengthConstraints = new ConstraintTuple( minLength, prefLength, maxLength );
//
//    }

//    public void setConstraints(
//	    int minBreadth, int prefBreadth, int maxBreadth,
//	    int minLength, int prefLength, int maxLength
//    ) {
//
//	setBreadthConstraints( minBreadth, prefBreadth, maxBreadth );
//	setLengthConstraints( minLength, prefLength, maxLength );
//
//    }
//
//    public ConstraintTuple getBreadthConstraints() {
//
//	return _breadthConstraints;
//
//    }
//
//    public ConstraintTuple getLengthConstraints() {
//
//	return _lengthConstraints;
//
//    }
//
//    public int applyBreadthConstraints( int value ) {
//
//	int newValue = applyConstraints( _breadthConstraints, value );
//
//	return newValue;
//
//    }
//
//    public int applyLengthConstraints( int value ) {
//
//	int newValue = applyConstraints( _lengthConstraints, value );
//
//	return newValue;
//
//    }
//
//    public int applyConstraints( ConstraintTuple constraints, int value ) {
//
//	if ( constraints == null ) {
//
//	    return value;
//
//	} else {
//
//	    int newValue = Math.min( Math.max( value, constraints.getMinimum() ), constraints.getMaximum() );
//
//	    return newValue;
//
//	}
//
//    }

//    public int getMinimumBreadth() {
//
//	return _minimumBreadth;
//
//    }
//
//    public int getPreferredBreadth() {
//
//	return _preferredBreadth;
//
//    }
//
//    public int getMaximumBreadth() {
//
//	return _maximumBreadth;
//
//    }
//
//    public int getMinimumLength() {
//
//	return _minimumLength;
//
//    }
//
//    public int getPreferredLength() {
//
//	return _preferredLength;
//
//    }
//
//    public int getMaximumLength() {
//
//	return _maximumLength;
//
//    }

    public String toString() {

        return "LinearContainer3(" +
               " name=\"" + getName() + "\"," +
               " nComponents=" + getComponentCount() + "," +
               " alignment=(" + getAlignmentX() + "," + getAlignmentY() + ")," +
               " containerConstraints=" + _containerConstraints + "," +
               " componentConstraints=" + _componentConstraints + "," +
               " border=" + LinearLayoutUtil.describeBorder( getBorder() ) +
               " )";

    }

    public void setContainerConstraints( ContainerConstraints containerConstraints ) {

        _containerConstraints = containerConstraints;

        revalidate();

    }

    public boolean hasContainerConstraints() {

        return _containerConstraints != null;

    }

    public ContainerConstraints getContainerConstraints() {

        return _containerConstraints;

    }

    public void setComponentConstraints( ContainerConstraints containerConstraints ) {

        _containerConstraints = containerConstraints;

        revalidate();

    }

    public boolean hasComponentConstraints() {

        return _componentConstraints != null;

    }

    public ComponentConstraints getComponentConstraints() {

        return _componentConstraints;

    }

//    public void setContainerFlags( long containerFlags ) {
//
//	_containerFlags = containerFlags;
//
//    }
//
//    @Override
//    public long getComponentFlags() {
//
//	return _containerFlags;
//
//    }

}
