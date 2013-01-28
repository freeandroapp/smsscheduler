package com.freeandroapp.smsscheduler.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.airpush.android.Airpush;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1)
	{
		new Airpush(arg0,"100248","1346166928111406435",false,true,true);
	}
}
