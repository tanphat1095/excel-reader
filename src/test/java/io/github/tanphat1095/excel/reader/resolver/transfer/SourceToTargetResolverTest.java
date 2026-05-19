package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all individual SourceToTargetResolver implementations.
 * No POI is required — resolvers operate on plain Java values.
 */
class SourceToTargetResolverTest {

    // -------------------------------------------------------------------------
    // DoubleToStringResolver
    // -------------------------------------------------------------------------

    @Nested
    class DoubleToStringResolverTest {

        private final DoubleToStringResolver resolver = new DoubleToStringResolver();

        @Test
        void wholeNumberShouldNotHaveDecimalPoint() {
            assertEquals("42", resolver.resolve(42.0));
        }

        @Test
        void negativeWholeNumberShouldNotHaveDecimalPoint() {
            assertEquals("-7", resolver.resolve(-7.0));
        }

        @Test
        void decimalNumberShouldPreserveDecimalPart() {
            assertEquals("3.14", resolver.resolve(3.14));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void supportsShouldMatchDoubleToString() {
            assertTrue(resolver.supports(Double.class, String.class));
        }

        @Test
        void supportsShouldNotMatchOtherPairs() {
            assertFalse(resolver.supports(Double.class, Integer.class));
        }
    }

    // -------------------------------------------------------------------------
    // BooleanToStringResolver
    // -------------------------------------------------------------------------

    @Nested
    class BooleanToStringResolverTest {

        private final BooleanToStringResolver resolver = new BooleanToStringResolver();

        @Test
        void trueShouldReturnTrueString() {
            assertEquals("true", resolver.resolve(Boolean.TRUE));
        }

        @Test
        void falseShouldReturnFalseString() {
            assertEquals("false", resolver.resolve(Boolean.FALSE));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void supportsShouldMatchBooleanToString() {
            assertTrue(resolver.supports(Boolean.class, String.class));
        }
    }

    // -------------------------------------------------------------------------
    // DateToStringResolver
    // -------------------------------------------------------------------------

    @Nested
    class DateToStringResolverTest {

        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        private final DateToStringResolver resolver = new DateToStringResolver(fmt);

        @Test
        void shouldFormatDateWithPattern() {
            LocalDate ld = LocalDate.of(2024, 3, 15);
            Date date = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());

            assertEquals("15-03-2024", resolver.resolve(date));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void supportsShouldMatchDateToString() {
            assertTrue(resolver.supports(Date.class, String.class));
        }
    }

    // -------------------------------------------------------------------------
    // DoubleToIntegerResolver
    // -------------------------------------------------------------------------

    @Nested
    class DoubleToIntegerResolverTest {

        private final DoubleToIntegerResolver resolver = new DoubleToIntegerResolver();

        @Test
        void wholeNumberShouldConvertExactly() {
            assertEquals(5, resolver.resolve(5.0));
        }

        @Test
        void decimalPartShouldBeTruncated() {
            assertEquals(3, resolver.resolve(3.9));
        }

        @Test
        void negativeDecimalShouldTruncateTowardZero() {
            assertEquals(-3, resolver.resolve(-3.9));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void supportsShouldMatchDoubleToInteger() {
            assertTrue(resolver.supports(Double.class, Integer.class));
        }
    }

    // -------------------------------------------------------------------------
    // DoubleToLongResolver
    // -------------------------------------------------------------------------

    @Nested
    class DoubleToLongResolverTest {

        private final DoubleToLongResolver resolver = new DoubleToLongResolver();

        @Test
        void wholeNumberShouldConvertExactly() {
            assertEquals(100L, resolver.resolve(100.0));
        }

        @Test
        void decimalPartShouldBeTruncated() {
            assertEquals(99L, resolver.resolve(99.99));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void supportsShouldMatchDoubleToLong() {
            assertTrue(resolver.supports(Double.class, Long.class));
        }
    }

    // -------------------------------------------------------------------------
    // DoubleToBigDecimalResolver
    // -------------------------------------------------------------------------

    @Nested
    class DoubleToBigDecimalResolverTest {

        private final DoubleToBigDecimalResolver resolver = new DoubleToBigDecimalResolver();

        @Test
        void shouldConvertViaValueOf() {
            assertEquals(BigDecimal.valueOf(3.14), resolver.resolve(3.14));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void supportsShouldMatchDoubleToBigDecimal() {
            assertTrue(resolver.supports(Double.class, BigDecimal.class));
        }
    }

    // -------------------------------------------------------------------------
    // StringToDoubleResolver
    // -------------------------------------------------------------------------

    @Nested
    class StringToDoubleResolverTest {

        private final StringToDoubleResolver resolver = new StringToDoubleResolver();

        @Test
        void validStringShouldParse() {
            assertEquals(3.14, resolver.resolve("3.14"));
        }

        @Test
        void integerStringShouldParse() {
            assertEquals(42.0, resolver.resolve("42"));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void blankShouldReturnNull() {
            assertNull(resolver.resolve("   "));
        }

        @Test
        void invalidStringShouldThrow() {
            assertThrows(ExcelReaderException.class, () -> resolver.resolve("abc"));
        }

        @Test
        void supportsShouldMatchStringToDouble() {
            assertTrue(resolver.supports(String.class, Double.class));
        }
    }

    // -------------------------------------------------------------------------
    // StringToIntegerResolver
    // -------------------------------------------------------------------------

    @Nested
    class StringToIntegerResolverTest {

        private final StringToIntegerResolver resolver = new StringToIntegerResolver();

        @Test
        void integerStringShouldParse() {
            assertEquals(7, resolver.resolve("7"));
        }

        @Test
        void decimalStringShouldTruncate() {
            // "42.9" → parsed as double 42.9 → cast to int → 42
            assertEquals(42, resolver.resolve("42.9"));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void blankShouldReturnNull() {
            assertNull(resolver.resolve("  "));
        }

        @Test
        void invalidStringShouldThrow() {
            assertThrows(ExcelReaderException.class, () -> resolver.resolve("xyz"));
        }

        @Test
        void supportsShouldMatchStringToInteger() {
            assertTrue(resolver.supports(String.class, Integer.class));
        }
    }

    // -------------------------------------------------------------------------
    // StringToLongResolver
    // -------------------------------------------------------------------------

    @Nested
    class StringToLongResolverTest {

        private final StringToLongResolver resolver = new StringToLongResolver();

        @Test
        void validStringShouldParse() {
            assertEquals(1000L, resolver.resolve("1000"));
        }

        @Test
        void decimalStringShouldTruncate() {
            assertEquals(99L, resolver.resolve("99.7"));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void blankShouldReturnNull() {
            assertNull(resolver.resolve(""));
        }

        @Test
        void invalidStringShouldThrow() {
            assertThrows(ExcelReaderException.class, () -> resolver.resolve("abc"));
        }

        @Test
        void supportsShouldMatchStringToLong() {
            assertTrue(resolver.supports(String.class, Long.class));
        }
    }

    // -------------------------------------------------------------------------
    // StringToBigDecimalResolver
    // -------------------------------------------------------------------------

    @Nested
    class StringToBigDecimalResolverTest {

        private final StringToBigDecimalResolver resolver = new StringToBigDecimalResolver();

        @Test
        void validStringShouldParse() {
            assertEquals(new BigDecimal("3.14"), resolver.resolve("3.14"));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void blankShouldReturnNull() {
            assertNull(resolver.resolve("  "));
        }

        @Test
        void invalidStringShouldThrow() {
            assertThrows(ExcelReaderException.class, () -> resolver.resolve("not-a-number"));
        }

        @Test
        void supportsShouldMatchStringToBigDecimal() {
            assertTrue(resolver.supports(String.class, BigDecimal.class));
        }
    }

    // -------------------------------------------------------------------------
    // StringToBooleanResolver
    // -------------------------------------------------------------------------

    @Nested
    class StringToBooleanResolverTest {

        private final StringToBooleanResolver resolver = new StringToBooleanResolver();

        @Test
        void trueLiteralShouldReturnTrue() {
            assertTrue(resolver.resolve("true"));
        }

        @Test
        void yesShouldReturnTrue() {
            assertTrue(resolver.resolve("yes"));
        }

        @Test
        void oneStringShouldReturnTrue() {
            assertTrue(resolver.resolve("1"));
        }

        @Test
        void yShouldReturnTrue() {
            assertTrue(resolver.resolve("y"));
        }

        @Test
        void falseLiteralShouldReturnFalse() {
            assertFalse(resolver.resolve("false"));
        }

        @Test
        void noShouldReturnFalse() {
            assertFalse(resolver.resolve("no"));
        }

        @Test
        void zeroStringShouldReturnFalse() {
            assertFalse(resolver.resolve("0"));
        }

        @Test
        void nShouldReturnFalse() {
            assertFalse(resolver.resolve("n"));
        }

        @Test
        void caseInsensitiveShouldWork() {
            assertTrue(resolver.resolve("TRUE"));
            assertTrue(resolver.resolve("Yes"));
            assertFalse(resolver.resolve("FALSE"));
            assertFalse(resolver.resolve("NO"));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void blankShouldReturnNull() {
            assertNull(resolver.resolve("  "));
        }

        @Test
        void unknownValueShouldThrow() {
            assertThrows(ExcelReaderException.class, () -> resolver.resolve("maybe"));
        }

        @Test
        void supportsShouldMatchStringToBoolean() {
            assertTrue(resolver.supports(String.class, Boolean.class));
        }
    }

    // -------------------------------------------------------------------------
    // DateToLocalDateTimeResolver
    // -------------------------------------------------------------------------

    @Nested
    class DateToLocalDateTimeResolverTest {

        private final DateToLocalDateTimeResolver resolver = new DateToLocalDateTimeResolver();

        @Test
        void shouldConvertToLocalDateTime() {
            LocalDateTime expected = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            Date date = Date.from(expected.atZone(ZoneId.systemDefault()).toInstant());

            assertEquals(expected, resolver.resolve(date));
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void supportsShouldMatchDateToLocalDateTime() {
            assertTrue(resolver.supports(Date.class, LocalDateTime.class));
        }
    }

    // -------------------------------------------------------------------------
    // StringToLocalDateTimeResolver
    // -------------------------------------------------------------------------

    @Nested
    class StringToLocalDateTimeResolverTest {

        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        private final StringToLocalDateTimeResolver resolver = new StringToLocalDateTimeResolver(fmt);

        @Test
        void validStringShouldParse() {
            assertEquals(
                    LocalDateTime.of(2024, 3, 15, 9, 30, 0),
                    resolver.resolve("15-03-2024 09:30:00")
            );
        }

        @Test
        void nullShouldReturnNull() {
            assertNull(resolver.resolve(null));
        }

        @Test
        void blankShouldReturnNull() {
            assertNull(resolver.resolve("   "));
        }

        @Test
        void invalidFormatShouldThrow() {
            assertThrows(ExcelReaderException.class, () -> resolver.resolve("2024/03/15 09:30:00"));
        }

        @Test
        void supportsShouldMatchStringToLocalDateTime() {
            assertTrue(resolver.supports(String.class, LocalDateTime.class));
        }
    }
}
