package net.homeip.tedk.webkitnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class TaskerFireReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
			boolean start = false;
			Bundle e = intent.getExtras();
			if (e != null) {

				Bundle b = e
						.getBundle("com.twofortyfouram.locale.intent.extra.BUNDLE");
				if (b != null) {
					start = b.getBoolean("start");
				}
			}
			final Intent serviceIntent = new Intent(context, NotificationService.class);
			if(start)
			{
				context.startService(serviceIntent);
				Log.d("TaskerFireReceiver", "start");
			}
			else
			{
				context.stopService(serviceIntent);
				Log.d("TaskerFireReceiver", "stop");
			}
		}

}
