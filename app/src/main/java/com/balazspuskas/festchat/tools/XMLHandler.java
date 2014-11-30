package com.balazspuskas.festchat.tools;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.database.Cursor;
import android.util.Log;
import com.balazspuskas.festchat.interfaces.IUpdateData;
import com.balazspuskas.festchat.services.IMService;
import com.balazspuskas.festchat.types.MessageInfo;
import com.balazspuskas.festchat.tools.LocalStorageHandler;
/*
 * Parses the xml data to FriendInfo array
 * XML Structure 
 * <?xml version="1.0" encoding="UTF-8"?>
 * 
 * <friends>
 * 		<user key="..." />
 * 		<friend username="..." status="..." IP="..." port="..." key="..." expire="..." />
 * 		<friend username="..." status="..." IP="..." port="..." key="..." expire="..." />
 * </friends>
 *
 *
 *status == online || status == unApproved
 * */

public class XMLHandler extends DefaultHandler
{
		private String userKey = new String();
		private IUpdateData updater;
        private LocalStorageHandler localstoragehandler;
		
		public XMLHandler(IUpdateData updater) {
			super();
			this.updater = updater;
		}

//		private Vector<FriendInfo> mFriends = new Vector<FriendInfo>();
//		private Vector<FriendInfo> mOnlineFriends = new Vector<FriendInfo>();
//		private Vector<FriendInfo> mUnapprovedFriends = new Vector<FriendInfo>();
		
		private Vector<MessageInfo> mUnreadMessages = new Vector<MessageInfo>();

		
		public void endDocument() throws SAXException 
		{
            int unreadMessagecount = mUnreadMessages.size();
            IMService.localstoragehandler.get().moveToFirst();
            int old_msgs = IMService.localstoragehandler.get().getCount();

            if (old_msgs == mUnreadMessages.size()){
                super.endDocument();
                return;
            }

            for (int i =0; i < old_msgs; i++ ){
                mUnreadMessages.remove(0);
            }

			MessageInfo[] messages = new MessageInfo[mUnreadMessages.size()];


			//Log.i("MessageLOG", "mUnreadMessages="+unreadMessagecount );
			for (int i = 0; i < mUnreadMessages.size(); i++)
			{
				messages[i] = mUnreadMessages.get(i);
				Log.i("MessageLOG", "i="+i );
			}
			
			this.updater.updateData(messages, userKey);
			super.endDocument();
		}		
		
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException 
		{				
			if (localName == "message") {
				MessageInfo message = new MessageInfo();
				message.userid = attributes.getValue(MessageInfo.USERID);
				message.sendt = attributes.getValue(MessageInfo.SENDT);
				message.messagetext = attributes.getValue(MessageInfo.MESSAGETEXT);
				Log.i("MessageLOG", message.userid + message.sendt + message.messagetext);
				mUnreadMessages.add(message);
			}
			super.startElement(uri, localName, name, attributes);
		}

		@Override
		public void startDocument() throws SAXException {			
			this.mUnreadMessages.clear();
			super.startDocument();
		}
		
		
}

