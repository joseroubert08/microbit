package com.samsung.microbit.service;

import android.app.Activity;

import com.samsung.microbit.ui.NotificationActivity;

import no.nordicsemi.android.dfu.DfuBaseService;

public class DfuService extends DfuBaseService {

	@Override
	protected Class<? extends Activity> getNotificationTarget() {
		// TODO Auto-generated method stub
		return NotificationActivity.class;
	}

}
