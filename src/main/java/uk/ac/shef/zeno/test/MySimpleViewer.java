package uk.ac.shef.zeno.test;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_MJPG;
import static org.monte.media.VideoFormatKeys.HeightKey;
import static org.monte.media.VideoFormatKeys.QualityKey;
import static org.monte.media.VideoFormatKeys.WidthKey;
import org.openni.*;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;

public class MySimpleViewer extends Component 
                          implements VideoStream.NewFrameListener {
    
    float mHistogram[];
    int[] mImagePixels;
    VideoStream mVideoStream;
    VideoFrameRef mLastFrame;
    BufferedImage mBufferedImage;
    AVIWriter writer;
    Format format;
    boolean paletteSet = false;
    public MySimpleViewer() {
        try {
            format = new Format(EncodingKey, ENCODING_AVI_MJPG, DepthKey, 24, QualityKey, 1f);
            format = format.prepend(MediaTypeKey, FormatKeys.MediaType.VIDEO, //
                FrameRateKey, new Rational(30, 1),//
                WidthKey, 640, //
                HeightKey, 480);
            writer = new AVIWriter(new File("out.avi"));
            writer.addTrack(format);
          
        } catch (IOException ex) {
            Logger.getLogger(MySimpleViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setStream(VideoStream videoStream) {
        if (mLastFrame != null) {
            mLastFrame.release();
            mLastFrame = null;
        }
        
        if (mVideoStream != null) {
            mVideoStream.removeNewFrameListener(this);
        }
        
        mVideoStream = videoStream;
        
        if (mVideoStream != null) {
            mVideoStream.addNewFrameListener(this);
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        if (mLastFrame == null) {
            return;
        }
        int width = mLastFrame.getWidth();
        int height = mLastFrame.getHeight();

        // make sure we have enough room
        if (mBufferedImage == null || mBufferedImage.getWidth() != width || mBufferedImage.getHeight() != height) {
            mBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        
        mBufferedImage.setRGB(0, 0, width, height, mImagePixels, 0, width);
        if (!paletteSet) {
             writer.setPalette(0, mBufferedImage.getColorModel());

            paletteSet = true;
        }

        int framePosX = (getWidth() - width) / 2;
        int framePosY = (getHeight() - height) / 2;
        g.drawImage(mBufferedImage, framePosX, framePosY, null);
        try {
            writer.write(0, this.mBufferedImage, 1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void onFrameReady(VideoStream stream) {
        if (mLastFrame != null) {
            mLastFrame.release();
            mLastFrame = null;
        }
        
        mLastFrame = mVideoStream.readFrame();
        ByteBuffer frameData = mLastFrame.getData().order(ByteOrder.LITTLE_ENDIAN);
        
        // make sure we have enough room
        if (mImagePixels == null || mImagePixels.length < mLastFrame.getWidth() * mLastFrame.getHeight()) {
            mImagePixels = new int[mLastFrame.getWidth() * mLastFrame.getHeight()];
        }
        
        switch (mLastFrame.getVideoMode().getPixelFormat())
        {
            case DEPTH_1_MM:
            case DEPTH_100_UM:
            case SHIFT_9_2:
            case SHIFT_9_3:
                calcHist(frameData);
                frameData.rewind();
                int pos = 0;
                while(frameData.remaining() > 0) {
                    int depth = (int)frameData.getShort() & 0xFFFF;
                    short pixel = (short)mHistogram[depth];
                    mImagePixels[pos] = 0xFF000000 | (pixel << 16) | (pixel << 8);
                    pos++;
                }
                break;
            case RGB888:
                pos = 0;
                while (frameData.remaining() > 0) {
                    int red = (int)frameData.get() & 0xFF;
                    int green = (int)frameData.get() & 0xFF;
                    int blue = (int)frameData.get() & 0xFF;
                    mImagePixels[pos] = 0xFF000000 | (red << 16) | (green << 8) | blue;
                    pos++;
                }
                break;
            default:
                // don't know how to draw
                mLastFrame.release();
                mLastFrame = null;
        }

        repaint();
    }

    private void calcHist(ByteBuffer depthBuffer) {
        // make sure we have enough room
        if (mHistogram == null || mHistogram.length < mVideoStream.getMaxPixelValue()) {
            mHistogram = new float[mVideoStream.getMaxPixelValue()];
        }
        
        // reset
        for (int i = 0; i < mHistogram.length; ++i)
            mHistogram[i] = 0;

        int points = 0;
        while (depthBuffer.remaining() > 0) {
            int depth = depthBuffer.getShort() & 0xFFFF;
            if (depth != 0) {
                mHistogram[depth]++;
                points++;
            }
        }

        for (int i = 1; i < mHistogram.length; i++) {
            mHistogram[i] += mHistogram[i - 1];
        }

        if (points > 0) {
            for (int i = 1; i < mHistogram.length; i++) {
                mHistogram[i] = (int) (256 * (1.0f - (mHistogram[i] / (float) points)));
            }
        }
    }
}
