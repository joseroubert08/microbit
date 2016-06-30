package com.samsung.microbit.core.bluetooth;

public interface UnexpectedConnectionEventListener {
	void handleConnectionEvent(int event, boolean gattForceClosed);
}

