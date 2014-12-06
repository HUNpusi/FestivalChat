/* 
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.uniobuda.nik.NYKY25.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
//import android.util.Log;
import android.util.Log;
import android.widget.Toast;


import hu.uniobuda.nik.NYKY25.Messaging;
import hu.uniobuda.nik.NYKY25.communication.SocketOperator;
import hu.uniobuda.nik.NYKY25.interfaces.IAppManager;
import hu.uniobuda.nik.NYKY25.interfaces.ISocketOperator;
import hu.uniobuda.nik.NYKY25.interfaces.IUpdateData;
import hu.uniobuda.nik.NYKY25.tools.LocalStorageHandler;
import hu.uniobuda.nik.NYKY25.tools.MessageController;
import hu.uniobuda.nik.NYKY25.tools.XMLHandler;
import hu.uniobuda.nik.NYKY25.types.MessageInfo;
import hu.uniobuda.nik.NYKY25.R;


/**
 * Application Service mintapélda sok módosítással.
 * A notification manager későbbi megvalósításra. Pl ha jött új üzenet.
 */
public class IMService extends Service implements IAppManager, IUpdateData {

	
	public static String USERNAME;
	public static final String TAKE_MESSAGE = "Take_Message";
	public static final String MESSAGE_LIST_UPDATED = "Take Message List";
	public ConnectivityManager conManager = null; 
	private final int UPDATE_TIME_PERIOD = 5000;
	private String rawFriendList = new String();
	private String rawMessageList = new String();
    public boolean IMService_ready = false;

	ISocketOperator socketOperator = new SocketOperator(this);

	private final IBinder mBinder = new IMBinder();
	private String android_id;
	private boolean authenticatedUser = false;

	private Timer timer;

	public static LocalStorageHandler localstoragehandler;
	private NotificationManager mNM;

//    LocationManager locationManager =
//            (LocationManager)getSystemService(LOCATION_SERVICE);


    public class IMBinder extends Binder {
		public IAppManager getService() {
			return IMService.this;
		}
		
	}
	   
    @Override
    public void onCreate() 
    {
        android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        localstoragehandler = new LocalStorageHandler(this);
    	conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

		timer = new Timer();
		Thread thread = new Thread()
		{
			@Override
			public void run() {			
				
				Random random = new Random();
				int tryCount = 0;
				while (socketOperator.startListening(10000 + random.nextInt(20000))  == 0 )
				{
					tryCount++;
					if (tryCount > 10)
					{
						// Ha nem tud porton figyelni, kilépés
						break;
					}
				}
			}
		};

		thread.start();
        IMService_ready = true;

    }


	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}

	/**
	 * Notification a service-ből. Depricated megvalósítás,
	 * To Do: NotificationCompat.Builder mBuilder-re javítani
	 **/
    public void showNotification()
	{       
    	String title = "You got a new Message!";
    	Notification notification = new Notification(R.drawable.stat_sample, title,System.currentTimeMillis());


        Intent i = new Intent(this, Messaging.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                i, 0);

        notification.setLatestEventInfo(this,"New message in FestivalChat! ",null,contentIntent);

        //Id-vel ellátni a notificationt, hogy a Messaging.java onResume-ben le tudja venni
        mNM.notify(001, notification);
    }
	 

	public String getAndroid_id() {
		return this.android_id;
	}

	public String sendMessage(String  android_id, String message) throws UnsupportedEncodingException
	{
        int loc_lat=0;
        int loc_long=0;
    try {
        updateLocation();
        if(lm==null)
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        loc_lat = (int)(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude()*10000000);
        loc_long = (int)(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude()*10000000);

    } catch (UnsupportedEncodingException e) {
        Log.w("sendMessage","Error in thread");

        e.printStackTrace();
    }


		String params = "android_id="+ URLEncoder.encode(this.android_id,"UTF-8") +
						"&message="+ URLEncoder.encode(message,"UTF-8") +
                        "&loc_lat="+ URLEncoder.encode(String.valueOf(loc_lat),"UTF-8") +
                        "&loc_long="+ URLEncoder.encode(String.valueOf(loc_long),"UTF-8") +
						"&action="  + URLEncoder.encode("sendMessage","UTF-8")+
						"&";
        Log.w(String.valueOf(loc_lat),String.valueOf(loc_long));
		return socketOperator.sendHttpRequest(params);
	}


	private String getMessageList() throws UnsupportedEncodingException 	{		
		// after authentication, server replies with friendList xml
		
		 rawMessageList = socketOperator.sendHttpRequest(getAuthenticateUserParams(android_id));
		 if (rawMessageList != null) {
			 this.parseMessageInfo(rawMessageList);
		 }
		 return rawMessageList;
	}
	
	

	/**
	 * Android_ID szerinti authentikáció. Talán jelenleg nem szükséges.
     * De későbbi fejlesztéshez hasznos lehet
	 * */
	public String authenticateUser(String androidID) throws UnsupportedEncodingException
	{
		this.authenticatedUser = false;
		
		String result =  socketOperator.sendHttpRequest(getAuthenticateUserParams(android_id));
		if (result != null && !result.equals(0))
		{			
			this.authenticatedUser = true;

			timer.schedule(new TimerTask()
			{			
				public void run() 
				{
					try {
						Intent i2 = new Intent(MESSAGE_LIST_UPDATED);
						String tmp2 = IMService.this.getMessageList();
                        if (tmp2 != null) {
							i2.putExtra("messageList", tmp2);
							sendBroadcast(i2);	
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}					
				}			
			}, UPDATE_TIME_PERIOD, UPDATE_TIME_PERIOD);
		}
		return result;
	}


    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            try {
                //Toast.makeText(IMService.this, "onLocationChanged",Toast.LENGTH_LONG).show();
                getUpdateLocationParams(location);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider,int status, Bundle extras) {}
    };

    LocationManager lm;

    public void updateLocation() throws UnsupportedEncodingException
    {



        Thread thread = new Thread()
        {
            @Override
            public void run() {

                Looper.prepare();

                if(lm==null)
                    lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000, 0, locationListenerNetwork);
                try {
                    getUpdateLocationParams(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                } catch (UnsupportedEncodingException e) {
                    //Log.w("updateLocation","Error in thread");
                    e.printStackTrace();
                }
                Looper.loop();
            }
        };
        thread.start();
    }


	public void messageReceived(String android_id, String message)
	{				
		Intent i = new Intent(TAKE_MESSAGE);

        i.putExtra(MessageInfo.USERID, android_id);
        i.putExtra(MessageInfo.MESSAGETEXT, message);
        sendBroadcast(i);
        showNotification();
    }
	
	private String getAuthenticateUserParams(String android_id) throws UnsupportedEncodingException
	{			
		String params = "action="  + URLEncoder.encode("authenticateUser","UTF-8")+
                        "&android_id=" + URLEncoder.encode(android_id,"UTF-8") +
						"&port="    + URLEncoder.encode(Integer.toString(socketOperator.getListeningPort()),"UTF-8");
		
		return params;		
	}

    private String getUpdateLocationParams(Location loc) throws UnsupportedEncodingException
    {
//        Toast.makeText(this, String.valueOf(loc.getLatitude()),Toast.LENGTH_LONG).show();
//        Toast.makeText(this, String.valueOf(loc.getLongitude()),Toast.LENGTH_LONG).show();
        String params = "action="  + URLEncoder.encode("locationUpdate","UTF-8")+
                "&android_id=" + URLEncoder.encode(Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID),"UTF-8") +
                "&loc_lat=" + URLEncoder.encode(String.valueOf(loc.getLatitude()*10000000),"UTF-8") +
                "&loc_long="    + URLEncoder.encode(String.valueOf(loc.getLongitude()*10000000), "UTF-8");

        return socketOperator.sendHttpRequest(params);
    }

	public void setUserKey(String value) 
	{		
	}

	public boolean isNetworkConnected() {
		return conManager.getActiveNetworkInfo().isConnected();
	}
	
	public boolean isUserAuthenticated(){
		return authenticatedUser;
	}
	
	public String getLastRawFriendList() {		
		return this.rawFriendList;
	}
	
	@Override
	public void onDestroy() {
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}
	
	public void exit() 
	{
		timer.cancel();
        if (socketOperator != null) socketOperator.exit();
		socketOperator = null;
		this.stopSelf();
	}
	
	public String signUpUser(String usernameText, String passwordText,
			String emailText) 
	{
		String params = "android_id=" + android_id +
						"&action=" + "signUpUser"+
						"&email=" + emailText+
						"&";
		
		String result = socketOperator.sendHttpRequest(params);		
		
		return result;
	}

    /*
    * XML-ben érkező message adatbázis átalakítása
     */
	private void parseMessageInfo(String xml)
	{			
		try 
		{
			SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
			sp.parse(new ByteArrayInputStream(xml.getBytes()), new XMLHandler(IMService.this));
		} 
		catch (ParserConfigurationException e) {			
			e.printStackTrace();
		}
		catch (SAXException e) {			
			e.printStackTrace();
		} 
		catch (IOException e) {			
			e.printStackTrace();
		}	
	}

	public void updateData(MessageInfo[] messages,
			String userKey)
	{
		this.setUserKey(userKey);
		MessageController.setMessagesInfo(messages);

		int i = 0;
		while (i < messages.length){
			messageReceived(messages[i].userid,messages[i].messagetext);
			i++;
		}
	}
}