/**
 * 
 */
package org.rapidandroid.activity;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import org.rapidandroid.ActivityConstants;
import org.rapidandroid.R;
import org.rapidandroid.content.translation.ModelTranslator;
import org.rapidandroid.content.translation.ParsedDataTranslator;
import org.rapidandroid.data.RapidSmsDBConstants;
import org.rapidandroid.data.controller.ParsedDataReporter;
import org.rapidsms.java.core.model.Field;
import org.rapidsms.java.core.model.Form;
import org.rapidsms.java.core.model.Message;
import org.rapidsms.java.core.parser.IParseResult;
import org.rapidsms.java.core.parser.service.ParsingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * 
 * @author Daniel Myung dmyung@dimagi.com
 * @created Jan 12, 2009
 * 
 *          Activity window for editing a Form.
 * 
 */

public class FormReviewer extends Activity {
	private static final int MENU_DONE = Menu.FIRST;
	private static final int MENU_FORMAT = Menu.FIRST + 1;
	private static final int MENU_DUMP_CSV = Menu.FIRST + 2;
	private static final int MENU_INJECT_DEBUG = Menu.FIRST + 3;

	private Form mForm;
	private ProgressDialog mLoadingDialog;
	
	final Handler mDebugHandler = new Handler();
	
	final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };
    
    private void updateResultsInUi() {
    	//showDialog(0);
    	mLoadingDialog.dismiss();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.form_edit);
		mLoadingDialog = new ProgressDialog(this,ProgressDialog.STYLE_HORIZONTAL);
		mLoadingDialog.setMessage("Loading data...");
		mLoadingDialog.setTitle("Please wait");
		mLoadingDialog.setIndeterminate(true);
		mLoadingDialog.setCancelable(false);			

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			int formID = extras.getInt(ActivityConstants.REVIEW_FORM);
			mForm = ModelTranslator.getFormById(formID);

			TextView txv_formname = (TextView) findViewById(R.id.txv_formname);
			TextView txv_prefix = (TextView) findViewById(R.id.txv_formprefix);
			TextView txv_description = (TextView) findViewById(R.id.txv_description);

			ListView lsv_fields = (ListView) findViewById(R.id.lsv_fields);

			txv_formname.setText(mForm.getFormName());
			txv_prefix.setText(mForm.getPrefix());
			txv_description.setText(mForm.getDescription());

			int len = mForm.getFields().length;
			String[] fields = new String[len];
			for (int i = 0; i < len; i++) {
				Field field = mForm.getFields()[i];
				fields[i] = field.getName() + " [" + field.getFieldType().getItemType() + "]";
			}
			lsv_fields.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fields));

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_DONE, 0, R.string.formreview_menu_done).setIcon(android.R.drawable.ic_menu_revert);
		;
		menu.add(0, MENU_FORMAT, 0, R.string.formreview_menu_format).setIcon(android.R.drawable.ic_menu_info_details);
		;
		menu.add(0, MENU_DUMP_CSV, 0, R.string.formreview_dump_csv).setIcon(android.R.drawable.ic_menu_info_details);
		;
		menu.add(0, MENU_INJECT_DEBUG, 0, "Generate Data");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
			case MENU_DONE:
				finish();
				return true;
			case MENU_FORMAT:
				// Intent mIntent = new Intent();
				// mIntent.putExtras(bundle);
				// setResult(RESULT_OK, mIntent)

				try {
					//this is because the randomization doesn't get instantiated for some weird reason
					//unless we blow away the dialog
					removeDialog(0);
				} catch (Exception ex) {
				}
				showDialog(0);
				return true;
			case MENU_DUMP_CSV:
				outputCSV();
				break;
			case MENU_INJECT_DEBUG:
				injectMessages();
				break;

		}
		return true;
	}

	/**
	 * 
	 */
	private void outputCSV() {
		// TODO Auto-generated method stub
		
		mLoadingDialog.setMessage("Outputting csv...");
		mLoadingDialog.show();
		// Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            public void run() {
            	ParsedDataReporter pdr = new ParsedDataReporter(getBaseContext());
            	Calendar now = Calendar.getInstance();
            	Calendar then = Calendar.getInstance();
            	then.set(Calendar.YEAR, 1990);
        		
            	pdr.exportFormDataToCSV(mForm,then, now);
                mDebugHandler.post(mUpdateResults);
            }
        };
        t.start();
		
	}
	
	

	/**
	 * 
	 */
	private void injectMessages() {
		// TODO Auto-generated method stub
		mLoadingDialog.show();
		// Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            public void run() {
            	doInjection();
                mDebugHandler.post(mUpdateResults);
            }
        };
        t.start();
	}
	
	private void doInjection() {
		
		for (int i = 0; i < 100; i++) {
			StringBuilder sb = this.generateRandomMessage();

			Uri writeMessageUri = RapidSmsDBConstants.Message.CONTENT_URI;

			ContentValues messageValues = new ContentValues();
			messageValues.put(RapidSmsDBConstants.Message.MESSAGE, sb.toString());
			messageValues.put(RapidSmsDBConstants.Message.PHONE, "6176453236");

			Date now = getRandomDate();

			messageValues.put(RapidSmsDBConstants.Message.TIME, Message.SQLDateFormatter.format(now));
			messageValues.put(RapidSmsDBConstants.Message.IS_OUTGOING, false);

			Uri msgUri = null;

			msgUri = getContentResolver().insert(writeMessageUri, messageValues);

			Vector<IParseResult> results = ParsingService.ParseMessage(mForm, sb.toString());
			ParsedDataTranslator.InsertFormData(this, mForm, Integer.valueOf(msgUri.getPathSegments().get(1))
																	.intValue(), results);
		}
		
	}

	private Date getRandomDate() {
		Calendar cdr = Calendar.getInstance();
		cdr.set(1999, 1, 1);
		cdr.set(Calendar.HOUR_OF_DAY, 6);
		cdr.set(Calendar.MINUTE, 0);
		cdr.set(Calendar.SECOND, 0);
		long val1 = cdr.getTimeInMillis();

		cdr.set(2009, 1, 29);
		cdr.set(Calendar.HOUR_OF_DAY, 23);
		cdr.set(Calendar.MINUTE, 0);
		cdr.set(Calendar.SECOND, 0);
		long val2 = cdr.getTimeInMillis();

		Random r = new Random();
		long randomTS = (long) (r.nextDouble() * (val2 - val1)) + val1;
		Date d = new Date(randomTS);
		return d;
	}

	private static String[] bools = new String[] { "t", "f", "true", "false", "yes", "no", "y", "n" };
	private static String[] heights = new String[] { "cm", "m", "meters", "meter" };
	private static String[] lengths = new String[] { "cm", "m", "meters", "meter" };
	private static String[] weights = new String[] { "kg", "kilos", "kilo", "kg.", "kgs" };
	private static String[] words = new String[] { "bos", "nyc", "jfk", "lax", "lun", "lhr", "asvasd", "alksjwlejrwer",
			"bshdkghk", "akhsdwer", "tiwowuy", "xvcxbxkhcvb" };
	Random r = new Random();

	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);

		String title = "Sample submission";

		if (mForm == null) {
			return null;
		}

		StringBuilder sb = generateRandomMessage();

		return new AlertDialog.Builder(FormReviewer.this).setTitle(title).setMessage(sb.toString().trim())
															.setPositiveButton("OK", null).create();

	}

	/**
	 * @return
	 */
	private StringBuilder generateRandomMessage() {
		StringBuilder sb = new StringBuilder();

		sb.append(mForm.getPrefix() + " ");

		Field[] fields = mForm.getFields();
		int len = fields.length;

		for (int i = 0; i < len; i++) {
			Field field = fields[i];

			String type = field.getFieldType().getTokenName();

			if (type.equals("word")) {
				sb.append(words[r.nextInt(words.length)]);
			} else if (type.equals("number")) {
				sb.append(r.nextInt(1000));
			} else if (type.equals("height")) {
				sb.append(r.nextInt(200) + " " + heights[r.nextInt(heights.length)]);
			} else if (type.equals("boolean")) {
				sb.append(bools[r.nextInt(bools.length)]);
			} else if (type.equals("length")) {
				sb.append(r.nextInt(200) + " " + lengths[r.nextInt(lengths.length)]);
			} else if (type.equals("ratio")) {
				String floatString = r.nextFloat() + "";
				sb.append(floatString.substring(0, 4));
			} else if (type.equals("weight")) {
				sb.append(r.nextInt(150) + " " + weights[r.nextInt(weights.length)]);
			}
			sb.append(" ");

		}
		return sb;
	}
}