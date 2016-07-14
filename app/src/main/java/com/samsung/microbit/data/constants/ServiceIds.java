package com.samsung.microbit.data.constants;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.samsung.microbit.data.constants.ServiceIds.*;

@Retention(RetentionPolicy.RUNTIME)
@IntDef({SERVICE_NONE, SERVICE_PLUGIN, SERVICE_BLE})
public @interface ServiceIds {
    int SERVICE_NONE = 0;
    int SERVICE_PLUGIN = 1;
    int SERVICE_BLE = 2;
}
