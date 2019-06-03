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
import java.net.HttpURLConnection;
import java.net.URL;

public class BroadcastD extends BroadcastReceiver {
    String INTENT_ACTION = Intent.ACTION_BOOT_COMPLETED;
    private final String SERVER_ADDRESS = "http://13.209.67.88";
    String english;
    String korean;
    Context mcontext;
    @Override
    public void onReceive(Context context, Intent intent) {//알람 시간이 되었을때 onReceive를 호출함
        //NotificationManager 안드로이드 상태바에 메세지를 던지기위한 서비스 불러오고
        mcontext = context;
        selectToDatabase();


    }

    private void selectToDatabase() {
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
                //loading.dismiss();

                try {
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObj = (JSONObject) jsonParser.parse(result);
                    JSONArray ja = (JSONArray) jsonObj.get("response");
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

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    URL url = new URL(target);//URL 객체 생성

                    //URL을 이용해서 웹페이지에 연결하는 부분
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

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
        task.execute();
    }

}