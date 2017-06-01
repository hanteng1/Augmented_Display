package com.tenghan.adcanvas;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by hanteng on 2017-05-31.
 *
 * Page class
 * used to hold content
 * used to hold textures for animation effects
 */

public class Page {

    private final static int TEXTURE_SIZE = 3;
    private final static int FIRST_TEXTURE_ID = 0;
    private final static int SECOND_TEXTURE_ID = 1;
    private final static int BACK_TEXTURE_ID = 2;
    private final static int INVALID_TEXTURE_ID = -1;

    //when page is static
    private final static int[][] mPageApexOrders = new int[][] {
            new int[] {0, 1, 2, 3}, // for case A
            new int[] {1, 0, 3, 2}, // for case B
            new int[] {2, 3, 0, 1}, // for case C
            new int[] {3, 2, 1, 0}, // for case D
    };


    //when page is curled
    private final static int[][] mFoldVexOrders = new int[][] {
            new int[] {4, 3, 1, 2, 0}, // Case A
            new int[] {3, 3, 2, 0, 1}, // Case B
            new int[] {3, 2, 1, 3, 0}, // Case C
            new int[] {2, 2, 3, 1, 0}, // Case D
            new int[] {1, 0, 1, 3, 2}, // Case E
    };

    // page size
    float left;
    float right;
    float top;
    float bottom;
    float width;
    float height;

    //texture size, normally the same with page size
    float texWidth;
    float texHeight;

    //origin point and diagonal point
    GLPoint originP;
    GLPoint diagonalP;

    private GLPoint mXFoldP;
    private GLPoint mYFoldP;

    // vertexes and texture coordinates buffer for full page
    private FloatBuffer mFullPageVexBuf;
    private FloatBuffer mFullPageTexCoordsBuf;

    // storing 4 apexes data of page
    private float[] mApexes;
    // texture coordinates for page apex
    private float[] mApexTexCoords;
    // vertex size of front of fold page and unfold page
    private int mFrontVertexSize;
    // index of apex order array for current original point
    private int mApexOrderIndex;

    // mask color of back texture
    float[][] maskColor;

    // texture(front, back and second) ids allocated by OpenGL
    private int[] mTexIDs;
    // unused texture ids, will be deleted when next OpenGL drawing
    private int[] mUnusedTexIDs;
    // actual size of mUnusedTexIDs
    private int mUnusedTexSize;

    public Page() {
        init(0, 0, 0, 0);
    }
    public Page(float l, float r, float t, float b) {
        init(l, r, t, b);
    }

    private void init(float l, float r, float t, float b) {
        top = t;
        left = l;
        right = r;
        bottom = b;
        width = right - left;
        height = top - bottom;
        texWidth = width;
        texHeight = height;
        mFrontVertexSize = 0;
        mApexOrderIndex = 0;

        mXFoldP = new GLPoint();
        mYFoldP = new GLPoint();
        originP = new GLPoint();
        diagonalP = new GLPoint();

        maskColor = new float[][] {new float[] {0, 0, 0},
                new float[] {0, 0, 0},
                new float[] {0, 0, 0}};

        mTexIDs = new int[] {INVALID_TEXTURE_ID,
                INVALID_TEXTURE_ID,
                INVALID_TEXTURE_ID};
        mUnusedTexSize = 0;
        mUnusedTexIDs = new int[] {INVALID_TEXTURE_ID,
                INVALID_TEXTURE_ID,
                INVALID_TEXTURE_ID};

        createVertexesBuffer();
        buildVertexesOfFullPage();
    }

    public boolean isLeftPage() {
        return right <= 0;
    }
    public boolean isRightPage() {
        return left >= 0;
    }

    public float width() {
        return width;
    }
    public float height() {return height; }

    public boolean isFirstTextureSet() {
        return mTexIDs[FIRST_TEXTURE_ID] != INVALID_TEXTURE_ID;
    }
    public boolean isSecondTextureSet() {
        return mTexIDs[SECOND_TEXTURE_ID] != INVALID_TEXTURE_ID;
    }
    public boolean isBackTextureSet() {
        return mTexIDs[BACK_TEXTURE_ID] != INVALID_TEXTURE_ID;
    }

    //delete unused texture ids
    public void deleteUnusedTextures() {
        if (mUnusedTexSize > 0) {
            glDeleteTextures(mUnusedTexSize, mUnusedTexIDs, 0);
            mUnusedTexSize = 0;
        }
    }

    public Page setFirstTextureWithSecond() {
        if (mTexIDs[FIRST_TEXTURE_ID] > INVALID_TEXTURE_ID) {
            mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[FIRST_TEXTURE_ID];
        }

        maskColor[FIRST_TEXTURE_ID][0] = maskColor[SECOND_TEXTURE_ID][0];
        maskColor[FIRST_TEXTURE_ID][1] = maskColor[SECOND_TEXTURE_ID][1];
        maskColor[FIRST_TEXTURE_ID][2] = maskColor[SECOND_TEXTURE_ID][2];
        mTexIDs[FIRST_TEXTURE_ID] = mTexIDs[SECOND_TEXTURE_ID];
        mTexIDs[SECOND_TEXTURE_ID] = INVALID_TEXTURE_ID;
        return this;
    }

    public Page setSecondTextureWithFirst() {
        if (mTexIDs[SECOND_TEXTURE_ID] > INVALID_TEXTURE_ID) {
            mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[SECOND_TEXTURE_ID];
        }

        maskColor[SECOND_TEXTURE_ID][0] = maskColor[FIRST_TEXTURE_ID][0];
        maskColor[SECOND_TEXTURE_ID][1] = maskColor[FIRST_TEXTURE_ID][1];
        maskColor[SECOND_TEXTURE_ID][2] = maskColor[FIRST_TEXTURE_ID][2];
        mTexIDs[SECOND_TEXTURE_ID] = mTexIDs[FIRST_TEXTURE_ID];
        mTexIDs[FIRST_TEXTURE_ID] = INVALID_TEXTURE_ID;
        return this;

    }

    public Page swapTexturesWithPage(Page page) {
        // [second page]: second -> first
        mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[SECOND_TEXTURE_ID];
        mTexIDs[SECOND_TEXTURE_ID] = mTexIDs[FIRST_TEXTURE_ID];

        // [first page] first -> [second page] back of first
        mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[BACK_TEXTURE_ID];
        mTexIDs[BACK_TEXTURE_ID] = page.mTexIDs[FIRST_TEXTURE_ID];

        // [first page] back of first -> [second page] first
        mTexIDs[FIRST_TEXTURE_ID] = page.mTexIDs[BACK_TEXTURE_ID];
        page.mTexIDs[BACK_TEXTURE_ID] = INVALID_TEXTURE_ID;

        // [first page] second -> [first page] first
        page.mTexIDs[FIRST_TEXTURE_ID] = page.mTexIDs[SECOND_TEXTURE_ID];
        page.mTexIDs[SECOND_TEXTURE_ID] = INVALID_TEXTURE_ID;
        return this;
    }

    int getBackTextureID() {
        // In single page mode, the back texture is same with the first texture
        if (mTexIDs[BACK_TEXTURE_ID] == INVALID_TEXTURE_ID) {
            return mTexIDs[FIRST_TEXTURE_ID];
        }
        else {
            return mTexIDs[BACK_TEXTURE_ID];
        }
    }

    //is given point the page?
    boolean contains(float x, float y) {
        return left < right && bottom < top &&
                left <= x && x < right &&
                bottom <= y && y < top;
    }

    boolean isXInRange(float x, float ratio) {
        final float w = width * ratio;
        return originP.x < 0 ? x < (originP.x + w) : x > (originP.x - w);
    }

    boolean isXOutsidePage(float x) {
        return originP.x < 0 ? x > diagonalP.x : x < diagonalP.x;
    }

    private void computeIndexOfApexOrder() {
        mApexOrderIndex = 0;
        if (originP.x < right && originP.y < 0) {
            mApexOrderIndex = 3;
        }
        else {
            if (originP.y > 0) {
                mApexOrderIndex++;
            }
            if (originP.x < right) {
                mApexOrderIndex++;
            }
        }
    }

    Page setOriginAndDiagonalPoints(boolean hasSecondPage, float dy) {
        if (hasSecondPage && left < 0) {
            originP.x = left;
            diagonalP.x = right;
        }
        else {
            originP.x = right;
            diagonalP.x = left;
        }

        if (dy > 0) {
            originP.y = bottom;
            diagonalP.y = top;
        }
        else {
            originP.y = top;
            diagonalP.y = bottom;
        }

        computeIndexOfApexOrder();

        // set texture coordinates
        originP.texX = (originP.x - left) / texWidth;
        originP.texY = (top - originP.y) / texHeight;
        diagonalP.texX = (diagonalP.x - left) / texWidth;
        diagonalP.texY = (top - diagonalP.y) / texHeight;
        return this;
    }

    void invertYOfOriginPoint() {
        float t = originP.y;
        originP.y = diagonalP.y;
        diagonalP.y = t;

        t = originP.texY;
        originP.texY = diagonalP.texY;
        diagonalP.texY = t;

        // re-compute index for apex order since original point is changed
        computeIndexOfApexOrder();
    }

    //compute x coordinate of texture
    public float textureX(float x) {
        return (x - left) / texWidth;
    }

    //compute y coordinate of texture
    public float textureY(float y) {
        return (top - y) / texHeight;
    }

    //delete all texture
    public void deleteAllTextures() {
        glDeleteTextures(TEXTURE_SIZE, mTexIDs, 0);
        mTexIDs[FIRST_TEXTURE_ID] = INVALID_TEXTURE_ID;
        mTexIDs[SECOND_TEXTURE_ID] = INVALID_TEXTURE_ID;
        mTexIDs[BACK_TEXTURE_ID] = INVALID_TEXTURE_ID;
    }

    public void setFirstTexture(Bitmap b) {
        // compute mask color
        int color = PageUtils.computeAverageColor(b, 30);
        maskColor[FIRST_TEXTURE_ID][0] = Color.red(color) / 255.0f;
        maskColor[FIRST_TEXTURE_ID][1] = Color.green(color) / 255.0f;
        maskColor[FIRST_TEXTURE_ID][2] = Color.blue(color) / 255.0f;

        glGenTextures(1, mTexIDs, FIRST_TEXTURE_ID);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTexIDs[FIRST_TEXTURE_ID]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, b, 0);
    }

    public void setSecondTexture(Bitmap b) {
        // compute mask color
        int color = PageUtils.computeAverageColor(b, 30);
        maskColor[SECOND_TEXTURE_ID][0] = Color.red(color) / 255.0f;
        maskColor[SECOND_TEXTURE_ID][1] = Color.green(color) / 255.0f;
        maskColor[SECOND_TEXTURE_ID][2] = Color.blue(color) / 255.0f;

        glGenTextures(1, mTexIDs, SECOND_TEXTURE_ID);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTexIDs[SECOND_TEXTURE_ID]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, b, 0);
    }

    public void setBackTexture(Bitmap b) {
        if (b == null) {
            // back texture is same with the first texture
            if (mTexIDs[BACK_TEXTURE_ID] != INVALID_TEXTURE_ID) {
                mUnusedTexIDs[mUnusedTexSize++] = mTexIDs[BACK_TEXTURE_ID];
            }
            mTexIDs[BACK_TEXTURE_ID] = INVALID_TEXTURE_ID;
        }
        else {
            // compute mask color
            int color = PageUtils.computeAverageColor(b, 50);
            maskColor[BACK_TEXTURE_ID][0] = Color.red(color) / 255.0f;
            maskColor[BACK_TEXTURE_ID][1] = Color.green(color) / 255.0f;
            maskColor[BACK_TEXTURE_ID][2] = Color.blue(color) / 255.0f;

            glGenTextures(1, mTexIDs, BACK_TEXTURE_ID);
            glBindTexture(GL_TEXTURE_2D, mTexIDs[BACK_TEXTURE_ID]);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, b, 0);
        }
    }

    //draw front page when page is flipping
    public void drawFrontPage(VertexProgram program,
                              Vertexes vertexes) {
        // 1. draw unfold part and curled part with the first texture
        glUniformMatrix4fv(program.mMVPMatrixLoc, 1, false,
                VertexProgram.MVPMatrix, 0);
        glBindTexture(GL_TEXTURE_2D, mTexIDs[FIRST_TEXTURE_ID]);
        glUniform1i(program.mTextureLoc, 0);
        vertexes.drawWith(GL_TRIANGLE_STRIP,
                program.mVertexPosLoc,
                program.mTexCoordLoc,
                0, mFrontVertexSize);

        // 2. draw the second texture
        glBindTexture(GL_TEXTURE_2D, mTexIDs[SECOND_TEXTURE_ID]);
        glUniform1i(program.mTextureLoc, 0);
        glDrawArrays(GL_TRIANGLE_STRIP,
                mFrontVertexSize,
                vertexes.mVertexesSize - mFrontVertexSize);
    }

    //draw full page
    public void drawFullPage(VertexProgram program, boolean isFirst) {
        if (isFirst) {
            drawFullPage(program, mTexIDs[FIRST_TEXTURE_ID]);
        }
        else {
            drawFullPage(program, mTexIDs[SECOND_TEXTURE_ID]);
        }
    }

    private void drawFullPage(VertexProgram program, int textureID) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        glUniform1i(program.mTextureLoc, 0);

        glVertexAttribPointer(program.mVertexPosLoc, 3, GL_FLOAT, false, 0,
                mFullPageVexBuf);
        glEnableVertexAttribArray(program.mVertexPosLoc);

        glVertexAttribPointer(program.mTexCoordLoc, 2, GL_FLOAT, false, 0,
                mFullPageTexCoordsBuf);
        glEnableVertexAttribArray(program.mTexCoordLoc);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }

    //vertex buffer
    private void createVertexesBuffer() {
        // 4 vertexes for full page
        mFullPageVexBuf = ByteBuffer.allocateDirect(48)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        mFullPageTexCoordsBuf = ByteBuffer.allocateDirect(32)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        mApexes = new float[12];
        mApexTexCoords = new float[8];
    }

    //vertexes of page when page is flipped vertically
    public void buildVertexesOfPageWhenVertical(Vertexes frontVertexes,
                                                PointF xFoldP1) {
        // if xFoldX and yFoldY are both outside the page, use the last vertex
        // order to draw page
        int index = 4;

        // compute xFoldX and yFoldY points
        if (!isXOutsidePage(xFoldP1.x)) {
            // use the case B of vertex order to draw page
            index = 1;
            float cx = textureX(xFoldP1.x);
            mXFoldP.set(xFoldP1.x, originP.y, 0, cx, originP.texY);
            mYFoldP.set(xFoldP1.x, diagonalP.y, 0, cx, diagonalP.texY);
        }

        // get apex order and fold vertex order
        final int[] apexOrder = mPageApexOrders[mApexOrderIndex];
        final int[] vexOrder = mFoldVexOrders[index];

        // need to draw first texture, add xFoldX and yFoldY first. Remember
        // the adding order of vertex in float buffer is X point prior to Y
        // point
        if (vexOrder[0] > 1) {
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the leftover vertexes for the first texture
        for (int i = 1; i < vexOrder[0]; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], 0,
                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }

        // the vertex size for drawing front of fold page and first texture
        mFrontVertexSize = frontVertexes.mNext / 3;

        // if xFoldX and yFoldY are in the page, need add them for drawing the
        // second texture
        if (vexOrder[0] > 1) {
            mXFoldP.z = mYFoldP.z = -1;
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the remaining vertexes for the second texture
        for (int i = vexOrder[0]; i < vexOrder.length; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], -1,
                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }
    }

    //vertexes of page when page is slope
    public void buildVertexesOfPageWhenSlope(Vertexes frontVertexes,
                                             PointF xFoldP1,
                                             PointF yFoldP1,
                                             float kValue) {
        // compute xFoldX point
        float halfH = height * 0.5f;
        int index = 0;
        mXFoldP.set(xFoldP1.x, originP.y, 0, textureX(xFoldP1.x), originP.texY);
        if (isXOutsidePage(xFoldP1.x)) {
            index = 2;
            mXFoldP.x = diagonalP.x;
            mXFoldP.y = originP.y + (xFoldP1.x - diagonalP.x) / kValue;
            mXFoldP.texX = diagonalP.texX;
            mXFoldP.texY = textureY(mXFoldP.y);
        }

        // compute yFoldY point
        mYFoldP.set(originP.x, yFoldP1.y, 0, originP.texX, textureY(yFoldP1.y));
        if (Math.abs(yFoldP1.y) > halfH)  {
            index++;
            mYFoldP.x = originP.x + kValue * (yFoldP1.y - diagonalP.y);
            if (isXOutsidePage(mYFoldP.x)) {
                index++;
            }
            else {
                mYFoldP.y = diagonalP.y;
                mYFoldP.texX = textureX(mYFoldP.x);
                mYFoldP.texY = diagonalP.texY;
            }
        }

        // get apex order and fold vertex order
        final int[] apexOrder = mPageApexOrders[mApexOrderIndex];
        final int[] vexOrder = mFoldVexOrders[index];

        // need to draw first texture, add xFoldX and yFoldY first. Remember
        // the adding order of vertex in float buffer is X point prior to Y
        // point
        if (vexOrder[0] > 1) {
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the leftover vertexes for the first texture
        for (int i = 1; i < vexOrder[0]; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], 0,
                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }

        // the vertex size for drawing front of fold page and first texture
        mFrontVertexSize = frontVertexes.mNext / 3;

        // if xFoldX and yFoldY are in the page, need add them for drawing the
        // second texture
        if (vexOrder[0] > 1) {
            mXFoldP.z = mYFoldP.z = -1;
            frontVertexes.addVertex(mXFoldP).addVertex(mYFoldP);
        }

        // add the remaining vertexes for the second texture
        for (int i = vexOrder[0]; i < vexOrder.length; ++i) {
            int k = apexOrder[vexOrder[i]];
            int m = k * 3;
            int n = k << 1;
            frontVertexes.addVertex(mApexes[m], mApexes[m + 1], -1,
                    mApexTexCoords[n], mApexTexCoords[n + 1]);
        }
    }

    //vertexes of full page
    private void buildVertexesOfFullPage() {
        int i = 0;
        int j = 0;

        mApexes[i++] = right;
        mApexes[i++] = bottom;
        mApexes[i++] = 0;
        mApexTexCoords[j++] = textureX(right);
        mApexTexCoords[j++] = textureY(bottom);

        mApexes[i++] = right;
        mApexes[i++] = top;
        mApexes[i++] = 0;
        mApexTexCoords[j++] = textureX(right);
        mApexTexCoords[j++] = textureY(top);

        mApexes[i++] = left;
        mApexes[i++] = top;
        mApexes[i++] = 0;
        mApexTexCoords[j++] = textureX(left);
        mApexTexCoords[j++] = textureY(top);

        mApexes[i++] = left;
        mApexes[i++] = bottom;
        mApexes[i] = 0;
        mApexTexCoords[j++] = textureX(left);
        mApexTexCoords[j] = textureY(bottom);

        mFullPageVexBuf.put(mApexes, 0, 12).position(0);
        mFullPageTexCoordsBuf.put(mApexTexCoords, 0, 8).position(0);
    }

}
