package com.example.stacks;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.util.IOUtils;
import com.example.stacks.aws.S3Uploader;
import com.example.stacks.aws.S3Utils;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_CODE_DICT= 1001;
    public static String IMAGE_FILE = "capture.jpg";
    private Button detect;
    private Button saveBtn;
    //php서버 ip
    private final String SERVER_ADDRESS = "http://13.209.67.88";
    private Camera camera =  null;
    private ScrollView scrollView;
    private ListView detectedTextListView;
    private ArrayAdapter<String> arrayAdapter;
    private File resultFile;
    S3Uploader s3uploaderObj;
    String urlFromS3 = null;
    ProgressDialog progressDialog;
    private String TAG = MainActivity.class.getCanonicalName();
    TextView tvStatus;
    ImageView ivSelectedImage;
    String imagePATH;
    File savefile;
    private static final String API_KEY = "AIzaSyAZKcZ0yPJOj_vQxZDTaapBizy8SJBhqK8";
    private static int ONE_MINUTE = 5626;
    Intent intent;
    String HOEWON_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        intent = getIntent();
        HOEWON_ID = intent.getStringExtra("id");
        //알람 호출
        new AlarmHATT(getApplicationContext()).Alarm();
        //강제적 외부 데이터베이스 접근 허용
        if(android.os.Build.VERSION.SDK_INT>9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        detect = (Button) findViewById(R.id.detect_button);
        saveBtn = (Button) findViewById(R.id.saveBtn);
        detectedTextListView = (ListView) findViewById(R.id.detectedTextListView);
        ivSelectedImage = (ImageView)findViewById(R.id.image_selected);

        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }
        });

        detectedTextListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                int check_position = detectedTextListView.getCheckedItemPosition();   //리스트뷰의 포지션을 가져옴.
                final String selected_item = (String)adapterView.getAdapter().getItem(position);  //리스트뷰의 포지션 내용을 가져옴.
                //번역
                TranslateOptions options = TranslateOptions.newBuilder()
                        .setApiKey(API_KEY)
                        .build();
                Translate translate = options.getService();
                final Translation translation =
                        translate.translate(selected_item, Translate.TranslateOption.targetLanguage("ko"));


                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(selected_item+"   "+translation.getTranslatedText());
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        insertToDatabase(selected_item.trim(), translation.getTranslatedText());
                    }
                });
                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "NO Button Click", Toast.LENGTH_SHORT).show();

                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                }

        });

        //카메라 권한 확인
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        if(permissionCheck == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},0);
            Toast.makeText(getApplicationContext(),"권한없음",Toast.LENGTH_SHORT).show();
        }else {
            final CameraSurfaceView cameraView = new CameraSurfaceView(getApplicationContext());
            FrameLayout previewFrame = (FrameLayout) findViewById(R.id.previewFrame);
            previewFrame.addView(cameraView);
            //카메라 화면 터치시
            previewFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    camera.autoFocus (new Camera.AutoFocusCallback() {
                        public void onAutoFocus(boolean success, Camera camera) {
                            if(success){
                                Toast.makeText(getApplicationContext(),"Auto Focus Success",Toast.LENGTH_SHORT).show();
                                //카메라 포커스 조절 후 화면 캡처
                                cameraView.capture(new Camera.PictureCallback() {
                                    public void onPictureTaken(byte[] data, Camera camera) {
                                        try {
                                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                            String outUriStr = MediaStore.Images.Media.insertImage(
                                                    getContentResolver(),
                                                    bitmap,
                                                    "caputred Image", "Caputred Image using Camera.");
                                            if (outUriStr == null) {
                                                Log.d("SampleCapture", "Image insert failed");
                                                return;
                                            } else {
                                                Uri outUri = Uri.parse(outUriStr);
                                                sendBroadcast(new Intent(
                                                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, outUri));
                                            }
                                            Toast.makeText(getApplicationContext(),"카메라로 찍은 사진을 앨범에 저장했습니다.",Toast.LENGTH_SHORT).show();
                                            camera.startPreview();
                                        } catch (Exception e) {
                                            Log.e("SampleCapture", "Failed to insert image", e);
                                        }
                                    }
                                });
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"Auto Focus Failed",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, DictionaryActivity.class);
                    startActivity(intent);
                }
            });

        }
    }
    private void insertToDatabase(String english, String korean){
        class InsertData extends AsyncTask<String, Void, String>{
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
            }
            @Override
            protected String doInBackground(String... params) {
                try{
                    String english = (String)params[0];
                    String korean = (String)params[1];

                    String link=SERVER_ADDRESS+"/insert.php";

                    String data  = URLEncoder.encode("english", "UTF-8") + "=" + URLEncoder.encode(english, "UTF-8");
                    data += "&" + URLEncoder.encode("korean", "UTF-8") + "=" + URLEncoder.encode(korean, "UTF-8");

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
        task.execute(english,korean);
    }

    private void insertToDatabase2(String HOEWON_ID, String PHOTO_ID, String PHOTO_DATE, String SAVE_PATH, String INPUT_ID, String INPUT_DATE){
        class InsertData extends AsyncTask<String, Void, String>{
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
            }
            @Override
            protected String doInBackground(String... params) {
                try{
                    String HOEWON_ID = (String)params[0];
                    String PHOTO_ID = (String)params[1];
                    String PHOTO_PATH= (String)params[2];
                    String SAVE_PATH= (String)params[3];
                    String INPUT_ID = (String)params[4];
                    String INPUT_DATE = (String)params[5];

                    String link=SERVER_ADDRESS+"/insert2.php";

                    String data  = URLEncoder.encode("HOEWON_ID", "UTF-8") + "=" + URLEncoder.encode(HOEWON_ID, "UTF-8");
                    data += "&" + URLEncoder.encode("PHOTO_ID", "UTF-8") + "=" + URLEncoder.encode(PHOTO_ID, "UTF-8");
                    data += "&" + URLEncoder.encode("PHOTO_DATE", "UTF-8") + "=" + URLEncoder.encode(PHOTO_PATH, "UTF-8");
                    data += "&" + URLEncoder.encode("SAVE_PATH", "UTF-8") + "=" + URLEncoder.encode(SAVE_PATH, "UTF-8");
                    data += "&" + URLEncoder.encode("INPUT_ID", "UTF-8") + "=" + URLEncoder.encode(INPUT_ID, "UTF-8");
                    data += "&" + URLEncoder.encode("INPUT_DATE", "UTF-8") + "=" + URLEncoder.encode(INPUT_DATE, "UTF-8");

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
        task.execute(HOEWON_ID, PHOTO_ID, PHOTO_DATE,  SAVE_PATH, INPUT_ID,  INPUT_DATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 23) {
            int check1 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            int check2 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (check1 != PackageManager.PERMISSION_GRANTED && check2 != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 101);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getPath(Uri uri) { //파일을 실제 주소 얻기
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Images.Media.DATA};

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    private void showExif(ExifInterface exif) {
        String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if(date == null)
            date   = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String filename = imagePATH.substring(imagePATH.lastIndexOf("/")+1);

        insertToDatabase2(HOEWON_ID,filename,date,imagePATH,HOEWON_ID,new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()).toString());
//        myAttribute += getTagString(ExifInterface.TAG_FLASH, exif);
//        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE, exif);
//        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE_REF, exif);
//        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE, exif);
//        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE_REF, exif);
//        myAttribute += getTagString(ExifInterface.TAG_IMAGE_LENGTH, exif);
//        myAttribute += getTagString(ExifInterface.TAG_IMAGE_WIDTH, exif);
//        myAttribute += getTagString(ExifInterface.TAG_MAKE, exif);
//        myAttribute += getTagString(ExifInterface.TAG_MODEL, exif);
//        myAttribute += getTagString(ExifInterface.TAG_ORIENTATION, exif);
//        myAttribute += getTagString(ExifInterface.TAG_WHITE_BALANCE, exif);

    }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                String selectedImagePath = getPath(selectedImageUri);
                if (selectedImagePath != null) {
                    detectText(selectedImagePath);
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImageUri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    ivSelectedImage.setImageBitmap(BitmapFactory.decodeStream(imageStream));
                    //uploadImageTos3(selectedImageUri);
                    imagePATH =selectedImagePath;

                    try {
                        ExifInterface exif = new ExifInterface(imagePATH);
                        showExif(exif);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
                    }
                }

            }
            if(requestCode == REQUEST_CODE_DICT){
                Toast toast = Toast.makeText(getBaseContext(),
                        "onActivity결과가 호출됨. 요청코드 : "+ requestCode + ",결과코드 : "+resultCode,Toast.LENGTH_LONG);
                toast.show();
            }
        }

    }

    //텍스트 검출
    public void detectText(String selectedImagePath) {
        File file = new File(selectedImagePath);
        try {
            InputStream in = new FileInputStream(file.getAbsolutePath());
            ByteBuffer imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(in));

            Image image = new Image();
            image.withBytes(imageBytes);

            DetectTextTask task = new DetectTextTask();
            task.execute(image);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    //Rekognition 연동
    private class DetectTextTask extends AsyncTask<Image, Void, List<TextDetection>> {
        ProgressDialog loading;
        @Override
        protected List<TextDetection> doInBackground(Image... params) {
            // Amazon Cognito 인증 공급자를 초기화합니다
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    "ap-northeast-2:d9554364-bf43-4fd5-8a09-f88a103446e6", // 자격 증명 풀 ID
                    Regions.AP_NORTHEAST_2 // 리전
            );

            AmazonRekognitionClient rekognitionClient = new AmazonRekognitionClient(credentialsProvider);

            DetectTextRequest request = new DetectTextRequest().withImage(params[0]);

            DetectTextResult result = rekognitionClient.detectText(request);

            return result.getTextDetections();
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
        }

        //결과값 string 생성
        @Override
        protected void onPostExecute(List<TextDetection> textDetections) {
            super.onPostExecute(textDetections);
            loading.dismiss();

            List<String> detectedTextList = new ArrayList<>();
            String resultstr = "";
            for (TextDetection text : textDetections) {
                resultstr = text.getDetectedText() + "\n";
                detectedTextList.add(resultstr);
            }
            arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, detectedTextList);
            detectedTextListView.setAdapter(arrayAdapter);

        }

    }

    private void makeFile(String resultstr){
        String dirPath = getFilesDir().getAbsolutePath();
        File file = new File(dirPath);
        // 일치하는 폴더가 없으면 생성
        if( !file.exists() ) {
            file.mkdirs();
        }
        // txt 파일 생성
        String testStr = resultstr;
        savefile = new File(dirPath+"/test.txt");
        try{
            FileOutputStream fos = new FileOutputStream(savefile);
            fos.write(testStr.getBytes());
            fos.close();
        } catch(IOException e){}
        // 파일이 1개 이상이면 파일 이름 출력
        if ( file.listFiles().length > 0 )
            for ( File f : file.listFiles() ) {
                String str = f.getName();
                Log.v(null,"fileName : "+str);
            }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void uploadImageTos3(Uri imageUri) {
        final String path = getPath(imageUri);
        if (path != null) {
            showLoading("Uploading details !!");
            s3uploaderObj.initUpload(path);
            s3uploaderObj.setOns3UploadDone(new S3Uploader.S3UploadInterface() {
                @Override
                public void onUploadSuccess(String response) {
                    if (response.equalsIgnoreCase("Success")) {
                        hideLoading();
                        urlFromS3 = S3Utils.generates3ShareUrl(getApplicationContext(), path);
                        if(!TextUtils.isEmpty(urlFromS3)) {
                            tvStatus.setText("Uploaded : "+urlFromS3);
                        }
                    }
                }

                @Override
                public void onUploadError(String response) {
                    hideLoading();
                    tvStatus.setText("Error : "+response);
                    Log.e(TAG, "Error Uploading");

                }
            });
        }
    }


    private void showLoading(String message) {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private class AlarmHATT{
        private Context context;
        public AlarmHATT(Context context){
            this.context = context;
        }
        public void Alarm() {
            AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(MainActivity.this, BroadcastD.class);
            PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Calendar calendar = Calendar.getInstance();
            //알람시간 calendar에 set해주기
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 14, 58
                    , 0);
            //알람 예약
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
        }

    }
    private class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;

        public CameraSurfaceView(Context context) {
            super(context);
            mHolder = getHolder();
            mHolder.addCallback(this);
            //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }


        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            try{
                camera.setPreviewDisplay(mHolder);
                int m_resWidth;
                int m_resHeight;
                m_resWidth = camera.getParameters().getPictureSize().width;
                Camera.Parameters parameters = camera.getParameters();
                m_resWidth =1280;
                m_resHeight = 720;
                parameters.setPictureSize(m_resWidth,m_resHeight);
                camera.setParameters(parameters);
            }catch (Exception e){
                Log.e("CameraSurfaceView", "Failded to set camera preview",e);
            }
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            camera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            camera.stopPreview();
            camera = null;
        }

        public boolean capture(Camera.PictureCallback handler){
            if(camera!=null){
                camera.takePicture(null,null,handler);
                return true;

            }else{
                return false;
            }
        }
    }
}




