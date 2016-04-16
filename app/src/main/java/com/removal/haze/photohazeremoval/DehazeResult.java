package com.removal.haze.photohazeremoval;

import android.graphics.Bitmap;

/**
 * Created by iisaev on 16/04/16.
 */
public class DehazeResult {
    private final Bitmap original;
    private final Bitmap hazeRemoved;
    private final Bitmap depthMap;

    public DehazeResult(Bitmap original, Bitmap hazeRemoved, Bitmap depthMap) {
        this.original = original;
        this.hazeRemoved = hazeRemoved;
        this.depthMap = depthMap;
    }

    public Bitmap getOriginal() {
        return original;
    }

    public Bitmap getDehazed() {
        return hazeRemoved;
    }

    public Bitmap getDepthMap() {
        return depthMap;
    }
}
