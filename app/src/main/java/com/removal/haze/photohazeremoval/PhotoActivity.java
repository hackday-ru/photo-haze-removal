package com.removal.haze.photohazeremoval;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.removal.haze.photohazeremoval.bitmap.BitmapLoader;
import com.removal.haze.photohazeremoval.lib.Constants;
import com.removal.haze.photohazeremoval.lib.Toaster;
import com.removal.haze.photohazeremoval.lib.UriToUrl;

public class PhotoActivity extends Activity {

    private ImageView mainImageView;

    private ImageButton originalImageButton;
    private ImageButton dehazedImageButton;
    private ImageButton depthMapImageButton;

    private Uri imageUri;

    private String imageUrl;

    private int sourceId;

    private DehazeResult getDehazeResult(Bitmap src) {

        Bitmap dehazed = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        for (int x = 0; x < src.getWidth(); ++x) {
            for (int y = 0; y < src.getHeight(); ++y) {
                int pixel = src.getPixel(x, y);
                int A = Color.alpha(pixel);
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);
                dehazed.setPixel(x, y, Color.argb(A, R, (int) (1.2 * G), B));
            }
        }
        Bitmap depthMap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        for (int x = 0; x < src.getWidth(); ++x) {
            for (int y = 0; y < src.getHeight(); ++y) {
                int pixel = src.getPixel(x, y);
                int A = Color.alpha(pixel);
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);
                depthMap.setPixel(x, y, Color.argb(A, (int) (1.2 * R), G, B));
            }
        }
        return new DehazeResult(src, dehazed, depthMap);
    }

    private Bitmap downScale(Bitmap bitmap, int wantedWidth) {
        float scale = (float) wantedWidth / bitmap.getWidth();
        return Bitmap.createScaledBitmap(bitmap, wantedWidth, (int) (bitmap.getHeight() * scale), true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        Intent intent = getIntent();
        imageUri = intent.getData();
        mainImageView = (ImageView) findViewById(R.id.imageView);
        originalImageButton = (ImageButton) findViewById(R.id.originalImageButton);
        dehazedImageButton = (ImageButton) findViewById(R.id.dehazedImageButton);
        depthMapImageButton = (ImageButton) findViewById(R.id.depthMapImageButton);

        if (savedInstanceState == null) {
            sourceId = getIntent().getExtras().getInt(Constants.EXTRA_KEY_IMAGE_SOURCE);
            imageUri = getIntent().getData();
            try {
                loadImage();
            } catch (Exception e) {
                Toaster.make(getApplicationContext(), R.string.error_img_not_found);
                backToMain();
            }
        } else {
            imageUrl = savedInstanceState.getString(Constants.KEY_URL);
            sourceId = savedInstanceState.getInt(Constants.KEY_SOURCE_ID);
            setImage((Bitmap) savedInstanceState.getParcelable(Constants.KEY_BITMAP));
        }
    }


    private void loadImage() throws Exception {
        BitmapWorkerTask bitMapWorker = new BitmapWorkerTask();
        bitMapWorker.execute();
    }

    private void backToMain() {
        //recycleBitmap();

        /*if (loading_dialog.isShowing()) {
            hideLoading();
        }*/

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);

        /*if (source_id == 1) {
            UriToUrl.deleteUri(getApplicationContext(), imageUri);
        }

        if (save_status || source_id == 1) {
            UriToUrl.sendBroadcast(getApplicationContext(), outputURL);
        }*/

        finish();
    }

    private void setImage(Bitmap bitmap) {
        //hideLoading();
        try {
            if (bitmap != null) {
                mainImageView.setImageBitmap(bitmap);
                Bitmap downScaledImage = downScale(bitmap, 300);
                DehazeResult result = getDehazeResult(downScaledImage);
                originalImageButton.setImageBitmap(result.getOriginal());
                dehazedImageButton.setImageBitmap(result.getHazeRemoved());
                depthMapImageButton.setImageBitmap(result.getDepthMap());
            } else {
                Toaster.make(getApplicationContext(), R.string.error_img_not_found);
                backToMain();
            }
        } catch (Exception e) {
            Toaster.make(getApplicationContext(), e.getMessage());
            e.printStackTrace();
            backToMain();
        }
    }

    private class BitmapWorkerTask extends AsyncTask<Void, Void, Bitmap> {
        DisplayMetrics metrics;
        BitmapLoader bitmapLoader;

        public BitmapWorkerTask() {
            metrics = getResources().getDisplayMetrics();
            imageUrl = UriToUrl.get(getApplicationContext(), imageUri);
            bitmapLoader = new BitmapLoader();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //toolbox.setVisibility(View.GONE);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Void... arg0) {
            try {
                return bitmapLoader.load(getApplicationContext(), new int[]{metrics.widthPixels, metrics.heightPixels}, imageUrl);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                //toolbox.setVisibility(View.VISIBLE);
                setImage(bitmap);
            } else {
                Toaster.make(getApplicationContext(), R.string.error_img_not_found);
                backToMain();
            }
        }
    }

}
