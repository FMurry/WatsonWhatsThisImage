package xyz.fmsoft.watsonimagerecognition;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;
    ProgressDialog progressDialog;

    private File photoFile;
    private ArrayList<String> responses;
    private int listCount;
    @BindView(R.id.main_response)TextView response;
    @BindView(R.id.main_yes_button)Button yesButton;
    @BindView(R.id.main_no_button)Button noButton;
    @BindView(R.id.main_json)TextView json;
    @BindView(R.id.main_photo)ImageView photo;
    @BindView(R.id.main_camera_button)FloatingActionButton photoButton;
    @BindView(R.id.main_gallery_button)FloatingActionButton galleryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        listCount = 0;
        responses = new ArrayList<>();
        photoButton.setOnClickListener(this);
        galleryButton.setOnClickListener(this);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);
        photoFile = null;
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Getting Results.....");
        json.setVisibility(View.GONE);
        yesButton.setVisibility(View.GONE);
        noButton.setVisibility(View.GONE);




    }

    /**
     * Dispatch incoming result to the correct fragment.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        responses.clear();
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            //Bitmap image = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            Bitmap image = (Bitmap)data.getExtras().get("data");

            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                photoFile = createImageFile();
                FileOutputStream out = new FileOutputStream(photoFile);
                out.write(bytes.toByteArray());
                out.flush();
                out.close();
                progressDialog.show();
                new photoRecognition().execute(photoFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
            photo.setImageBitmap(image);
            photoFile.deleteOnExit();

        }
        if(requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK){
            Uri galleryUri = data.getData();
            Bitmap image = getGalleryImagePath(galleryUri);
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                photoFile = createImageFile();
                FileOutputStream out = new FileOutputStream(photoFile);
                out.write(bytes.toByteArray());
                out.flush();
                out.close();
                progressDialog.show();
                new photoRecognition().execute(photoFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
            photo.setImageBitmap(image);
            photoFile.deleteOnExit();



        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE: {
                // permission granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Thanks!", Toast.LENGTH_SHORT).show();
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                else{
                    Toast.makeText(this, "Please Grant Permission", Toast.LENGTH_SHORT).show();
                }
            }
            case REQUEST_GALLERY_IMAGE: {
                //permission granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.main_camera_button:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //Need to set permissions
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_IMAGE_CAPTURE);
                }
                else {
                    photoFile = null;
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);

                }
                break;
            case R.id.main_gallery_button:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //Need to set permissions
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_IMAGE_CAPTURE);
                }
                else{
                    photoFile = null;
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE);
                }
                break;
            case R.id.main_yes_button:
                response.setText("Nice!!!");
                noButton.setVisibility(View.GONE);
                yesButton.setVisibility(View.GONE);
                break;
            case R.id.main_no_button:
                listCount++;
                if(listCount < responses.size()){
                    response.setText(responses.get(listCount));
                }
                else{
                    response.setText("Lets take another photo");
                    noButton.setVisibility(View.GONE);
                    yesButton.setVisibility(View.GONE);
                }
        }
    }

    public File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg",storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    public Bitmap getGalleryImagePath(Uri uri) {
        if(uri == null){
            return null;
        }
        String[] data = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri,data,null,null,null);
        cursor.moveToFirst();
        int index = cursor.getColumnIndex(data[0]);
        String path = cursor.getString(index);
        cursor.close();
        cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{ MediaStore.MediaColumns._ID}
        ,MediaStore.MediaColumns.DATA + "=?", new String[] {path}, null);
        cursor.moveToFirst();
        int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
        cursor.close();
        return MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(),id,MediaStore.Images.Thumbnails.MICRO_KIND, null);


    }

    private class photoRecognition extends AsyncTask<File, Integer, String>{

        @Override
        protected String doInBackground(File... files) {
            if(files == null){

              return null;
            }else {
                if(getResources().getString(R.string.WATSON_API_KEY).equals("Omitted") || getResources().getString(R.string.WATSON_API_KEY).length() < 10){
                    Log.e(TAG, "API key may not be configured correctly");
                    return "No response, did you forget the API key?";
                }
                VisualRecognition visualRecognition = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
                visualRecognition.setApiKey(getString(R.string.WATSON_API_KEY));
                ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                        .images(photoFile)
                        .build();
                VisualClassification result = visualRecognition.classify(options).execute();
                try {
                    if (result.getImages().size() <= 0) {
                        return "Sorry didn't quite catch that";
                    } else {
                        Log.d(TAG, result.toString());
                        for (int i = 0; i < result.getImages().get(0).getClassifiers().get(0).getClasses().size(); i++) {
                            responses.add(result.getImages().get(0).getClassifiers().get(0).getClasses().get(i).getName() + "?");
                        }
                        try {
                            return "Is this a " + result.getImages().get(0).getClassifiers().get(0).getClasses().get(0).getName() + "?";
                        } catch (NullPointerException ex) {
                            return "Sorry didn't quite catch that";
                        }
                    }
                }catch (NullPointerException ex){
                    return "Sorry didn't quite catch that";
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if(s != null) {
                yesButton.setVisibility(View.VISIBLE);
                noButton.setVisibility(View.VISIBLE);
                response.setText(s);
                Log.d(TAG, s);
                progressDialog.dismiss();
            }else{
                response.setText("Sorry, didn't quite catch that");
                json.setVisibility(View.VISIBLE);
                progressDialog.dismiss();
            }
        }
    }


}
