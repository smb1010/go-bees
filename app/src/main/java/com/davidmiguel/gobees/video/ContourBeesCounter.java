package com.davidmiguel.gobees.video;

import android.support.annotation.NonNull;
import android.util.Log;

import com.davidmiguel.gobees.video.processors.BackgroundSubtractor;
import com.davidmiguel.gobees.video.processors.Blur;
import com.davidmiguel.gobees.video.processors.ContoursFinder;
import com.davidmiguel.gobees.video.processors.Morphology;

import org.opencv.core.Mat;

/**
 * Counts the number of bees based on the area of detected moving contours.
 */
public class ContourBeesCounter implements BeesCounter {

    private static final String TAG = "ContourBeesCounter";
    private static ContourBeesCounter INSTANCE;

    private Blur blur;
    private BackgroundSubtractor bs;
    private Morphology morphology;
    private ContoursFinder cf;
    private Mat processedFrame;

    /**
     * Default ContourBeesCounter constructor.
     * History is initialized to 10 and shadows threshold to 0.7.
     * minArea is initialized to 15 and maxArea to 800.
     */
    private ContourBeesCounter() {
        blur = new Blur();
        bs = new BackgroundSubtractor();
        morphology = new Morphology();
        cf = new ContoursFinder();
    }

    public static ContourBeesCounter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ContourBeesCounter();
        }
        return INSTANCE;
    }

    @Override
    public int countBees(@NonNull Mat frame) {
        final long t0 = System.nanoTime();
        Mat r0 = blur.process(frame);
        Mat r1 = bs.process(r0);
        Mat r2 = morphology.process(r1);
        processedFrame = cf.process(r2);
        r0.release();
        r1.release();
        r2.release();
        Log.d(TAG, "countBees time: " + (System.nanoTime() - t0) / 1000000);
        return cf.getNumBees();
    }

    @Override
    public Mat getProcessedFrame() {
        return processedFrame;
    }

    @Override
    public void updateBlobSize(BlobSize size) {
        switch (size) {
            case SMALL:
                morphology.setDilateKernel(2);
                morphology.setErodeKernel(3);
                break;
            case NORMAL:
                morphology.setDilateKernel(3);
                morphology.setErodeKernel(3);
                break;
            case BIG:
            default:
                morphology.setDilateKernel(3);
                morphology.setErodeKernel(2);
        }
    }

    @Override
    public void updateMinArea(Double minArea) {
        cf.setMinArea(minArea);
    }

    @Override
    public void updateMaxArea(Double maxArea) {
        cf.setMaxArea(maxArea);
    }
}