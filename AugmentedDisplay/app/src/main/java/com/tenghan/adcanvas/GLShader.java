package com.tenghan.adcanvas;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glShaderSource;

/**
 * Created by hanteng on 2017-06-01.
 * load and compile shader script
 */

public class GLShader {
    private final static String TAG = "GLShader";
    private final int INVALID_GL_HANDLE = -1;

    // shader object reference
    int mShaderRef;

    /**
     * Default constructor
     */
    public GLShader() {
        mShaderRef = INVALID_GL_HANDLE;
    }

    public GLShader compile(Context context, int type, int resId)
            throws PageException {
        // read shader scripts from resource
        String codes = readGLSLFromResource(context, resId);
        if (codes.length() < 1) {
            throw new PageException("Empty GLSL shader for resource id:"
                    + resId);
        }

        // create a shader
        mShaderRef = glCreateShader(type);
        if (mShaderRef != INVALID_GL_HANDLE) {
            // upload shader scripts to GL
            glShaderSource(mShaderRef, codes);

            // compile shader scripts
            glCompileShader(mShaderRef);

            // get compile results to check if it is successful
            final int[] result = new int[1];
            glGetShaderiv(mShaderRef, GL_COMPILE_STATUS, result, 0);
            if (result[0] == 0) {
                // delete shader if compile is failed
                Log.e(TAG, "Can'top compile shader for type: " + type +
                        "Error: " + glGetError());
                Log.e(TAG, "Compile shader error: " +
                        glGetShaderInfoLog(mShaderRef));
                glDeleteShader(mShaderRef);
                throw new PageException("Can't compile shader for" +
                        "type: " + type);
            }
        } else {
            throw new PageException("Can't create shader. Error: " +
                    glGetError());
        }

        return this;
    }

    /**
     * Delete shader
     */
    public void delete() {
        if (mShaderRef != INVALID_GL_HANDLE) {
            glDeleteShader(mShaderRef);
            mShaderRef = INVALID_GL_HANDLE;
        }
    }


    public int getShaderRef() {
        return mShaderRef;
    }

    String readGLSLFromResource(Context context, int resId) throws
            PageException {
        StringBuilder s = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(
                    context.getResources().openRawResource(resId)));
            String line;

            while ((line = reader.readLine()) != null) {
                s.append(line);
                s.append("\n");
            }
        }
        catch (IOException e) {
            throw new PageException("Could not open resource: "
                    + resId , e);
        }
        finally {
            // close
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException e) {
            }
        }

        return s.toString();
    }
}
