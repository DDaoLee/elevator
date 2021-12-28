package kr.co.company.eleva;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.json.JSONArray;
import org.json.JSONObject;

class ThreadSendAndRecieve implements Runnable {
    private JSONObject getObj;
    private JSONObject giveObj;
    private SockClient sc = new SockClient();

    public ThreadSendAndRecieve(JSONObject getObj){
        this.getObj = getObj;
    }

    @Override
    public void run(){
        giveObj = sc.send_And_recv(getObj);
    }
    public JSONObject getResult(){
        return giveObj;
    }
}

class SockClient {


    Socket socket;

    final int port = 5000; // 포트
    final String serverIp = "192.168.5.5"; // 주소

    JSONObject recvObj = new JSONObject();
    JSONArray recvObjArray = new JSONArray();

    int size = 1024;
    byte[] recvBuffer = new byte[size];

    int recvSize = 0;
    int start_id_index = 5;
    int id_size = 7;
    int errorcode_index = 13;
    int start_datetime_index = 14;
    short code = 0;
    short errorcode = 0;

    String cmd = "";
    String ID = "";

    String datetime = "";
    String date = "";
    String time = "";

    public JSONObject send_And_recv(JSONObject obj) {
        // socket 연결
        try {
            socket = new Socket(serverIp, port);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // send(JSONObject obj) 기능
        try {

            OutputStream os = socket.getOutputStream();

            byte cmd = (byte) Integer.parseInt(String.valueOf(obj.get("cmd")));
            byte length = (byte) Integer.parseInt(String.valueOf(obj.get("length")));

            byte[] reqBuffer = new byte[7];

            reqBuffer[0] = (byte) 0xA5; //STX_1
            reqBuffer[1] = (byte) 0x5A; //STX_2
            reqBuffer[2] = length; //length_Low
            reqBuffer[3] = (byte) 0x00; //length_High
            reqBuffer[4] = cmd; //Command
            reqBuffer[5] = (byte) 0x46; //CRC_Low
            reqBuffer[6] = (byte) 0xB1; //CRC_High

            os.write(reqBuffer);
            os.flush();


        } catch (Exception e) {
            e.printStackTrace();
        }


        // recv() 기능
        try {

            InputStream is = socket.getInputStream();

            recvSize = is.read(recvBuffer);

            cmd = String.valueOf(recvBuffer[4]);

            for (int i = start_id_index; i < start_id_index + id_size; i++) {
                ID += String.valueOf(recvBuffer[i]);
            }

            while (true) {

                errorcode = 0;
                errorcode |= ((short) recvBuffer[errorcode_index] << 8) & 0xFF00;
                errorcode |= ((short) recvBuffer[errorcode_index - 1]) & 0x00FF;

                recvSize = recvSize - ((125 - errorcode) * 8) - 10;

                for (int i = start_datetime_index; i < recvSize; i = i + 8) {
                    for (int j = 0; j < 3; j++) {
                        if (recvBuffer[i + j] < 10) {
                            date = date + "0" + recvBuffer[i + j];
                        } else {
                            date += recvBuffer[i + j];
                        }

                        if (j != 2) {
                            date += "-";
                        }
                    }

                    for (int j = 3; j < 6; j++) {
                        if (recvBuffer[i + j] < 10) {
                            time = time + "0" + recvBuffer[i + j];
                        } else {
                            time += recvBuffer[i + j];
                        }

                        if (j != 5) {
                            time += ":";
                        }
                    }

                    datetime = date + " " + time;

                    code |= ((short) recvBuffer[i + 7] << 8) & 0xFF00;
                    code |= ((short) recvBuffer[i + 6]) & 0x00FF;


                    JSONObject recvObjectData = new JSONObject();
                    recvObjectData.put("datetime", datetime);
                    recvObjectData.put("code", code);
                    recvObjArray.put(recvObjectData);

                    date = time = "";
                    code = 0;
                }

                if (errorcode == 125) {
                    is.read(recvBuffer);
                } else {
                    is.close();
                    break;
                }
            }

            recvObj.put("cmd", cmd);
            recvObj.put("ID", ID);
            recvObj.put("info", recvObjArray);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return recvObj;
    }
}
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            JSONObject sendObj = new JSONObject();
            sendObj.put("cmd", (byte) 0x21);
            sendObj.put("length", (byte) 0x06);
            sendObj.put("data", null);


            ThreadSendAndRecieve sarThread = new ThreadSendAndRecieve(sendObj);
            Thread thread = new Thread(sarThread);

            thread.start();
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            JSONObject Data = sarThread.getResult();

            TextView tx = (TextView) findViewById(R.id.text);
            tx.setText(Data.toString());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

