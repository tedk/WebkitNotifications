package net.homeip.tedk.webkitnotifications;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.accessibility.AccessibilityEvent;

public class NotificationListenerService extends AccessibilityService {

   // always verify the host - dont check for certificate
   private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
         return true;
      }
   };

   /**
    * Trust every server - dont check for any certificate
    */
   private static void trustAllHosts() {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
         public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
         }

         public void checkClientTrusted(X509Certificate[] chain, String authType)
               throws CertificateException {}

         public void checkServerTrusted(X509Certificate[] chain, String authType)
               throws CertificateException {}
      } };

      // Install the all-trusting trust manager
      try {
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(null, trustAllCerts, new java.security.SecureRandom());
         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static enum Place {
      WORK, HOME, OTHER
   }

   private final LocationListener locationListener = new LocationListener() {
      public void onLocationChanged(Location location) {
         updateLocation(location);
      }

      public void onProviderDisabled(String provider) {}

      public void onProviderEnabled(String provider) {}

      public void onStatusChanged(String provider, int status, Bundle extras) {}
   };
   private static final Location work = new Location(
         LocationManager.NETWORK_PROVIDER);
   private static final Location home = new Location(
         LocationManager.NETWORK_PROVIDER);
   static {
      work.setLongitude(-111.903639);
      work.setLatitude(33.463407);
      home.setLongitude(-111.785858);
      home.setLatitude(33.414156);
   }

   private Place currentPlace;
   private DateFormat dateFormat;
   private ConnectivityManager cm;
   private AudioManager am;
   private PackageManager pm;
   private LocationManager locationManager;

   private URL serverUrl;
   private String encodedAuthentication;

   @Override
   protected void onServiceConnected() {
      super.onServiceConnected();
      trustAllHosts();
      currentPlace = Place.OTHER;
      dateFormat = DateFormat.getTimeInstance(DateFormat.LONG);
      pm = getPackageManager();
      am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
            1000 * 60 * 5, 0, locationListener);

      try {
         serverUrl = new URL(getResources().getString(R.string.server_url));
      } catch (MalformedURLException e) {
         // Log.e("NotificationListenerService", "Bad server_url", e);
      }
      String authString = getResources().getString(R.string.server_username)
            + ":" + getResources().getString(R.string.server_password);
      encodedAuthentication = Base64.encodeToString(authString.getBytes(),
            Base64.DEFAULT);
   }

   @Override
   public boolean onUnbind(Intent intent) {
      locationManager.removeUpdates(locationListener);
      currentPlace = null;
      dateFormat = null;
      pm = null;
      am = null;
      cm = null;
      locationManager = null;

      serverUrl = null;
      return super.onUnbind(intent);
   }

   private synchronized boolean updateLocation(Location currentLocation) {
      boolean close = false;
      Place newPlace = Place.OTHER;
      float workDistance = currentLocation.distanceTo(work);
      float homeDistance = currentLocation.distanceTo(home);
      if (currentLocation.hasAccuracy()) {
         if (workDistance <= currentLocation.getAccuracy() * 2.f) {
            close = true;
            newPlace = Place.WORK;
         } else if (homeDistance <= currentLocation.getAccuracy() * 2.f) {
            close = true;
            newPlace = Place.HOME;
         } else {
            close = false;
            newPlace = Place.OTHER;
         }
      } else {
         if (workDistance <= 2000.f) {
            close = true;
            newPlace = Place.WORK;
         } else if (homeDistance <= 2000.f) {
            close = true;
            newPlace = Place.HOME;
         } else {
            close = false;
            newPlace = Place.OTHER;
         }
      }
      if (am != null) {
         if (!newPlace.equals(currentPlace)) {
            currentPlace = newPlace;
            if (currentPlace.equals(Place.WORK)) {
               am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
               am.setStreamVolume(AudioManager.STREAM_MUSIC, 0,
                     AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            } else {
               am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
               am.setStreamVolume(AudioManager.STREAM_MUSIC,
                     am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                     AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
         }
      }
      return close;
   }

   @Override
   public void onAccessibilityEvent(AccessibilityEvent event) {
      if (event == null) return;
      Notification n = (Notification) event.getParcelableData();
      if (n == null) return;

      String text = n.tickerText == null ? null : n.tickerText.toString();
      if (text == null) return; // ignore blank notifications (downloads, gps,
                                // keyboard, etc.)

      NetworkInfo ni = cm.getActiveNetworkInfo();
      if (ni == null || !ni.getState().equals(NetworkInfo.State.CONNECTED))
         return;

      if (!updateLocation(locationManager
            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER))) return;

      String time = dateFormat.format(new Date());
      String packageName = event.getPackageName().toString();
      String appName = null;
      try {
         appName = pm
               .getApplicationLabel(pm.getApplicationInfo(packageName, 0))
               .toString();
      } catch (Exception e) {
         // Log.e("NotificationListenerService",
         // "Could not load application name", e);
      }
      if (appName == null) appName = packageName;
      String num = Integer.toString(n.number);
      String icon = null;
      int iconPadding = 0;
      ByteArrayOutputStream baos = null;
      try {
         Context c = createPackageContext(packageName, 0);
         baos = new ByteArrayOutputStream();
         BitmapFactory.decodeResource(c.getResources(), n.icon).compress(
               Bitmap.CompressFormat.PNG, 100, baos);
         baos.flush();
         icon = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP
               | Base64.URL_SAFE);
         while (icon.endsWith("=")) {
            ++iconPadding;
            icon = icon.substring(0, icon.length() - 1);
         }
      } catch (Exception e) {
         // Log.e("NotificationListenerService", "Could not load icon", e);
      } finally {
         if (baos != null) try {
            baos.close();
         } catch (Exception e) {}
      }
      new SendNotification().execute(time, appName, text, num, icon,
            Integer.toString(iconPadding));
   }

   @Override
   public void onInterrupt() {

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

         // Log.d("NotificationListenerService", time + ", " + app + ", " + text
         // + ", " + num + ", " + (icon == null ? "null" : icon.length()));

         OutputStream os = null;
         OutputStreamWriter wr = null;
         InputStream is = null;
         InputStreamReader isr = null;
         BufferedReader rd = null;
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
            conn.setHostnameVerifier(DO_NOT_VERIFY);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setFixedLengthStreamingMode(json.length());
            conn.setRequestProperty("Content-length",
                  Integer.toString(json.length()));
            conn.setRequestProperty("Authorization", "Basic "
                  + encodedAuthentication);
            os = conn.getOutputStream();
            wr = new OutputStreamWriter(os);
            wr.write(json);
            wr.flush();

            // Get the response
            is = conn.getInputStream();
            isr = new InputStreamReader(is);
            rd = new BufferedReader(isr);
            /*
             * String line; while ((line = rd.readLine()) != null) {
             * Log.d("NotificationListenerService", line); }
             */
         } catch (Exception e) {
            // Log.e("NotificationListenerService", "Network Exception", e);
         } finally {
            if (rd != null) try {
               rd.close();
            } catch (Exception e) {}
            if (isr != null) try {
               isr.close();
            } catch (Exception e) {}
            if (is != null) try {
               is.close();
            } catch (Exception e) {}
            if (wr != null) try {
               wr.close();
            } catch (Exception e) {}
            if (os != null) try {
               os.close();
            } catch (Exception e) {}
         }

         return null;
      }

   }

}
