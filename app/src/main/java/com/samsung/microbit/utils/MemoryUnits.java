package com.samsung.microbit.utils;

public abstract class MemoryUnits {
    private MemoryUnits() {
    }

    private static final long CONVERSION_VALUE = 1024;

    public abstract long toBytes(long value);

    public abstract long toKilobytes(long value);

    public abstract long toMegabytes(long value);

    public abstract long toGigabytes(long value);

    public abstract long toTerabytes(long value);

    public static class Bytes extends MemoryUnits {

        @Override
        public long toBytes(long value) {
            return value;
        }

        @Override
        public long toKilobytes(long value) {
            return value / CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE);
        }

        @Override
        public long toGigabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE);
        }

        @Override
        public long toTerabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE *
                    CONVERSION_VALUE);
        }

        public static MemoryUnits instance() {
            return new Bytes();
        }
    }

    public static class Kilobytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value;
        }

        @Override
        public long toMegabytes(long value) {
            return value / CONVERSION_VALUE;
        }

        @Override
        public long toGigabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE);
        }

        @Override
        public long toTerabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE);
        }

        public static MemoryUnits instance() {
            return new Kilobytes();
        }
    }

    public static class Megabytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value;
        }

        @Override
        public long toGigabytes(long value) {
            return value / CONVERSION_VALUE;
        }

        @Override
        public long toTerabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE);
        }

        public static MemoryUnits instance() {
            return new Megabytes();
        }
    }

    public static class Gigabytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toGigabytes(long value) {
            return value;
        }

        @Override
        public long toTerabytes(long value) {
            return value / CONVERSION_VALUE;
        }

        public static MemoryUnits instance() {
            return new Gigabytes();
        }
    }

    public static class Terabytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE *
                    CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toGigabytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toTerabytes(long value) {
            return value;
        }

        public static MemoryUnits instance() {
            return new Terabytes();
        }
    }
}
