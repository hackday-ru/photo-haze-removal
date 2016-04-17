package com.removal.haze.photohazeremoval;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.removal.haze.photohazeremoval.bitmap.BitmapLoader;
import com.removal.haze.photohazeremoval.lib.Constants;
import com.removal.haze.photohazeremoval.lib.Toaster;
import com.removal.haze.photohazeremoval.lib.UriToUrl;

import java.util.Random;

import inc.haze.lib.GuidedFilter;
import inc.haze.lib.HazeRemover;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoActivity extends AppCompatActivity {

    private ImageView mainImageView;

    private ImageButton originalImageButton;
    private int buttonWidth;
    private ImageButton dehazedImageButton;
    private ImageButton depthMapImageButton;

    private ImageDehazeResult downScaledDehazeResult;
    private ImageDehazeResult originalDehazeResult;

    private final HazeRemover hazeRemover = new HazeRemover(new GuidedFilter(), 1500, 1500);

    private PhotoViewAttacher photoViewAttacher;

    private Uri imageUri;

    private String imageUrl;
    private ImageButton saveImageButton;

    private Random random = new Random();
    private int sourceId;

    private static final int DOWNSCALE_WIDTH = 1024;

    private ProgressDialog progressDialog;

    Bitmap originalImage;
    Bitmap buttonIcon;

    private Bitmap downScale(Bitmap bitmap, int wantedWidth) {
        float scale = (float) wantedWidth / bitmap.getWidth();
        return Bitmap.createScaledBitmap(bitmap, wantedWidth, (int) (bitmap.getHeight() * scale), true);
    }

    private void setButtonImage(ImageButton button, Bitmap bitmap) {
        button.setImageBitmap(bitmap);
    }

    private Bitmap getButtonBitmap(Bitmap src) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int expectedWidth = buttonWidth;
        float scale = (float) buttonWidth / src.getWidth();
        int expectedHeight = (int) (src.getHeight() * scale);
        if (5 * expectedHeight > metrics.heightPixels) {
            expectedHeight = metrics.heightPixels / 5;
            scale = (float) expectedHeight / src.getHeight();
            expectedWidth = (int) (buttonWidth * scale);
        }
        return Bitmap.createScaledBitmap(src, expectedWidth, expectedHeight, true);
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
        saveImageButton = (ImageButton) findViewById(R.id.saveImageButton);
        saveImageButton.setVisibility(View.INVISIBLE);

        if (savedInstanceState == null) {
            sourceId = getIntent().getExtras().getInt(Constants.EXTRA_KEY_IMAGE_SOURCE);
            imageUri = getIntent().getData();
            try {
                loadImage();
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.make(getApplicationContext(), R.string.error_img_not_found);
                backToMain();
            }
        } else {
            imageUrl = savedInstanceState.getString(Constants.KEY_URL);
            sourceId = savedInstanceState.getInt(Constants.KEY_SOURCE_ID);
            setImage((Bitmap) savedInstanceState.getParcelable(Constants.KEY_BITMAP));
        }
        // Attach a PhotoViewAttacher, which takes care of all of the zooming functionality.
        photoViewAttacher = new PhotoViewAttacher(mainImageView);

    }


    private void loadImage() throws Exception {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        buttonWidth = metrics.widthPixels / 3;

        BitmapLoaderTask bitMapLoader = new BitmapLoaderTask();
        bitMapLoader.execute();
        //BitmapWorkerTask bitMapWorker = new BitmapWorkerTask();
        //bitMapWorker.execute();
    }

    private void backToMain() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private class ResultOfProcessing {
        private ImageDehazeResult originalResult;
        private ImageDehazeResult downScaledResult;

        public ResultOfProcessing(ImageDehazeResult originalResult, ImageDehazeResult downScaledResult) {
            this.setOriginalResult(originalResult);
            this.setDownScaledResult(downScaledResult);
        }

        public ImageDehazeResult getOriginalResult() {
            return originalResult;
        }

        public void setOriginalResult(ImageDehazeResult originalResult) {
            this.originalResult = originalResult;
        }

        public ImageDehazeResult getDownScaledResult() {
            return downScaledResult;
        }

        public void setDownScaledResult(ImageDehazeResult downScaledResult) {
            this.downScaledResult = downScaledResult;
        }
    }

    private void setImage(Bitmap bitmap) {
        //hideLoading();
        try {
            if (bitmap != null) {
                mainImageView.setImageBitmap(bitmap);
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

    @Override
    public void onBackPressed() {
        backToMain();
    }

    private class BitmapLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        DisplayMetrics metrics;
        BitmapLoader bitmapLoader;

        public BitmapLoaderTask() {
            metrics = getResources().getDisplayMetrics();
            imageUrl = UriToUrl.get(getApplicationContext(), imageUri);
            bitmapLoader = new BitmapLoader();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                return bitmapLoader.load(getApplicationContext(), new int[]{metrics.widthPixels, metrics.heightPixels}, imageUrl);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            try {
                super.onPostExecute(bitmap);
                buttonIcon = getButtonBitmap(bitmap);
                if (bitmap.getWidth() < DOWNSCALE_WIDTH) {
                    originalImage = bitmap;
                } else {
                    originalImage = downScale(bitmap, DOWNSCALE_WIDTH);
                }
                setImage(originalImage);
                photoViewAttacher.update();

                setButtonImage(originalImageButton, buttonIcon);
                setButtonImage(dehazedImageButton, buttonIcon);
                setButtonImage(depthMapImageButton, buttonIcon);

                progressDialog = ProgressDialog.show(PhotoActivity.this,
                        "Please wait", "Haze removal algorithm is in progress");

                new BitmapWorkerTask().execute(originalImage, buttonIcon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class BitmapWorkerTask extends AsyncTask<Bitmap, Void, ResultOfProcessing> {
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
        }

        // Decode image in background.
        @Override
        protected ResultOfProcessing doInBackground(Bitmap... arg0) {
            try {
                Bitmap bitmap = arg0[0];
                if (bitmap != null) {
                    try {
                        ImageDehazeResult originalDehazeResult = dehaze(arg0[0]);
                        ImageDehazeResult downScaledDehazeResult = dehaze(arg0[1]);
                        return new ResultOfProcessing(originalDehazeResult, downScaledDehazeResult);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toaster.make(getApplicationContext(), R.string.error_img_not_found);
                    backToMain();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }

        private ImageDehazeResult dehaze(Bitmap src) {
            int[] pixels = new int[src.getWidth() * src.getHeight()];
            src.getPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
            return new ImageDehazeResult(hazeRemover.dehaze(pixels, src.getHeight(), src.getWidth()));
        }

        @Override
        protected void onPostExecute(final ResultOfProcessing res) {
            if (res != null) {
                downScaledDehazeResult = res.getDownScaledResult();
                originalDehazeResult = res.getOriginalResult();

                setButtonImage(dehazedImageButton, downScaledDehazeResult.getResult());
                setButtonImage(depthMapImageButton, downScaledDehazeResult.getDepth());


                originalImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setImage(originalImage);
                    }
                });

                dehazedImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setImage(originalDehazeResult.getResult());
                    }
                });

                depthMapImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setImage(originalDehazeResult.getDepth());
                    }
                });

                progressDialog.dismiss();
                saveImageButton.setVisibility(View.VISIBLE);
            }
        }
    }

}
