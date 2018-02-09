package com.liang.tind.opengldemo;

import android.app.ActivityManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private GLSurfaceView mGlSurfaceView;
    private boolean hasSetSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGlSurfaceView = new GLSurfaceView(this);


        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        boolean isSupportGL20 = activityManager.getDeviceConfigurationInfo().reqGlEsVersion >= 0x20000;
        if (isSupportGL20) {
            mGlSurfaceView.setEGLContextClientVersion(2);
            mGlSurfaceView.setRenderer(new MyGLRenderer());
            mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            hasSetSurface = true;
            setContentView(mGlSurfaceView);
        } else {
            setContentView(R.layout.activity_main);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasSetSurface) {
            mGlSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (hasSetSurface) {
            mGlSurfaceView.onPause();
        }
    }


    static class MyGLRenderer implements GLSurfaceView.Renderer {
        private static final String TAG = "MyGLRenderer";

        public static final int BYTE_LENGTH = 4;
        private float[] matrix = new float[16];

        // 顶点着色器的脚本
        private static final String verticesShader
                = "attribute vec4 vPosition;            \n" // 顶点位置属性vPosition
                + "attribute vec4 aColor;               \n"
                + "varying vec4 vColor;                 \n"
                + "uniform mat4 vMatrix;                 \n"
                + "void main(){                         \n"
                + "   gl_Position =vPosition * vMatrix;\n" // 确定顶点位置
                + "   gl_PointSize = 30.0;                 \n" //点的大小
                + "   vColor = aColor;                     \n"
                + "}";

        // 片元着色器的脚本
        private static final String fragmentShader
                = "precision mediump float;         \n" // 声明float类型的精度为中等(精度越高越耗资源)
                + "varying vec4 vColor;             \n" // uniform的属性uColor varying
                + "void main(){                     \n"
                + "   gl_FragColor = vColor;        \n" // 给此片元的填充色
                + "}";

        private int vPosition;
        private int uColor;
        private int mProgram;
        private int mMatrix;


        public MyGLRenderer() {

        }

        private FloatBuffer getVertices() {
            float[] tableVerticesWithTriangles = new float[]{
                    // triangle fan
                    //X,Y,R,G,B
                    0f, 0f, 1f, 1f, 1f,        //中点
                    -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
                    0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
                    0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
                    -0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
                    -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,

//                    // triangle 2
//                    -0.5f, -0.5f,
//                    0.5f, -0.5f,
//                    0.5f, 0.5f,

                    // mid line
                    -0.5f, 0f, 1f, 0f, 0f,
                    0.5f, 0f, 1f, 0f, 0f,

                    // mallets
                    0f, -0.4f, 0f, 0f, 1f,
                    0f, 0.4f, 1f, 0f, 0f
            };
            FloatBuffer tableData = ByteBuffer.allocateDirect(tableVerticesWithTriangles.length * BYTE_LENGTH)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            tableData.put(tableVerticesWithTriangles);

            return tableData;
        }

        /**
         * 创建shader程序的方法
         */
        public static int createProgram(String vertexSource, String fragmentSource) {
            //加载顶点着色器
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }

            // 加载片元着色器
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            // 创建程序
            int program = GLES20.glCreateProgram();
            // 若程序创建成功则向程序中加入顶点着色器与片元着色器
            if (program != 0) {
                // 向程序中加入顶点着色器
                GLES20.glAttachShader(program, vertexShader);
                // 向程序中加入片元着色器
                GLES20.glAttachShader(program, pixelShader);
                // 链接程序
                GLES20.glLinkProgram(program);
                // 存放链接成功program数量的数组
                int[] linkStatus = new int[1];
                // 获取program的链接情况
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                // 若链接失败则报错并删除程序
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e("ES20_ERROR", "Could not link program: ");
                    Log.e("ES20_ERROR", GLES20.glGetProgramInfoLog(program));
                    GLES20.glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }

        /**
         * 加载制定shader的方法
         *
         * @param shaderType shader的类型  GLES20.GL_VERTEX_SHADER   GLES20.GL_FRAGMENT_SHADER
         * @param sourceCode shader的脚本
         * @return shader索引
         */
        public static int loadShader(int shaderType, String sourceCode) {
            // 创建一个新shader
            int shader = GLES20.glCreateShader(shaderType);
            // 若创建成功则加载shader
            if (shader != 0) {
                // 加载shader的源代码
                GLES20.glShaderSource(shader, sourceCode);
                // 编译shader
                GLES20.glCompileShader(shader);
                // 存放编译成功shader数量的数组
                int[] compiled = new int[1];
                // 获取Shader的编译情况
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {//若编译失败则显示错误日志并删除此shader
                    Log.e("ES20_ERROR", "Could not compile shader " + shaderType + ":");
                    Log.e("ES20_ERROR", GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        public static boolean validateProgram(int programId) {
            GLES20.glValidateProgram(programId);
            int[] validateStatus = new int[1];
            GLES20.glGetProgramiv(programId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
            return validateStatus[0] != 0;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.e(TAG, "onSurfaceCreated: ");
            GLES20.glClearColor(0F, 1, 1, 0);
            FloatBuffer vertices = getVertices();
            mProgram = createProgram(verticesShader, fragmentShader);
            if (validateProgram(mProgram)) {
                GLES20.glUseProgram(mProgram);
                // 获取着色器中的属性引用id(传入的字符串就是我们着色器脚本中的属性名)
                vPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
                uColor = GLES20.glGetAttribLocation(mProgram, "aColor");
                mMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
                vertices.position(0);
                // 为画笔指定顶点位置数据(vPosition)
                GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 5 * BYTE_LENGTH, vertices);
                // 允许顶点位置数据数组
                GLES20.glEnableVertexAttribArray(vPosition);
                vertices.position(2);
                // 为画笔指定顶点颜色数据(uColor)
                GLES20.glVertexAttribPointer(uColor, 3, GLES20.GL_FLOAT, false, 5 * BYTE_LENGTH, vertices);
                // 允许顶点颜色数据数组
                GLES20.glEnableVertexAttribArray(uColor);


            } else {
                Log.e(TAG, "ES20_ERROR:log ==" + GLES20.glGetProgramInfoLog(mProgram));
            }

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.e(TAG, "onSurfaceChanged: ");
            GLES20.glViewport(0, 0, width, height);
            final float ratio = width > height ? (width / height) : (height / width);
            if (width>height){
                Matrix.orthoM(matrix,0,-ratio,ratio,-1f,1f,-1f,1f);
            }else {
                Matrix.orthoM(matrix,0,-1f,1f,-ratio,ratio,-1f,1f);
            }

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUniformMatrix4fv(mMatrix,1,false,matrix,0);

            // 设置 顶点 属性uColor(颜色索引,R,G,B,A)
//            GLES20.glUniform4f(uColor, 0.0f, 1.0f, 0.0f, 1.0f);
            // 绘制
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);


            // 设置 中间线 属性uColor(颜色索引,R,G,B,A)
//            GLES20.glUniform4f(uColor, 1.0f, 0.0f, 0.0f, 1.0f);
            // 绘制
            GLES20.glDrawArrays(GLES20.GL_LINES, 6, 2);

            // 设置 木槌 属性uColor(颜色索引,R,G,B,A)
//            GLES20.glUniform4f(uColor, 0.0f, 0.0f, 1.0f, 1.0f);
            // 绘制
            GLES20.glDrawArrays(GLES20.GL_POINTS, 8, 2);

        }
    }
}
