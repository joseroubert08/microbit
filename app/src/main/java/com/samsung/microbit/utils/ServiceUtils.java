package com.samsung.microbit.utils;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.NameValuePair;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;

import static com.samsung.microbit.BuildConfig.DEBUG;

public class ServiceUtils {
    private ServiceUtils() {
    }

    private static void logi(Class serviceClass, String message) {
        if(serviceClass.equals(IPCService.class)) {
            IPCService.logi(message);
        } else if(serviceClass.equals(PluginService.class)) {
            PluginService.logi(message);
        } else if(serviceClass.equals(BLEService.class)) {
            BLEService.logi(message);
        }
    }

    public static void sendtoIPCService(Class serviceClass, int mbsService, int functionCode, CmdArg cmd,
                                        NameValuePair[] args) {
        if (DEBUG) {
            if(cmd != null) {
                logi(serviceClass, serviceClass.getName() + ": sendtoIPCService(), " + mbsService + "," +
                        functionCode + "," +
                        "(" + cmd.getValue() + "," + cmd.getCMD() + "");
            } else {
                logi(serviceClass, serviceClass.getName() + ": sendtoIPCService(), " + mbsService + "," + functionCode);
            }
        }

        IPCMessageManager.sendIPCMessage(IPCService.class, mbsService, functionCode, cmd, args);
    }

    public static void sendtoPluginService(Class serviceClass, int mbsService, int functionCode, CmdArg cmd,
                                        NameValuePair[] args) {
        if (DEBUG) {
            if(cmd != null) {
                logi(serviceClass, serviceClass.getName() + ": sendtoPluginService(), " + mbsService + "," +
                        functionCode + "," +
                        "(" + cmd.getValue() + "," + cmd.getCMD() + "");
            } else {
                logi(serviceClass, serviceClass.getName() + ": sendtoPluginService(), " + mbsService + "," + functionCode);
            }
        }

        IPCMessageManager.sendIPCMessage(PluginService.class, mbsService, functionCode, cmd, args);
    }

    public static void sendtoBLEService(Class serviceClass, int mbsService, int functionCode, CmdArg cmd,
                                        NameValuePair[] args) {
        if (DEBUG) {
            if(cmd != null) {
                logi(serviceClass, serviceClass.getName() + ": sendtoBLEService(), " + mbsService + "," + functionCode + "," + cmd
                        .getValue() + "," + cmd.getCMD() + "");
            } else {
                logi(serviceClass, serviceClass.getName() + ": sendtoBLEService(), " + mbsService + "," + functionCode);
            }
        }

        IPCMessageManager.sendIPCMessage(BLEService.class, mbsService, functionCode, cmd, args);
    }
}
