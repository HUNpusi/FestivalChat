package com.balazspuskas.festchat.interfaces;
import com.balazspuskas.festchat.types.MessageInfo;


public interface IUpdateData {
	public void updateData(MessageInfo[] messages, String userKey);

}
