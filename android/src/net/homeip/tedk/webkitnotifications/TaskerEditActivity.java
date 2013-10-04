package net.homeip.tedk.webkitnotifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class TaskerEditActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasker_edit);
		
		boolean start = false;
		Bundle e = getIntent().getExtras();
		if(e != null)
		{
			
			Bundle b = e.getBundle("com.twofortyfouram.locale.intent.extra.BUNDLE");
			if(b != null)
			{
				start = b.getBoolean("start");
			}
		}
		
		RadioButton startButton = (RadioButton) findViewById(R.id.startButton);
		startButton.setChecked(start);
		RadioButton stopButton = (RadioButton) findViewById(R.id.stopButton);
		stopButton.setChecked(!start);
		
	}

	@Override
	public void finish() {
		RadioGroup radios = (RadioGroup) findViewById(R.id.radios);
		boolean start = radios.getCheckedRadioButtonId() == R.id.startButton;
		Bundle b = new Bundle();
		b.putBoolean("start", start);
		Intent data = new Intent();
		data.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", start ? "Start" : "Stop");
		data.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", b);
		setResult(RESULT_OK, data);
		super.finish();
	}
	
}
