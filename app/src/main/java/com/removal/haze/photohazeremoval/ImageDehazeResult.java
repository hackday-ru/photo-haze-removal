package com.removal.haze.photohazeremoval;

import android.graphics.Bitmap;

import inc.haze.lib.DehazeResult;

/**
 * Created by iisaev on 17/04/16.
 */
public class ImageDehazeResult {
    private Bitmap result;
    private Bitmap depth;

    public ImageDehazeResult(DehazeResult dehazeResult) {
        int w = dehazeResult.getWidth();
        int h = dehazeResult.getHeight();
        result = Bitmap.createBitmap(dehazeResult.getResult(), w, h, Bitmap.Config.ARGB_8888);
        depth = Bitmap.createBitmap(dehazeResult.getDepth(), w, h, Bitmap.Config.ARGB_8888);
    }

    public Bitmap getResult() {
        return result;
    }

    public Bitmap getDepth() {
        return depth;
    }
}
