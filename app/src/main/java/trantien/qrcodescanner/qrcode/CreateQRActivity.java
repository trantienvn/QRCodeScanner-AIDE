package trantien.qrcodescanner.qrcode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
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
import trantien.qrcodescanner.R;
import trantien.qrcodescanner.qrcode.view.UpdateView;

public class CreateQRActivity extends AppCompatActivity{
	//private UpdateView mCallback;
    private String QR_TYPE;
    String[] format = {
		"QR CODE",
		"AZTEC",
		"CODABAR",
		"CODE 39",
		"CODE 128",
		"DATA MATRIX",
		"EAN 8",
		"EAN 13",
		"ITF",
		"PDF 417",
		"UPC A"
    };
	
	//@Override
	public void showQr(String qrString, String Type) {
		Intent intent =  new Intent(CreateQRActivity.this, QRActivity.class);
        intent.putExtra("QR_STRING",qrString);
        intent.putExtra("QR_TYPE",Type);
       // intent.putExtra(ADD_TO_HISTORY,true);
        startActivity(intent);
	}
	
   // Fragment mFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create);
		init();
        
	}
public void init(){
	final EditText textView =  (EditText) findViewById(R.id.text_input);
	final TextView length = (TextView) findViewById(R.id.length);
	Button button =  (Button) findViewById(R.id.create_button);
	QR_TYPE = BarcodeFormat.QR_CODE.toString();


	Spinner spinnerDropDown = (Spinner) findViewById(R.id.spinner1);

	ArrayAdapter<String> adapter= new ArrayAdapter<String>(getApplicationContext(),android.
														   R.layout.simple_spinner_dropdown_item ,format);

	spinnerDropDown.setAdapter(adapter);
	spinnerDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

                QR_TYPE = format[position].replace(" ","_");
                switch (QR_TYPE)
                {
                    case "QR_CODE":
                    case "AZTEC":
                    case "DATA_MATRIX":
                    case "PDF_417":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_TEXT);
                        length.setText("1 - 1000");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1000)});
                        break;

                    case "CODABAR":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        length.setText("1 - 16");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(16)});
                        break;

                    case "CODE_39":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        length.setText("1 - 25");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(25)});
                        break;

                    case "CODE_128":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        length.setText("1 - 128");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(128)});
                        break;

                    case "EAN_8":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        length.setText("7");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(7)});
                        break;

                    case "EAN_13":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        length.setText("12");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(12)});
                        break;

                    case "ITF":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        length.setText("14");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(14)});
                        break;

                    case "UPC_A":
                        textView.getText().clear();
                        textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                        length.setText("11");
                        textView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(11)});
                        break;


                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


	button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (QR_TYPE)
                {
                    case "EAN_8":
						{
							if (textView.getText().length() < 7)
							{
								textView.setError("Number length should be 7");
								return;
							}

						}
						break;

                    case "EAN_13":
						{
							if (textView.getText().length() < 12)
							{
								textView.setError("Number length should be 12");
								return;
							}
						}
						break;

                    case "ITF":
						{
							if (textView.getText().length() < 14)
							{
								textView.setError("Number length should be 14");
								return;
							}
						}
						break;

                    case "UPC_A":
						{
							if (textView.getText().length() < 11)
							{
								textView.setError("Number length should be 11");
								return;
							}
						}
						break;

                    default:
                        if(TextUtils.isEmpty(textView.getText().toString())) {
                            textView.setError("Text can't be empty");
                            return;
                        }
                        break;
                }

                String s  =  textView.getText().toString();
                showQr(s, QR_TYPE);
            }
        });
}
    public class ScanBitmap extends AsyncTask<Bitmap, Void, Result> {

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
				Toast.makeText(CreateQRActivity.this, "Unsupported Code try again", Toast.LENGTH_SHORT).show();
				return;
			}
			
		}
	}
	
}
