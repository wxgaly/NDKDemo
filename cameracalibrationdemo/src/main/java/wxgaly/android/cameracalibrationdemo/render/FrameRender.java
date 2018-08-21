package wxgaly.android.cameracalibrationdemo.render;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import wxgaly.android.cameracalibrationdemo.CameraCalibrator;

public abstract class FrameRender {
    protected CameraCalibrator mCalibrator;

    public abstract Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame);
}
