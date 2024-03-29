package org.owwlo.watchcat.ExoPlayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.GlUtil;

import org.owwlo.watchcat.R;

import java.io.IOException;
import java.util.Locale;

import javax.microedition.khronos.opengles.GL10;

public class BitmapOverlayVideoProcessor
        implements VideoProcessingGLSurfaceView.VideoProcessor {

    private static final int OVERLAY_WIDTH = 512;
    private static final int OVERLAY_HEIGHT = 256;

    private final Context context;
    private final Paint paint;
    private final int[] textures;
    private final Bitmap overlayBitmap;
    private final Canvas overlayCanvas;

    private int program;
    @Nullable
    private GlUtil.Attribute[] attributes;
    @Nullable
    private GlUtil.Uniform[] uniforms;

    private float bitmapScaleX;
    private float bitmapScaleY;

    public BitmapOverlayVideoProcessor(Context context) {
        this.context = context.getApplicationContext();
        paint = new Paint();
        paint.setTextSize(24);
        paint.setAntiAlias(true);
        paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
        textures = new int[1];
        overlayBitmap = Bitmap.createBitmap(OVERLAY_WIDTH, OVERLAY_HEIGHT, Bitmap.Config.ARGB_8888);
        overlayCanvas = new Canvas(overlayBitmap);
    }

    @Override
    public void initialize() {
        String vertexShaderCode;
        String fragmentShaderCode;
        try {
            vertexShaderCode = GlUtil.loadAsset(context, "bitmap_overlay_video_processor_vertex.glsl");
            fragmentShaderCode =
                    GlUtil.loadAsset(context, "bitmap_overlay_video_processor_fragment.glsl");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        program = GlUtil.compileProgram(vertexShaderCode, fragmentShaderCode);
        GlUtil.Attribute[] attributes = GlUtil.getAttributes(program);
        GlUtil.Uniform[] uniforms = GlUtil.getUniforms(program);
        for (GlUtil.Attribute attribute : attributes) {
            if (attribute.name.equals("a_position")) {
                attribute.setBuffer(new float[]{-1, -1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, 1, 1, 0, 1}, 4);
            } else if (attribute.name.equals("a_texcoord")) {
                attribute.setBuffer(new float[]{0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1}, 4);
            }
        }
        this.attributes = attributes;
        this.uniforms = uniforms;
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, /* level= */ 0, overlayBitmap, /* border= */ 0);
    }

    @Override
    public void setSurfaceSize(int width, int height) {
        bitmapScaleX = (float) width / OVERLAY_WIDTH;
        bitmapScaleY = (float) height / OVERLAY_HEIGHT;
    }

    String getTimeElapsed(final double tv) {
        int hours = (int) (tv / 3600);
        int minutes = (int) ((tv % 3600) / 60);
        double seconds = tv % 60.;

        return String.format(Locale.US, "%02d:%02d:%.02f", hours, minutes, seconds);
    }

    @Override
    public void draw(int frameTexture, long frameTimestampUs, float[] transformMatrix) {
        // Draw to the canvas and store it in a texture.
        String text = this.context.getString(R.string.streaming_player_elapsed_prefix_text) + " " + getTimeElapsed(frameTimestampUs / (float) C.MICROS_PER_SECOND);
        overlayBitmap.eraseColor(Color.TRANSPARENT);
        overlayCanvas.drawText(text, /* x= */ 75, /* y= */ 75, paint);
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
        GLUtils.texSubImage2D(
                GL10.GL_TEXTURE_2D, /* level= */ 0, /* xoffset= */ 0, /* yoffset= */ 0, overlayBitmap);
        GlUtil.checkGlError();

        // Run the shader program.
        GlUtil.Uniform[] uniforms = Assertions.checkNotNull(this.uniforms);
        GlUtil.Attribute[] attributes = Assertions.checkNotNull(this.attributes);
        GLES20.glUseProgram(program);
        for (GlUtil.Uniform uniform : uniforms) {
            switch (uniform.name) {
                case "tex_sampler_0":
                    uniform.setSamplerTexId(frameTexture, /* unit= */ 0);
                    break;
                case "tex_sampler_1":
                    uniform.setSamplerTexId(textures[0], /* unit= */ 1);
                    break;
                case "scaleX":
                    uniform.setFloat(bitmapScaleX);
                    break;
                case "scaleY":
                    uniform.setFloat(bitmapScaleY);
                    break;
                case "tex_transform":
                    uniform.setFloats(transformMatrix);
                    break;
                default: // fall out
            }
        }
        for (GlUtil.Attribute copyExternalAttribute : attributes) {
            copyExternalAttribute.bind();
        }
        for (GlUtil.Uniform copyExternalUniform : uniforms) {
            copyExternalUniform.bind();
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4);
        GlUtil.checkGlError();
    }
}
