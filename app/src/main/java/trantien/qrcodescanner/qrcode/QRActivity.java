package trantien.qrcodescanner.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.HybridBinarizer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import trantien.qrcodescanner.R;
import trantien.qrcodescanner.qrcode.create.QRCode;
import java.io.OutputStream;
import trantien.qrcodescanner.qrcode.create.QRCodeEncoder;

public class QRActivity extends AppCompatActivity {
    Bitmap qrBitmap;
	String type = null;
	String result1=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.qr_layout);
		TextView content= (TextView) findViewById(R.id.qrcontent);
		result1 = getIntent().getExtras().getString("QR_STRING");
		type = getIntent().getExtras().getString("QR_TYPE");
		content.setText(result1);
		if (type != null) {
			findViewById(R.id.saveimg).setVisibility(View.VISIBLE);
			
            QRCode qrCode = new QRCode(result1);
            try {
                qrBitmap =  qrCode.getSimpleBitmap(Color.BLACK, null, type);
                if (qrBitmap != null) {
                    ScanBitmap scanBitmap = new ScanBitmap();
                    scanBitmap.execute(qrBitmap);
                }

				findViewById(R.id.saveimg).setOnClickListener(new View.OnClickListener(){

						@Override
						public void onClick(View p1) {
							savaImage(qrBitmap,null);
						}
					});
            } catch (WriterException e) {
                e.printStackTrace();
            }
        } else {
			findViewById(R.id.saveimg).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.saveimg)).setText("Sao Chép");
			Uri uri = Uri.parse(getIntent().getExtras().getString("BROWSE_IMAGE_URI"));
            try {
                qrBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ScanBitmap scanBitmap = new ScanBitmap();
                scanBitmap.execute(qrBitmap);

				findViewById(R.id.saveimg).setOnClickListener(new View.OnClickListener(){

						@Override
						public void onClick(View p1) {
							//savaImage(qrBitmap,null);
							copyText(result1,getApplicationContext());
						}
					});
            } catch (Exception e) {

            }
		}
    }
	
	public void savaImage(Bitmap bitmap,View view )
    {
        FileOutputStream outStream = null;
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/Pictures");
        Log.d("checkpath",sdCard.getAbsolutePath());
        dir.mkdirs();
        String fileName = String.format("IMG_%d.png", System.currentTimeMillis());
        File outFile = new File(dir, fileName);
        try {
            outStream = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        //Snackbar.make(view,"saved in master-qr",Snackbar.LENGTH_SHORT).show();
        try {
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
		//galleryAddPic(outFile);
		//refreshGallery(outFile);
        refreshGallery(outFile);
    }
	void writefile(){
		File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/Download");
		dir.mkdirs();
		String fileName = String.format("%d.svg", System.currentTimeMillis());
        File outFile = new File(dir, fileName);
		try{
			FileOutputStream out = new FileOutputStream(outFile);
			out.write(QRCodeEncoder.svg(result1).getBytes());
			out.flush();
			out.close();
		}catch(Exception e){
			
		}
	}
    public void refreshGallery(File file) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
		sendBroadcast(intent);
    }
    public static boolean isStoragePermissionGranted(Activity activity, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

	class ScanBitmap extends AsyncTask<Bitmap, Void, Result> {

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
            if (result != null) {

                if (qrBitmap != null) {
                    ((ImageView)findViewById(R.id.qr_image)).setImageBitmap(qrBitmap);
                }
			}
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            savaImage(qrBitmap, null);
        }
    }

    public static void copyText(String text, Context x) {
        ClipboardManager clipboard = (ClipboardManager) x.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Text", text);
        clipboard.setPrimaryClip(clip);
    }
	/*void saveImg({
	 if (isStoragePermissionGranted(this,this))
	 {
	 new Utils().savaImage(qrBitmap,parentView);
	 }
	 }*/
}

