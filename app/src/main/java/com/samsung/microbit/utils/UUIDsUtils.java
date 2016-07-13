package com.samsung.microbit.utils;

import java.util.UUID;

/**
 * Provides common functionality to work with UUID values.
 * For example, make UUID value from String and etc.
 */
public class UUIDsUtils {
    private UUIDsUtils() {
    }

    public static UUID makeUUID(String baseUUID, long shortUUID) {
        UUID u = UUID.fromString(baseUUID);
        long msb = u.getMostSignificantBits();
        long mask = 0x0ffffL;
        shortUUID &= mask;
        msb &= ~(mask << 32);
        msb |= (shortUUID << 32);
        u = new UUID(msb, u.getLeastSignificantBits());
        return u;
    }
}
