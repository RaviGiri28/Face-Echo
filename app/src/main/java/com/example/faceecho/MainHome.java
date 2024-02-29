package com.example.faceecho;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

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

    private ImageButton cameraSwitchButton;
    private boolean isFrontCamera = true;
    private ImageButton captureButton;
    private int happyCounter = 0;
    private int sadCounter = 0;
    private int neutralCounter = 0;
    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private static final int MSG_CHECK_INTERNET = 1;
    private static final long DELAY_AFTER_CAPTURE = 1000;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);
        initMainHome();
    }

    private void initMainHome() {
        textureView = findViewById(R.id.textureView);
        faceBoxImageView = findViewById(R.id.faceBoxImageView);
        FloatingActionButton viewImageButton = findViewById(R.id.viewImageButton);
        viewImageButton.setOnClickListener(v -> openGallery());

        cameraSwitchButton = findViewById(R.id.cameraSwitchButton);
        textureView.setSurfaceTextureListener(textureListener);
        textureView.setOnClickListener(v -> {
            happyCounter = 0;
            sadCounter = 0;
            neutralCounter = 0;
        });

        cameraSwitchButton.setOnClickListener(v -> {
            isFrontCamera = !isFrontCamera;
            closeCamera();
            openCamera();
        });

        captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> capturePicture());

        // Show the splash screen while checking for internet connection
        new Handler().postDelayed(() -> {
            if (isInternetConnected()) {
                // Internet is connected, proceed with face detection
                startFaceDetection();
            } else {
                // No internet, show a message
                Toast.makeText(this, "Please turn on the internet connection", Toast.LENGTH_LONG).show();
            }
        }, SPLASH_DURATION);
    }

    private void openGallery() {
        // Replace this with your gallery or file manager intent
        String imagePath = Environment.getExternalStorageDirectory() + "/FaceEcho/IMG_20220229_123456.jpg";

        // Create an intent to view the image
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(imagePath);

        // Get the content URI using FileProvider
        Uri contentUri = FileProvider.getUriForFile(this, "com.example.faceecho.fileprovider", file);

        // Set the data and type for the intent
        intent.setDataAndType(contentUri, "image/*");

        // Add FLAG_GRANT_READ_URI_PERMISSION to grant read permissions
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Check if there is an activity that can handle this intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Start the activity
            startActivity(intent);
        } else {
            // Handle the case where no suitable activity is found
            Toast.makeText(this, "No app can handle this action", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
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
            int rotation = 0;

            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageReader.class);
            }

            if (jpegSizes != null && jpegSizes.length > 0) {
                int width = jpegSizes[0].getWidth();
                int height = jpegSizes[0].getHeight();
                ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                List<Surface> outputSurfaces = new ArrayList<>(2);
                outputSurfaces.add(reader.getSurface());
                outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(reader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

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
                        new Handler().postDelayed(() -> {
                            // Enable face detection after capturing the image
                                startFaceDetection();
                            closeCamera();
                            openCamera();
                        }, DELAY_AFTER_CAPTURE);
                    }

                    void save(byte[] bytes) throws IOException {
                        OutputStream output = null;
                        try {
                            output = new FileOutputStream(file);
                            output.write(bytes);
                        } finally {
                            if (output != null) {
                                output.close();
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

                cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            session.capture(captureBuilder.build(), null, mBackgroundHandler);
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
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.shutter_sound);
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
            showNoInternetNotification();
            return;
        }
        textureView.getBitmap();
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
                        processFace(face);
                    }
                    isDetecting = false;
                })
                .addOnFailureListener(e -> {
                    isDetecting = false;
                });
    }

    private void processFace(FirebaseVisionFace face) {
        if (face.getSmilingProbability() >= 0.5) {
            showExpression("Happy", happyCounter);
            happyCounter++;
        } else {
            if (face.getRightEyeOpenProbability() < 0.5 && face.getLeftEyeOpenProbability() < 0.5) {
                showExpression("Sad", sadCounter);
                sadCounter++;
            } else {
                showExpression("Neutral", neutralCounter);
                neutralCounter++;
            }
        }
    }

    private void showExpression(String expression, int counter) {
        if (counter < 1) {
            Toast.makeText(MainHome.this, "Expression: " + expression, Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoInternetNotification() {
        runOnUiThread(() -> {
            Toast.makeText(MainHome.this, "Please turn on the internet connection for face detection", Toast.LENGTH_LONG).show();
        });
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = isFrontCamera ? getFrontCameraId(manager) : getRearCameraId(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
        return manager.getCameraIdList()[0];
    }

    private String getRearCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return manager.getCameraIdList()[0];
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
            cameraDevice = null;
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
            SparseArray<Face> faces = faceDetector.detect(frame);

            for (int i = 0; i < faces.size(); i++) {
                com.google.android.gms.vision.face.Face face = faces.valueAt(i);

                float x1 = face.getPosition().x;
                float y1 = face.getPosition().y;
                float x2 = x1 + face.getWidth();
                float y2 = y1 + face.getHeight();
                runOnUiThread(() -> {
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
        faceDetector.release();
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
