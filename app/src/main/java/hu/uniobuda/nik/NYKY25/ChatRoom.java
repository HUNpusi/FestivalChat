package hu.uniobuda.nik.NYKY25;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import hu.uniobuda.nik.NYKY25.interfaces.IAppManager;
import hu.uniobuda.nik.NYKY25.R;
import hu.uniobuda.nik.NYKY25.services.IMService;

import java.io.UnsupportedEncodingException;

//
//Itt jön létre a service, amely kezeli a message-ek lekérését, elküldését, fogadását.
//Később jelezhet, hogy új üzenet érkezett, vagy egyéb továbbfejlesztésre ad lehetőséget.


public class ChatRoom extends Activity {

    private String android_id;

    private IAppManager imService;
    protected static final int NOT_CONNECTED_TO_SERVICE = 0;
    protected static final int FILL_BOTH_USERNAME_AND_PASSWORD = 1;
    public static final String AUTHENTICATION_FAILED = "0";
    public static final String FRIEND_LIST = "FRIEND_LIST";
    protected static final int MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT = 2 ;
    protected static final int NOT_CONNECTED_TO_NETWORK = 3;
    private boolean was_onCreate = false;


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {

//            Ezzel a metódussal lehet hozzáférni a service hez

            imService = ((IMService.IMBinder)service).getService();

            if (imService.isUserAuthenticated() == true)
            {
                Intent i = new Intent(ChatRoom.this, Messaging.class);
                startActivity(i);
                ChatRoom.this.finish();
            }
        }

        public void onServiceDisconnected(ComponentName className) {

//            Service disconnect esetén ez a metódus fut le. Nem jó ha ez lefut.

            imService = null;
            Toast.makeText(ChatRoom.this, R.string.local_service_stopped,
                    Toast.LENGTH_SHORT).show();
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        was_onCreate=true;
        setContentView(R.layout.chatroom_about);

        android_id = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
        startService(new Intent(ChatRoom.this,  IMService.class));

        //A handler postDelay-el szükséges, mert auto bejelentkezés esetén a service
        //még nem épült fel, de már jelentkezne be. Gyorsabb a fő szál, mint a háttér szál.
        //500ms delay elég.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {


                if (imService == null) {
                    showDialog(NOT_CONNECTED_TO_SERVICE);
                    return;
                }
                else if (imService.isNetworkConnected() == false)
                {
                    showDialog(NOT_CONNECTED_TO_NETWORK);

                }
                else
                {
                    Thread loginThread = new Thread(){
                        private Handler handler = new Handler();
                        @Override
                        public void run() {
                            String result = null;
                            try {
                                result = imService.authenticateUser(android_id);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            if (result == null || result.equals(AUTHENTICATION_FAILED))
                            {
								/*
								 * Authentikáció hiba
								 */
                                handler.post(new Runnable(){
                                    public void run() {
                                        showDialog(MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT);
                                    }
                                });

                            }
                            else {

								/*
								 * Nem történt hiba, indítható a Messaging activity
								 *
								 */
                                handler.post(new Runnable(){
                                    public void run() {
                                        Intent i = new Intent(ChatRoom.this, Messaging.class);
                                        startActivity(i);
                                        ChatRoom.this.finish();
                                    }
                                });

                            }

                        }
                    };
                    loginThread.start();

                }

            }
        }, 500);
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.chat_room, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }


    @Override
    protected void onPause()
    {
        unbindService(mConnection);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        bindService(new Intent(ChatRoom.this, IMService.class), mConnection, Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }


}
