package com.petrows.mtcservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Settings {
	private final static String TAG = "Settings";

	final static String MTCBroadcastIrkeyUp = "com.microntek.irkeyUp";
	//final static String MTCBroadcastIrkeyDown = "com.microntek.irkeyDown";
	final static String MTCBroadcastACC = "com.microntek.acc";

	final static List<Integer> MTCKeysPrev = Arrays.asList(45, 58, 22);
	final static List<Integer> MTCKeysNext = Arrays.asList(46, 59, 24);
	final static List<Integer> MTCKeysPause = Arrays.asList(3);

	final static String MTCBroadcastWidget = "com.android.MTClauncher.action.INSTALL_WIDGETS";
	final static int MTCWidgetAdd = 10520;
	//final static int MTCWidgetRemove = 10521;

	private ArrayList<Integer> speedValues = new ArrayList<Integer>();
	private String speedValuesDef = "";
	private float volumeMax = 30f;
	private int buildTimestamp = 0;

	private AudioManager am = null;

	private static Settings instance = null;
	private static SharedPreferences prefs;

	private Context ctx;

	private Settings(Context context) {
		ctx = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		speedValuesDef = ctx.getString(R.string.cfg_def_speed_values);

		am = ((AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE));

		// Get max volume
		try {
			String vol_max_s = ((AudioManager) ctx.getSystemService("audio"))
					.getParameters("cfg_maxvolume=");
			volumeMax = Float.parseFloat(vol_max_s);
		} catch (Exception e) {
			volumeMax = 0f;
		}

		if (0f == volumeMax) {
			Log.e(TAG, "Cant get max vlume, set to default 30.0");
			volumeMax = 30.0f;
		}
		Log.d(TAG, "Max volume = " + String.valueOf(volumeMax));

		buildTimestamp = Integer.parseInt(getPropValue("ro.build.date.utc"));
		Log.d(TAG, "Build timestamp: " + String.valueOf(buildTimestamp));

		Log.d(TAG, "Settings created");
	}

	public static Settings get(Context context) {
		if (null == instance)
			instance = new Settings(context);
		return (instance);
	}

	public static void destroy() {
		instance = null;
	}

	public static boolean isNotifitcationServiceEnabled() {
		return (Build.VERSION.SDK_INT >= 19);
	}

	private void setCfgBool(String name, boolean val) {
		Editor editor = prefs.edit();
		editor.putBoolean(name, val);
		editor.commit();
	}

	private void setCfgString(String name, String val) {
		Editor editor = prefs.edit();
		editor.putString(name, val);
		editor.commit();
	}

	//dsa
	public void mySleep(long ms) {
		long endTime = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < endTime) {
			synchronized (this) {
				try {
					wait(endTime -
							System.currentTimeMillis());
				} catch (Exception e) {
				}
			}
		}
	}

	public void startMyServices() {
		if (getServiceEnable()) {
			if (!ServiceMain.isRunning) {
				if (isNotifitcationServiceEnabled()) {
					if (NotificationService.isInit) mySleep(2000);
				}
				Log.d(TAG, "Starting service!");
				ctx.startService(new Intent(ctx, ServiceMain.class));
			}

			if (isNotifitcationServiceEnabled()) {
				if (!NotificationService.isInit) {
					if (ServiceMain.isRunning) mySleep(2000);
					Log.d(TAG, "Starting Notification!");
					ctx.startService(new Intent(ctx, NotificationService.class));
				}
			}
		} else {
			ctx.stopService(new Intent(ctx, ServiceMain.class));
			ctx.stopService(new Intent(ctx, NotificationService.class));
		}
	}

	public void announce(Service srv, int id) {
		Intent notificationIntent = new Intent(srv, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(srv, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification note = new NotificationCompat.Builder(srv)
				.setContentTitle(srv.getString(R.string.app_service_title) + " " + Settings.get(srv).getVersion())
				.setContentText(srv.getString(R.string.app_service_descr))
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.ic_launcher).build();
		srv.startForeground(id, note);
	}

	public String getVersion() {
		String version = "?";
		try {
			PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), 0);
			version = pInfo.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return version;
	}

	public static String getPropValue(String value)
	{
		Process p = null;
		String ret = "";
		try {
			p = new ProcessBuilder("/system/bin/getprop", value).redirectErrorStream(true).start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line=br.readLine()) != null){
				ret = line;
			}
			p.destroy();
		} catch (IOException e) {
// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public static boolean getServiceEnable() {
		return prefs.getBoolean("service.enable", true);
	}

	public void setServiceEnable(boolean enable) {
		setCfgBool("service.enable", enable);
	}

	public static boolean getServiceToast() {
		return prefs.getBoolean("service.toast", true);
	}

	public void showToast(String text) {
		showToast(text, Toast.LENGTH_SHORT);
	}

	public void showToast(String text, int length) {
		Log.d(TAG, "Toast: " + text);
		if (getServiceToast())
			Toast.makeText(ctx, text, length).show();
	}

	public boolean getCallerEnable() {
		return prefs.getBoolean("caller.enable", true);
	}

	public int getCallerVersionAuto() {
		// We should test ro.build.date.utc to be > 1 jan 2015
		if (buildTimestamp < 1420070400) // < 1 jan 2015
			return 1;
		else
			return 2;
	}

	public int getCallerVersion() {
		int version = Integer.parseInt(prefs.getString("caller.version", "0"));
		if (0 == version)
		{
			return getCallerVersionAuto();
		}
		return version;
	}

	public boolean getMediaKeysEnable() {
		return prefs.getBoolean("keys.enable", true);
	}

	public boolean getSafeVolumeEnable() {
		return prefs.getBoolean("svol.enable", false);
	}

	public int getSafeVolumeLevel() {
		return Integer.valueOf(prefs.getString("svol.level", "5"));
	}

	public boolean getSpeedEnable() {
		return prefs.getBoolean("speed.enable", true);
	}

	public int getSpeedChangeValue() {
		return Integer.valueOf(prefs.getString("speed.speedvol", "5"));
	}

	public List<Integer> getSpeedValues() {
		// Load speed values
		if (speedValues == null || speedValues.size() <= 0) {
			// Try to calc it
			String spd_cfg = prefs
					.getString("speed.speedrange", speedValuesDef);
			// Calc it
			List<String> speed_vals_str = Arrays.asList(spd_cfg
					.split("\\s*,\\s*"));
			StringBuilder speed_vals_clr = new StringBuilder();
			for (String spd_step : speed_vals_str) {
				Integer s = -1;
				try {
					s = Integer.valueOf(spd_step);
				} catch (Exception e) {
					s = -1;
				}
				if (s > 0 && s < 500) {
					if (speedValues.size() > 0)
						speed_vals_clr.append(", ");
					speed_vals_clr.append(s.toString());
					speedValues.add(s);
				}
			}

			setCfgString("speed.speedrange", speed_vals_clr.toString());
		}
		return speedValues;
	}

	public boolean getMute() {
		return am.getParameters("av_mute=").equals("true");
	}

	// This function is reversed from package
	// android.microntek.service.MicrontekServer
	private int mtcGetRealVolume(int paramInt) {
		float f1 = 100.0F * paramInt / volumeMax;
		float f2;
		if (f1 < 20.0F) {
			f2 = f1 * 3.0F / 2.0F;
		} else if (f1 < 50.0F) {
			f2 = f1 + 10.0F;
		} else {
			f2 = 20.0F + f1 * 4.0F / 5.0F;
		}
		return (int) f2;
	}

	public void setVolume(int level) {
		if (level < 0 || level > (int) volumeMax) {
			Log.w(TAG, "Volume level " + level + " is wrong, ignore it");
			return;
		}

		Log.d(TAG, "Settings new volume system: " + level + ", real: "
				+ mtcGetRealVolume(level));
		android.provider.Settings.System.putInt(ctx.getContentResolver(),
				"av_volume=", level);
		am.setParameters("av_volume=" + mtcGetRealVolume(level));
	}

	public void setVolumeSafe() {
		showToast(ctx.getString(R.string.toast_safe_volume));
		setVolume(getSafeVolumeLevel());
	}

	public int getVolume() {
		return android.provider.Settings.System.getInt(
				ctx.getContentResolver(), "av_volume=", 15);
	}
}
