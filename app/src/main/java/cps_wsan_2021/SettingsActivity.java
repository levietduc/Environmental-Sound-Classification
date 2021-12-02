package cps_wsan_2021;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.example.cps_wsan_2021_scratch.R;

import cps_wsan_2021.File.SaveConfig;

public class SettingsActivity extends AppCompatActivity {
    private EditText mEditTextLbl;
    private EditText mEditTextFileID;
    private EditText mEditTextDuration;
    private Button mSaveButton;
    private CheckBox mChkBoxCont;
    private CheckBox mChkBoxName;
    private CheckBox mChkBoxMAC;
    private CheckBox mChkBoxTime;
    private SaveConfig saveCfg;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mEditTextLbl=findViewById(R.id.inpLabel);
        mEditTextFileID=findViewById(R.id.txtInpStartIdx);
        mEditTextDuration=findViewById(R.id.editTextDuration);

        mSaveButton=findViewById(R.id.buttonSaveConfig);
        mChkBoxCont=findViewById(R.id.chkBoxCont);
        mChkBoxName=findViewById(R.id.checkBoxThingyName);
        mChkBoxMAC=findViewById(R.id.checkBoxMAC);
        mChkBoxTime=findViewById(R.id.checkBoxTime);

        saveCfg=MainActivity.transferConfig();
        if (saveCfg.mLabel!=null)
        {
            mEditTextLbl.setText(saveCfg.mLabel);
        }
        String strIdx=String.valueOf(saveCfg.mIdx);
        mEditTextFileID.setText(strIdx);
        mChkBoxCont.setChecked(saveCfg.mCont);
        mChkBoxName.setChecked(saveCfg.mIncludeThingyName);
        mChkBoxMAC.setChecked(saveCfg.mIncludeMAC);
        mChkBoxTime.setChecked(saveCfg.mIncludeTime);


        mSaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String strLabel;
                String strIdx;
                String strDuration;
                if(mEditTextLbl.getText().length()==0)
                {
                    strLabel=new String("sound");
                }
                else strLabel= mEditTextLbl.getText().toString();

                if(mEditTextFileID.getText().length()==0)
                {
                    strIdx=new String("00");
                }
                else strIdx= mEditTextFileID.getText().toString();

                if(mEditTextDuration.getText().length()==0)
                {
                    strDuration=new String("1000");
                }
                else strDuration= mEditTextDuration.getText().toString();

                int inx=Integer.valueOf(strIdx);
                int duration=Integer.valueOf(strDuration);
                boolean cont=mChkBoxCont.isChecked();
                boolean mac=mChkBoxMAC.isChecked();
                boolean time=mChkBoxTime.isChecked();
                boolean name=mChkBoxName.isChecked();

                SaveConfig cfg=new SaveConfig(strLabel,inx,duration,cont,name,mac,time);
                MainActivity.returnConfig(cfg);

                Intent intent = new Intent(SettingsActivity.this,
                       MainActivity.class);
               //intent.putExtra("Text","hello");
               startActivity(intent);
            }
        });


    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}