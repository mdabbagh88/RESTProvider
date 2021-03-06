
package novoda.rest.test.apps.linkedin;

import java.io.IOException;
import java.util.Map;

import novoda.rest.test.R;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthUtil;

public class LinkedIn extends Activity {
    private static final String TAG = LinkedIn.class.getSimpleName();

    private OAuthorize authorize;

    private SharedPreferences pref;

    private ListView list;

    private CPAsyncHandler asyncHandler;

    private EditText status;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Map<String, String> firstLegToken = OAuthUtil.parseQuerystring(intent.getDataString()
                .split("\\?")[1]);
        try {
            Map<String, String> secondLegToken = authorize.getAccessToken(firstLegToken
                    .get("oauth_token"), firstLegToken.get("oauth_verifier"));
            Editor e = pref.edit();
            e.putString("oauth_token", secondLegToken.get("oauth_token"));
            e.putString("oauth_token_secret", secondLegToken.get("oauth_token_secret"));
            e.commit();
            actOnView();
        } catch (OAuthException e) {
            Log.e(TAG, "an error occured in onNewIntent", e);
        } catch (ClientProtocolException e) {
            Log.e(TAG, "an error occured in onNewIntent", e);
        } catch (IOException e) {
            Log.e(TAG, "an error occured in onNewIntent", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linked_in);
        asyncHandler = new CPAsyncHandler(getContentResolver());
        pref = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_PRIVATE);
        authorize = new OAuthorize(
                "Jwwr5FH4qrfn7_Mr19QgBg3zZG_v-j6DZOuPKPcnvM0WhpTx_Asqpw-dtJM1xDzC",
                "LwPZ9J00brVoiKQu75Y_rCfcK1Y8lMP18jFGkwDcArXN8tfGZC1xs70N9DHwzQgL",
                "https://api.linkedin.com/uas/oauth/requestToken",
                "https://api.linkedin.com/uas/oauth/authorize",
                "https://api.linkedin.com/uas/oauth/accessToken");

        actOnView();
        status = (EditText)findViewById(R.id.status);
        list = (ListView)findViewById(R.id.list);
    }

    private void actOnView() {
        if (pref.contains("oauth_token")) {
            ((Button)findViewById(R.id.authorize)).setEnabled(false);
            ((Button)findViewById(R.id.query)).setEnabled(true);
            ((Button)findViewById(R.id.update)).setEnabled(true);
            ((EditText)findViewById(R.id.status)).setEnabled(true);
        } else {
            ((Button)findViewById(R.id.authorize)).setEnabled(true);
            ((Button)findViewById(R.id.query)).setEnabled(false);
            ((Button)findViewById(R.id.update)).setEnabled(false);
            ((EditText)findViewById(R.id.status)).setEnabled(false);
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Delete oauth token");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 1:
            pref.edit().clear().commit();
            actOnView();
            return true;
        }
        return false;
    }

    public void onUpdateClick(View view) {
        ContentValues values = new ContentValues(1);
        values.put(Constants.CURRENT_STATUS, status.getText().toString());
        asyncHandler.startUpdate(1, null, Uri.parse("content://novoda.rest.test.linkedin"), values,
                null, null);
    }

    public void onAuthorizeClick(View view) {
        try {
            Map<String, String> p = authorize.getRequestToken();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri
                    .parse("https://api.linkedin.com/uas/oauth/authorize?oauth_token="
                            + p.get("oauth_token")));
            startActivity(intent);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OAuthException e) {
            e.printStackTrace();
        }
    }

    public void onQueryClick(final View view) {
        asyncHandler.startQuery(1, null, Uri.parse("content://novoda.rest.test.linkedin"), null,
                null, null, null);
    }

    private class CPAsyncHandler extends AsyncQueryHandler {

        public CPAsyncHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            if (cursor != null) {
                list.setAdapter(new SimpleCursorAdapter(LinkedIn.this,
                        android.R.layout.simple_list_item_2, cursor, new String[] {
                                "last-name", "headline"
                        }, new int[] {
                                android.R.id.text1, android.R.id.text2
                        }));
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            super.onUpdateComplete(token, cookie, result);

            // Expected result of 204 no content from Server
            if (result == 204) {
                Toast.makeText(LinkedIn.this, "Status updated correctly!", Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(LinkedIn.this, "Something went wrong :(", Toast.LENGTH_LONG).show();
            }
        }
    }
}
