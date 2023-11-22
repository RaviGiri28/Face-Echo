package com.example.faceecho;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.SparseIntArray;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;


import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.content.Context;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
public class MainHome extends AppCompatActivity {
    private TextureView textureView;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private ImageView faceBoxImageView;
    private boolean isDetecting = false;

    ImageButton cameraSwitchButton;
    private boolean isFrontCamera = true;
    private ImageButton captureButton;
    private int happyCounter = 0;
    private int sadCounter = 0;
    private int neutralCounter=0;
    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private static final int MSG_CHECK_INTERNET = 1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // You'll also need to set up a few other variables like a background thread and a camera state callback.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show the splash screen while checking for internet connection
        new Handler().postDelayed(() -> {
            if (isInternetConnected()) {
                // Internet is connected, proceed to MainHome
                setContentView(R.layout.activity_main_home);
                initMainHome();
                }else {
            // No internet, show message on the splash screen

                Toast.makeText(this, "Please turn on the internet connection", Toast.LENGTH_LONG).show();
        }
    }, SPLASH_DURATION);
}

    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void initMainHome() {
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);
        faceBoxImageView = findViewById(R.id.faceBoxImageView);
        cameraSwitchButton = findViewById(R.id.cameraSwitchButton);
        textureView.setOnClickListener(v -> {
            // Reset counters when the user clicks on the screen
            happyCounter = 0;
            sadCounter = 0;
            neutralCounter = 0;
        });

        cameraSwitchButton.setOnClickListener(v -> {
            // Toggle between front and rear cameras
            isFrontCamera = !isFrontCamera;
            closeCamera();
            openCamera(); // Re-open the camera with the new camera ID
        });
        captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> capturePicture());
    }

    private void capturePicture() {
        if (cameraDevice == null) {
            return;
        }
        playShutterSound();
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            CaptureRequest.Builder captureBuilder = null;
            int rotation = 0;
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageReader.class);

                // Capture image with custom size
                int width = 640;
                int height = 480;
                if (jpegSizes != null && jpegSizes.length > 0) {
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();
                }
                ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                List<Surface> outputSurfaces = new ArrayList<>(2);
                outputSurfaces.add(reader.getSurface());
                outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

                captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(reader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                // Check orientation based on device
                rotation = getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                final File file = new File(Environment.getExternalStorageDirectory() + "/FaceEcho/"
                        + "IMG_" + timestamp + ".jpg");

                ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = null;
                        try {
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            save(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null) {
                                image.close();
                            }
                        }
                    }

                    void save(byte[] bytes) throws IOException {
                        OutputStream output = null;
                        try {
                            output = new FileOutputStream(file);
                            output.write(bytes);
                        } finally {
                            if (output != null) {
                                output.close();
                                // Add the captured image to the gallery
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.Images.Media.TITLE, "FaceEcho Captured Image");
                                values.put(MediaStore.Images.Media.DESCRIPTION, "FaceEcho Captured Image");
                                values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
                                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                runOnUiThread(() -> {
                                    Toast.makeText(MainHome.this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }
                };


                HandlerThread mBackgroundThread = new HandlerThread("CameraBackground");
                mBackgroundThread.start();
                Handler mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
                final CameraCaptureSession.CaptureCallback captureListener =
                        new CameraCaptureSession.CaptureCallback() {
                            // You can add capture callbacks if needed
                        };

                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shutter_sound); // Assume you have a sound file named shutter_sound.mp3 in the raw folder
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
                // Fix: Pass the mBackgroundHandler to the capture session
                CaptureRequest.Builder finalCaptureBuilder = captureBuilder;
                cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            session.capture(finalCaptureBuilder.build(), captureListener, mBackgroundHandler);
                            session.close();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    }
                }, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void playShutterSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shutter_sound); // Assume you have a sound file named shutter_sound.mp3 in the raw folder
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
    }

    private void closeCamera() {
        if (cameraCaptureSessions != null) {
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if (!isDetecting) {
                isDetecting = true;
                startFaceDetection();
            }
        }
    };

    private void startFaceDetection() {
        if (!isInternetConnected()) {
            // Internet is not connected, show a notification or message
            showNoInternetNotification();
            return;
        }
        textureView.getBitmap(); // Refresh the TextureView
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(textureView.getBitmap());
        detector.detectInImage(image)
                .addOnSuccessListener(faces -> {
                    for (FirebaseVisionFace face : faces) {
                        // Process each detected face
                        processFace(face);
                    }
                    isDetecting = false; // Reset the flag after processing all faces
                })
                .addOnFailureListener(e -> {
                    isDetecting = false; // Reset the flag in case of failure
                });
    }

    private void processFace(FirebaseVisionFace face) {
        // Check for different facial expressions
        if (face.getSmilingProbability() >= 0.5) {
            // Display happy expression
            showExpression("Happy", happyCounter);
            happyCounter++;
        } else {
            if (face.getRightEyeOpenProbability() < 0.5 && face.getLeftEyeOpenProbability() < 0.5) {
                // Both eyes are closed, indicating a possible sad expression
                showExpression("Sad",  sadCounter);
                sadCounter++;
            } else{

                showExpression("Neutral",neutralCounter);
                neutralCounter++;
            }
        }
    }

    private String getAgeRange(int age) {
        // Define your age ranges based on your preferences
        if (age < 18) {
            return "0-17 years";
        } else if (age < 35) {
            return "18-34 years";
        } else if (age < 50) {
            return "35-49 years";
        } else {
            return "50+ years";
        }
    }

    private void showNoInternetNotification() {
        // You can display a notification or show a message to the user here
        runOnUiThread(() -> {
            Toast.makeText(MainHome.this, "Please turn on the internet connection for face detection", Toast.LENGTH_LONG).show();
        });
    }

    private void showExpression(String expression, int counter) {
        if (counter < 1) {
            Toast.makeText(MainHome.this, "Expression: " + expression, Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = isFrontCamera ? getFrontCameraId(manager) : getRearCameraId(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            // Add camera permission request here if not already granted.

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String getFrontCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            }
        }
        return manager.getCameraIdList()[0]; // Fallback to the first camera if front camera not found
    }

    private String getRearCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return manager.getCameraIdList()[0]; // Fallback to the first camera if rear camera not found
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice= null;
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
                    cameraCaptureSessions = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(MainHome.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            return;
        }
        Bitmap bitmap = textureView.getBitmap();
        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();
        if (!faceDetector.isOperational()) {
            // Handle the error accordingly
        } else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<com.google.android.gms.vision.face.Face> faces = faceDetector.detect(frame);

            for (int i = 0; i < faces.size(); i++) {
                Face face = faces.valueAt(i);

                float x1 = face.getPosition().x;
                float y1 = face.getPosition().y;
                float x2 = x1 + face.getWidth();
                float y2 = y1 + face.getHeight();
                runOnUiThread(() -> {
                    // Convert the coordinates to absolute values for the view
                    int viewWidth = textureView.getWidth();
                    int viewHeight = textureView.getHeight();

                    int rectLeft = (int) (x1 * viewWidth / bitmap.getWidth());
                    int rectTop = (int) (y1 * viewHeight / bitmap.getHeight());
                    int rectRight = (int) (x2 * viewWidth / bitmap.getWidth());
                    int rectBottom = (int) (y2 * viewHeight / bitmap.getHeight());
                    faceBoxImageView.setX(rectLeft);
                    faceBoxImageView.setY(rectTop);
                    ViewGroup.LayoutParams params = faceBoxImageView.getLayoutParams();
                    params.width = rectRight - rectLeft;
                    params.height = rectBottom - rectTop;
                    faceBoxImageView.setLayoutParams(params);
                });
            }
        }
        faceDetector.release(); // Release the FaceDetector
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
