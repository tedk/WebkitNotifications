package net.homeip.tedk.webkitnotifications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		boolean running = isMyServiceRunning();
		
		final Button startButton = (Button) findViewById(R.id.startButton);
		final Button stopButton = (Button) findViewById(R.id.stopButton);
		
		startButton.setEnabled(!running);
		stopButton.setEnabled(running);
		
		final Intent serviceIntent = new Intent(MainActivity.this, NotificationService.class);
		
		startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startButton.setEnabled(false);
				startService(serviceIntent);
				stopButton.setEnabled(true);
			}
		});
		
		stopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				stopButton.setEnabled(false);
				stopService(serviceIntent);
				startButton.setEnabled(true);
			}
		});
	}
	
	// http://www.mobile-web-consulting.de/post/5272654457/android-check-if-a-service-is-running
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("net.homeip.tedk.webkitnotifications.NotificationService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivity(new Intent(MainActivity.this, SettingsActivity.class));
			return true;
		case R.id.action_enable:
			startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
