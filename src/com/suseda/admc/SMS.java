package com.suseda.admc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;



public class SMS extends Service {
	private static final int HELLO_ID = 1;
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	@Override
	public void onStart(Intent i, int startId) {
		this.SMSRun.run();
        this.stopSelf();
	}
	*/
	public Runnable SMSRun = new Runnable() {
	
		public void run() {
		//Do something
			sNotif();
			SMSStart SMS = new SMSStart();
	        SMS.execute();
		}
	};
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
		this.SMSRun.run();
        this.stopSelf();
        return START_STICKY;
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
	
	
    // ================== SMS Handle ===================== //
    
    public class SMSStart extends AsyncTask<Void, Void, String>{

		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub
			//Toast.makeText(getBaseContext(), "Okeeeee.............", 
            //        Toast.LENGTH_SHORT).show();
			SMSExecute();
			return null;
		}

	}
    

    public void SMSExecute(){
		String urlService = getUrlService();

		if (urlService == ""){
			return;
		}
		//Toast.makeText(getBaseContext(),
		//		"SMS Start", Toast.LENGTH_SHORT).show();
		
		String nomer = getNoTelp();
		
		Cursor inbox = getInbox(); // ambil inbox yang belum terbaca
		transferInbox(inbox,urlService); // kirim inbox ke server
		
		
		// send outbox as SMS
		NodeList outbox = getOutbox(nomer);
		int jmlOutbox = outbox.getLength();
		String idOutbox = "";
		String tujuan = "";
		String msg = "";
		for (int i = 0; i < jmlOutbox; i++) {
            idOutbox = outbox.item(i).getChildNodes().item(0).getFirstChild().getNodeValue();
            tujuan = outbox.item(i).getChildNodes().item(1).getFirstChild().getNodeValue();
            msg = outbox.item(i).getChildNodes().item(2).getFirstChild().getNodeValue();
            sendSMS(tujuan,msg,idOutbox,urlService);
        }
		
		// Sinkronisasi status sent item //
		syncSent(urlService);  
		
		// hapus inbox //
		deleteInbox();
    }
    
	public String getNoTelp(){
		String noTelp;
		SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
		noTelp = sharedPrefs.getString("prefNoTelp", "0");
		return noTelp;
	}
	
	public String getKey(){
		String key;
		SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
		key = sharedPrefs.getString("prefKey", "");
		return key;
	}
	
	public String[] getIDInbox(String nomer){
		String urlService = getUrlService() + "sms/getIDInbox/" + nomer;
		String html = grabHTML(urlService);
		String[] id;
		id = new String[]{html};
		return id;
	}
	
	public Cursor getInbox(){
		Uri uriSMSURI = Uri.parse("content://sms/inbox");
		String where = "read = ?";
	    String[] read =  new String[]{"0"} ;
	    
		Cursor cur = getContentResolver().query(uriSMSURI, null, where, read,null);
		return cur;
	}
	
	public void transferInbox(Cursor cur, String urlService){
		HttpClient httpclient = new DefaultHttpClient();
		
		String url = urlService + "sms/inbox";
		HttpPost httppost = new HttpPost(url);
		
		String id, nomer, pengirim, text;
		Long tgl;
		
		nomer = getNoTelp();
		while (cur.moveToNext()){
			id = cur.getString(cur.getColumnIndexOrThrow("_id"));
			pengirim = cur.getString(cur.getColumnIndexOrThrow("address"));
			text = cur.getString(cur.getColumnIndexOrThrow("body"));
			tgl = cur.getLong(cur.getColumnIndexOrThrow("date"))/1000;
			
			// setting ke terbaca, jika gagal kembalikan ke belum terbaca //
			updateRead(id);
			
			try {
				List<NameValuePair> valInbox = new ArrayList<NameValuePair>(5);
				valInbox.add(new BasicNameValuePair("id", id));
				valInbox.add(new BasicNameValuePair("nomer", nomer));
				valInbox.add(new BasicNameValuePair("pengirim", pengirim));
				valInbox.add(new BasicNameValuePair("text", text));
				valInbox.add(new BasicNameValuePair("tgl", tgl.toString()));

				httppost.setEntity(new UrlEncodedFormEntity(valInbox));
				
				String status = httpclient.execute(httppost, new BasicResponseHandler());
				
				if (status.equals("Ok")){
					//
				} else {
					updateUnRead(id);
				}
					 
			} catch (ClientProtocolException e) {
		         // TODO Auto-generated catch block
			} catch (IOException e) {
		         // TODO Auto-generated catch block
			} 
		}
	}
	
	public String grabHTML(String url){
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

		String html = "";
		InputStream in = null;
		try {
			in = response.getEntity().getContent();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder str = new StringBuilder();
		String line = null;
		try {
			while((line = reader.readLine()) != null)
			{
			    str.append(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		html = str.toString();
		return html;
	}
	
	public void executeHTML(String url){
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	private String getUrlService(){
		
		String url = "http://192.168.100.215:88/adm/index.php/service/";
		String urlService = "";

	    URL requestURL = null;
	    
	    int respon = -1;
		try {
			requestURL = new URL(url);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) requestURL.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			respon = connection.getResponseCode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		if (respon == -1){
			urlService = "";
		} else {
			urlService = url;
		}
		
		return urlService;
	}
	
	// Ambil outbox //
	public NodeList getOutbox(String nomer){
        String url = getUrlService() + "sms/outbox/" + nomer;
        NodeList nodeList = getNodeFromXML(url);
        return nodeList;
    }
	
	public void sendSMS(String phoneNumber, String message, final String idOutbox, final String urlService)
    {
	      
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        
        Long createdTime ;
        createdTime = System.currentTimeMillis();
        
        final Intent sentIntent = new Intent(SENT);
        sentIntent.putExtra("createdTime", createdTime);
        sentIntent.putExtra("phoneNumber", phoneNumber);
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            sentIntent, 0);      
 
        final Intent deliveredIntent = new Intent(DELIVERED);
        deliveredIntent.putExtra("createdTime", createdTime);
        deliveredIntent.putExtra("phoneNumber", phoneNumber);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
        	deliveredIntent, 0);
 
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context con, Intent intent) {
            	//Intent updateSMS = new Intent(con, UpdateSMS.class);
            	//updateSMS.putExtra("idOutbox", idOutbox);
            	//updateSMS.putExtra("urlService", urlService);
            	intent = sentIntent;
            	Long tgl = intent.getLongExtra("createdTime", 0);     
            	String no = intent.getStringExtra("phoneNumber");
            	
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                    	updateSent(tgl,-1);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    	updateSent(tgl,64);
                    	//Toast.makeText(getBaseContext(), "Kirim SMS Error " + no , 
                        //        Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    	updateSent(tgl,64);
                    	//Toast.makeText(getBaseContext(), "Kirim SMS Error " + no , 
                        //       Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    	updateSent(tgl,64);
                    	//Toast.makeText(getBaseContext(), "Kirim SMS Error " + no , 
                        //        Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    	updateSent(tgl,64);
                    	//Toast.makeText(getBaseContext(), "Kirim SMS Error " + no , 
                        //        Toast.LENGTH_SHORT).show();
                        break;
                }
                //con.startService(updateSMS);
                con.unregisterReceiver(this);
            }
        }, new IntentFilter(SENT));
        
        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context con, Intent intent) {
            	intent = deliveredIntent;
            	Long tgl = intent.getLongExtra("createdTime", 0);
            	String no = intent.getStringExtra("phoneNumber");
            	switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                    	updateSent(tgl,0);
                    	break;
                    case Activity.RESULT_CANCELED:
                    	//Toast.makeText(getBaseContext(), "Delivery SMS Error " + no , 
                        //        Toast.LENGTH_SHORT).show();
                        break;                        
                }
            	con.unregisterReceiver(this);
            }
        }, new IntentFilter(DELIVERED));        
 
		// Simpan Sent Ite //
		ContentValues values = new ContentValues();
        values.put("address", phoneNumber);
        values.put("body", message);
        values.put("date", createdTime);
        //values.put("status", -1);
        getContentResolver().insert(Uri.parse("content://sms/sent"), values);
                
        // kirim sms //
        SmsManager sms = SmsManager.getDefault();
        
        if (message.length()<=160){
        	sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
        } else {
    	    ArrayList<String> multiSMS = sms.divideMessage(message);
    	    
    	    ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
    	    for (int i = 0; i < multiSMS.size(); i++){
    	    	sentIntents.add(sentPI);
    	    }
    	    
    	    ArrayList<PendingIntent>deliveredIntents = new ArrayList<PendingIntent>();
    	    for (int i = 0; i < multiSMS.size(); i++){
    	    	deliveredIntents.add(deliveredPI);
    	    }
    	    	    
    	    sms.sendMultipartTextMessage(phoneNumber, null,multiSMS, sentIntents, deliveredIntents);
        }
        
        outboxToSent(idOutbox,createdTime);
    }

	public void updateRead(String id){
		ContentValues values = new ContentValues();
		values.put("read",true);
		getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id="+id, null);
		// Delete Read //
		//getContentResolver().delete(Uri.parse("content://sms/"+id), null, null);
	}
	
	public void updateUnRead(String id){
		ContentValues values = new ContentValues();
		values.put("read",false);
		getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id="+id, null);
		// Delete Read //
		//getContentResolver().delete(Uri.parse("content://sms/"+id), null, null);
	}
	
	public void updateSent(Long tgl, int status){
		ContentValues values = new ContentValues();
		values.put("status",status);
		getContentResolver().update(Uri.parse("content://sms/sent"), values, "date="+tgl.toString(), null);
	}
	
	public void outboxToSent(String idOutbox,Long tgl){
		String url = getUrlService() + "sms/outbox_to_sent/" + idOutbox + "/" + tgl.toString() ;
		executeHTML(url); 
	}
	
	// Memproses url XML menjadi node //
	public NodeList getNodeFromXML(String url){
        URL URL = null;
        InputStream stream = null;
        DocumentBuilder builder = null;
        Document document = null;
        org.w3c.dom.Element root;
        NodeList nodeList;
        try {
			URL = new URL(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // The URL of the site you posted goes here.
        try {
			stream = URL.openStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Set up and initialize the document.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringElementContentWhitespace(true);
        try {
			builder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			document = builder.parse(stream);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        document.getDocumentElement().normalize();

        root = document.getDocumentElement();
        nodeList = root.getChildNodes();
        return nodeList;
	}
	
	public void syncSent(String urlService){
		// sinkronisasi status sent item ke server //
		// diambil sent item 1 jam terakhir //
		// syarat jam harus cocok //
		String url = null;
		String id;
		Long jamSekarang = System.currentTimeMillis();
		Long maxSync = System.currentTimeMillis() - 3600000;
		Long tgl = null;
		int status;
		
		Cursor cur = getContentResolver().query(Uri.parse("content://sms/sent"), null, 
			    "date >= ? ", new String[]{maxSync.toString()}, null);
		while (cur.moveToNext()){
			id = cur.getString(cur.getColumnIndexOrThrow("_id"));
			tgl = cur.getLong(cur.getColumnIndexOrThrow("date"));
			status = cur.getInt(cur.getColumnIndexOrThrow("status"));
        	//String url1 = urlService + "sms/update_sent_item/" + tgl.toString() + "/" + "SendingError";

            switch (status)
            {
                case -1:
                	url = urlService + "sms/update_sent_item/" + tgl.toString() + "/" + "SendingOK";
                    break;
                case 0:
                	url = urlService + "sms/update_sent_item/" + tgl.toString() + "/" + "DeliveryOK";
                    break;
                case 32:
                	url = urlService + "sms/update_sent_item/" + tgl.toString() + "/" + "SendingError";
                    break;
                case 64:
                	url = urlService + "sms/update_sent_item/" + tgl.toString() + "/" + "SendingError";
                    break;
            }
            if (url != null){
            	executeHTML(url);
            	if (status == 0){
            		getContentResolver().delete(Uri.parse("content://sms/"+id), null, null);
            	}
            }
		}
	}

	public void deleteInbox(){
		// delete inbox yang sudah terbaca dan lebih dari 1 jam //
		String url = null;
		String id;
		Boolean read;
		Long jamSekarang = System.currentTimeMillis();
		Long maxSync = System.currentTimeMillis() - 3600000;
		Long tgl = null;
		
		Cursor cur = getContentResolver().query(Uri.parse("content://sms/inbox"), null, 
			    "date <= " + maxSync.toString() + " and read = 1", null, null);
		while (cur.moveToNext()){
			id = cur.getString(cur.getColumnIndexOrThrow("_id"));
			getContentResolver().delete(Uri.parse("content://sms/"+id), null, null);
		}
	}
}