package top.geek_studio.chenlongcould.musicplayer.ui.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.appthemehelper.util.NavigationViewUtil;
import com.kabouzeid.chenlongcould.musicplayer.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import top.geek_studio.chenlongcould.musicplayer.App;
import top.geek_studio.chenlongcould.musicplayer.dialogs.ChangelogDialog;
import top.geek_studio.chenlongcould.musicplayer.dialogs.ScanMediaFolderChooserDialog;
import top.geek_studio.chenlongcould.musicplayer.glide.SongGlideRequest;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.helper.SearchQueryHelper;
import top.geek_studio.chenlongcould.musicplayer.live2d.LAppLive2DManager;
import top.geek_studio.chenlongcould.musicplayer.live2d.LAppView;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.FileManager;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.SoundManager;
import top.geek_studio.chenlongcould.musicplayer.loader.AlbumLoader;
import top.geek_studio.chenlongcould.musicplayer.loader.ArtistLoader;
import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistSongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.MyViewModel;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.service.MusicService;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.base.AbsSlidingMusicPanelActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.intro.AppIntroActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.folders.FoldersFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.LibraryFragment;
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * MainActivity {@link R.layout#activity_main_drawer_layout}
 */
public class MainActivity extends AbsSlidingMusicPanelActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int APP_INTRO_REQUEST = 100;
    public static final int PURCHASE_REQUEST = 101;

    private static final int LIBRARY = 0;
    private static final int FOLDERS = 1;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Nullable
    MainActivityFragmentCallbacks currentFragment;

    ////////////////// LIVE 2D ////////////////////
    private LAppLive2DManager live2DMgr;
    private LAppView mlAppView;
    private FrameLayout mLive2DContent;
    ////////////////// LIVE 2D ////////////////////

    private MyViewModel mViewModel;

    @Nullable
    private View navigationDrawerHeader;

    private boolean blockRequestPermissions;

    private boolean pressBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            navigationView.setFitsSystemWindows(false); // for header to go below statusbar
        }

        setUpDrawerLayout();

        if (savedInstanceState == null) {
            setMusicChooser(PreferenceUtil.getInstance(this).getLastMusicChooser());
        } else {
            restoreCurrentFragment();
        }

        if (!checkShowIntro()) {
            showChangelog();
        }

        mViewModel = ViewModelProviders.of(this).get(MyViewModel.class);

    }

    /**
     * 设置 Live2D
     */
    private void setUpLive2D() {
        FileManager.init(this);
        SoundManager.init(this);

        live2DMgr = new LAppLive2DManager();

        mlAppView = live2DMgr.createView(this, true);
        mlAppView.setBackground(null);

        mLive2DContent = new FrameLayout(this);
        mLive2DContent.addView(mlAppView);

        navigationView.addView(mLive2DContent);

        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mlAppView.getLayoutParams();
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.width = 400;
        lp.height = 800;

        mlAppView.setLongClickable(true);
        mlAppView.setOnLongClickListener(view -> {
            new MaterialDialog.Builder(MainActivity.this).title("Hi")
                    .content("这里是 ACG Player 助手酱 ~")
                    .showListener(dialogInterface -> mViewModel.dialogs.add(dialogInterface))
                    .dismissListener(dialogInterface -> mViewModel.dialogs.remove(dialogInterface))
                    .negativeText("Change Me!")
                    .onNegative((dialog, which) -> live2DMgr.changeModel())
                    .show();
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mlAppView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpLive2D();

        if (mlAppView != null) mlAppView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileManager.clear();
        SoundManager.release();
        mLive2DContent.removeAllViews();
        mlAppView = null;
        mLive2DContent = null;

        for (DialogInterface d : mViewModel.dialogs) {
            if (d != null) d.dismiss();
        }

        mViewModel.dialogs.clear();
    }

    private void setMusicChooser(int key) {
        if (!App.isProVersion() && key == FOLDERS) {
            Toast.makeText(this, R.string.folder_view_is_a_pro_feature, Toast.LENGTH_LONG).show();
            startActivityForResult(new Intent(this, PurchaseActivity.class), PURCHASE_REQUEST);
            key = LIBRARY;
        }

        PreferenceUtil.getInstance(this).setLastMusicChooser(key);
        switch (key) {
            case LIBRARY:
                navigationView.setCheckedItem(R.id.nav_library);
                setCurrentFragment(LibraryFragment.newInstance());
                break;
            case FOLDERS:
                navigationView.setCheckedItem(R.id.nav_folders);
                setCurrentFragment(FoldersFragment.newInstance(this));
                break;
        }
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, null).commit();
        currentFragment = (MainActivityFragmentCallbacks) fragment;
    }

    private void restoreCurrentFragment() {
        currentFragment = (MainActivityFragmentCallbacks) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false;
            if (!hasPermissions()) {
                requestPermissions();
            }
            checkSetUpPro(); // good chance that pro version check was delayed on first start
        } else if (requestCode == PURCHASE_REQUEST) {
            if (resultCode == RESULT_OK) {
                checkSetUpPro();
            }
        }
    }

    @Override
    protected void requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions();
    }

    @Override
    protected View createContentView() {
        @SuppressLint("InflateParams")
        View contentView = getLayoutInflater().inflate(R.layout.activity_main_drawer_layout, null);
        ViewGroup drawerContent = contentView.findViewById(R.id.drawer_content_container);
        drawerContent.addView(wrapSlidingMusicPanel(R.layout.activity_main_content));
        return contentView;
    }

    private void setUpNavigationView() {
        int accentColor = ThemeStore.accentColor(this);
        NavigationViewUtil.setItemIconColors(navigationView, ATHUtil.resolveColor(this, R.attr.iconColor, ThemeStore.textColorSecondary(this)), accentColor);
        NavigationViewUtil.setItemTextColors(navigationView, ThemeStore.textColorPrimary(this), accentColor);

        checkSetUpPro();
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            drawerLayout.closeDrawers();
            switch (menuItem.getItemId()) {
                case R.id.nav_library:
                    new Handler().postDelayed(() -> setMusicChooser(LIBRARY), 200);
                    break;
                case R.id.nav_folders:
                    new Handler().postDelayed(() -> setMusicChooser(FOLDERS), 200);
                    break;
                case R.id.buy_pro:
                    new Handler().postDelayed(() -> startActivityForResult(new Intent(MainActivity.this, PurchaseActivity.class), PURCHASE_REQUEST), 200);
                    break;
                case R.id.action_scan:
                    new Handler().postDelayed(() -> {
                        ScanMediaFolderChooserDialog dialog = ScanMediaFolderChooserDialog.create();
                        dialog.show(getSupportFragmentManager(), "SCAN_MEDIA_FOLDER_CHOOSER");
                    }, 200);
                    break;
                case R.id.nav_settings:
                    new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)), 200);
                    break;
                case R.id.nav_about:
                    new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, AboutActivity.class)), 200);
                    break;
                case R.id.nav_data_overview:
                    // TODO: DATA OVERVIEW
                    break;
                case R.id.nav_exit:
                    stopService(new Intent(MainActivity.this, MusicService.class));
                    finish();
                    break;
            }
            return true;
        });
    }

    private void checkSetUpPro() {
        if (App.isProVersion()) {
            setUpPro();
        }
    }

    private void setUpPro() {
        navigationView.getMenu().removeGroup(R.id.navigation_drawer_menu_category_buy_pro);
    }

    private void setUpDrawerLayout() {
        setUpNavigationView();

        drawerLayout.setScrimColor(Color.TRANSPARENT);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                Log.d(TAG, "onDrawerSlide: " + slideOffset);
                MainActivity.super.setBlur(slideOffset, false);
                MainActivity.super.translationRootView(slideOffset, 'x');
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    private void updateNavigationDrawerHeader() {
        if (!MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            Song song = MusicPlayerRemote.getCurrentSong();
            if (navigationDrawerHeader == null) {
                navigationDrawerHeader = navigationView.inflateHeaderView(R.layout.navigation_drawer_header);
                navigationDrawerHeader.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    if (getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        expandPanel();
                    }
                });
            }
            ((TextView) navigationDrawerHeader.findViewById(R.id.title)).setText(song.title);
            ((TextView) navigationDrawerHeader.findViewById(R.id.text)).setText(MusicUtil.getSongInfoString(song));
            SongGlideRequest.Builder.from(Glide.with(this), song)
                    .checkIgnoreMediaStore(this).build()
                    .into(((ImageView) navigationDrawerHeader.findViewById(R.id.image)));
        } else {
            if (navigationDrawerHeader != null) {
                navigationView.removeHeaderView(navigationDrawerHeader);
                navigationDrawerHeader = null;
            }
        }
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        updateNavigationDrawerHeader();
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        updateNavigationDrawerHeader();
        handlePlaybackIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handleBackPress() {

        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
            return true;
        }

        if (super.handleBackPress() || (currentFragment != null && currentFragment.handleBackPress()))
            return true;

        if (!pressBack) {
            pressBack = true;
            Toast.makeText(this, "Press again to exit!", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> pressBack = false, 2000);
            return true;
        } else {
            finish();
            return true;
        }
    }

    private void handlePlaybackIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (intent.getAction() != null && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            final List<Song> songs = SearchQueryHelper.getSongs(this, intent.getExtras());
            if (MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE) {
                MusicPlayerRemote.openAndShuffleQueue(songs, true);
            } else {
                MusicPlayerRemote.openQueue(songs, 0, true);
            }
            handled = true;
        }

        if (uri != null && uri.toString().length() > 0) {
            MusicPlayerRemote.playFromUri(uri);
            handled = true;
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "playlistId", "playlist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                List<Song> songs = new ArrayList<>(PlaylistSongLoader.getPlaylistSongList(this, id));
                MusicPlayerRemote.openQueue(songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "albumId", "album");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(AlbumLoader.getAlbum(this, id).songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "artistId", "artist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(ArtistLoader.getArtist(this, id).getSongs(), position, true);
                handled = true;
            }
        }
        if (handled) {
            setIntent(new Intent());
        }
    }

    private long parseIdFromIntent(@NonNull Intent intent, String longKey,
                                   String stringKey) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, String.valueOf(e.getMessage()));
                }
            }
        }
        return id;
    }

    @Override
    public void onPanelExpanded(View view) {
        super.onPanelExpanded(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onPanelCollapsed(View view) {
        super.onPanelCollapsed(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    private boolean checkShowIntro() {
        if (!PreferenceUtil.getInstance(this).introShown()) {
            PreferenceUtil.getInstance(this).setIntroShown();
            ChangelogDialog.setChangelogRead(this);
            blockRequestPermissions = true;
            new Handler().postDelayed(() -> startActivityForResult(new Intent(MainActivity.this, AppIntroActivity.class), APP_INTRO_REQUEST), 50);
            return true;
        }
        return false;
    }

    private void showChangelog() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int currentVersion = pInfo.versionCode;
            if (currentVersion != PreferenceUtil.getInstance(this).getLastChangelogVersion()) {
                ChangelogDialog.create().show(getSupportFragmentManager(), "CHANGE_LOG_DIALOG");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public interface MainActivityFragmentCallbacks {
        boolean handleBackPress();
    }
}
