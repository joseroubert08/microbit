package com.samsung.microbit.plugin;

import com.samsung.microbit.data.model.CmdArg;

public interface AbstractPlugin {
    void handleEntry(CmdArg cmd);
    void destroy();
}
