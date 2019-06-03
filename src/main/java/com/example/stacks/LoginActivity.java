package com.example.stacks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LoginActivity extends AppCompatActivity {

    private Context mContext;

    private Button btn_custom_login;
    private Button btn_custom_logout;
    private final String SERVER_ADDRESS = "http://13.209.67.88";

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        mContext = getApplicationContext();
        btn_custom_login = (Button) findViewById(R.id.btn_custom_login);
        btn_custom_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Session session = Session.getCurrentSession();
                session.addCallback(new SessionCallback());
                session.open(AuthType.KAKAO_LOGIN_ALL, LoginActivity.this);
            }

        });

        btn_custom_logout = (Button) findViewById(R.id.btn_custom_logout);
        btn_custom_logout.setOnClickListener(new View.OnClickListener() {
                    @Override
                public void onClick(View view){
                    UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
                        @Override
                        public void onCompleteLogout() {
                            Toast.makeText(LoginActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }

        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public class SessionCallback implements ISessionCallback {
        private Context mContext;

        // 로그인에 성공한 상태
        @Override
        public void onSessionOpened() {
            requestMe();
        }

        // 로그인에 실패한 상태
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            Log.e("SessionCallback :: ", "onSessionOpenFailed : " + exception.getMessage());
        }
        // 사용자 정보 요청
        public void requestMe() {
            List<String> keys = new ArrayList<>();
            keys.add("properties.nickname");
            keys.add("properties.profile_image");
            keys.add("kakao_account.age_range");
            keys.add("kakao_account.gender");
            keys.add("kakao_account.phone_number");
            keys.add("kakao_account.email");
            // 사용자정보 요청 결과에 대한 Callback
            UserManagement.getInstance().me(keys, new MeV2ResponseCallback() {
                // 세션 오픈 실패. 세션이 삭제된 경우,
                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Log.e("SessionCallback :: ", "onSessionClosed : " + errorResult.getErrorMessage());
                }

                // 사용자정보 요청에 성공한 경우,
                @Override
                public void onSuccess(MeV2Response userProfile) {
                    Log.e("SessionCallback :: ", "onSuccess");

                    String id = String.valueOf(userProfile.getId());
                    String name = userProfile.getNickname();
                    String age_range =String.valueOf(userProfile.getKakaoAccount().getAgeRange());
                    String sex_cd = String.valueOf(userProfile.getKakaoAccount().getGender());
                    String email = userProfile.getKakaoAccount().getEmail();
                    long now = System.currentTimeMillis();
                    Date data = new Date(now);
                    String date =String.valueOf(data);

                    insertToDatabase(id, name, age_range, sex_cd, email,date);

                    Toast.makeText(LoginActivity.this, "안녕하세요 "+name+"님 :)", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("id",id);
                    startActivity(intent);

                    Log.e("Profile : ", id + "");

                    Log.e("Profile : ", name + "");

                    Log.e("Profile : ", age_range + "");

                    Log.e("Profile : ", sex_cd+ "");

                    Log.e("Profile : ", email + "");

                    Log.e("Profile : ", date+ "");

                }

                // 사용자 정보 요청 실패

                @Override

                public void onFailure(ErrorResult errorResult) {

                    Log.e("SessionCallback :: ", "onFailure : " + errorResult.getErrorMessage());

                }

            });

        }


    }

    private void insertToDatabase(String id, String name,String age_range,String sex_cd,String email,String date){
        class InsertData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(LoginActivity.this, "Please Wait", null, true, true);
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
            }
            @Override
            protected String doInBackground(String... params) {
                try{
                    String id = (String)params[0];
                    String name = (String)params[1];
                    String age_range =(String) params[2];
                    String sex_cd = (String)params[3];
                    String email = (String)params[4];
                    String date = (String)params[6];
                    if (email == null) email = "not registration";
                    String link = SERVER_ADDRESS+"/insert3.php";

                    String data  = URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(id, "UTF-8");
                    data += "&" + URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(name, "UTF-8");
                    data += "&" + URLEncoder.encode("age_range", "UTF-8") + "=" + URLEncoder.encode(age_range, "UTF-8");
                    data += "&" + URLEncoder.encode("sex_cd", "UTF-8") + "=" + URLEncoder.encode(sex_cd, "UTF-8");
                    data += "&" + URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(email, "UTF-8");
                    data += "&" + URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(id, "UTF-8");
                    data += "&" + URLEncoder.encode("date", "UTF-8") + "=" + URLEncoder.encode(date, "UTF-8");

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
        task.execute(id, name, age_range, sex_cd, email,id, date);
    }
}