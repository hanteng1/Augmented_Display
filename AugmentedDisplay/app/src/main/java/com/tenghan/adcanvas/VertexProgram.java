package com.tenghan.adcanvas;

import android.content.Context;
import android.opengl.Matrix;

import com.tenghan.augmenteddisplay.R;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;

/**
 * Created by hanteng on 2017-06-01.
 * vertext shader program
 */

public class VertexProgram extends GLProgram{
    // variable names defined in GLSL scripts
    final static String VAR_MVP_MATRIX    = "u_MVPMatrix";
    final static String VAR_VERTEX_POS    = "a_vexPosition";
    final static String VAR_TEXTURE_COORD = "a_texCoord";
    final static String VAR_TEXTURE       = "u_texture";

    // universal model-view matrix
    final static float[] MVMatrix = new float[16];
    // universal model-view-project matrix
    final static float[] MVPMatrix = new float[16];

    // variable handles after compiled & linked shader scripts
    int mMVPMatrixLoc;
    int mVertexPosLoc;
    int mTexCoordLoc;
    int mTextureLoc;

    public VertexProgram() {
        super();

        // init with invalid value
        mTextureLoc = INVALID_GL_HANDLE;
        mMVPMatrixLoc = INVALID_GL_HANDLE;
        mTexCoordLoc = INVALID_GL_HANDLE;
        mVertexPosLoc = INVALID_GL_HANDLE;
    }


    public VertexProgram init(Context context) throws PageException {
        super.init(context, R.raw.vertex_shader, R.raw.fragment_shader);
        return this;
    }


    protected void getVarsLocation() {
        if (mProgramRef != 0) {
            mVertexPosLoc = glGetAttribLocation(mProgramRef, VAR_VERTEX_POS);
            mTexCoordLoc = glGetAttribLocation(mProgramRef, VAR_TEXTURE_COORD);
            mMVPMatrixLoc = glGetUniformLocation(mProgramRef, VAR_MVP_MATRIX);
            mTextureLoc = glGetUniformLocation(mProgramRef, VAR_TEXTURE);
        }
    }


    public void delete() {
        super.delete();

        mTextureLoc = INVALID_GL_HANDLE;
        mMVPMatrixLoc = INVALID_GL_HANDLE;
        mTexCoordLoc = INVALID_GL_HANDLE;
        mVertexPosLoc = INVALID_GL_HANDLE;
    }

    public void initMatrix(float left, float right, float bottom, float top) {
        float[] projectMatrix = new float[16];
        Matrix.orthoM(projectMatrix, 0, left, right, bottom, top, 0, 6000);
        Matrix.setIdentityM(MVMatrix, 0);
        Matrix.setLookAtM(MVMatrix, 0, 0, 0, 3000, 0, 0, 0, 0, 1, 0);
        Matrix.setIdentityM(MVPMatrix, 0);
        Matrix.multiplyMM(MVPMatrix, 0, projectMatrix, 0, MVMatrix, 0);
    }
}
