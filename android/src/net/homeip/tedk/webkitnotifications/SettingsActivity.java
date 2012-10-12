package net.homeip.tedk.webkitnotifications;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_config);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean authPref = sharedPref.getBoolean("pref_auth", false);
        findPreference("pref_auth_username").setEnabled(authPref);
        findPreference("pref_auth_password").setEnabled(authPref);
	}
	
	public static final String KEY_PREF_AUTH = "pref_auth";
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(KEY_PREF_AUTH)) {
            boolean authPref = sharedPreferences.getBoolean(key, false);
            findPreference("pref_auth_username").setEnabled(authPref);
            findPreference("pref_auth_password").setEnabled(authPref);
        }
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}

	
}
