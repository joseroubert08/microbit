package com.samsung.microbit;

import android.app.Activity;
import no.nordicsemi.android.dfu.DfuBaseService;

public class DfuService extends DfuBaseService {

	@Override
	protected Class<? extends Activity> getNotificationTarget() {
		// TODO Auto-generated method stub
		return NotificationActivity.class;
	}

}
