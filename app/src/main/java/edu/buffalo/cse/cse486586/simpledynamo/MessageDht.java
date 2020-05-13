package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Pair;

import java.io.Serializable;
import java.util.Map;

public class MessageDht implements Serializable {


    private int msgType;
    private Map<String, String> contentValues;
    private String parentPort;
    private String selfPort;
    private String toPort;
    private String queryKey;           //also used for delete key
    private String queryResponse;
    private String messageOwner;       //where the message actually belongs

    public String getMessageOwner() {
        return messageOwner;
    }

    public void setMessageOwner(String messageOwner) {
        this.messageOwner = messageOwner;
    }

    public String getQueryResponse() {
        return queryResponse;
    }

    public void setQueryResponse(String queryResponse) {
        this.queryResponse = queryResponse;
    }

    public String getParentPort() {
        return parentPort;
    }

    public void setParentPort(String parentPort) {
        this.parentPort = parentPort;
    }

    public String getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public String getToPort() {
        return toPort;
    }

    public void setToPort(String toPort) {
        this.toPort = toPort;
    }

    public Map<String, String> getContentValues() {
        return contentValues;
    }

    public void setContentValues(Map<String, String> contentValues) {
        this.contentValues = contentValues;
    }

    public String getSelfPort() {
        return selfPort;
    }

    public void setSelfPort(String selfPort) {
        this.selfPort = selfPort;
    }

}
