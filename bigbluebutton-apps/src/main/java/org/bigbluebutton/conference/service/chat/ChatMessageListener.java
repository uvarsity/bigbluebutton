package org.bigbluebutton.conference.service.chat;

import org.bigbluebutton.conference.service.messaging.MessagingConstants;
import org.bigbluebutton.conference.service.messaging.redis.MessageHandler;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.HashMap;

import org.bigbluebutton.core.api.IBigBlueButtonInGW;

public class ChatMessageListener implements MessageHandler{

	private IBigBlueButtonInGW bbbGW;
	
	public void setBigBlueButtonInGW(IBigBlueButtonInGW bbbGW) {
		this.bbbGW = bbbGW;
	}

	@Override
	public void handleMessage(String pattern, String channel, String message) {
		if (channel.equalsIgnoreCase(MessagingConstants.TO_CHAT_CHANNEL)) {

			JsonParser parser = new JsonParser();
			JsonObject obj = (JsonObject) parser.parse(message);
			JsonObject headerObject = (JsonObject) obj.get("header");
			JsonObject payloadObject = (JsonObject) obj.get("payload");
			JsonObject messageObject = (JsonObject) payloadObject.get("message");

			String eventName = headerObject.get("name").toString();
			eventName = eventName.replace("\"", "");

			if (eventName.equalsIgnoreCase(MessagingConstants.SEND_PUBLIC_CHAT_MESSAGE_REQUEST) ||
				eventName.equalsIgnoreCase(MessagingConstants.SEND_PRIVATE_CHAT_MESSAGE_REQUEST)){

				String meetingID = payloadObject.get("meeting_id").toString().replace("\"", "");
				String requesterID = payloadObject.get("requester_id").toString().replace("\"", "");

				//case getChatHistory
				if(eventName.equalsIgnoreCase("get_chat_history")) {
					String replyTo = meetingID + "/" + requesterID;
					bbbGW.getChatHistory(meetingID, requesterID, replyTo);
				}
				else {
					String chatType = messageObject.get("chat_type").toString().replace("\"", "");
					String fromUserID = messageObject.get("from_userid").toString().replace("\"", "");
					String fromUsername = messageObject.get("from_username").toString().replace("\"", "");
					String fromColor = messageObject.get("from_color").toString().replace("\"", "");
					String fromTime = messageObject.get("from_time").toString().replace("\"", "");
					String fromTimezoneOffset = messageObject.get("from_tz_offset").toString().replace("\"", "");
					String toUserID = messageObject.get("to_userid").toString().replace("\"", "");
					String toUsername = messageObject.get("to_username").toString().replace("\"", "");
					String tempChat = messageObject.get("message").toString();
					String chatText = tempChat.substring(1, tempChat.length() - 1).replace("\\\"", "\"");

					Map<String, String> map = new HashMap<String, String>();
					map.put(ChatKeyUtil.CHAT_TYPE, chatType); 
					map.put(ChatKeyUtil.FROM_USERID, fromUserID);
					map.put(ChatKeyUtil.FROM_USERNAME, fromUsername);
					map.put(ChatKeyUtil.FROM_COLOR, fromColor);
					map.put(ChatKeyUtil.FROM_TIME, fromTime);
					map.put(ChatKeyUtil.FROM_TZ_OFFSET, fromTimezoneOffset);
					map.put(ChatKeyUtil.TO_USERID, toUserID);
					map.put(ChatKeyUtil.TO_USERNAME, toUsername);
					map.put(ChatKeyUtil.MESSAGE, chatText);

					if(eventName.equalsIgnoreCase(MessagingConstants.SEND_PUBLIC_CHAT_MESSAGE_REQUEST)) {
						bbbGW.sendPublicMessage(meetingID, requesterID, map);
					}
					else if(eventName.equalsIgnoreCase(MessagingConstants.SEND_PRIVATE_CHAT_MESSAGE_REQUEST)) {
						bbbGW.sendPrivateMessage(meetingID, requesterID, map);
					}
				}
			}
		}
	}
}
