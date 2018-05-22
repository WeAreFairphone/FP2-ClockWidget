package community.fairphone.clock;

import android.app.Activity;
import android.os.Bundle;

public class CentralActivity extends Activity {
    private static final String TAG = CentralActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
