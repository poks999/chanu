package com.chanapps.four.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.CoverPageFragment;
import com.chanapps.four.fragment.PickNewThreadBoardDialogFragment;
import com.chanapps.four.service.NetworkProfileManager;

public class
        CoverPageActivity
        extends AbstractDrawerActivity
        implements ChanIdentifiedActivity,
        ListView.OnItemClickListener
{
    public static final String TAG = CoverPageActivity.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, CoverPageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    @Override
    protected void createViews(Bundle bundle) {
        Fragment fragment = new CoverPageFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, CoverPageFragment.TAG).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cover_page_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.refresh_menu:
                setProgressBarIndeterminateVisibility(true);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.new_thread_menu:
                new PickNewThreadBoardDialogFragment(handler)
                        .show(getFragmentManager(), PickNewThreadBoardDialogFragment.TAG);
                return true;
            case R.id.offline_chan_view_menu:
                GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.global_rules_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this,
                        R.layout.board_rules_dialog, R.raw.global_rules_header, R.raw.global_rules_detail);
                rawResourceDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.COVER_PAGE_ACTIVITY, ChanBoard.POPULAR_BOARD_CODE);
	}

    @Override
    public boolean isSelfBoard(String boardAsMenu) {
        return isCoverPage(boardAsMenu);
    }

    @Override
    public void refresh() {
        CoverPageFragment fragment = (CoverPageFragment)getSupportFragmentManager()
                .findFragmentByTag(CoverPageFragment.TAG);
        if (fragment != null)
            fragment.refresh();
    }

}