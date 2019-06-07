package com.example.stacks;

//import android.app.Notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
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
    String SAVE_PATH;
    @Override
    public void onReceive(Context context, Intent intent) {//알람 시간이 되었을때 onReceive를 호출함
        //NotificationManager 안드로이드 상태바에 메세지를 던지기위한 서비스 불러오고
        mcontext = context;
        HOEWON_ID = intent.getStringExtra("HOEWON_ID");
        if (Get_Internet(mcontext) !=0) {
            selectToDatabase(HOEWON_ID);
         }

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
                    if(ja.size()!= 0) {
                        JSONObject jo = (JSONObject) ja.get((int) (Math.random() * ja.size()));
                        english = String.valueOf(jo.get("english"));
                        korean = String.valueOf(jo.get("korean"));
                        Log.e("EEEEE",english);

                        selectToDatabase2(HOEWON_ID, english);


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
    public static int Get_Internet(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return 1;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return 2;
            }
        }
        return 0;
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

    private void selectToDatabase2(final String HOEWON_ID, final String ENGLISH) {
        class selectData2 extends AsyncTask<String, Void, String> {
            String target;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                target = SERVER_ADDRESS + "/select2.php";
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                try{
                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObj= (JSONObject)jsonParser.parse(result);
                    JSONArray ja = (JSONArray)jsonObj.get("response");
                    if(ja.size()!= 0) {
                        JSONObject jo = (JSONObject) ja.get(0);
                        NotificationManager notificationmanager = (NotificationManager) mcontext.getSystemService(Context.NOTIFICATION_SERVICE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(mcontext, 0, new Intent(mcontext, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

                        SAVE_PATH = String.valueOf(jo.get("SAVE_PATH"));
                        File imgFile = new  File(SAVE_PATH);

                        NotificationCompat.Builder builder;
                        if(android.os.Build.VERSION.SDK_INT >= 26){
                            NotificationChannel mChannel = new NotificationChannel(
                                    "channelID", "STACKs_Channel", NotificationManager.IMPORTANCE_HIGH);
                            notificationmanager.createNotificationChannel(mChannel);
                            builder = new NotificationCompat.Builder(mcontext,mChannel.getId());
                        }else {
                            builder = new NotificationCompat.Builder(mcontext);
                        }

                        builder.setSmallIcon(R.drawable.account).setTicker("HETT").setWhen(System.currentTimeMillis())
                                .setNumber(1).setContentTitle(english).setContentText(korean)
                                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS).setContentIntent(pendingIntent).setAutoCancel(true)
                                .setChannelId("channelID");
                        builder.setPriority(NotificationCompat.PRIORITY_HIGH);    //요거해줘야지 위에서 내려오는거 보임

                        if(imgFile.exists()) {
                            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            builder.setStyle(new NotificationCompat.BigPictureStyle()
                                    // bigPicture의 이미지 파일은 Bitmap으로 처리되어야 합니다.
                                    .bigPicture(myBitmap));
                            notificationmanager.notify(0, builder.build());
                        }

                        insertToDatabase(HOEWON_ID, english, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()).toString());

                        Log.e("EEEEE",SAVE_PATH);
                    }
                }catch(ParseException e){
                    e.printStackTrace();
                }

            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    String HOEWON_ID = (String)params[0];
                    String ENGLISH = (String)params[1];

                    String postData = "HOEWON_ID="+HOEWON_ID;
                    postData +="&ENGLISH="+ENGLISH;
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
        selectData2 task = new selectData2();
        task.execute(HOEWON_ID,ENGLISH);
    }

}