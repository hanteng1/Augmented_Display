package com.tenghan.adcanvas;

import android.content.Context;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glUseProgram;

/**
 * Created by hanteng on 2017-06-01.
 * load, compile, link shader scripts.
 */

public class GLProgram {
    // invalid GL getShaderRef including program reference and variable location
    protected final int INVALID_GL_HANDLE = -1;

    // GLSL program reference
    protected int mProgramRef;

    // Vertex shader
    protected GLShader mVertex;

    // Fragment shader
    protected GLShader mFragment;

    public GLProgram() {
        mProgramRef = INVALID_GL_HANDLE;
        mVertex = new GLShader();
        mFragment = new GLShader();
    }

    public GLProgram init(Context context, int vertexResId, int fragmentResId)
            throws PageException {
        // 1. init shader
        try {
            mVertex.compile(context, GL_VERTEX_SHADER, vertexResId);
            mFragment.compile(context, GL_FRAGMENT_SHADER, fragmentResId);
        }
        catch (PageException e) {
            mVertex.delete();
            mFragment.delete();
            throw e;
        }

        // 2. create texture program and link shader
        mProgramRef = glCreateProgram();
        if (mProgramRef == 0) {
            mVertex.delete();
            mFragment.delete();
            throw new PageException("Can't create texture program");
        }

        // 3. attach vertex and fragment shader
        glAttachShader(mProgramRef, mVertex.getShaderRef());
        glAttachShader(mProgramRef, mFragment.getShaderRef());
        glLinkProgram(mProgramRef);

        // 4. check shader link status
        int[] result = new int[1];
        glGetProgramiv(mProgramRef, GL_LINK_STATUS, result, 0);
        if (result[0] == 0) {
            delete();
            throw new PageException("Can't link program");
        }

        // 5. get all variable handles defined in scripts
        // subclass should implement getVarsLocation to be responsible for its
        // own variables in script
        glUseProgram(mProgramRef);
        getVarsLocation();
        return this;
    }


    public void delete() {
        mVertex.delete();
        mFragment.delete();

        if (mProgramRef != INVALID_GL_HANDLE) {
            glDeleteProgram(mProgramRef);
            mProgramRef = INVALID_GL_HANDLE;
        }
    }

    public int getProgramRef() {
        return mProgramRef;
    }


    protected void getVarsLocation() {
    }
}
