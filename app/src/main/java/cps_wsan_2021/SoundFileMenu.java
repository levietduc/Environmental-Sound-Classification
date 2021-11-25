package cps_wsan_2021;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static cps_wsan_2021.common.Utils.showToast;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cps_wsan_2021_scratch.R;
import com.example.cps_wsan_2021_scratch.databinding.ActivitySoundFileMenuBinding;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cps_wsan_2021.common.MediaStoreAudio;
import cps_wsan_2021.common.PermissionRationaleDialogFragment;

public class SoundFileMenu extends AppCompatActivity implements  PermissionRationaleDialogFragment.PermissionDialogListener
{
    private final int READ_EXTERNAL_STORAGE_REQUEST = 0x1045;

    private ActivitySoundFileMenuBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySoundFileMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        openMediaStore();


        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private boolean haveStoragePermission() {
        int a;
        a= ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return (a == PERMISSION_GRANTED);
    }

    public void requestPermission() {
        if (!haveStoragePermission()) {
            String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};

            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST);
        }
    }
    //----------------------
    private void openMediaStore() {
        if (haveStoragePermission()) {
            queryAudio();
            //PStest showImages();
        } else {
            requestPermission();
        }
    }
    private void showImages() {
        // viewModel.loadImages();
        // binding.welcomeView.visibility = View.GONE;
        // binding.permissionRationaleView.visibility = View.GONE;
    }
    @Override
    public void onRequestPermission(final String permission, final int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    @Override
    public void onCancellingPermissionRationale() {
        showToast(this, getString(R.string.requested_permission_not_granted_rationale));

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    queryAudio();
                    // Permission is granted. Continue the action or workflow
                    // in your app.

                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Intent intent = new Intent(SoundFileMenu.this,
                            MainActivity.class);
                    intent.putExtra("Text","hello");
                    startActivity(intent);
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }


    private List<MediaStoreAudio> queryAudio() {

        List<MediaStoreAudio> audiolist = new ArrayList<MediaStoreAudio>();

        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }


        String[] projection ={
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DURATION,
                String.valueOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)};
        String selection = MediaStore.Audio.Media.DURATION +" >= ?"; //location
        /**
         * The `selectionArgs` is a list of values that will be filled in for each `?`
         * in the `selection`.
         */
        /*String selection = MediaStore.Video.Media.DURATION +
                " >= ?";
        String[] selectionArgs = new String[] {
                String.valueOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
        };*/

        Date utilDate = new Date(22,10,2008);
        String[] selectionArgs = {"1224626"};//{"2019.08.02 01:44:51.596"};
        String sortOrder=MediaStore.Audio.Media.DATE_ADDED +" DESC";
        Cursor cursor = getApplicationContext().getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        // Cache column indices.
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        int nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
        int durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
        int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
        int dateColumn=cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            long id = cursor.getLong(idColumn);
            String name = cursor.getString(nameColumn);
            String date=cursor.getString(dateColumn);
            int duration = cursor.getInt(durationColumn);
            int size = cursor.getInt(sizeColumn);

            Uri contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

            // Stores column values and the contentUri in a local object
            // that represents the media file.
            audiolist.add(new MediaStoreAudio(id,name,date,contentUri, duration));
        }
        return audiolist;
    }

}

