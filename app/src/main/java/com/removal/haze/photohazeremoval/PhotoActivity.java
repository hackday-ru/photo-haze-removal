package com.removal.haze.photohazeremoval;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.removal.haze.photohazeremoval.bitmap.BitmapLoader;
import com.removal.haze.photohazeremoval.lib.Constants;
import com.removal.haze.photohazeremoval.lib.Toaster;
import com.removal.haze.photohazeremoval.lib.UriToUrl;

import java.io.File;
import java.math.BigInteger;
import java.util.Random;

import inc.haze.lib.DehazeResult;
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

    private ProgressBar dehazedProgressBar;
    private ProgressBar depthMapProgressBar;

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
        //return new DehazeResult(src, dehazed, depthMap);
        return null;
    }

    private Bitmap downScale(Bitmap bitmap, int wantedWidth) {
        float scale = (float) wantedWidth / bitmap.getWidth();
        return Bitmap.createScaledBitmap(bitmap, wantedWidth, (int) (bitmap.getHeight() * scale), true);
    }

    private class BitMapPair {
        Bitmap original;
        Bitmap scaled;

        public BitMapPair(Bitmap original, Bitmap scaled) {
            this.original = original;
            this.scaled = scaled;
        }
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

    private int getDisplayWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    public String nextFileName() {
        return new BigInteger(130, random).toString(32);
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
        dehazedProgressBar = (ProgressBar) findViewById(R.id.dehazedProgressBar);

        depthMapImageButton = (ImageButton) findViewById(R.id.depthMapImageButton);
        depthMapProgressBar = (ProgressBar) findViewById(R.id.depthMapProgressBar);
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

    void saveToFile(Bitmap bitmap) {
        File file = new File(".");
        Uri theUri = Uri.fromFile(file).buildUpon().scheme("file.new").build();
        Intent theIntent = new Intent(Intent.ACTION_PICK);
        theIntent.setData(theUri);
        theIntent.putExtra(Intent.EXTRA_TITLE, "A Custom Title"); //optional
        theIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); //optional
        try {
            startActivityForResult(theIntent, 123);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 123: {
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    String theFilePath = data.getData().getPath();
                }
                break;
            }
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
            buttonIcon = getButtonBitmap(bitmap);
            if (bitmap.getWidth() < DOWNSCALE_WIDTH) {
                originalImage = bitmap;
            } else {
                originalImage = downScale(bitmap, DOWNSCALE_WIDTH);
            }
            setImage(originalImage);
            originalImageButton.setImageBitmap(buttonIcon);
            dehazedImageButton.setImageBitmap(buttonIcon);
            depthMapImageButton.setImageBitmap(buttonIcon);
         /*   try {
                progressDialog = ProgressDialog.show(getBaseContext(), "Loading", "");
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            new BitmapWorkerTask().execute(originalImage, buttonIcon);
            super.onPostExecute(bitmap);
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
            photoViewAttacher.update();
            super.onPreExecute();
        }

        // Decode image in background.
        @Override
        protected ResultOfProcessing doInBackground(Bitmap... arg0) {
            try {
                Bitmap bitmap = arg0[0];
                if (bitmap != null) {
                    try {
                        downScaledDehazeResult = dehaze(buttonIcon);
                        originalDehazeResult = dehaze(arg0[1]);
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

                dehazedImageButton.setImageBitmap(downScaledDehazeResult.getResult());
                depthMapImageButton.setImageBitmap(downScaledDehazeResult.getDepth());


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

                dehazedProgressBar.setVisibility(View.INVISIBLE);
                depthMapProgressBar.setVisibility(View.INVISIBLE);
                //progressDialog.hide();

                saveImageButton.setVisibility(View.VISIBLE);
                saveImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                       /* try {
                            sendBroadcast(new Intent(
                                    Intent.ACTION_MEDIA_MOUNTED,
                                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        new AsyncTask<Void, Void, Integer>() {
                            @Override
                            protected Integer doInBackground(Void... voids) {
                                try {
                                    String sourceUrl = MediaStore.Images.Media.insertImage(getContentResolver(), res.originalResult.getSource(), "source1", "");
                                    String resultUrl = MediaStore.Images.Media.insertImage(getContentResolver(), res.originalResult.getResult(), "result1", "");
                                    String depthUrl = MediaStore.Images.Media.insertImage(getContentResolver(), res.originalResult.getDepth(), "depth1", "");
                                    if (sourceUrl == null || resultUrl == null || depthUrl == null) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return -1;
                            }

                            @Override
                            protected void onPostExecute(Integer res) {
                                if (res == 0) {
                                    Toaster.make(getApplicationContext(), "Files were saved to gallery");
                                } else {
                                    Toaster.make(getApplicationContext(), "Failed to save images");
                                }
                                super.onPostExecute(res);
                            }
                        };//.execute();
                        saveImageButton.setVisibility(View.INVISIBLE);*/
                        //saveToFile(res.originalResult.getSource());
                    }
                });
            }
        }
    }

}
