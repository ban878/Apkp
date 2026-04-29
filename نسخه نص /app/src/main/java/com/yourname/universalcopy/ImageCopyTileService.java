package com.yourname.universalcopy;

import android.content.Intent;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class ImageCopyTileService extends TileService {
    @Override
    public void onClick() {
        super.onClick();
        
        try {
            Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeIntent);
        } catch (Exception e) {}

        // استخدام النص من ملف string ليدعم اللغتين
        Toast.makeText(this, getString(R.string.toast_feature_disabled), Toast.LENGTH_SHORT).show();
    }
}