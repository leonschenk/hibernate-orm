/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link Duration}, which is represented internally
 * as ({@code long seconds}, {@code int nanoseconds}), approximately
 * 28 decimal digits of precision. This quantity must be stored in
 * the database as a single integer with units of nanoseconds, since
 * the ANSI SQL {@code interval} type is not well-supported.
 *
 * In practice, the 19 decimal digits of a SQL {@code bigint} are
 * capable of representing six centuries in nanoseconds and are
 * sufficient for many applications. However, by default, we map
 * Java {@link Duration} to SQL {@code numeric(21,6)} here, which
 * can comfortably represent 60 millenia of nanos.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class DurationJavaType extends AbstractClassJavaType<Duration> {
	/**
	 * Singleton access
	 */
	public static final DurationJavaType INSTANCE = new DurationJavaType();
	private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance( Locale.ENGLISH );
	private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = new ThreadLocal<>() {
		@Override
		protected DecimalFormat initialValue() {
			return new DecimalFormat( "0.000000000", DECIMAL_FORMAT_SYMBOLS );
		}
	};

	public DurationJavaType() {
		super( Duration.class, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( context.getPreferredSqlTypeCodeForDuration() );
	}

	@Override
	public String toString(Duration value) {
		if ( value == null ) {
			return null;
		}
		String seconds = String.valueOf( value.getSeconds() );
		String nanos = String.valueOf( value.getNano() );
		String zeros = StringHelper.repeat( '0', 9 - nanos.length() );
		return seconds + zeros + nanos;
	}

	@Override
	public Duration fromString(CharSequence string) {
		if ( string == null ) {
			return null;
		}
		int cutoff = string.length() - 9;
		return Duration.ofSeconds(
				Long.parseLong( string.subSequence( 0, cutoff ).toString() ),
				Long.parseLong( string.subSequence( cutoff, string.length() ).toString() )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(Duration duration, Class<X> type, WrapperOptions options) {
		if ( duration == null ) {
			return null;
		}

		if ( Duration.class.isAssignableFrom( type ) ) {
			return (X) duration;
		}

		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return (X) new BigDecimal( duration.getSeconds() )
					.movePointRight( 9 )
					.add( new BigDecimal( duration.getNano() ) )
					.movePointLeft( 9 );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) duration.toString();
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( duration.toNanos() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Duration wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if (value instanceof Duration) {
			return (Duration) value;
		}

		if (value instanceof BigDecimal) {
			return fromDecimal( value );
		}

		if (value instanceof Double) {
			return fromDecimal( value );
		}

		if (value instanceof Long) {
			return Duration.ofNanos( (Long) value );
		}

		if (value instanceof String) {
			return Duration.parse( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}

	private Duration fromDecimal(Object number) {
		final String formatted = DECIMAL_FORMAT.get().format( number );
		final int dotIndex = formatted.indexOf( '.' );
		if (dotIndex == -1) {
			return Duration.ofSeconds( Long.parseLong( formatted ) );
		}
		return Duration.ofSeconds(
				Long.parseLong( formatted.substring( 0, dotIndex ) ),
				Long.parseLong( formatted.substring( dotIndex + 1 ) )
		);
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		if ( jdbcType.getDefaultSqlTypeCode() == SqlTypes.INTERVAL_SECOND ) {
			// Usually the maximum precision for interval types
			return 18;
		}
		else {
			// 19+9 = 28 digits is the maximum possible Duration
			// precision, but is an unnecessarily large default,
			// except for cosmological applications. Thirty
			// millenia in both timelike directions should be
			// sufficient time for most businesses!
			return Math.min( 21, dialect.getDefaultDecimalPrecision() );
		}
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 9;
	}
}
