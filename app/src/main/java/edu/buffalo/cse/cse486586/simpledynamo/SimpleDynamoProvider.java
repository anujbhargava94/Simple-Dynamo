package edu.buffalo.cse.cse486586.simpledynamo;

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
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static AtomicBoolean queryFlag = new AtomicBoolean(false);
    Map<String, String> messageGlobal;
    String singleMessageGlobal;
    private static String PREFNAME = "PREF";
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    AtomicBoolean inserting = new AtomicBoolean(false);
    AtomicBoolean deleteFlag = new AtomicBoolean(false);
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
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(selection);
        editor.apply();
        String ownerPort = getOwnerPortByHash(selection);
        MessageDht msg = new MessageDht();
        msg.setMsgType(MessageDhtType.DELETE);
        msg.setMessageOwner(ownerPort);
        msg.setToPort(ownerPort);
        msg.setSelfPort(myPort);
        msg.setQueryKey(selection);
        deleteFlag.set(true);
        sendMessage(msg);
        while (deleteFlag.get()) {

        }
        deleteFlag.set(false);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        if (values == null || values.size() < 1) {
            return null;
        }
        String key = values.getAsString(KEY_FIELD);
        String value = values.getAsString(VALUE_FIELD);

        try {
            String ownerPort = getOwnerPortByHash(key);
            Pair<String, String> msgContent = new Pair<String, String>(key, value);
            MessageDht msg = new MessageDht();
            msg.setMsgType(MessageDhtType.INSERT);
            msg.setMessageOwner(ownerPort);
            msg.setContent(msgContent);
            msg.setToPort(ownerPort);
            inserting.set(true);
            sendMessage(msg);
            while (inserting.get()) {

            }
            inserting.set(false);
//            SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
//            SharedPreferences.Editor editor = sharedPref.edit();
//            editor.putString(key, value);
//            editor.commit();
//            Log.v("inserted", values.toString());
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
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
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
            sendMessage(messageDht);
            queryFlag.set(true);
            while (queryFlag.get()) {

            }

        } else {

            String ownerPort = getOwnerPortByHash(selection);
            MessageDht msg = new MessageDht();
            Pair<String, String> queryContent = new Pair<String, String>(selection, null);
            msg.setContent(queryContent);
            msg.setQueryKey(selection);
            msg.setMessageOwner(ownerPort);
            msg.setToPort(ownerPort);
            msg.setSelfPort(myPort);
            msg.setMsgType(MessageDhtType.QUERYSINGLE);
            sendMessage(msg);
            queryFlag.set(true);
            while (queryFlag.get()) {
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
            queryFlag.set(false);
            return cursor;
        }
        if (messageGlobal != null) {
            String[] columnValues = new String[2];
            cursor = new MatrixCursor(columnNames, messageGlobal.size());
            if (getContext() == null) return cursor;
            for (String key : messageGlobal.keySet()) {
                columnValues[0] = key;
                //columnValues[1] = messageGlobal.get(key);
                String msgDhtJson = messageGlobal.get(key);
                MessageDht msg = new MessageDht();
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<MessageDht>() {
                    }.getType();
                    msg = gson.fromJson(msgDhtJson, type);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                columnValues[1] = msg.getContent().second;
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
        queryFlag.set(false);
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
                                Gson json = new Gson();
                                String key = msgReceived.getContent().first;
                                String value = json.toJson(msgReceived);
                                insertInPref(key, value);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (msgReceived.getMsgType() == MessageDhtType.QUERYGLOBAL) {
                            Log.d(SERVER_TAG, "inside query global");
                            SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
                            Map<String, String> messageLocal = (Map<String, String>) sharedPref.getAll();
                            Map<String, String> messageGlobal = msgReceived.getQueryContent();
                            if (messageGlobal != null) {
                                messageGlobal.putAll(messageLocal);
                            }
                            msgReceived.setQueryContent(messageGlobal);
                            sendMessage(socket.getOutputStream(), msgReceived);
                        } else if (msgReceived.getMsgType() == MessageDhtType.QUERYSINGLE) {
                            SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
                            String queryValue = sharedPref.getString(msgReceived.getQueryKey(), null);
                            Pair<String, String> query = new Pair<String, String>(msgReceived.getQueryKey(), queryValue);
                            msgReceived.setContent(query);
                            msgReceived.setQueryResponse(queryValue);
                            sendMessage(socket.getOutputStream(), msgReceived);
                        } else if (msgReceived.getMsgType() == MessageDhtType.DELETE) {
                            Log.d(SERVER_TAG, "inside delete");
                            SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.remove(msgReceived.getQueryKey());
                            editor.apply();
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

    private void sendMessage(MessageDht msg) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
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
    }


    private void clientSocket(MessageDht msg) {
        try {
            Socket socket;
            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(msg.getToPort()));
            try {
                sendMessage(socket.getOutputStream(), msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ClientTask extends AsyncTask<MessageDht, Void, Void> {

        @Override
        protected Void doInBackground(MessageDht... msgs) {
            try {
                Socket socket;
                if (msgs[0].getMsgType() == MessageDhtType.INSERT) {
                    for (int i = 0; i < 3; i++) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(msgs[0].getToPort()));
                        try {
                            sendMessage(socket.getOutputStream(), msgs[0]);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        msgs[0].setToPort(getSuccessor(msgs[0].getToPort()));
                    }
                    inserting.set(false);
                } else if (msgs[0].getMsgType() == MessageDhtType.QUERYSINGLE) {
                    MessageDht[] msgReceived = new MessageDht[3];
                    String tailPort = getSuccessor(getSuccessor(msgs[0].getToPort()));
                    String finalValue = null;
                    String tailValue = null;
                    String nonNullValue = null;
                    for (int i = 0; i < 3; i++) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(msgs[0].getToPort()));
                        try {
                            sendMessage(socket.getOutputStream(), msgs[0]);
                            msgReceived[i] = readMessage(socket.getInputStream());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        msgs[0].setToPort(getSuccessor(msgs[0].getToPort()));
                    }

                    Map<String, Integer> map = new HashMap<String, Integer>();
                    for (int i = 0; i < 3; i++) {
                        if (msgReceived[i] != null && msgReceived[i].getContent() != null) {
                            nonNullValue = msgReceived[i].getContent().second;
                            if (msgReceived[i].getSelfPort().equals(tailPort)) {
                                tailValue = msgReceived[i].getContent().second;
                            }
                            String val = msgReceived[i].getContent().second;
                            if (map.containsKey(val)) {
                                map.put(msgReceived[i].getContent().second, map.get(val) + 1);
                            } else {
                                map.put(msgReceived[i].getContent().second, 1);
                            }
                        }
                    }

                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        if (entry.getValue() > 1) {
                            finalValue = entry.getKey();
                        }
                    }
                    if (finalValue == null) {
                        if (tailValue != null) {
                            finalValue = tailValue;
                        } else {
                            finalValue = nonNullValue;
                        }
                    }
                    singleMessageGlobal = finalValue;
                    queryFlag.set(false);

                } else if (msgs[0].getMsgType() == MessageDhtType.DELETE) {
                    for (int i = 0; i < 3; i++) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(msgs[0].getToPort()));
                        try {
                            sendMessage(socket.getOutputStream(), msgs[0]);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        msgs[0].setToPort(getSuccessor(msgs[0].getToPort()));
                    }
                    deleteFlag.set(false);
                } else if (msgs[0].getMsgType() == MessageDhtType.QUERYGLOBAL) {
                    for (String entry : ring) {
                        if (!entry.equals(myPort)) {
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(entry));
                            try {
                                sendMessage(socket.getOutputStream(), msgs[0]);
                                msgs[0] = readMessage(socket.getInputStream());

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    messageGlobal = msgs[0].getQueryContent();
                    queryFlag.set(false);
                }
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

    private String getOwnerPortByHash(String key) {
        String port = null;
        try {
            String keyHash = genHash(key);
            String currPortHash = genHashForPort(ring.get(0));
            String prePortHash = genHashForPort(ring.get(4));

            if ((prePortHash.compareTo(keyHash) > 0 && currPortHash.compareTo(keyHash) > 0) ||
                    (prePortHash.compareTo(keyHash) < 0 && currPortHash.compareTo(keyHash) < 0)) {
                return ring.get(0);
            }

            for (int i = 1; i < ring.size(); i++) {

                currPortHash = genHashForPort(ring.get(i));
                prePortHash = genHashForPort(ring.get(i - 1));
                if (prePortHash.compareTo(keyHash) > 0 && currPortHash.compareTo(keyHash) < 0) {
                    return ring.get(i);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(CLIENT_TAG, e.toString());
        }
        return port;
    }

    private void insertInPref(String key, String value) {
        SharedPreferences sharedPref = getContext().getSharedPreferences(PREFNAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
        Log.v("inserted", value.toString());
    }


}
