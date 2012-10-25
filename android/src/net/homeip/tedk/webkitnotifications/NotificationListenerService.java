package net.homeip.tedk.webkitnotifications;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.accessibility.AccessibilityEvent;

public class NotificationListenerService extends AccessibilityService implements
		OnSharedPreferenceChangeListener {

	/**
	 * Trust every server - dont check for any certificate Source:
	 * http://stackoverflow
	 * .com/questions/2893819/telling-java-to-accept-self-signed-ssl-certificate
	 */
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static {
		trustAllHosts();
	}

	private DateFormat dateFormat = null;
	private ConnectivityManager cm = null;
	private PackageManager pm = null;

	private URL serverUrl = null;
	private boolean authenticationEnabled = false;
	private String encodedAuthentication = null;

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		setUp();
	}

	public void setUp() {
		dateFormat = DateFormat.getTimeInstance(DateFormat.LONG);
		pm = getPackageManager();
		cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);

		try {
			serverUrl = new URL(sharedPref.getString("pref_url", ""));
		} catch (MalformedURLException e) {
			//Log.e("NotificationListenerService", "Bad server_url", e);
		}
		authenticationEnabled = sharedPref.getBoolean("pref_auth", false);
		if (authenticationEnabled) {
			String authString = sharedPref.getString("pref_auth_username", "")
					+ ":" + sharedPref.getString("pref_auth_password", "");
			encodedAuthentication = Base64.encodeToString(
					authString.getBytes(), Base64.DEFAULT);
		}
	}
	
	public void tearDown() {
		dateFormat = null;
		pm = null;
		cm = null;

		serverUrl = null;
		authenticationEnabled = false;
		encodedAuthentication = null;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		tearDown();
		setUp();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		tearDown();
		return super.onUnbind(intent);
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event == null)
			return;
		Notification n = (Notification) event.getParcelableData();
		if (n == null)
			return;

		String text = n.tickerText == null ? null : n.tickerText.toString();
		if (text == null)
			return; // ignore blank notifications (downloads, gps,
					// keyboard, etc.)

		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null || !ni.getState().equals(NetworkInfo.State.CONNECTED))
			return;

		String time = dateFormat.format(new Date());
		String packageName = event.getPackageName().toString();
		String appName = null;
		try {
			appName = pm.getApplicationLabel(
					pm.getApplicationInfo(packageName, 0)).toString();
		} catch (Exception e) {
			//Log.e("NotificationListenerService", "Could not load application name", e);
		}
		if (appName == null)
			appName = packageName;
		String num = Integer.toString(n.number);
		String icon = null;
		int iconPadding = 0;
		ByteArrayOutputStream baos = null;
		try {
			Context c = createPackageContext(packageName, 0);
			baos = new ByteArrayOutputStream();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap iconBitmap = BitmapFactory.decodeResource(c.getResources(),
					n.icon, options);
			Bitmap iconWithBackground = Bitmap.createBitmap(
					iconBitmap.getWidth(), iconBitmap.getHeight(),
					iconBitmap.getConfig());
			Canvas background = new Canvas(iconWithBackground);
			background.drawColor(Color.BLACK);
			background.drawBitmap(iconBitmap, 0.f, 0.f, null);
			iconWithBackground.compress(Bitmap.CompressFormat.PNG, 100, baos);
			baos.flush();
			icon = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP
					| Base64.URL_SAFE);
			while (icon.endsWith("=")) {
				++iconPadding;
				icon = icon.substring(0, icon.length() - 1);
			}
		} catch (Exception e) {
			//Log.e("NotificationListenerService", "Could not load icon", e);
		} finally {
			if (baos != null)
				try {
					baos.close();
				} catch (Exception e) {
				}
		}
		new SendNotification().execute(time, appName, text, num, icon,
				Integer.toString(iconPadding));
	}

	@Override
	public void onInterrupt() {
		// do nothing for now
	}

	private class SendNotification extends AsyncTask<String, Void, Void> {

		// format: time, app, text, num, icon
		@Override
		protected Void doInBackground(String... params) {
			String time = params[0];
			String app = params[1];
			String text = params[2];
			String num = params[3];
			String icon = params.length > 4 ? params[4] : null;
			String iconPadding = params.length > 5 ? params[5] : null;

			//Log.d("NotificationListenerService", time + ", " + app + ", " + text + ", " + num + ", " + (icon == null ? "null" : icon.length()));

			OutputStream os = null;
			OutputStreamWriter wr = null;
			try {
				JSONObject j = new JSONObject();
				j.put("type", "notification");
				JSONObject n = new JSONObject();
				n.put("time", time);
				n.put("app", app);
				n.put("text", text);
				n.put("num", num);
				n.put("icon", icon);
				n.put("iconPadding", iconPadding);
				j.put("content", n);
				String json = j.toString();

				HttpsURLConnection conn = (HttpsURLConnection) serverUrl
						.openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setFixedLengthStreamingMode(json.length());
				conn.setRequestProperty("Content-length",
						Integer.toString(json.length()));
				if (authenticationEnabled) {
					conn.setRequestProperty("Authorization", "Basic "
							+ encodedAuthentication);
				}
				os = conn.getOutputStream();
				wr = new OutputStreamWriter(os);
				wr.write(json);
				wr.flush();
			} catch (Exception e) {
				//Log.e("NotificationListenerService", "Network Exception", e);
			} finally {
				if (wr != null)
					try {
						wr.close();
					} catch (Exception e) {
					}
				if (os != null)
					try {
						os.close();
					} catch (Exception e) {
					}
			}

			return null;
		}

	}

}
