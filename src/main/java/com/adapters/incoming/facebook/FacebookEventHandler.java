package com.adapters.incoming.facebook;

import com.github.messenger4j.exceptions.MessengerApiException;
import com.github.messenger4j.exceptions.MessengerIOException;
import com.github.messenger4j.receive.events.AttachmentMessageEvent;
import com.github.messenger4j.receive.events.PostbackEvent;
import com.github.messenger4j.receive.events.QuickReplyMessageEvent;
import com.github.messenger4j.receive.events.TextMessageEvent;
import com.github.messenger4j.send.MessengerSendClient;
import com.github.messenger4j.send.SenderAction;
import com.polafacebook.process.engine.ConversationEngine;
import com.polafacebook.process.engine.message.IncomingMessage;
import com.polafacebook.process.engine.message.attachment.UrlAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;

import static com.polafacebook.process.engine.message.attachment.Attachment.Type.IMAGE;

/**
 * Created by Piotr on 23.09.2017.
 */

/**
 * This handler is responsible for the conversion of messenger4j's events to messages digestible by the engine.
 */
public class FacebookEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(FacebookEventHandler.class);

    private final ConversationEngine conversationEngine;
    private final FacebookEventHelper facebookEventHelper;
    private final MessengerSendClient sendClient;

    private Date lastTimestampServed = new Date(0L);

    public FacebookEventHandler(ConversationEngine conversationEngine, MessengerSendClient sendClient, FacebookEventHelper facebookEventHelper) {
        this.conversationEngine = conversationEngine;
        this.facebookEventHelper = facebookEventHelper;
        this.sendClient = sendClient;
    }

    private boolean hasBeenServed(Date timestamp) {
        boolean result = timestamp.compareTo(lastTimestampServed) <= 0;
        lastTimestampServed = timestamp;
        return result;
    }

    private void promptEngine(IncomingMessage message) {
        conversationEngine.doAction(message);
    }

    private void sendMarkSeen(String senderId) {
        try {
            this.sendClient.sendSenderAction(senderId, SenderAction.MARK_SEEN);
        } catch (MessengerApiException e) {
            logger.error("MARK_SEEN could not be sent. An unexpected error occurred.", e);
        } catch (MessengerIOException e) {
            logger.error("MARK_SEEN could not be sent. An unexpected error occurred.", e);
        }
    }

    public void onTextMessageEvent(TextMessageEvent event) {
        final Date timestamp = event.getTimestamp();
        if (this.hasBeenServed(timestamp)) {
            logger.debug("Discarding a duplicate event: " + event.getMid() + " with timestamp " + event.getTimestamp(), event);
            return;
        }

        final String messageText = event.getText();
        final String senderId = event.getSender().getId();

        IncomingMessage toEngineMessage = new IncomingMessage(messageText, senderId);
        sendMarkSeen(senderId);

        this.promptEngine(toEngineMessage);
    }

    public void onQuickReplyMessageEvent(QuickReplyMessageEvent event) {
        final Date timestamp = event.getTimestamp();
        if (this.hasBeenServed(timestamp)) {
            logger.debug("Discarding a duplicate event: ", event);
            return;
        }

        final String senderId = event.getSender().getId();
        final String text = event.getText();
        final String quickReplyPayload = event.getQuickReply().getPayload();

        IncomingMessage toEngineMessage = new IncomingMessage(text, senderId, quickReplyPayload);
        sendMarkSeen(senderId);

        this.promptEngine(toEngineMessage);
    }

    public void onAttachmentMessageEvent(AttachmentMessageEvent event) {
        final Date timestamp = event.getTimestamp();
        if (this.hasBeenServed(timestamp)) {
            logger.debug("Discarding a duplicate event: ", event);
            return;
        }

        final String senderId = event.getSender().getId();

        /*
        //TODO: place it elsewhere in a converter?
        if (facebookEventHelper.isLike(event)) {
            IncomingMessage toEngineMessage = new IncomingMessage("", senderId, "AFFIRMATIVE");
            sendMarkSeen(senderId);
            conversationEngine.doAction(toEngineMessage);
            return;
        }
        */

        final List<AttachmentMessageEvent.Attachment> attachments = event.getAttachments();

        attachments.forEach(attachment -> {
            final AttachmentMessageEvent.AttachmentType attachmentType = attachment.getType();
            final AttachmentMessageEvent.Payload payload = attachment.getPayload();

            String payloadAsString = null;
            if (payload.isBinaryPayload()) {
                payloadAsString = payload.asBinaryPayload().getUrl();
            }

            IncomingMessage toEngineMessage = new IncomingMessage("", senderId);
            switch (attachmentType) {
                case IMAGE:
                    try {
                        toEngineMessage.addAttachment(new UrlAttachment(IMAGE, payloadAsString));
                    } catch (MalformedURLException e) {
                        logger.error("Image UrlAttachment could not be added. An unexpected error occurred.", e);
                    }
                    break;
                default:
                    logger.debug("Unsupported attachment type received: " + attachmentType);
            }

            this.promptEngine(toEngineMessage);
        });
    }

    public void onPostbackEvent(PostbackEvent event) {
        final Date timestamp = event.getTimestamp();
        if (this.hasBeenServed(timestamp)) {
            logger.debug("Discarding a duplicate event: ", event);
            return;
        }

        logger.debug("Received PostbackEvent: {}", event);

        final String senderId = event.getSender().getId();
        final String recipientId = event.getRecipient().getId();
        final String payload = event.getPayload();

        logger.info("Received postback for user '{}' and page '{}' with payload '{}' at '{}'",
                senderId, recipientId, payload, timestamp);

        this.promptEngine(new IncomingMessage("", senderId, payload));
    }

    //TODO: opt-ins?
    /*
    public void onOptInEvent(OptInEvent event) {
        final String senderId = event.getSender().getId();
        final String recipientId = event.getRecipient().getId();
        final String passThroughParam = event.getRef();
        final Date timestamp = event.getTimestamp();

        sendTextMessage(senderId, "Authentication successful");
    }
    */


}
