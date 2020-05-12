package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {

    static final int SERVER_PORT = 10000;
    private static final String SERVER_TAG = "Server Log";
    private static final String CLIENT_TAG = "Client Log";
    static String prePort;
    static String succPort;
    static String myPort;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    private static boolean queryFlag = false;
    Map<String, String> messageGlobal;
    String singleMessageGlobal;
    private static String PREFNAME = "PREF";
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    private List<String> ring = new LinkedList<String>(
            Arrays.asList(REMOTE_PORT4, REMOTE_PORT1, REMOTE_PORT0, REMOTE_PORT2, REMOTE_PORT3)
    );


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(selection);
        editor.apply();
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        if (values == null || values.size() < 1) {
            return null;
        }
        String key = values.getAsString(KEY_FIELD);
        String value = values.getAsString(VALUE_FIELD);

        try {
            String keyHash = genHash(key);
            String selfPortHash = genHashForPort(myPort);
            String prePortHash = genHashForPort(prePort);
            Log.v("inserting", values.toString());
            if ((keyHash.compareTo(prePortHash) > 0 && keyHash.compareTo(selfPortHash) < 0)
                    || (keyHash.compareTo(prePortHash) > 0 && keyHash.compareTo(selfPortHash) > 0 && selfPortHash.compareTo(prePortHash) < 0) //12
                    || (keyHash.compareTo(prePortHash) < 0 && keyHash.compareTo(selfPortHash) < 0 && selfPortHash.compareTo(prePortHash) < 0) //1
                    || (selfPortHash.equals(prePortHash))) {
                SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(key, value);
                editor.commit();
                Log.v("inserted", values.toString());
            } else {
                Log.v("insert delegate", values.toString());
                sendInsertRequestToSucc(values, succPort);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.v("insert", values.toString());
        return null;
    }


    @Override
    public boolean onCreate() {
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        serverSocketInit();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        MatrixCursor cursor = null;
        String[] columnNames = new String[]{KEY_FIELD, VALUE_FIELD};
        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);

        if (selection.equals("@")) {
            messageGlobal = (Map<String, String>) sharedPref.getAll();
            //Log.d("query debug", selection + ":" + message);
        } else if (selection.equals("*")) {
            messageGlobal = (Map<String, String>) sharedPref.getAll();
            MessageDht messageDht = new MessageDht();
            messageDht.setSelfPort(myPort);
            messageDht.setToPort(succPort);
            messageDht.setQueryContent(messageGlobal);
            messageDht.setMsgType(MessageDhtType.QUERYGLOBAL);
            sendGlobalMessageQuery(messageDht);
            queryFlag = true;
            while (queryFlag) {

            }

        } else {

            String message = sharedPref.getString(selection, "0");
            if (message != null && !message.equals("0")) {
                Log.d("query debug", selection + ":" + message);

                String[] columnValues = new String[]{selection, message};
                cursor = new MatrixCursor(columnNames, 1);
                try {
                    if (getContext() == null) return cursor;
                    cursor.addRow(columnValues);
                    cursor.moveToFirst();
                } catch (Exception e) {
                    cursor.close();
                    throw new RuntimeException(e);
                }
                Log.v("query", selection);
                queryFlag = false;
                return cursor;
            } else {
                MessageDht msg = new MessageDht();
                msg.setQueryKey(selection);
                msg.setToPort(succPort);
                msg.setSelfPort(myPort);
                msg.setMsgType(MessageDhtType.QUERYSINGLE);
                sendSingleMessageQuery(msg);
                queryFlag = true;
                while (queryFlag) {
                }
            }

            String[] columnValues = new String[]{selection, singleMessageGlobal};
            cursor = new MatrixCursor(columnNames, 1);
            try {
                if (getContext() == null) return cursor;
                cursor.addRow(columnValues);
                cursor.moveToFirst();
            } catch (Exception e) {
                cursor.close();
                throw new RuntimeException(e);
            }
            Log.v("query", selection);
            queryFlag = false;
            return cursor;
        }
        if (messageGlobal != null) {
            String[] columnValues = new String[2];
            cursor = new MatrixCursor(columnNames, messageGlobal.size());
            if (getContext() == null) return cursor;
            for (String key : messageGlobal.keySet()) {
                columnValues[0] = key;
                columnValues[1] = messageGlobal.get(key);
                cursor.addRow(columnValues);
            }
            try {
                cursor.moveToFirst();
            } catch (Exception e) {
                cursor.close();
                throw new RuntimeException(e);
            }
        }
        Log.v("query", selection);
        queryFlag = false;
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHashForPort(String input) throws NoSuchAlgorithmException {
        return genHash(String.valueOf((Integer.parseInt(input) / 2)));
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();

                    MessageDht msgReceived = null;
                    try {
                        msgReceived = readMessage(socket.getInputStream());
                        if (msgReceived.getMsgType() == MessageDhtType.INSERT) {
                            Log.d(SERVER_TAG, "inside insert cond 1");
                            try {
                                Map<String, String> map = msgReceived.getContentValues();
                                Map.Entry<String, String> entry = map.entrySet().iterator().next();
                                String key = entry.getKey();
                                String value = entry.getValue();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(KEY_FIELD, key);
                                contentValues.put(VALUE_FIELD, value);
                                insert(mUri, contentValues);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (msgReceived.getMsgType() == MessageDhtType.QUERYGLOBAL) {
                            Log.d(SERVER_TAG, "inside query global");
                            if (msgReceived.getSelfPort().equals(myPort)) {
                                queryFlag = false;
                                messageGlobal = msgReceived.getQueryContent();
                            } else {
                                SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
                                Map<String, String> messageLocal = (Map<String, String>) sharedPref.getAll();
                                Map<String, String> messageGlobal = msgReceived.getQueryContent();
                                if (messageGlobal != null) {
                                    messageGlobal.putAll(messageLocal);
                                }
                                msgReceived.setQueryContent(messageGlobal);
                                msgReceived.setToPort(succPort);
                                sendGlobalMessageQuery(msgReceived);
                            }
                        } else if (msgReceived.getMsgType() == MessageDhtType.QUERYSINGLE) {
                            SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
                            String message = sharedPref.getString(msgReceived.getQueryKey(), "0");
                            Log.d(SERVER_TAG, "found on " + myPort + " " + message);
                            if (message == null || message.equals("0")) {
                                msgReceived.setToPort(succPort);
                                sendSingleMessageQuery(msgReceived);
                            } else {
                                msgReceived.setToPort(msgReceived.getSelfPort());
                                msgReceived.setQueryResponse(message);
                                msgReceived.setMsgType(MessageDhtType.QUERYSINGLERESPONSE);
                                sendSingleMessageQueryResponse(msgReceived);
                            }
                        } else if (msgReceived.getMsgType() == MessageDhtType.QUERYSINGLERESPONSE) {
                            singleMessageGlobal = msgReceived.getQueryResponse();
                            queryFlag = false;
                        }
                    } catch (Exception e) {
                        Log.d(SERVER_TAG, "socket timeout for server while reading");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendSingleMessageQueryResponse(MessageDht msgReceived) {
        //clientSocket(msgReceived);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgReceived);

    }

    private void sendGlobalMessageQuery(MessageDht msg) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        //clientSocket(msg);

    }

    private void sendSingleMessageQuery(MessageDht msg) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        //clientSocket(msg);

    }

    //TODO in background
    private void sendJoinRequest(String toPort, String fromPort) {
        if (fromPort.equals("11108")) {
            succPort = fromPort;
            prePort = fromPort;
            Log.d(CLIENT_TAG, "self join" + prePort + "-->" + myPort + "-->" + succPort);
        } else {
            MessageDht msg = new MessageDht();
            msg.setMsgType(MessageDhtType.JOINREQUEST);
            msg.setToPort(toPort);
            msg.setSelfPort(fromPort);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            //Log.d(CLIENT_TAG,"join request"+toPort+"-->"+myPort+"-->"+fromPort);

        }

    }

    private void sendJoinResponse(String toPort, String pre, String succ) {
        MessageDht msg = new MessageDht();
        msg.setMsgType(MessageDhtType.JOINRESPONSE);
        msg.setToPort(toPort);
        msg.setPrePort(pre);
        msg.setSuccPort(succ);
        //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        clientSocket(msg);
    }

    private void sendJoinPreUpdate(String toPort, String pre) {
        MessageDht msg = new MessageDht();
        msg.setMsgType(MessageDhtType.JOINUPDATE);
        msg.setToPort(toPort);
        msg.setPrePort(pre);
        //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        clientSocket(msg);
    }

    private void sendInsertRequestToSucc(ContentValues values, String succPort) {
        MessageDht msg = new MessageDht();
        msg.setMsgType(MessageDhtType.INSERT);
        msg.setToPort(succPort);
        Map<String, String> map = new HashMap<String, String>();
        String key = values.getAsString(KEY_FIELD);
        String value = values.getAsString(VALUE_FIELD);
        map.put(key, value);
        msg.setContentValues(map);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        //clientSocket(msg);
    }


    private void clientSocket(MessageDht msg) {
        try {
            Socket socket;
            //if (msgs[0].getMsgType() == MessageDhtType.JOINRESPONSE) {
            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(msg.getToPort()));
            sendMessage(socket.getOutputStream(), msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ClientTask extends AsyncTask<MessageDht, Void, Void> {

        @Override
        protected Void doInBackground(MessageDht... msgs) {
            try {
                Socket socket;
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[0].getToPort()));
                sendMessage(socket.getOutputStream(), msgs[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void serverSocketInit() {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(SERVER_TAG, "Can't create a ServerSocket");
            return;
        } catch (Exception e) {
            Log.e(SERVER_TAG, "ServerTask UnknownException");
        }
    }

    private void sendMessage(OutputStream out, MessageDht msgToSend) throws Exception {
        ObjectOutputStream dOut = new ObjectOutputStream(out);
        dOut.writeObject(msgToSend);
        dOut.flush();
    }

    private MessageDht readMessage(InputStream in) throws Exception {
        ObjectInputStream dIn = new ObjectInputStream(in);
        MessageDht msgFromServer = (MessageDht) dIn.readObject();
        return msgFromServer;
    }

    private String getSuccessor(String port) {
        int index = ring.indexOf(port);
        int succIndex = index + 1;
        if (succIndex == ring.size()) {
            succIndex = 0;
        }
        return ring.get(succIndex);
    }

    private String getPredecessor(String port) {
        int index = ring.indexOf(port);
        int preIndex = index - 1;
        if (preIndex == -1) {
            preIndex = ring.size() - 1;
        }
        return ring.get(preIndex);
    }


}
