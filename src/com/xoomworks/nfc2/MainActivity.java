package com.xoomworks.nfc2;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends Activity {

	
	private static final String TAG = "NFCWriterTag";
	private NfcAdapter mNfcAdapter;
	private IntentFilter[] mWriteTagFilters;
	private PendingIntent mNfcPendingIntent;
	private boolean silent = false;
	private boolean writeProtect = false;
	private Context context;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);
        IntentFilter discovery = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDiscovered = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        
        mWriteTagFilters = new IntentFilter[] {discovery};
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	if (mNfcAdapter != null)
    	{
    		if (!mNfcAdapter.isEnabled())
    		{
    			Toast.makeText(this, "Please enable NFC", Toast.LENGTH_SHORT).show();
    		}
    		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    	}
    	else
		{
			Toast.makeText(this, "Sorry, No NFC Adapter found", Toast.LENGTH_SHORT).show();
		}
    }
    
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if (mNfcAdapter != null)
    		mNfcAdapter.disableForegroundDispatch(this);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))
    	{
    		Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    		if (supportedTechs(detectedTag.getTechList()))
    		{
    			if (writableTag(detectedTag))
    			{
    				Toast.makeText(context, "The tag can be written. Yupii", Toast.LENGTH_SHORT).show();
    			} else
    			{
    				Toast.makeText(this, "This tag is not writable", Toast.LENGTH_SHORT).show();
    			}
    		} else
    		{
    			Toast.makeText(this, "This tag type is not supported", Toast.LENGTH_SHORT).show();
    		}
    	}
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    public WriteResponse writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        String mess = "";

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    return new WriteResponse(0,"Tag is read-only");

                }
                if (ndef.getMaxSize() < size) {
                    mess = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.";
                    return new WriteResponse(0,mess);
                }

                ndef.writeNdefMessage(message);
                if(writeProtect)  ndef.makeReadOnly();
                mess = "Wrote message to pre-formatted tag.";
                return new WriteResponse(1,mess);
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        mess = "Formatted tag and wrote message";
                        return new WriteResponse(1,mess);
                    } catch (IOException e) {
                        mess = "Failed to format tag.";
                        return new WriteResponse(0,mess);
                    }
                } else {
                    mess = "Tag doesn't support NDEF.";
                    return new WriteResponse(0,mess);
                }
            }
        } catch (Exception e) {
            mess = "Failed to write tag";
            return new WriteResponse(0,mess);
        }
    }
    
    
    private class WriteResponse
    {
    	int status;
    	String message;
    	
    	WriteResponse(int status, String message)
    	{
    		this.status = status;
    		this.message = message;
    	}
    	public int getStatus()
    	{
    		return status;
    	}
    	public String getMessage()
    	{
    		return message;
    	}
    }
    
    public static boolean supportedTechs(String[] techs)
    {
    	boolean ultralight = false;
    	boolean nfcA = false;
    	boolean ndef = false;
    	for (String tech:techs)
    	{
    		if (tech.equals("android.nfc.tech.MifareUltralight"))
    		{
    			ultralight = true;
    		}else if(tech.equals("android.nfc.tech.NfcA")) {   
                nfcA=true;  
            } else if(tech.equals("android.nfc.tech.Ndef") || tech.equals("android.nfc.tech.NdefFormatable")) {  
                 ndef=true;  
            }  
    	}
    	if (ultralight && nfcA && ndef)
    	{
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private boolean writableTag(Tag tag)
    {
    	try
    	{
    		Ndef ndef = Ndef.get(tag);
    		if (ndef != null)
    		{
    			ndef.connect();
    			if (!ndef.isWritable())
    			{
    				Toast.makeText(context, "Tag is read-only", Toast.LENGTH_SHORT).show();
    				ndef.close();
    				return false;
    			}
    			ndef.close();
    			return true;
    		}
    	}
    	catch (Exception e)
    	{
    		Toast.makeText(context, "Failed to read tag", Toast.LENGTH_SHORT).show();
    	}
    	return false;
    }
    
    private NdefMessage getTagAsNdef()
    {
    	boolean addAAR = false;
    	String uniqueID = "smartwhere.com/nfc.html";
    	byte[] uriField = uniqueID.getBytes(Charset.forName("US-ASCII"));
    	byte[] payload = new byte[uriField.length + 1];
    	payload[0] = 0x01;
    	System.arraycopy(uriField, 0, payload, 1, uriField.length);
    	NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload);
    	if (addAAR)
    	{
    		return new NdefMessage(new NdefRecord[]{
    			rtdUriRecord, NdefRecord.createApplicationRecord("com.tapwise.nfcreadtag")
    		});
    	} else
    	{
    		return new NdefMessage(new NdefRecord[]{
    			rtdUriRecord
    		});
    	}
    }
}
