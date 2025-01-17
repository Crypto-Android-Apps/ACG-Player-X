package top.geek_studio.chenlongcould.musicplayer.appshortcuts.shortcuttype;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import top.geek_studio.chenlongcould.musicplayer.Common.R;
import top.geek_studio.chenlongcould.musicplayer.appshortcuts.AppShortcutIconGenerator;
import top.geek_studio.chenlongcould.musicplayer.appshortcuts.AppShortcutLauncherActivity;

/**
 * 最后添加
 *
 * @author Adrian Campos
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public final class LastAddedShortcutType extends BaseShortcutType {

    /**
     * 构造
     *
     * @param context ctx
     */
    public LastAddedShortcutType(Context context) {
        super(context);
    }

    /**
     * 获取 ID
     */
    public static String getId() {
        return ID_PREFIX + "last_added";
    }

    /**
     * @see super#getShortcutInfo()
     */
    public ShortcutInfo getShortcutInfo() {
        return new ShortcutInfo.Builder(context, getId())
                .setShortLabel(context.getString(R.string.app_shortcut_last_added_short))
                .setLongLabel(context.getString(R.string.last_added))
                .setIcon(AppShortcutIconGenerator.generateThemedIcon(context, R.drawable.ic_app_shortcut_last_added))
                .setIntent(getPlaySongsIntent(AppShortcutLauncherActivity.SHORTCUT_TYPE_LAST_ADDED))
                .build();
    }
}
