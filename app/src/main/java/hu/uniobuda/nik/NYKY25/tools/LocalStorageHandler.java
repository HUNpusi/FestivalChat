package hu.uniobuda.nik.NYKY25.tools;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Vector;

import hu.uniobuda.nik.NYKY25.types.MessageInfo;

public class LocalStorageHandler extends SQLiteOpenHelper {

	private static final String TAG = LocalStorageHandler.class.getSimpleName();
	
	private static final String DATABASE_NAME = "AndroidIM.db";
	private static final int DATABASE_VERSION = 1;
	
	private static final String _ID = "_id";
	private static final String TABLE_NAME_MESSAGES = "androidim_messages";
	public static final String MESSAGE_RECEIVER = "receiver";
	public static final String MESSAGE_SENDER = "sender";
	private static final String MESSAGE_MESSAGE = "message";
	
	
	private static final String TABLE_MESSAGE_CREATE
	= "CREATE TABLE " + TABLE_NAME_MESSAGES
	+ " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
	+ MESSAGE_SENDER + " VARCHAR(25), "
	+MESSAGE_MESSAGE + " VARCHAR(255));";
	
	private static final String TABLE_MESSAGE_DROP = 
			"DROP TABLE IF EXISTS "
			+ TABLE_NAME_MESSAGES;
	
	
	public LocalStorageHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_MESSAGE_CREATE);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrade der DB von V: "+ oldVersion + " zu V:" + newVersion + "; Alle Daten werden gel�scht!");
		db.execSQL(TABLE_MESSAGE_DROP);
		onCreate(db);

	}

    public void removeAll()
    {
        // db.delete(String tableName, String whereClause, String[] whereArgs);
        // If whereClause is null, it will delete all rows.
        SQLiteDatabase db = getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db.delete(TABLE_NAME_MESSAGES,null,null);

    }


	
	public void insert(String sender, String message){
		long rowId = -1;
		try{
			
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(MESSAGE_SENDER, sender);
			values.put(MESSAGE_MESSAGE, message);
			rowId = db.insert(TABLE_NAME_MESSAGES, null, values);
			
		} catch (SQLiteException e){
			Log.e(TAG, "insert()", e);
		} finally {
			Log.d(TAG, "insert(): rowId=" + rowId);
		}
		
	}
	
//	public Cursor get(String sender, String receiver) {
//
//			SQLiteDatabase db = getWritableDatabase();
//			String SELECT_QUERY = "SELECT * FROM " + TABLE_NAME_MESSAGES + " WHERE " + MESSAGE_SENDER + " LIKE '" + sender + "' AND " + MESSAGE_RECEIVER + " LIKE '" + receiver + "' OR " + MESSAGE_SENDER + " LIKE '" + receiver + "' AND " + MESSAGE_RECEIVER + " LIKE '" + sender + "' ORDER BY " + _ID + " ASC";
//			return db.rawQuery(SELECT_QUERY,null);
//
//			//return db.query(TABLE_NAME_MESSAGES, null, MESSAGE_SENDER + " LIKE ? OR " + MESSAGE_SENDER + " LIKE ?", sender , null, null, _ID + " ASC");
//
//	}

    public Cursor get(){

        SQLiteDatabase db = getWritableDatabase();
        String SELECT_QUERY = "SELECT * FROM " + TABLE_NAME_MESSAGES + " ORDER BY "+_ID + " ASC";
        return db.rawQuery(SELECT_QUERY,null);


    }

    //Select query, hogy a kapott rekordok benne vannak-e a belső táblában.
    //Erősen erőforrás pazarló megoldás, de működik. Jövőben javítani kell.
    public Vector<MessageInfo> kulonbseg(Vector<MessageInfo> be_list){
        Vector<MessageInfo> ki_list = new Vector<MessageInfo>();

        SQLiteDatabase db = getWritableDatabase();
        MessageInfo[] tomb = be_list.toArray(new MessageInfo[be_list.size()]);
        for (int i=0;i < be_list.size();i++) {
            String SELECT_QUERY = "SELECT _id FROM " + TABLE_NAME_MESSAGES +" WHERE ("+MESSAGE_SENDER+" = '"+tomb[i].userid+"' and "+MESSAGE_MESSAGE+" = '"+tomb[i].messagetext+"')";
            if (db.rawQuery(SELECT_QUERY,null).getCount() <= 0 ){
                MessageInfo msg_out = new MessageInfo();
                msg_out.userid = tomb[i].userid;
                msg_out.messagetext=tomb[i].messagetext;
                msg_out.sendt = tomb[i].sendt;
                //Log.w("User_ID",msg_out.userid);
                ki_list.add(msg_out);
            }
            //Log.w("db.rawQuery", String.valueOf(i));

        }

        return ki_list;
    }


	
	

}
