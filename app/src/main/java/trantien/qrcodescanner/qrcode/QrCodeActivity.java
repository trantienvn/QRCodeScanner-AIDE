package trantien.qrcodescanner.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.io.IOException;
import trantien.qrcodescanner.R;
import trantien.qrcodescanner.qrcode.camera.CameraManager;
import trantien.qrcodescanner.qrcode.decode.CaptureActivityHandler;
import trantien.qrcodescanner.qrcode.decode.DecodeManager;
import trantien.qrcodescanner.qrcode.decode.InactivityTimer;
import trantien.qrcodescanner.qrcode.view.QrCodeFinderView;

/**
 * Created by xingli on 12/26/15.
 * <p/>
 * 二维码扫描类。
 */
public class QrCodeActivity extends Activity implements Callback, OnClickListener {

    public static final String INTENT_OUT_STRING_SCAN_RESULT = "scan_result";
    private static final String INTENT_IN_INT_SUPPORT_TYPE = "support_type";
    private static final int REQUEST_PERMISSIONS = 1;
    private CaptureActivityHandler mCaptureActivityHandler;
    private boolean mHasSurface;
	private TextView create;
    private InactivityTimer mInactivityTimer;
    private QrCodeFinderView mQrCodeFinderView;
    private SurfaceView mSurfaceView;
    private View mLlFlashLight;
    private boolean mNeedFlashLightOpen = true;
    private ImageView mIvFlashLight;
    private TextView mTvFlashLightText;
    private ViewStub mSurfaceViewStub;
    private DecodeManager mDecodeManager = new DecodeManager();

	private static final int BROWSE_IMAGE_REQUEST_CODE = 99;
	public String data = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);
		Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        initView();
        initData();
    }

    private void initView() {
        final TextView tvPic = (TextView) findViewById(R.id.qr_code_header_black_pic);
        mIvFlashLight = (ImageView) findViewById(R.id.qr_code_iv_flash_light);
        mTvFlashLightText = (TextView) findViewById(R.id.qr_code_tv_flash_light);
        mQrCodeFinderView = (QrCodeFinderView) findViewById(R.id.qr_code_view_finder);
        mLlFlashLight = findViewById(R.id.qr_code_ll_flash_light);
		create = findViewById(R.id.create);
		((TextView) findViewById(R.id.ketqua)).setOnLongClickListener(new View.OnLongClickListener(){

				@Override
				public boolean onLongClick(View p1) {
					QRActivity.copyText(data,getApplicationContext());

					return false;
				}
			});
		create.setOnClickListener(this);
        mSurfaceViewStub = (ViewStub) findViewById(R.id.qr_code_view_stub);
        mHasSurface = false;
        getWindow().getDecorView().postDelayed(new Runnable() {
				@Override
				public void run() {
					mIvFlashLight.setOnClickListener(QrCodeActivity.this);
					tvPic.setOnClickListener(QrCodeActivity.this);
				}
			}, 1000);
    }

    private void initData() {
        CameraManager.init();
        mInactivityTimer = new InactivityTimer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, REQUEST_PERMISSIONS);
        }
    }

    private void initCamera() {
        if (null == mSurfaceView) {
            mSurfaceViewStub.setLayoutResource(R.layout.layout_surface_view);
            mSurfaceView = (SurfaceView) mSurfaceViewStub.inflate();
        }
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        if (mHasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCaptureActivityHandler != null) {
            try {
                mCaptureActivityHandler.quitSynchronously();
                mCaptureActivityHandler = null;
                mHasSurface = false;
                if (null != mSurfaceView) {
                    mSurfaceView.getHolder().removeCallback(this);
                }
                CameraManager.get().closeDriver();
            } catch (Exception e) {
                // 关闭摄像头失败的情况下,最好退出该Activity,否则下次初始化的时候会显示摄像头已占用.
                finish();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        findViewById(R.id.qr_code_view_background).setVisibility(View.VISIBLE);
        mQrCodeFinderView.setVisibility(View.GONE);
        mDecodeManager.showPermissionDeniedDialog(this);
    }

    @Override
    protected void onDestroy() {
        if (null != mInactivityTimer) {
            mInactivityTimer.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     */
    public void handleDecode(Result result) {
        mInactivityTimer.onActivity();
        if (null == result) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
					@Override
					public void refresh() {
						restartPreview();
					}
				});
        } else {
            final String resultString = result.getText();
			data = resultString;
			
			((TextView) findViewById(R.id.ketqua)).setText(resultString);
			restartPreview();
			
            //handleResult(resultString);

        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            if (!CameraManager.get().openDriver(surfaceHolder)) {
                showPermissionDeniedDialog();
                return;
            }
        } catch (IOException e) {
            // 基本不会出现相机不存在的情况
            Toast.makeText(this, getString(R.string.qr_code_camera_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (RuntimeException re) {
            re.printStackTrace();
            showPermissionDeniedDialog();
            return;
        }
        mQrCodeFinderView.setVisibility(View.VISIBLE);
        mLlFlashLight.setVisibility(View.VISIBLE);
        findViewById(R.id.qr_code_view_background).setVisibility(View.GONE);
        turnFlashLightOff();
        if (mCaptureActivityHandler == null) {
            mCaptureActivityHandler = new CaptureActivityHandler(this);
        }
    }

    private void restartPreview() {
        if (null != mCaptureActivityHandler) {
            try {
                mCaptureActivityHandler.restartPreviewAndDecode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    public Handler getCaptureActivityHandler() {
        return mCaptureActivityHandler;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.qr_code_iv_flash_light:
                if (mNeedFlashLightOpen) {
                    turnFlashlightOn();
                } else {
                    turnFlashLightOff();
                }
                break;
			case R.id.qr_code_header_black_pic:
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, "Select the image with the QR Code"), BROWSE_IMAGE_REQUEST_CODE);
				break;
			case R.id.create:
				startActivity(new Intent(this,CreateQRActivity.class));
				break;
			
        }
    }

    private void turnFlashlightOn() {
        try {
            CameraManager.get().setFlashLight(true);
            mNeedFlashLightOpen = false;
            mTvFlashLightText.setText(getString(R.string.qr_code_close_flash_light));
            mIvFlashLight.setBackgroundResource(R.drawable.flashlight_turn_on);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnFlashLightOff() {
        try {
            CameraManager.get().setFlashLight(false);
            mNeedFlashLightOpen = true;
            mTvFlashLightText.setText(getString(R.string.qr_code_open_flash_light));
            mIvFlashLight.setBackgroundResource(R.drawable.flashlight_turn_off);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 0) {
            int cameraPermission = grantResults[0];
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
												  REQUEST_PERMISSIONS);
            }
        }
    }
Uri i;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == BROWSE_IMAGE_REQUEST_CODE) {
			try {
				i= data.getData();
				Bitmap qrBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
				GalleryScan scanBitmap = new GalleryScan();
				scanBitmap.execute(qrBitmap);
			} catch (Exception e) {

			}
		}
	}
	
    private void handleResult(String resultString) {
        if (TextUtils.isEmpty(resultString)) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
					@Override
					public void refresh() {
						restartPreview();
					}
				});
        } else {
            mDecodeManager.showResultDialog(this, resultString, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						restartPreview();
					}
				});
        }
    }
	public class GalleryScan extends AsyncTask<Bitmap, Void, Result> {

		@Override
		protected Result doInBackground(Bitmap... bitmaps) {

			Reader reader = new MultiFormatReader();
			Result result = null;
			int[] intArray = new int[bitmaps[0].getWidth() * bitmaps[0].getHeight()];
			bitmaps[0].getPixels(intArray, 0, bitmaps[0].getWidth(), 0, 0, bitmaps[0].getWidth(), bitmaps[0].getHeight());
			LuminanceSource source = new RGBLuminanceSource(bitmaps[0].getWidth(), bitmaps[0].getHeight(), intArray);
			BinaryBitmap bMap = new BinaryBitmap(new HybridBinarizer(source));

			try {
				result = reader.decode(bMap);
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (ChecksumException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(Result result) {
			super.onPostExecute(result);
			if (result == null) {
				Toast.makeText(QrCodeActivity.this, "Unsupported Code try again", Toast.LENGTH_SHORT).show();
				return;
			}
			data = result.getText();
			((TextView) findViewById(R.id.ketqua)).setText(data);
			restartPreview();
			showQr(data,null);
		}
	}

	public void showQr(String qrString, String Type) {
		Intent intent =  new Intent(this, QRActivity.class);
        intent.putExtra("QR_STRING",qrString);
        intent.putExtra("QR_TYPE",Type);
		intent.putExtra("BROWSE_IMAGE_URI",i.toString());
        startActivity(intent);
	}
}
