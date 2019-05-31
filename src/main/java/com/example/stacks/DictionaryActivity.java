package com.example.stacks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DictionaryActivity extends Activity {
    private final String SERVER_ADDRESS = "http://13.209.67.88";
    private ListView listView;
    private ArrayList<String> Items;
    private ArrayAdapter<String> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dictionary);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        selectToDatabase();

    }

    private void selectToDatabase() {
        class selectData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;
            String target;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                target = SERVER_ADDRESS + "/select.php";
                loading = ProgressDialog.show(DictionaryActivity.this, "Please Wait", null, true, true);
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                loading.dismiss();
                Items = new ArrayList<String>();
//                String english = "\"korean\":\"";
//                String korean = "\"\",";
//                int start= result.indexOf(english);
//                int end =result.indexOf(korean);
//                while(result != null) {
//                    String print = result.substring(start + english.length(), end);
//                    Items.add(print);
//                }
                Items.add(result);
                adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, Items);
                listView = (ListView)findViewById(R.id.ListView);
                listView.setAdapter(adapter);

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
