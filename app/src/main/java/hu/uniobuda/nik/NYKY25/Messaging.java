package hu.uniobuda.nik.NYKY25;


import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import hu.uniobuda.nik.NYKY25.interfaces.IAppManager;
import hu.uniobuda.nik.NYKY25.tools.LocalStorageHandler;
import hu.uniobuda.nik.NYKY25.types.MessageInfo;
import hu.uniobuda.nik.NYKY25.R;
import hu.uniobuda.nik.NYKY25.services.IMService;


public class Messaging extends Activity {

	private static final int MESSAGE_CANNOT_BE_SENT = 0;
	public String username;
	private EditText messageText;
	private EditText messageHistoryText;
	private Button sendMessageButton;
	private IAppManager imService;
	private LocalStorageHandler localstoragehandler;
	private Cursor dbCursor;

	private ServiceConnection mConnection = new ServiceConnection() {
      
		public void onServiceConnected(ComponentName className, IBinder service) {
            imService = ((IMService.IMBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
        	imService = null;
            Toast.makeText(Messaging.this, R.string.local_service_stopped,
                    Toast.LENGTH_SHORT).show();
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chatroom_activity);
				
		messageHistoryText = (EditText) findViewById(R.id.messageHistory);
		messageText = (EditText) findViewById(R.id.message);
		messageText.requestFocus();
		sendMessageButton = (Button) findViewById(R.id.sendMessageButton);

        //localstorage ürítése, hogy a szerverről lekérje az új üzeneteket.
		localstoragehandler = new LocalStorageHandler(this);
        localstoragehandler.removeAll();
        localstoragehandler = new LocalStorageHandler(this);
//        dbCursor = localstoragehandler.get();
//
//		if (dbCursor.getCount() > 0){
//		int noOfScorer = 0;
//		dbCursor.moveToFirst();
//		    while ((!dbCursor.isAfterLast())&&noOfScorer<dbCursor.getCount())
//		    {
//		        noOfScorer++;
//
//				this.appendToMessageHistory(dbCursor.getString(1) , dbCursor.getString(2));
//		        dbCursor.moveToNext();
//		    }
//		}
//		localstoragehandler.close();


		sendMessageButton.setOnClickListener(new OnClickListener(){
			CharSequence message;
			Handler handler = new Handler();
			public void onClick(View arg0) {
				message = messageText.getText();
				if (message.length()>0) 
				{		
					appendToMessageHistory(imService.getAndroid_id(), message.toString());
					
					localstoragehandler.insert(imService.getAndroid_id(), message.toString());
								
					messageText.setText("");
					Thread thread = new Thread(){					
						public void run() {
							try {
								if (imService.sendMessage(imService.getAndroid_id(), message.toString()) == null)
								{
									
									handler.post(new Runnable(){	

										public void run() {
											
									        Toast.makeText(getApplicationContext(),R.string.message_cannot_be_sent, Toast.LENGTH_LONG).show();

											
											//showDialog(MESSAGE_CANNOT_BE_SENT);										
										}
										
									});
								}
							} catch (UnsupportedEncodingException e) {
								Toast.makeText(getApplicationContext(),R.string.message_cannot_be_sent, Toast.LENGTH_LONG).show();

								e.printStackTrace();
							}
						}						
					};
					thread.start();
										
				}
				
			}});
		
		messageText.setOnKeyListener(new OnKeyListener(){
			public boolean onKey(View v, int keyCode, KeyEvent event) 
			{
				if (keyCode == 66){
					sendMessageButton.performClick();
					return true;
				}
				return false;
			}
			
			
		});
				
	}


    //To Do: support package-el kiváltani ezt a depricated-et, DialogFragmentre
	@Override
	protected Dialog onCreateDialog(int id) {
		int message = -1;
		switch (id)
		{
		case MESSAGE_CANNOT_BE_SENT:
			message = R.string.message_cannot_be_sent;
		break;
		}
		
		if (message == -1)
		{
			return null;
		}
		else
		{
			return new AlertDialog.Builder(Messaging.this)       
			.setMessage(message)
			.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			})        
			.create();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(messageReceiver);
		unbindService(mConnection);
	}

	@Override
	protected void onResume() 
	{

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(001);

		super.onResume();
		bindService(new Intent(Messaging.this, IMService.class), mConnection , Context.BIND_AUTO_CREATE);
				
		IntentFilter i = new IntentFilter();
		i.addAction(IMService.TAKE_MESSAGE);
		registerReceiver(messageReceiver, i);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        menu.add(0, Menu.FIRST, 0, R.string.exit_application);

        //menu.add(0, Menu.FIRST+1, 0, R.string.user_name );

        return result;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {

        switch(item.getItemId())
        {
            case Menu.FIRST:
            {
                imService.exit();
                finish();

                return true;
            }
//            case Menu.FIRST+1:
//            {
//                //To Do: Ide kéne a változtatható username-et megcsinálni.
//                return true;
//            }
        }

        return super.onMenuItemSelected(featureId, item);
    }


    public class  MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) 
		{		
			Bundle extra = intent.getExtras();
			String username = extra.getString(MessageInfo.USERID);
			String message = extra.getString(MessageInfo.MESSAGETEXT);
			
			if (username != null && message != null)
			{
                //if (localstoragehandler.get().getCount() <  )
//				if (friend.userName.equals(username)) {
					appendToMessageHistory(username, message);
					localstoragehandler.insert(username, message);
					
//				}
//				else {
//					if (message.length() > 15) {
//						message = message.substring(0, 15);
//					}
//					Toast.makeText(Messaging.this,  username + " says '"+
//													message + "'",
//													Toast.LENGTH_SHORT).show();
//				}
			}			
		}
		
	};
	private MessageReceiver messageReceiver = new MessageReceiver();
	
	public  void appendToMessageHistory(String username, String message) {
		if (username != null && message != null) {
			messageHistoryText.append(username + ":\n");								
			messageHistoryText.append(message + "\n");
		}
	}
	
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    if (localstoragehandler != null) {
	    	localstoragehandler.close();
	    }
	    if (dbCursor != null) {
	    	dbCursor.close();
	    }

        imService.exit();
        finish();
	}
	

}
