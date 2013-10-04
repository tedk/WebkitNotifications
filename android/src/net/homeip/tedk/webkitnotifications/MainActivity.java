package net.homeip.tedk.webkitnotifications;

import android.app.Activity;
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
		
		boolean running = NotificationService.isRunning();
		
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
