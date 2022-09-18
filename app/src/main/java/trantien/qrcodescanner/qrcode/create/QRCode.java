package trantien.qrcodescanner.qrcode.create;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import java.util.Map;

public class QRCode {
    public final static int DEFAULT_BG = 0xFFFFFFFF;
    public final static int DEFAULT_FG = 0xFF000000;
    public static int foreground = DEFAULT_FG;
    public static int background = DEFAULT_BG;
    public static int WIDTH = 2000;
    public final static int HEIGHT = 2000;

    private String str;

    
    public QRCode(String string) {
        this.str = string;
    }

    @Override
    public String toString() {
        return "QRCode{" + "str='" + str + '\'' + '}';
    }

    public Bitmap getSimpleBitmap(@Nullable Map<EncodeHintType, Object> hints) throws WriterException {
        return getSimpleBitmap(DEFAULT_FG, hints);
    }

    public Bitmap getSimpleBitmap(int foregroundColor, @Nullable Map<EncodeHintType, Object> hints) throws WriterException {


        QRCodeEncoder qrCode = new QRCodeEncoder(str, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), HEIGHT,HEIGHT); // HEIGHT AND WIDTH

        return qrCode.encodeAsBitmap(foregroundColor, hints);
    }

    public Bitmap getSimpleBitmap(int foregroundColor, @Nullable Map<EncodeHintType, Object> hints, String type) throws WriterException {

        Log.d("checkgetSimpleBitmap",type);

        switch (type)
        {
            case "QR_CODE":
            case "AZTEC":
				WIDTH = 2000;
				break;
            case "DATA_MATRIX":
                WIDTH = 2000;
                break;

            default:
                WIDTH = 4000;
                break;
        }


        QRCodeEncoder qrCode = new QRCodeEncoder(str, null,
                Contents.Type.TEXT, type, WIDTH, HEIGHT); // HEIGHT AND WIDTH

        return qrCode.encodeAsBitmap(foregroundColor, hints);
    }


}
