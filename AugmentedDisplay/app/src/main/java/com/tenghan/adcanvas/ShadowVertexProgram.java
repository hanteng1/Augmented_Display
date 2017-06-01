package com.tenghan.adcanvas;

import android.content.Context;

import com.tenghan.augmenteddisplay.R;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;

/**
 * Created by hanteng on 2017-06-01.
 */

public class ShadowVertexProgram extends GLProgram{

    // variable names defined in shader scripts
    final static String VAR_MVP_MATRIX  = "u_MVPMatrix";
    final static String VAR_VERTEX_Z    = "u_vexZ";
    final static String VAR_VERTEX_POS  = "a_vexPosition";

    int mMVPMatrixLoc;
    int mVertexZLoc;
    int mVertexPosLoc;

    public ShadowVertexProgram() {
        super();

        mMVPMatrixLoc = INVALID_GL_HANDLE;
        mVertexZLoc = INVALID_GL_HANDLE;
        mVertexPosLoc = INVALID_GL_HANDLE;
    }

    public ShadowVertexProgram init(Context context) throws
            PageException {
        super.init(context,
                R.raw.shadow_vertex_shader,
                R.raw.shadow_fragment_shader);
        return this;
    }

    /**
     * Get variable handles from linked shader program
     */
    protected void getVarsLocation() {
        if (mProgramRef != 0) {
            mVertexZLoc = glGetUniformLocation(mProgramRef, VAR_VERTEX_Z);
            mVertexPosLoc = glGetAttribLocation(mProgramRef, VAR_VERTEX_POS);
            mMVPMatrixLoc = glGetUniformLocation(mProgramRef, VAR_MVP_MATRIX);
        }
    }

    /**
     * Delete shader resources
     */
    public void delete() {
        super.delete();

        mMVPMatrixLoc = INVALID_GL_HANDLE;
        mVertexZLoc = INVALID_GL_HANDLE;
        mVertexPosLoc = INVALID_GL_HANDLE;
    }
}
