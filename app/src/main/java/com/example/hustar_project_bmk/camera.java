package com.example.hustar_project_bmk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class camera extends AppCompatActivity {
    // --------------------------------//
    Button back_btn,btnupload,btnresult;
    ImageView img1;
    TextView upload_txt, txtname;
    private Uri imageUri;
    ProgressBar dialog;
    // -------버튼 타입 설정-----------//

    // ----------------------------//
    private static final int REQUEST_IMAGE_CAPTURE = 672;
    final static int TAKE_PICTURE = 1;
    private static final String TAG = "camera";
    private String imageFilePath;
    private Uri photoUri;
    // ------ 카메라 설정을 위한 상수 설정----------//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //-----------버튼 레이아웃 설정정-----------------/
        back_btn = (Button) findViewById(R.id.btn_back_sign);
        img1 = (ImageView) findViewById(R.id.Click_img1);
        btnupload = findViewById(R.id.nextbtn);
        btnresult  = findViewById(R.id.nextbtn_0);
        dialog = findViewById(R.id.progressBar);
        upload_txt = findViewById(R.id.textupload);
        txtname = findViewById(R.id.txtname);
        txtname.setText("책 화면을 촬영을 위해서 "+"\n"+"카메라 아이콘을 클릭해주세요");
        //-----------버튼 이벤트 설정-------------------//

        btnupload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadToFirebase();   // 업로드 함수로 이동
                upload_txt.setVisibility(View.VISIBLE);
            }
        });

        img1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTakePhotoIntent();
                //Intent galleryIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // 사진 찍는 함수 설정
                //startActivityForResult(galleryIntent, TAKE_PICTURE);
                // 카메라 찍기
                dialog.setVisibility(View.VISIBLE);
                btnresult.setVisibility(View.VISIBLE);
                btnupload.setVisibility(View.VISIBLE);

            }
        });

        // -------------------레이아웃 이동 코드 ------------------//
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(camera.this,login.class);   // camera-> login class로 이동하는 이벤트
                startActivity(myIntent); // 이벤트 시작하는 코드
                finish(); // 이벤트 종료
            }
        });
        btnresult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(camera.this, Text.class);   // camera-> camera_data class로 이동하는 이벤트
                startActivity(myIntent); // 이벤트 시작하는 코드
                finish(); // 이벤트 종료
            }
        });
    }
    // -------------------레이아웃 이동 코드 ------------------//

    //------------------- 카메라 관련 액티비티 설정  ------------//
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
            Bitmap resize = Bitmap.createScaledBitmap(bitmap, 720, 1280, true);
            ExifInterface exif = null;

            try {
                exif = new ExifInterface(imageFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int exifOrientation ;
            int exifDegree;

            if (exif != null) {
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                exifDegree = exifOrientationToDegrees(exifOrientation);

            } else {
                exifDegree = 0;
            }
            ((ImageView)findViewById(R.id.Click_img1)).setImageBitmap(rotate(resize, exifDegree));
        }
    }
    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
    private Bitmap rotate(Bitmap bitmap, float degree) {
        if(degree != 0 && bitmap != null)
        {
            Matrix m = new Matrix();
            m.setRotate(degree, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);

            try
            {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted)
                {
                    bitmap.recycle();
                    bitmap = converted;
                }
            }
            catch(OutOfMemoryError ex)
            {
                // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
            }
        }
        return bitmap;
    }
    private void sendTakePhotoIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, getPackageName(), photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TEST_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,      /* prefix */
                ".jpg",         /* suffix */
                storageDir          /* directory */
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }
//-----------------------카메라 관련 액티비티 설정---------------------//


    // ------------------------ 카메라 퍼미션 설정 -----------------//
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            Log.d("로그", "Permission: " + permissions[0] + " was " + grantResults[0]);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    // ------------------------ 카메라 퍼미션 설정 -----------------//


    // ---- 파이어베이스 사진 업로드  -------------------------------//
    private  void uploadToFirebase() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // 파이어베이스 참조 설정
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        // 현재 년월일 시간 지정
        Date now = new Date();
        // 현재 날짜 클래스 설정
        //String filename = formatter.format(now) + ".png";
        // 현재 날짜를 변환하여 문자열로 전환

        String filename2 = "2" + ".png";

        StorageReference storageRef = storage.getReferenceFromUrl("gs://hustar-9eeb8.appspot.com").child("my_folder/" + filename2);
        // 현재 파이어베이스에서 접속하고자하는 url 및 폴더 및 저장할 이름설정
        storageRef.putFile(photoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            // 이미지를 파이어베이스에 저장


            // 저장 성공
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(getApplicationContext(), "업로드 완료!", Toast.LENGTH_SHORT).show();
            }
            // 저장 실패
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "업로드 실패!", Toast.LENGTH_SHORT).show();
            }
            // 저장 중 로딩 시간
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                // ------- 저장 중 로딩 시간을 구하는 식 --------------//
                int bytesTransferred = (int) snapshot.getBytesTransferred();
                int totalBytes = (int) snapshot.getTotalByteCount();
                int progress = (100 * bytesTransferred) / totalBytes;
                //System.out.println("t12321"+progress);
                dialog.setProgress(progress);
                if (progress == 100)
                {
                    upload_txt.setText("결과화면으로 넘어가세요");
                }

                // ------- 저장 중 로딩 시간을 구하는 식 --------------//
            }
        });

        //----------카메라 권한 요청  ---------------------------------//
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

    }
        //----------카메라 권한 요청  ---------------------------------//
}


