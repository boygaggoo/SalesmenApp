package eu.janmuller.application.salesmenapp.activity;

import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.WindowManager;
import eu.janmuller.application.salesmenapp.R;
import roboguice.activity.RoboActivity;

/**
 * Created with IntelliJ IDEA.
 * Coder: Jan Müller
 * Date: 17.10.13
 * Time: 17:07
 */
public class BaseActivity extends RoboActivity {

    protected ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // vynutime landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        super.onCreate(savedInstanceState);

        if ((mActionBar = getActionBar()) != null) {

            mActionBar.setDisplayShowTitleEnabled(false);
            mActionBar.setLogo(R.drawable.logo);
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
