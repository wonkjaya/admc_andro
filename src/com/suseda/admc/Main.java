package com.suseda.admc;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class Main extends Activity {

	private static final String ALARM_REFRESH_ACTION = "it.trento.alchemiasoft.casagranda.simone.alarmmanagertutorial.ALARM_REFRESH_ACTION";
	private static final int ALARM_CODE = 20;

	private Global global = new Global();

	private BroadcastReceiver alarmReceiver;
	private PendingIntent pendingIntent;

	private AlarmManager alarmManager;

	private int sambung = 0;
	
	private static final int RESULT_SETTINGS = 1;
	
	private static final int HELLO_ID = 1;
	
	// ========================================================= //
	
	// The handler that manage the UI updates
	private Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ALARM_CODE:
				if (sambung ==3){
					sambung = 0;
				} else {
					sambung +=1;
				}
				
				String str = ". ";
				int i =1;
				String cDot = "";
				for (i=1;i<=sambung;i++){
					cDot += str;
				}
				
				TextView tStatus = (TextView) findViewById(R.id.textView3);
				
		        tStatus.setText("Tersambung " +cDot);
		        
		        /*
		        Intent serviceSMS = new Intent(Main.this, SMS.class);
		        startService(serviceSMS);
				*/
		        
		        
		        
		        //SMSStart();
				//Toast.makeText(getBaseContext(),
				//		"Alarm Received 1", Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
		}
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        //Intent serviceSMS = new Intent(this, SMS.class);
        //startService(serviceSMS);
		//startRepeating();
		
		
		Intent iSMS = new Intent(this, SMS.class);
		PendingIntent piSMS = PendingIntent.getService(this, 0, iSMS, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(piSMS);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10000, piSMS);
		
		//awal();
        showUserSettings();
        //sNotif();
        
	}

	
	@SuppressWarnings("deprecation")
	private void sNotif(){
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "Buzz...";
		long when = System.currentTimeMillis();

		@SuppressWarnings("deprecation")
		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "ADM Client";
		CharSequence contentText = "Tersambung...";
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		

		mNotificationManager.notify(HELLO_ID, notification);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	//@Override
	protected void awal() {
		//super.onStart();
		// We get the AlarmManager
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		// We prepare the pendingIntent for the AlarmManager
		Intent intent = new Intent(ALARM_REFRESH_ACTION);
		pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		// We create and register a broadcast receiver for alarms
		alarmReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// We increment received alarms
				// We notify to handler the arrive of alarm
				Message msg = myHandler.obtainMessage(ALARM_CODE, intent);
				myHandler.sendMessage(msg);
			}
		};
		// We register dataReceiver to listen ALARM_REFRESH_ACTION
		IntentFilter filter = new IntentFilter(ALARM_REFRESH_ACTION);
		registerReceiver(alarmReceiver, filter);
	}
	
	public void startRepeating(View v) {
		// We get value for repeating alarm
		
		int startTime = 3000;
		long intervals = 3000;
		// We have to register to AlarmManager
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.MILLISECOND, startTime);
		// We set a repeating alarm
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar
				.getTimeInMillis(), intervals, pendingIntent);
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
 
        case R.id.menu_settings:
            Intent i = new Intent(this, UserSettingActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);
            break;
 
        }
        
        return true;
    }
 
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
 
        switch (requestCode) {
        case RESULT_SETTINGS:
            showUserSettings();
            break;
 
        }
 
    }
 /*
    @Override
    public void onBackPressed() {
    	
        new AlertDialog.Builder(this)
            .setMessage("Aplikasi Ini Tidak Boleh Ditutup...!")
            .setPositiveButton(android.R.string.yes, new OnClickListener() {

                public void onClick(DialogInterface arg0, int arg1) {
                    //Main.super.onBackPressed();
                }
            }).create().show();
            
    }
   */ 
    private void showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        
        Long tsLong = System.currentTimeMillis();
        String ts = tsLong.toString();
        
        String msg;
        msg = "No Telp: " + sharedPrefs.getString("prefNoTelp", "NULL");
        TextView settingsTextView = (TextView) findViewById(R.id.textView3);
        settingsTextView.setText(msg);
    }
    
}
