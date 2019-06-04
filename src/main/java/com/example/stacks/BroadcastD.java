package com.example.stacks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class BroadcastD extends BroadcastReceiver {
    String INTENT_ACTION = Intent.ACTION_BOOT_COMPLETED;
    private final String SERVER_ADDRESS = "http://13.209.67.88";
    String english;
    String korean;
    Context mcontext;
    String HOEWON_ID;
    String PHOTO_ID;
    @Override
    public void onReceive(Context context, Intent intent) {//알람 시간이 되었을때 onReceive를 호출함
        //NotificationManager 안드로이드 상태바에 메세지를 던지기위한 서비스 불러오고
        mcontext = context;
        HOEWON_ID = intent.getStringExtra("HOEWON_ID");
        selectToDatabase(HOEWON_ID);

    }

    private void selectToDatabase(String HOEWON1_ID) {
        class selectData extends AsyncTask<String, Void, String> {
            String target;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                target = SERVER_ADDRESS + "/select.php";
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObj = (JSONObject) jsonParser.parse(result);
                    JSONArray ja = (JSONArray) jsonObj.get("response");
                    if (ja != null) {
                        JSONObject jo = (JSONObject)ja.get((int)(Math.random()*ja.size()));

                        english = String.valueOf(jo.get("english"));
                        korean = String.valueOf(jo.get("korean"));

                        NotificationManager notificationmanager = (NotificationManager) mcontext.getSystemService(Context.NOTIFICATION_SERVICE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(mcontext, 0, new Intent(mcontext, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                        Notification.Builder builder = new Notification.Builder(mcontext);
                        builder.setSmallIcon(R.drawable.account).setTicker("HETT").setWhen(System.currentTimeMillis())
                                .setNumber(1).setContentTitle(english).setContentText(korean)
                                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setContentIntent(pendingIntent).setAutoCancel(true);

                        notificationmanager.notify(1, builder.build());
                        insertToDatabase(HOEWON_ID, english,new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()).toString());
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    String HOEWON_ID = (String)params[0];
                    String postData = "HOEWON_ID=" +HOEWON_ID;
                    URL url = new URL(target);//URL 객체 생성
                    //URL을 이용해서 웹페이지에 연결하는 부분
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setConnectTimeout(5000);
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    OutputStream outputStream
                            = httpURLConnection.getOutputStream();
                    outputStream.write(postData.getBytes("UTF-8"));
                    outputStream.flush();
                    outputStream.close();

                    //바이트단위 입력스트림 생성 소스는 httpURLConnection
                    InputStream inputStream = httpURLConnection.getInputStream();

                    //웹페이지 출력물을 버퍼로 받음 버퍼로 하면 속도가 더 빨라짐
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String temp;

                    //문자열 처리를 더 빠르게 하기 위해 StringBuilder클래스를 사용함
                    StringBuilder stringBuilder = new StringBuilder();

                    //한줄씩 읽어서 stringBuilder에 저장함
                    while ((temp = bufferedReader.readLine()) != null) {
                        stringBuilder.append(temp + "\n");//stringBuilder에 넣어줌
                    }

                    //사용했던 것도 다 닫아줌
                    bufferedReader.close();
                    inputStream.close();
                    httpURLConnection.disconnect();
                    return stringBuilder.toString().trim();//trim은 앞뒤의 공백을 제거함

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;

            }
        }
        selectData task = new selectData();
        task.execute(HOEWON_ID);
    }

    private void insertToDatabase(String HOEWON_ID, String ENGLISH, String INPUT_DATE){
        class InsertData extends AsyncTask<String, Void, String>{
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
            }
            @Override
            protected String doInBackground(String... params) {
                try{
                    String HOEWON_ID = (String)params[0];
                    //String PHOTO_ID = (String)params[1];
                    String ENGLISH = (String)params[1];
                    String INPUT_DATE = (String)params[2];

                    String link=SERVER_ADDRESS+"/insert5.php";

                    String data  = URLEncoder.encode("HOEWON_ID", "UTF-8") + "=" + URLEncoder.encode(HOEWON_ID, "UTF-8");
                    //data += "&" + URLEncoder.encode("PHOTO_ID", "UTF-8") + "=" + URLEncoder.encode(PHOTO_ID, "UTF-8");
                    data += "&" + URLEncoder.encode("ENGLISH", "UTF-8") + "=" + URLEncoder.encode(ENGLISH, "UTF-8");
                    data += "&" + URLEncoder.encode("DATE", "UTF-8") + "=" + URLEncoder.encode(INPUT_DATE, "UTF-8");

                    URL url = new URL(link);
                    URLConnection conn = url.openConnection();

                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write( data );
                    wr.flush();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    StringBuilder sb = new StringBuilder();

                    String line = null;

                    // Read Server Response

                    while((line = reader.readLine()) != null)
                    {
                        sb.append(line);
                        break;
                    }
                    return sb.toString();
                }

                catch(Exception e){
                    return new String("Exception: " + e.getMessage());
                }
            }

        }
        InsertData task = new InsertData();
        task.execute(HOEWON_ID,ENGLISH,INPUT_DATE);
    }
}