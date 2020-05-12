package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.Serializable;
import java.util.Map;

public class MessageDht implements Serializable {


    private Enum<MessageDhtType> msgType;
    private Map<String, String> contentValues;
    private String parentPort;
    private String selfPort;
    private String prePort;
    private String succPort;
    private String toPort;
    private Map<String, String> queryContent;
    private String queryKey;
    private String queryResponse;

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

    public Map<String, String> getQueryContent() {
        return queryContent;
    }

    public void setQueryContent(Map<String, String> queryContent) {
        this.queryContent = queryContent;
    }

    public Enum<MessageDhtType> getMsgType() {
        return msgType;
    }

    public void setMsgType(Enum<MessageDhtType> msgType) {
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

    public String getPrePort() {
        return prePort;
    }

    public void setPrePort(String prePort) {
        this.prePort = prePort;
    }

    public String getSuccPort() {
        return succPort;
    }

    public void setSuccPort(String succPort) {
        this.succPort = succPort;
    }
}
