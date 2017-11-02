package com.obtuse.util;

/*
 * Copyright © 2015 Obtuse Systems Corporation
 */

import com.obtuse.exceptions.HowDidWeGetHereError;
import com.obtuse.util.gowing.*;
import com.obtuse.util.gowing.p2a.GowingEntityReference;
import com.obtuse.util.gowing.p2a.GowingUnPackerParsingException;
import com.obtuse.util.gowing.p2a.holders.GowingStringHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.obtuse.util.ObtuseApproximateCalendarDate.DateParsingException.Reason;
import static com.obtuse.util.ObtuseCalendarDate.OACD_DATE_PATTERN;
import static com.obtuse.util.ObtuseCalendarDate.parseCalendarDate;

/**
 Represent an approximate calendar date.
 <p/>
 Instances of this class are immutable and implement {@link GowingPackable}.
 <p/>
 Instances of this class are sortable by their approximate calendar date.
 */

public class ObtuseApproximateCalendarDate extends GowingAbstractPackableEntity implements Comparable<ObtuseApproximateCalendarDate> {

//    private static boolean s_allowOldStyleDateRanges = false;
    private static boolean s_requireNewStyleDateRanges = true;

    private final boolean _isUnknownApproximateDate;

    /**
     A runtime exception thrown when {@link ObtuseApproximateCalendarDate#parse(String)} is unable to parse an approximate date string.
     <p/>This class is derived from the {@link IllegalArgumentException} since there are probably a lot of catch clauses out there that
     are interested in IAE exceptions which will probably be interested in this runtime exception.
     */

    public static class DateParsingException extends IllegalArgumentException {

        public enum Reason {

            /**
             Not sure what went wrong.
             */

            UNKNOWN,

            /**
             Unable to parse empty strings.
             */

            EMPTY_STRING,

	        /* Date range's starting date is invalid. */

            RANGE_INVALID_STARTING_DATE,

            /**
             Date range's ending date is invalid.
             */

            RANGE_INVALID_ENDING_DATE,

            /**
             Date range's first date is after its second date.
             */

            RANGE_BACKWARDS,

            /**
             Date range is structurally invalid (missing separator, more than one separator, missing either or both parentheses in an old-style date range, etc).
             */

            RANGE_INVALID,

            /**
             Year is invalid (probably outside of supported range).
             */

            YEAR_INVALID,
            /**
             Decade is invalid (ends in a "0s" or a "0S" but otherwise invalid).
             */

            DECADE_INVALID,

            /**
             Month is invalid (contains exactly one hyphen but is otherwise invalid).
             */

            MONTH_INVALID,

            /**
             Date is invalid (contains at least two hyphens but is otherwise invalid).
             */

            DATE_INVALID,

            /**
             No idea what the string might represent.
             */

            INCOMPREHENSIBLE

        }

        private final Reason _reason;

        @SuppressWarnings("unused")
        public DateParsingException() {

            super( "unknown error" );

            _reason = Reason.UNKNOWN;

        }

        @SuppressWarnings("unused")
        public Reason getReason() {

            return _reason;

        }

        public DateParsingException( Reason reason, @NotNull String msg ) {

            super( msg );

            _reason = reason;

        }

        public DateParsingException( Reason reason, @NotNull String msg, @Nullable Throwable cause ) {

            super( msg, cause );

            _reason = reason;

        }

        public DateParsingException( Reason reason, @NotNull Throwable cause ) {

            super( "unknown error", cause );

            _reason = reason;

        }

    }

    public static final String FORMATTED_UNKNOWN_APPROXIMATE_DATE = "<unknown>";

    /**
     An unknown date which encompasses the entire range of supported dates.
     <p/>See {@link #isUnknownApproximateDate()} for an easy way to determine if a particular approximate date is actually an unknown date
     (i.e. this instance or some other instance with the same earliest and latest dates).
     <p/>Suggestion: consider using the {@code UNKNOWN_APPROXIMATE_DATE} instance in contexts where you might be tempted to use {@code null}.
     */

    public static final ObtuseApproximateCalendarDate UNKNOWN_APPROXIMATE_DATE = new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), ObtuseCalendarDate.getLatestSupportedDate() );

    private static final EntityTypeName ENTITY_TYPE_NAME = new EntityTypeName( ObtuseApproximateCalendarDate.class );

    private static final int VERSION = 1;

    private static final EntityName NOMINAL_DATE_NAME = new EntityName( "_dn" );

    private static final EntityName EARLIEST_DATE_NAME = new EntityName( "_de" );

    private static final EntityName LATEST_DATE_NAME = new EntityName( "_dl" );

    private static final EntityName PRECISION_NAME = new EntityName( "_pr" );

    public static GowingEntityFactory FACTORY = new GowingEntityFactory( ENTITY_TYPE_NAME ) {

        @Override
        public int getOldestSupportedVersion() {

            return VERSION;
        }

        @Override
        public int getNewestSupportedVersion() {

            return VERSION;
        }

        @NotNull
        @Override
        public GowingPackable createEntity( @NotNull GowingUnPacker unPacker, @NotNull GowingPackedEntityBundle bundle, GowingEntityReference er )
                throws GowingUnPackerParsingException {

            return new ObtuseApproximateCalendarDate( unPacker, bundle, er );

        }

    };

    public enum DatePrecision {

        DATE {
            public String title() {

                return "Date";
            }
        },
        MONTH {
            public String title() {

                return "Month";
            }
        },
        YEAR {
            public String title() {

                return "Year";
            }
        },
        DECADE {
            public String title() {

                return "Decade";
            }
        },
        RANGE {
            public String title() {

                return "Range";
            }
        };

        public abstract String title();

    }

    private static boolean s_verboseTesting = false;

    private final ObtuseCalendarDate _nominalCalendarDate;

    private final DatePrecision _precision;

    private ObtuseCalendarDate _earliestPossibleDate = null;
    private ObtuseCalendarDate _latestPossibleDate = null;

    public ObtuseApproximateCalendarDate( @NotNull ObtuseCalendarDate nominalCalendarDate, @NotNull DatePrecision precision ) {

        super( new GowingNameMarkerThing() );

        _nominalCalendarDate = nominalCalendarDate;

        // We need to specify/save the earliest and latest possible dates now since we cannot re-discover them later.

        if ( precision == DatePrecision.RANGE ) {

            _earliestPossibleDate = nominalCalendarDate;
            _latestPossibleDate = nominalCalendarDate;

        }

        _precision = precision;

        _isUnknownApproximateDate = false;

    }

    public ObtuseApproximateCalendarDate() {

        this( new ObtuseCalendarDate( new Date() ), DatePrecision.DATE );

    }

    public ObtuseApproximateCalendarDate( @Nullable ObtuseCalendarDate xEarliestPossibleDate, @Nullable ObtuseCalendarDate xLatestPossibleDate ) {

        super( new GowingNameMarkerThing() );

        ObtuseCalendarDate earliestPossibleDate =
                xEarliestPossibleDate == null ? ObtuseCalendarDate.getEarliestSupportedDate() : xEarliestPossibleDate;
        ObtuseCalendarDate latestPossibleDate = xLatestPossibleDate == null ? ObtuseCalendarDate.getLatestSupportedDate() : xLatestPossibleDate;

        if ( earliestPossibleDate.compareTo( latestPossibleDate ) > 0 ) {

            throw new IllegalArgumentException(
                    "ObtuseApproximateCalendarDate:  attempt to create range-precise instance with earliest date " + earliestPossibleDate +
                    " which is after latest possible date " + latestPossibleDate
            );

        }

        _earliestPossibleDate = earliestPossibleDate;
        _latestPossibleDate = latestPossibleDate;

        _nominalCalendarDate = earliestPossibleDate;

        _precision = DatePrecision.RANGE;

        _isUnknownApproximateDate = _earliestPossibleDate.equals( ObtuseCalendarDate.getEarliestSupportedDate() ) && _latestPossibleDate.equals( ObtuseCalendarDate.getLatestSupportedDate() );

    }

    public ObtuseApproximateCalendarDate(
            GowingUnPacker unPacker,
            GowingPackedEntityBundle bundle,
            GowingEntityReference er
    ) throws GowingUnPackerParsingException {

        super( unPacker, bundle.getSuperBundle() );

        _precision = ObtuseApproximateCalendarDate.makePrecision( bundle );

        if ( _precision == DatePrecision.RANGE ) {

            _earliestPossibleDate = ObtuseApproximateCalendarDate.makeCalendarDate( bundle, ObtuseApproximateCalendarDate.EARLIEST_DATE_NAME );
            _latestPossibleDate = ObtuseApproximateCalendarDate.makeCalendarDate( bundle, ObtuseApproximateCalendarDate.LATEST_DATE_NAME );
            _nominalCalendarDate = _earliestPossibleDate;

        } else {

            _nominalCalendarDate = ObtuseApproximateCalendarDate.makeCalendarDate( bundle, ObtuseApproximateCalendarDate.NOMINAL_DATE_NAME );

        }

        _isUnknownApproximateDate = _earliestPossibleDate.equals( ObtuseCalendarDate.getEarliestSupportedDate() ) && _latestPossibleDate.equals( ObtuseCalendarDate.getLatestSupportedDate() );

    }

    /**
     Determine if this instance encompass the entire range of supported dates and is thus an 'unknown' date.
     <p/>
     Since instances of this class are immutable, the determination of whether or not a newly created instance is equal to {@link #UNKNOWN_APPROXIMATE_DATE} is determined when an instance is created.
     This method returns said pre-computed knowledge (i.e. this method is very fast).
     @return {@code true} if this instance encompasses the entire range of supported dates; {@code false} otherwise.
     */

    public boolean isUnknownApproximateDate() {

        return _isUnknownApproximateDate;

    }

    private static ObtuseCalendarDate makeCalendarDate( GowingPackedEntityBundle bundle, EntityName whichDate )
            throws GowingUnPackerParsingException {

        try {

            return new ObtuseCalendarDate( bundle.getNotNullField( whichDate ).StringValue() );

        } catch ( com.obtuse.util.exceptions.ParsingException e ) {

            throw new GowingUnPackerParsingException( e + " recovering date string" );

        }

    }

    private static DatePrecision makePrecision( GowingPackedEntityBundle bundle ) throws GowingUnPackerParsingException {

        try {

            return DatePrecision.valueOf( bundle.getNotNullField( ObtuseApproximateCalendarDate.PRECISION_NAME ).StringValue() );

        } catch ( IllegalArgumentException e ) {

            throw new GowingUnPackerParsingException( e + " recovering precision" );

        }

    }

    @NotNull
    @Override
    public GowingPackedEntityBundle bundleThyself(
            boolean isPackingSuper, GowingPacker packer
    ) {

        GowingPackedEntityBundle bundle = new GowingPackedEntityBundle(
                ObtuseApproximateCalendarDate.ENTITY_TYPE_NAME,
                ObtuseApproximateCalendarDate.VERSION,
                super.bundleRoot( packer ),
                packer.getPackingContext()
        );

        if ( _precision == DatePrecision.RANGE ) {

            bundle.addHolder( new GowingStringHolder(
                    ObtuseApproximateCalendarDate.EARLIEST_DATE_NAME,
                    getEarliestPossibleDate().getDateString(),
                    true
            ) );
            bundle.addHolder( new GowingStringHolder(
                    ObtuseApproximateCalendarDate.LATEST_DATE_NAME,
                    getLatestPossibleDate().getDateString(),
                    true
            ) );

        } else {

            bundle.addHolder( new GowingStringHolder(
                    ObtuseApproximateCalendarDate.NOMINAL_DATE_NAME,
                    getNominalCalendarDate().getDateString(),
                    true
            ) );

        }

        bundle.addHolder( new GowingStringHolder( ObtuseApproximateCalendarDate.PRECISION_NAME, getPrecision().name(), true ) );

        return bundle;

    }

    @Override
    public boolean finishUnpacking( GowingUnPacker unPacker ) {

        return true;

    }

    @NotNull
    public ObtuseCalendarDate getEarliestPossibleDate() {

        if ( _earliestPossibleDate == null ) {

            switch ( _precision ) {

                case DATE:

                    _earliestPossibleDate = _nominalCalendarDate;
                    _latestPossibleDate = _nominalCalendarDate;

                    break;

                case MONTH:

                    String monthRounded = ObtuseUtil.lpad( _nominalCalendarDate.getYear(), 4, '0' ) + '-' +
                                          ObtuseUtil.lpad( _nominalCalendarDate.getMonthOfYear() + 1, 2, '0' ) + '-' +
                                          "01";
//		Logger.logMsg( "month rounded is \"" + monthRounded + "\"" );

                    _earliestPossibleDate = parseCalendarDate( monthRounded );

                    break;

                case YEAR:

                    _earliestPossibleDate = parseCalendarDate(
                            ObtuseUtil.lpad( _nominalCalendarDate.getYear(), 4, '0' ) + "-01-01"
                    );

                    break;

                case DECADE:

                    _earliestPossibleDate = parseCalendarDate(
                            ObtuseUtil.lpad( 10 * ( _nominalCalendarDate.getYear() / 10 ), 4, '0' ) + "-01-01"
                    );

                    break;

                case RANGE:

                    _earliestPossibleDate = parseCalendarDate( ObtuseCalendarDate.EARLIEST_SUPPORTED_DATE_STRING );

                    break;

//		    throw new HowDidWeGetHereError( "ObtuseApproximateCalendarDate:  attempt to compute earliest date for range precision date (should have been provide via constructor)" );

                default:
                    throw new HowDidWeGetHereError( "ObtuseApproximateCalendarDate:  unsupported precision " + _precision );

            }

        }

        return _earliestPossibleDate;

    }

    @NotNull
    public ObtuseCalendarDate getLatestPossibleDate() {

        if ( _latestPossibleDate == null ) {

            switch ( _precision ) {

                case DATE:

                    _earliestPossibleDate = _nominalCalendarDate;
                    _latestPossibleDate = _nominalCalendarDate;

                    break;

                case MONTH:

//	        try {

                    String yyyymmdd = ObtuseUtil.lpad( _nominalCalendarDate.getYear(), 4, '0' ) + '-' +
                                      ObtuseUtil.lpad( _nominalCalendarDate.getMonthOfYear() + 1, 2, '0' ) + '-' +
                                      ObtuseUtil.lpad( ObtuseCalendarDate.getDaysInMonth(
                                              _nominalCalendarDate.getYear(),
                                              _nominalCalendarDate.getMonthOfYear()
                                      ), 2, '0' );

                    _latestPossibleDate = parseCalendarDate( yyyymmdd );

                    break;

//		}
//		return parse(
//
//			"01" );

                case YEAR:

                    _latestPossibleDate = parseCalendarDate(
                            ObtuseUtil.lpad( _nominalCalendarDate.getYear(), 4, '0' ) + "-12-31"
                    );

                    break;

                case DECADE:

                    _latestPossibleDate = parseCalendarDate(
                            ObtuseUtil.lpad( 10 * ( _nominalCalendarDate.getYear() / 10 ) + 9, 4, '0' ) + "-12-31"
                    );

                    break;

                case RANGE:

                    _latestPossibleDate = parseCalendarDate( ObtuseCalendarDate.LATEST_SUPPORTED_DATE_STRING );

//		    throw new HowDidWeGetHereError( "ObtuseApproximateCalendarDate:  attempt to compute latest date for range precision date (should have been provide via constructor)" );

                default:
                    throw new HowDidWeGetHereError( "ObtuseApproximateCalendarDate:  unsupported precision " + _precision );

            }

        }

        return _latestPossibleDate;

    }

    /**
     Determine if this approximate date includes some other specific date.

     @param otherSpecificDate the other specific date.
     @return <tt>true</tt> if this approximate date includes the other specific date; <tt>false</tt> otherwise.
     */

    public boolean includes( ObtuseCalendarDate otherSpecificDate ) {

        return getEarliestPossibleDate().compareTo( otherSpecificDate ) <= 0 && otherSpecificDate.compareTo( getLatestPossibleDate() ) <= 0;

    }

    /**
     Determine if this approximate date overlaps some other approximate date.

     @param otherApproximateDate the other approximate date.
     @return <tt>true</tt> if they overlap; <tt>false</tt> otherwise.
     */

    public boolean overlaps( ObtuseApproximateCalendarDate otherApproximateDate ) {

        // Are we completely before the other date?

        if ( getLatestPossibleDate().compareTo( otherApproximateDate.getEarliestPossibleDate() ) < 0 ) {

            return false;

        }

        // No. Are we completely after the other date?

        if ( getEarliestPossibleDate().compareTo( otherApproximateDate.getLatestPossibleDate() ) > 0 ) {

            return false;

        }

        // No. We must overlap the other date.

        return true;

    }

    public ObtuseCalendarDate getNominalCalendarDate() {

        return _nominalCalendarDate;

    }

    public DatePrecision getPrecision() {

        return _precision;

    }

    /**
     Format an approximate calendar date while taking into account its precision.
     <p/>Dates are formatted as follows:
     <ul>
     <li>date-precise dates are presented by formatting their nominal date. For example, a date-precise date of 1998-07-12 yields <tt>"1998-07-12"</tt>.</li>
     <li>month-precise dates have the day of month chopped off. For example, a month-precise date of 1998-07-12 yields <tt>"1998-07"</tt>.</li>
     <li>year-precise dates have the month and day of month chopped off. For example, a year-precise date of 1998-07-12 yields <tt>"1998"</tt>.</li>
     <li>decade-precise dates have the month and day of month chopped off, the last digit of the year replaced with 0, and an 's' added at the end.
     For example, a decade-precise date of 1998-07-12 yields <tt>"1990s"</tt>.</li>
     <li>range-precise dates are formatted as <tt>(<i>start of date range</i>,<i>end-of-date-range</i>)</tt>.
     For example, a range-precise date running from 1998-07-12 to 2012-2-15 would be formatted as <tt>"(1998-07-12,2012-02-15)"</tt>.</li>
     </ul>
     It should be noted that the precision of an ObtuseApproximateCalendarDate instance is determined by what was specified when it was constructed.
     For example, a month-precise date created using
     <blockquote><tt>new ObtuseApproximateCalendarDate( ObtuseCalendarDate.parseCalendarDate "2012-05-10" ), DatePrecision.MONTH )</tt></blockquote>
     would format as <tt>"2012-05"</tt> whereas the equivalent range-precise date created using
     <blockquote><tt>new ObtuseApproximateCalendarDate( ObtuseCalendarDate.parseCalendarDate "2012-05-01" ), ObtuseCalendarDate.parseCalendarDate "2012-05-31" ) )</tt></blockquote>
     would format as <tt>"(2012-05-01,2012-05-31)"</tt>.
     Note that each of these forms are distinguishable - you can tell if the original {@link ObtuseApproximateCalendarDate} was a date, month, year, or decade-precise date just by looking at how it is formatted by this method.
     There is probably going to be a static <tt>parse( String <i>approximateDateString</i>)</tt> method someday which takes advantage of this distinguishability to automagically parse an
     approximate date string correctly (e.g. <tt>"2015-10-12"</tt> would yield the date-precise date 2015-10-12 whereas <tt>"2015-10"</tt> would yield the month-precise date 2015-10.

     @return this instance's formatted date taking into account its precision.
     */

    public String format() {

        switch ( _precision ) {

            case DATE:
                return getNominalCalendarDate().getDateString();

            case MONTH:
                return getNominalCalendarDate().getDateString().substring( 0, 7 );

            case YEAR:
                return getNominalCalendarDate().getDateString().substring( 0, 4 );

            case DECADE:
                return getNominalCalendarDate().getDateString().substring( 0, 3 ) + "0s";

            case RANGE:
                if ( isUnknownApproximateDate() ) {

                    return FORMATTED_UNKNOWN_APPROXIMATE_DATE;

                }

                if ( s_requireNewStyleDateRanges ) {

                    return getEarliestPossibleDate().getDateString() + ":" + getLatestPossibleDate().getDateString();

                } else {

                    return "(" + getEarliestPossibleDate().getDateString() + "," + getLatestPossibleDate().getDateString() + ")";

                }

            default:
                return getNominalCalendarDate().getDateString();

        }

    }

    /**
     Compare two instances primarily based on their earliest possible date, secondarily on their latest possible date, and tertiarily on the specified precisions.

     @param rhs the other instance.
     @return -1, 0 or 1 depending on whether this instance is less than, equal to or greater than the other instance.
     */

    public int compareTo( @NotNull ObtuseApproximateCalendarDate rhs ) {

        int rval = getEarliestPossibleDate().compareTo( rhs.getEarliestPossibleDate() );
        if ( rval == 0 ) {

            rval = getLatestPossibleDate().compareTo( rhs.getLatestPossibleDate() );
            if ( rval == 0 ) {

                // DatePrecision values with higher ordinals are considered to be greater than values with lower ordinals.

                rval = getPrecision().ordinal() - rhs.getPrecision().ordinal();

            }

        }

        return rval;

    }

    /**
     Determine if two instances are equivalent (same earliest possible date and same latest possible date).
     */

    public boolean equivalent( @NotNull ObtuseApproximateCalendarDate rhs ) {

        return getEarliestPossibleDate().equals( rhs.getEarliestPossibleDate() ) &&
               getLatestPossibleDate().equals( rhs.getLatestPossibleDate() );

    }

    /**
     An implementation of equals which is, by definition, consistent with {@link #compareTo} when equals returns true.

     @param rhs the other instance.
     @return true if both instance's specified date and precision are the same; false otherwise.
     */

    public boolean equals( Object rhs ) {

        return rhs instanceof ObtuseApproximateCalendarDate && compareTo( (ObtuseApproximateCalendarDate)rhs ) == 0;

    }

    public String toString() {

        return "ObtuseApproximateCalendarDate( " + format() + " )";

    }

//    private static final Pattern OACD_CENTURY_PATTERN = Pattern.compile( "(\\d\\d\\d\\d)\\s*[cC]" );
    private static final Pattern OACD_DECADE_PATTERN = Pattern.compile( "(\\d\\d\\d\\d)\\s*[sS]" );
    private static final Pattern OACD_YEAR_PATTERN = Pattern.compile( "(\\d\\d\\d\\d)" );
    private static final Pattern OACD_MONTH_PATTERN = Pattern.compile( "(\\d\\d\\d\\d)\\s*-\\s*(\\d\\d)" );

    @NotNull
    public static ObtuseApproximateCalendarDate parse( @NotNull String dateString )
            throws DateParsingException {

        dateString = dateString.trim();

        if ( dateString.length() == 0 ) {

            throw new DateParsingException( Reason.EMPTY_STRING, "unable to parse empty strings" );

        }

        if ( FORMATTED_UNKNOWN_APPROXIMATE_DATE.equalsIgnoreCase( dateString ) ) {

            return UNKNOWN_APPROXIMATE_DATE;

        }

        // If it starts with an opening parentheses, ends with a closing parentheses, and has a comma somewhere then it could only be a date range "(yyyy-mm-dd,yyyy-mm-dd)").

        if ( s_requireNewStyleDateRanges ) {

            if ( dateString.indexOf( ':' ) >= 0 || dateString.indexOf( ';' ) >= 0 ) {

                return parseDateRange( dateString );

            }

        } else if ( dateString.startsWith( "(" ) || dateString.indexOf( ',' ) >= 0 || dateString.endsWith( ")" ) ) {

            return parseDateRange( dateString );

        }

        // Could it be a decade?

        Matcher decadeMatcher = OACD_DECADE_PATTERN.matcher( dateString );
        if ( decadeMatcher.matches() ) {

            String tmpDateString = decadeMatcher.group( 1 ) + "-01-01";
            try {

                ObtuseCalendarDate tmpCalendarDate = ObtuseCalendarDate.parseCalendarDate( tmpDateString );

                try {

                    return new ObtuseApproximateCalendarDate( tmpCalendarDate, DatePrecision.DECADE );

                } catch ( Exception e ) {

                    throw new HowDidWeGetHereError( "exceptions should be impossible here (dateString is \"" + dateString + "\")", e );

                }

            } catch ( IllegalArgumentException e ) {

                throw new DateParsingException(
                        Reason.DECADE_INVALID,
                        "invalid decade \"" + dateString + "\" (must be \"YYYYs\" or \"YYYYS\")",
                        e
                );

            }

        }

        // If it is just four digits then it could only be a year.

        String cleanedYearString = dateString;
        if ( cleanedYearString.endsWith( "-00-00" ) ) {

            cleanedYearString = cleanedYearString.substring( 0, cleanedYearString.length() - 6 );

        }

        Matcher yearMatcher = OACD_YEAR_PATTERN.matcher( cleanedYearString );
        if ( yearMatcher.matches() ) {

            String tmpDateString = yearMatcher.group( 1 ) + "-01-01";

            try {

                ObtuseCalendarDate tmpCalendarDate = ObtuseCalendarDate.parseCalendarDate( tmpDateString );

                try {

                    return new ObtuseApproximateCalendarDate( tmpCalendarDate, DatePrecision.YEAR );

                } catch ( Exception e ) {

                    throw new HowDidWeGetHereError( "exceptions should be impossible here (dateString is \"" + dateString + "\")", e );

                }

            } catch ( IllegalArgumentException e ) {

                throw new DateParsingException(
                        Reason.YEAR_INVALID,
                        "invalid year \"" + cleanedYearString + "\" (must be four digit year between " +
                        ObtuseCalendarDate.EARLIEST_SUPPORTED_DATE_STRING.substring( 0, 4 ) + " and " +
                        ObtuseCalendarDate.LATEST_SUPPORTED_DATE_STRING.substring( 0, 4 ) + ")",
                        e
                );

            }

        }

        // Could it be a month?

        String cleanedMonthString = dateString;
        if ( cleanedMonthString.endsWith( "-00" ) ) {

            cleanedMonthString = cleanedMonthString.substring( 0, cleanedMonthString.length() - 3 );

        }

        Matcher monthMatcher = OACD_MONTH_PATTERN.matcher( cleanedMonthString );
        if ( monthMatcher.matches() ) {

            String tmpDateString = cleanedMonthString + "-01";

            try {

                ObtuseCalendarDate tmpCalendarDate = ObtuseCalendarDate.parseCalendarDate( tmpDateString );

                try {

                    return new ObtuseApproximateCalendarDate( tmpCalendarDate, DatePrecision.MONTH );

                } catch ( Exception e ) {

                    throw new HowDidWeGetHereError( "exceptions should be impossible here (dateString is \"" + dateString + "\")", e );

                }

            } catch ( IllegalArgumentException e ) {

                throw new DateParsingException(
                        Reason.MONTH_INVALID,
                        "invalid month \"" + cleanedMonthString + "\" (must be YYYY-MM)",
                        e
                );

            }

        }

        // Does it look like a simple calendar date?

        Matcher dateMatcher = OACD_DATE_PATTERN.matcher( dateString );
        if ( dateMatcher.matches() ) {

            try {

                ObtuseCalendarDate tmpCalendarDate = ObtuseCalendarDate.parseCalendarDate( dateString );

                try {

                    return new ObtuseApproximateCalendarDate( tmpCalendarDate, DatePrecision.DATE );

                } catch ( Exception e ) {

                    throw new HowDidWeGetHereError( "exceptions should be impossible here (dateString is \"" + dateString + "\")", e );

                }

            } catch ( IllegalArgumentException e ) {

                throw new DateParsingException(
                        Reason.DATE_INVALID,
                        "invalid date \"" + dateString + "\" (must be \"YYYY-MM-DD\")"
                );

            }

        }

        // No idea what this fish is.

        throw new DateParsingException( Reason.INCOMPREHENSIBLE, "unable to make sense of \"" + dateString + "\"" );

    }

    @NotNull
    public static ObtuseApproximateCalendarDate parseDateRange( String dateString )
            throws DateParsingException {

        final String trimmedDateString = dateString.trim();

        final String enquotedDateString = ObtuseUtil.enquoteToJavaString( trimmedDateString );

        String firstDateString;
        String secondDateString;

        int semiColonOffset = trimmedDateString.indexOf( ';' );
        int colonOffset = trimmedDateString.indexOf( ':' );
        int commaOffset = trimmedDateString.indexOf( ',' );
        int separatorOffset = -1;
        boolean tooManySeparators = false;
        boolean noSeparators = false;

        String mustBe;

        if ( s_requireNewStyleDateRanges ) {

            mustBe = "(must be \"YYYY-MM-DD:YYYY-MM-DD\" or \"YYYY-MM-DD;YYYY-MM-DD\")";

            if ( semiColonOffset >= 0 && colonOffset >= 0 ) {

                throw new DateParsingException(
                        Reason.RANGE_INVALID,
                        "more than one separator in date range " + enquotedDateString +
                        " (must be exactly one semi-colon or exactly one colon)"
                );

            }

            if ( semiColonOffset >= 0 && colonOffset >= 0 ) {

                tooManySeparators = true;

            } else if ( semiColonOffset >= 0 ) {

                separatorOffset = semiColonOffset;

            } else if ( colonOffset >= 0 ) {

                separatorOffset = colonOffset;

            } else {

                noSeparators = true;

            }

            if ( separatorOffset >= 0 ) {

                String tailPortion = trimmedDateString.substring( separatorOffset + 1 );
                if ( tailPortion.indexOf( ';' ) >= 0 || tailPortion.indexOf( ':' ) >= 0 ) {

                    tooManySeparators = true;

                }

            }

            if ( tooManySeparators ) {

                throw new DateParsingException(
                        Reason.RANGE_INVALID,
                        "too many colons or semicolons in date range " +
                        enquotedDateString +
                        " " +
                        mustBe
                );

            }

            if ( noSeparators ) {

                throw new DateParsingException(
                        Reason.RANGE_INVALID,
                        "no colon or semicolon in date range " +
                        enquotedDateString +
                        " " +
                        mustBe
                );

            }

            firstDateString = trimmedDateString.substring( 0, separatorOffset ).trim();
            secondDateString = trimmedDateString.substring( separatorOffset + 1 ).trim();

        } else {

            mustBe = "(must be \"(YYYY-MM-DD,YYYY-MM-DD)\")";

            if ( !trimmedDateString.startsWith( "(" ) || trimmedDateString.endsWith( ")" ) ) {

                throw new DateParsingException(
                        Reason.RANGE_INVALID,
                        "more than one separator in date range " + enquotedDateString +
                        " (must be exactly one semi-colon or exactly one colon)"
                );

            }

            if ( commaOffset >= 0 ) {

                separatorOffset = commaOffset;
                String tailPortion = trimmedDateString.substring( separatorOffset + 1 );
                if ( tailPortion.indexOf( ',' ) >= 0 ) {

                    tooManySeparators = true;

                }

            } else {

                noSeparators = true;

            }

            if ( tooManySeparators ) {

                throw new DateParsingException(
                        Reason.RANGE_INVALID,
                        "too many commas in date range " +
                        enquotedDateString +
                        " " +
                        mustBe
                );

            }

            if ( noSeparators ) {

                throw new DateParsingException(
                        Reason.RANGE_INVALID,
                        "comma in date range " +
                        enquotedDateString +
                        " " +
                        mustBe
                );

            }

            firstDateString = trimmedDateString.substring( 1, separatorOffset ).trim();
            secondDateString = trimmedDateString.substring( separatorOffset + 1, trimmedDateString.length() - 1 ).trim();

        }

//        if ( noSeparators ) {
//
//            if ( s_requireNewStyleDateRanges ) {
//
//                throw new ObtuseApproximateCalendarDate.DateParsingException(
//                        Reason.RANGE_INVALID,
//                        "no colon or semicolon in date range \"" +
//                        trimmedDateString +
//                        "\" (must be \"YYYY-MM-DD:YYYY-MM-DD\" or \"YYYY-MM-DD;YYYY-MM-DD\")"
//                );
//
//            } else {
//
//                throw new ObtuseApproximateCalendarDate.DateParsingException(
//                        Reason.RANGE_INVALID,
//                        "comma in date range \"" +
//                        trimmedDateString +
//                        "\" (must be \"(YYYY-MM-DD,YYYY-MM-DD)\")"
//                );
//
//            }
//
//        }
//
//        if ( tooManySeparators ) {
//
//            if ( s_requireNewStyleDateRanges ) {
//
//                throw new ObtuseApproximateCalendarDate.DateParsingException(
//                        Reason.RANGE_INVALID,
//                        "too many colons or semicolons in date range \"" +
//                        trimmedDateString +
//                        "\" (must be \"YYYY-MM-DD:YYYY-MM-DD\" or \"YYYY-MM-DD;YYYY-MM-DD\")"
//                );
//
//            } else {
//
//                throw new ObtuseApproximateCalendarDate.DateParsingException(
//                        Reason.RANGE_INVALID,
//                        "too many commas in date range \"" +
//                        trimmedDateString +
//                        "\" (must be \"(YYYY-MM-DD,YYYY-MM-DD)\")"
//                );
//
//            }
//
//        }

//        firstDateString = trimmedDateString.substring( 1, trimmedDateString.indexOf( ',' ) );
//        secondDateString = trimmedDateString.substring( trimmedDateString.indexOf( ',' ) + 1, trimmedDateString.length() - 1 );
//        if ( s_allowOldStyleDateRanges && ( trimmedDateString.startsWith( "(" ) && trimmedDateString.indexOf( ',' ) >= 0 && trimmedDateString.endsWith( ")" ) ) ) {
//
//            firstDateString = trimmedDateString.substring( 1, trimmedDateString.indexOf( ',' ) );
//            secondDateString = trimmedDateString.substring( trimmedDateString.indexOf( ',' ) + 1, trimmedDateString.length() - 1 );
//
//        }

//        {

//            if ( secondDateString.indexOf( ',' ) >= 0 ) {
//
//                throw new ObtuseApproximateCalendarDate.DateParsingException(
//                        Reason.RANGE_INVALID,
//                        "too many commas in date range \"" +
//                        trimmedDateString +
//                        "\" (must be \"(YYYY-MM-DD,YYYY-MM-DD)\")"
//                );
//
//            }

        Matcher firstDateMatcher = OACD_DATE_PATTERN.matcher( firstDateString );
        Matcher secondDateMatcher = OACD_DATE_PATTERN.matcher( secondDateString );

        if ( firstDateMatcher.matches() && secondDateMatcher.matches() ) {

            ObtuseCalendarDate firstDate;
            ObtuseCalendarDate secondDate;
            DateParsingException exception;

            try {

                firstDate = ObtuseCalendarDate.parseCalendarDate( firstDateString );

                try {

                    secondDate = ObtuseCalendarDate.parseCalendarDate( secondDateString );

                    if ( firstDate.compareTo( secondDate ) > 0 ) {

                        exception = new DateParsingException(
                                Reason.RANGE_BACKWARDS,
                                "starting date in date range is after ending date in date range " + enquotedDateString
                        );

                        ObtuseUtil.doNothing();

                    } else {

                        try {

                            return new ObtuseApproximateCalendarDate( firstDate, secondDate );

                        } catch ( Exception e ) {

                            throw new HowDidWeGetHereError( "exceptions should be impossible here (trimmed date string is " + enquotedDateString + ")", e );

                        }

                    }

                } catch ( IllegalArgumentException e ) {

                    exception = new DateParsingException(
                            Reason.RANGE_INVALID_ENDING_DATE,
                            "ending date in date range is invalid " +
                            enquotedDateString +
                            " " +
                            mustBe,
                            e
                    );

                    ObtuseUtil.doNothing();

                }

            } catch ( IllegalArgumentException e ) {

                exception = new DateParsingException(
                        Reason.RANGE_INVALID_ENDING_DATE,
                        "starting date in date range is invalid " +
                        enquotedDateString +
                        " " +
                        mustBe,
                        e
                );

                ObtuseUtil.doNothing();

            }

            // This if statement is intended to detect bugs in the above code.
            // Consequently, it being redundant is a 'good thing'.
            //noinspection ConstantConditions
            if ( exception == null ) {

                throw new HowDidWeGetHereError( "something went wrong - no idea what (trimmed date string is " + enquotedDateString + ")" );

            }

            throw exception;

        } else if ( !firstDateMatcher.matches() ) {

            throw new DateParsingException(
                    Reason.RANGE_INVALID_STARTING_DATE,
                    "starting date in date range is invalid " +
                    enquotedDateString +
                    " " +
                    mustBe
            );

        } else {

            throw new DateParsingException(
                    Reason.RANGE_INVALID_ENDING_DATE,
                    "ending date in date range is invalid " +
                    enquotedDateString +
                    " " +
                    mustBe
            );

        }

//        } else {
//
//            throw new ObtuseApproximateCalendarDate.DateParsingException(
//                    Reason.RANGE_INVALID,
//                    "invalid date range \"" + trimmedDateString + "\" " +
//                    mustBe
//            );
//
//        }

    }

    private static void checkParsing( @NotNull String dateString, @Nullable ObtuseApproximateCalendarDate expectedDate ) {

        try {

            ObtuseApproximateCalendarDate approximateDate = parse( dateString );
            if ( approximateDate.equals( expectedDate ) ) {

                Logger.logMsg( "\"" + dateString + "\" correctly parsed as \"" + approximateDate + "\"" );

            } else {

                Logger.logErr( "\"" + dateString + "\" incorrectly parsed as \"" + approximateDate + "\"" );

                ObtuseUtil.doNothing();

            }

        } catch ( Exception e ) {

            Logger.logErr( "exception parsing \"" + dateString + "\"", e );

            ObtuseUtil.doNothing();

        }

    }

    public static void main( String[] args ) {

        BasicProgramConfigInfo.init( "Obtuse", "Util", "Testing", null );

        Logger.logMsg( "unknown date is " + UNKNOWN_APPROXIMATE_DATE );

        checkParsing( "2000s", new ObtuseApproximateCalendarDate( parseCalendarDate( "2006-01-01" ), DatePrecision.DECADE ) );
        checkParsing( "2000", new ObtuseApproximateCalendarDate( parseCalendarDate( "2000-01-01" ), DatePrecision.YEAR ) );
        checkParsing( "2000-10", new ObtuseApproximateCalendarDate( parseCalendarDate( "2000-10-01" ), DatePrecision.MONTH ) );
        checkParsing( "2000-10-20", new ObtuseApproximateCalendarDate( parseCalendarDate( "2000-10-20" ), DatePrecision.DATE ) );
        if ( s_requireNewStyleDateRanges ) {

            checkParsing(
                    "2000-10-20;2015-10-20",
                    new ObtuseApproximateCalendarDate( parseCalendarDate( "2000-10-20" ), parseCalendarDate( "2015-10-20" ) )
            );

            checkParsing(
                    "2000-10-20:2015-10-20",
                    new ObtuseApproximateCalendarDate( parseCalendarDate( "2000-10-20" ), parseCalendarDate( "2015-10-20" ) )
            );

        } else {

            checkParsing(
                    "(2000-10-20,2015-10-20)",
                    new ObtuseApproximateCalendarDate( parseCalendarDate( "2000-10-20" ), parseCalendarDate( "2015-10-20" ) )
            );

        }

        Logger.logMsg( "new OACD(\"2016-08-03\",null) yields " +
                       new ObtuseApproximateCalendarDate( parseCalendarDate( "2016-08-03" ), (ObtuseCalendarDate)null ) );
        Logger.logMsg( "new OACD(null,\"2016-08-03\") yields " + new ObtuseApproximateCalendarDate( null, parseCalendarDate( "2016-08-03" ) ) );
        checkOverlap(
                new ObtuseApproximateCalendarDate( parseCalendarDate( "2005-03-07" ), ObtuseCalendarDate.getLatestSupportedDate() ),
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), parseCalendarDate( "2005-03-07" ) ),
                true
        );
        checkOverlap(
                new ObtuseApproximateCalendarDate( parseCalendarDate( "2005-01-01" ), ObtuseCalendarDate.getLatestSupportedDate() ),
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), parseCalendarDate( "2005-12-31" ) ),
                true
        );
        checkOverlap(
                new ObtuseApproximateCalendarDate( parseCalendarDate( "2005-03-07" ), ObtuseCalendarDate.getLatestSupportedDate() ),
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), parseCalendarDate( "2005-03-06" ) ),
                false
        );
        checkOverlap(
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), ObtuseCalendarDate.getLatestSupportedDate() ),
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), parseCalendarDate( "2005-03-07" ) ),
                true
        );
        checkOverlap(
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), ObtuseCalendarDate.getLatestSupportedDate() ),
                new ObtuseApproximateCalendarDate( parseCalendarDate( "2005-03-07" ), ObtuseCalendarDate.getLatestSupportedDate() ),
                true
        );
        checkOverlap(
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), ObtuseCalendarDate.getLatestSupportedDate() ),
                new ObtuseApproximateCalendarDate( ObtuseCalendarDate.getEarliestSupportedDate(), ObtuseCalendarDate.getLatestSupportedDate() ),
                true
        );

        equalsVsEquivalent(
                new ObtuseApproximateCalendarDate( parseCalendarDate( "2020-03-03" ), DatePrecision.MONTH ),
                new ObtuseApproximateCalendarDate( parseCalendarDate( "2020-03-01" ), parseCalendarDate( "2020-03-31" ) ),
                false,
                true
        );

// test the tester (verify that it correctly handles incorrect results)
//
//	equalsVsEquivalent(
//		new ObtuseApproximateCalendarDate( parse( "2020-03-03" ), DatePrecision.MONTH ),
//		new ObtuseApproximateCalendarDate( parse( "2020-03-01" ), parse( "2020-03-31" ) ),
//		true,
//		true
//	);
//	equalsVsEquivalent(
//		new ObtuseApproximateCalendarDate( parse( "2020-03-03" ), DatePrecision.MONTH ),
//		new ObtuseApproximateCalendarDate( parse( "2020-03-01" ), parse( "2020-03-31" ) ),
//		false,
//		false
//	);
//	equalsVsEquivalent(
//		new ObtuseApproximateCalendarDate( parse( "2020-03-03" ), DatePrecision.MONTH ),
//		new ObtuseApproximateCalendarDate( parse( "2020-03-01" ), parse( "2020-03-31" ) ),
//		true,
//		false
//	);

        Random rng = new Random( 12321421 );
        for ( DatePrecision precision : DatePrecision.values() ) {

            if ( precision != DatePrecision.RANGE ) {

                for ( int year = 1980; year < 2020; year += 1 ) {

                    try {

                        ObtuseCalendarDate calendarDate = new ObtuseCalendarDate(
                                "" +
                                year +
                                "-" + ObtuseUtil.lpad( 1 + rng.nextInt( 12 ), 2, '0' ) +
                                "-" + ObtuseUtil.lpad( 1 + rng.nextInt( 28 ), 2, '0' ) +
                                ""
                        );

                        ObtuseApproximateCalendarDate date = new ObtuseApproximateCalendarDate(
                                calendarDate,
                                precision
                        );

                        Logger.logMsg(
                                "" +
                                calendarDate +
                                " (" + date.getEarliestPossibleDate() + " <> " + date.getLatestPossibleDate() + ")" +
                                " rounds with " +
                                precision.name().toLowerCase() +
                                " precision formats as " +
                                date.format()
                        );

                    } catch ( com.obtuse.util.exceptions.ParsingException e ) {

                        e.printStackTrace();

                    }

                }

                checkOverlap( "2010-10-10", null, "2010-10-10", null, true );

                checkOverlap( "2012-10-10", DatePrecision.DATE, "2012-10-10", null, true );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-10-01", null, true );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2012-01-01", null, true );
                checkOverlap( "2012-10-10", DatePrecision.DECADE, "2010-01-01", null, true );

                checkOverlap( "2012-10-10", DatePrecision.DATE, "2012-10-10", null, true );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-10-31", null, true );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2012-12-31", null, true );
                checkOverlap( "2012-10-10", DatePrecision.DECADE, "2019-12-31", null, true );

                // as close to overlapping as possible but not actually overlapping

                checkOverlap( "2012-10-10", DatePrecision.DATE, "2012-10-09", DatePrecision.DATE, false );
                checkOverlap( "2012-10-01", DatePrecision.DATE, "2012-09-30", DatePrecision.MONTH, false );
                checkOverlap( "2012-01-01", DatePrecision.DATE, "2011-12-31", DatePrecision.YEAR, false );
                checkOverlap( "2010-01-01", DatePrecision.DATE, "2009-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-09-30", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-09-30", DatePrecision.MONTH, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2011-12-31", DatePrecision.YEAR, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2009-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2011-12-31", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2011-12-31", DatePrecision.MONTH, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2011-12-31", DatePrecision.YEAR, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2009-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.DECADE, "2009-12-31", null, false );

                checkOverlap( "2012-10-10", DatePrecision.DATE, "2012-10-11", DatePrecision.DATE, false );
                checkOverlap( "2012-10-31", DatePrecision.DATE, "2012-11-01", DatePrecision.MONTH, false );
                checkOverlap( "2012-12-31", DatePrecision.DATE, "2013-01-01", DatePrecision.YEAR, false );
                checkOverlap( "2019-12-31", DatePrecision.DATE, "2020-01-01", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-11-01", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-11-01", DatePrecision.MONTH, false );
                checkOverlap( "2012-12-10", DatePrecision.MONTH, "2013-01-01", DatePrecision.YEAR, false );
                checkOverlap( "2019-12-10", DatePrecision.MONTH, "2009-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2013-01-01", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2013-01-01", DatePrecision.MONTH, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2013-01-01", DatePrecision.YEAR, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2020-01-01", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.DECADE, "2020-01-01", null, false );

                // non-zero gap

                checkOverlap( "2012-10-10", DatePrecision.DATE, "2012-10-08", DatePrecision.DATE, false );
                checkOverlap( "2012-10-01", DatePrecision.DATE, "2012-08-31", DatePrecision.MONTH, false );
                checkOverlap( "2012-01-01", DatePrecision.DATE, "2010-12-31", DatePrecision.YEAR, false );
                checkOverlap( "2010-01-01", DatePrecision.DATE, "1999-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-09-30", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-08-31", DatePrecision.MONTH, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2010-12-31", DatePrecision.YEAR, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "1999-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2011-12-30", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2011-11-30", DatePrecision.MONTH, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2010-12-31", DatePrecision.YEAR, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "1999-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.DECADE, "2009-12-31", null, false );

                checkOverlap( "2012-10-10", DatePrecision.DATE, "2012-10-12", DatePrecision.DATE, false );
                checkOverlap( "2012-10-31", DatePrecision.DATE, "2012-12-01", DatePrecision.MONTH, false );
                checkOverlap( "2012-12-31", DatePrecision.DATE, "2014-01-01", DatePrecision.YEAR, false );
                checkOverlap( "2019-12-31", DatePrecision.DATE, "2030-01-01", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-11-02", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.MONTH, "2012-12-01", DatePrecision.MONTH, false );
                checkOverlap( "2012-12-10", DatePrecision.MONTH, "2014-01-01", DatePrecision.YEAR, false );
                checkOverlap( "2019-12-10", DatePrecision.MONTH, "2009-12-31", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2013-01-01", DatePrecision.DATE, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2013-02-01", DatePrecision.MONTH, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2014-01-01", DatePrecision.YEAR, false );
                checkOverlap( "2012-10-10", DatePrecision.YEAR, "2030-01-01", DatePrecision.DECADE, false );

                checkOverlap( "2012-10-10", DatePrecision.DECADE, "2030-01-01", null, false );

            }

        }

        checkIncludes( "2010-10-10", DatePrecision.DATE, "2010-10-08", false );
        checkIncludes( "2010-10-10", DatePrecision.DATE, "2010-10-09", false );
        checkIncludes( "2010-10-10", DatePrecision.DATE, "2010-10-10", true );
        checkIncludes( "2010-10-10", DatePrecision.DATE, "2010-10-11", false );
        checkIncludes( "2010-10-10", DatePrecision.DATE, "2010-10-12", false );

        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-09-29", false );
        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-09-30", false );
        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-10-01", true );
        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-10-02", true );
        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-10-30", true );
        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-10-31", true );
        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-11-01", false );
        checkIncludes( "2010-10-10", DatePrecision.MONTH, "2010-11-02", false );

        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2009-12-30", false );
        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2009-12-31", false );
        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2010-01-01", true );
        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2010-01-02", true );
        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2010-12-30", true );
        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2010-12-31", true );
        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2011-01-01", false );
        checkIncludes( "2010-10-10", DatePrecision.YEAR, "2011-01-02", false );

        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2009-12-30", false );
        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2009-12-31", false );
        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2010-01-01", true );
        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2010-01-02", true );
        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2019-12-30", true );
        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2019-12-31", true );
        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2020-01-01", false );
        checkIncludes( "2010-10-10", DatePrecision.DECADE, "2020-01-02", false );

    }

    private static void equalsVsEquivalent(
            ObtuseApproximateCalendarDate lhs,
            ObtuseApproximateCalendarDate rhs,
            @SuppressWarnings("SameParameterValue") boolean equalsOracle,
            @SuppressWarnings("SameParameterValue") boolean equivalentOracle
    ) {

        boolean equalsResult = lhs.equals( rhs );
        boolean equivalentResult = lhs.equivalent( rhs );
        if ( equalsOracle == equalsResult && equivalentOracle == equivalentResult ) {

            Logger.logMsg(
                    "" + lhs + " vs " + rhs + ( equalsOracle ? " are" : " are not" ) + " equals and" +
                    ( equivalentOracle ? " are" : " are not" ) + " equivalent"
            );

        } else if ( equalsOracle == equalsResult ) {

            Logger.logErr(
                    "" + lhs + " vs " + rhs + ( equalsOracle ? " are" : " are not" ) + " equals and" +
                    ( equivalentOracle ? " are" : " are not" ) + " equivalent",
                    new IllegalArgumentException( " - equivalent() disagrees" )
            );

        } else if ( equivalentOracle == equivalentResult ) {

            Logger.logErr(
                    "" + lhs + " vs " + rhs + ( equalsOracle ? " are" : " are not" ) + " equals and" +
                    ( equivalentOracle ? " are" : " are not" ) + " equivalent",
                    new IllegalArgumentException( " - equals() disagrees" )
            );

        } else {

            Logger.logErr(
                    "" + lhs + " vs " + rhs + ( equalsOracle ? " are" : " are not" ) + " equals and" +
                    ( equivalentOracle ? " are" : " are not" ) + " equivalent",
                    new IllegalArgumentException( " - both equals and equivalent disagree" )
            );

        }

    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean checkIncludes(
            @SuppressWarnings("SameParameterValue") String lhsDateString,
            DatePrecision precision,
            String rhsDateString,
            boolean oracle
    ) {

        try {

            ObtuseApproximateCalendarDate lhs = new ObtuseApproximateCalendarDate( parseCalendarDate( lhsDateString ), precision );
            ObtuseCalendarDate rhs = parseCalendarDate( rhsDateString );

            boolean result = lhs.includes( rhs );
            if ( result == oracle ) {

                if ( s_verboseTesting ) {

                    Logger.logMsg( "[ " +
                                   lhs.getEarliestPossibleDate() +
                                   ", " +
                                   lhs.getLatestPossibleDate() +
                                   " ] includes [ " +
                                   rhs +
                                   " ] yielded correct " +
                                   result );

                }

                return true;

            } else {

                Logger.logErr( "[ " +
                               lhs.getEarliestPossibleDate() +
                               ", " +
                               lhs.getLatestPossibleDate() +
                               " ] includes [ " +
                               rhs +
                               " ] yielded incorrect " +
                               result, new IllegalArgumentException( "include oops" ) );

                return false;

            }

        } catch ( Throwable e ) {

            Logger.logErr( "" + lhsDateString + "@" + precision + " includes " + rhsDateString + " blew up", e );

            return false;

        }

    }

    private static boolean checkOverlap(
            String lhsDateString,
            DatePrecision lhsPrecision,
            String rhsDateString,
            DatePrecision rhsPrecision,
            boolean oracle
    ) {

        if ( lhsPrecision == null || rhsPrecision == null ) {

            boolean rval = true;

            for ( DatePrecision p : DatePrecision.values() ) {

                if ( p != DatePrecision.RANGE ) {

                    if ( lhsPrecision == null ) {

                        if ( !checkOverlap( lhsDateString, p, rhsDateString, rhsPrecision, oracle ) ) {

                            rval = false;

                        }

                    } else {

                        if ( !checkOverlap( lhsDateString, lhsPrecision, rhsDateString, p, oracle ) ) {

                            rval = false;

                        }

                    }

                }

            }

            return rval;

        }

        try {

            ObtuseApproximateCalendarDate lhs = new ObtuseApproximateCalendarDate( parseCalendarDate( lhsDateString ), lhsPrecision );
            ObtuseApproximateCalendarDate rhs = new ObtuseApproximateCalendarDate( parseCalendarDate( rhsDateString ), rhsPrecision );

            return checkOverlap( lhs, rhs, oracle );

        } catch ( Throwable e ) {

            Logger.logErr( "" + lhsDateString + "@" + lhsPrecision + " overlaps " + rhsDateString + "@" + rhsPrecision + " blew up", e );

            return false;

        }

    }

    private static boolean checkOverlap(
            ObtuseApproximateCalendarDate lhs,
            ObtuseApproximateCalendarDate rhs,
            boolean oracle
    ) {

        boolean result1 = lhs.overlaps( rhs );
        boolean result2 = rhs.overlaps( lhs );

        if ( result1 != result2 ) {

            Logger.logErr( "" + lhs + " overlaps " + rhs + " yielded " + result1 + " but " + rhs + " overlaps " + lhs + " yielded " + result2 );

            return false;

        }

        if ( result1 == oracle ) {

            if ( s_verboseTesting ) {

                Logger.logMsg(
                        "[ " + lhs.getEarliestPossibleDate() + ", " + lhs.getLatestPossibleDate() + " ] " +
                        "overlaps [ " + rhs.getEarliestPossibleDate() + ", " + rhs.getLatestPossibleDate() + " ] " +
                        "yielded correct " + result1
                );

            }
//		Logger.logMsg( "" + lhs.format() + " overlaps " + rhs.format() + " yielded correct " + result1 );

            return true;

        }

        Logger.logErr( "" +
                       lhs.getNominalCalendarDate() +
                       "@" +
                       lhs.getPrecision() +
                       " overlaps " +
                       rhs.getNominalCalendarDate() +
                       "@" +
                       rhs.getPrecision() +
                       " yielded " +
                       result1 +
                       " but oracle expected " +
                       oracle, new IllegalArgumentException( "overlaps oops" ) );

        return false;

    }


}