package net.homeip.tedk.webkitnotifications;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings_config);
		    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		    boolean authPref = sharedPref.getBoolean("pref_auth", false);
		    findPreference("pref_auth_username").setEnabled(authPref);
		    findPreference("pref_auth_password").setEnabled(authPref);
		}
	}

	private SettingsFragment sf;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		sf = new SettingsFragment();
		getFragmentManager().beginTransaction().replace(android.R.id.content, sf).commit();
		
 
	}
	
	public static final String KEY_PREF_AUTH = "pref_auth";
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(KEY_PREF_AUTH)) {
            boolean authPref = sharedPreferences.getBoolean(key, false);
            sf.findPreference("pref_auth_username").setEnabled(authPref);
            sf.findPreference("pref_auth_password").setEnabled(authPref);
        }
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    sf.getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    sf.getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}

	
}
