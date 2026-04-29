package com.yourname.universalcopy;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class CopyTileService extends TileService {

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        if (tile == null) return;

        // قراءة الحالة الحالية للزر وعكسها مباشرة (يحل مشكلة الضغطتين)
        boolean isActive = (tile.getState() == Tile.STATE_ACTIVE);
        boolean newState = !isActive;

        tile.setState(newState ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();

        // إرسال الأمر للخدمة
        UniversalCopyService.toggleCopyMode(newState);
    }

    // تحديث شكل الزر تلقائياً عند قيام المستخدم بسحب شريط الإشعارات للأسفل
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        UniversalCopyService service = UniversalCopyService.getInstance();
        if (tile != null && service != null) {
            boolean isServiceActive = service.isSelectionModeActive();
            tile.setState(isServiceActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
}