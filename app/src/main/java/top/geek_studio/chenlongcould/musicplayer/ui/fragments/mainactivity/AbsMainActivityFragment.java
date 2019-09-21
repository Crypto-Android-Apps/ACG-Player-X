package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;

/**
 * BaseFragment
 *
 * @see top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.LibraryFragment
 * @see top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.folders.FoldersFragment
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsMainActivityFragment extends Fragment {

    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }
}
