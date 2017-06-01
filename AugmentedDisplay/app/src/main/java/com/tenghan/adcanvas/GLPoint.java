package com.tenghan.adcanvas;

/**
 * Created by hanteng on 2017-06-01.
 */

public final class GLPoint {

    // 3D coordinate
    float x;
    float y;
    float z;

    // texutre coordinate
    float texX;
    float texY;

    public void set(float x, float y, float z, float tX, float tY) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.texX = tX;
        this.texY = tY;
    }
}
