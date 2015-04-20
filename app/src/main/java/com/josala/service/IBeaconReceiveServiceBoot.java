package com.josala.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IBeaconReceiveServiceBoot extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			ScanService.actionStart(context, ScanService.class);
		}
		
	}

}
