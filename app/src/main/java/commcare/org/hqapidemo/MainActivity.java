package commcare.org.hqapidemo;

import android.app.DownloadManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * A simple demo application to demonstrate how to use the CCHQ Web APIs from an Android device.
 * Relevant documentation here https://confluence.dimagi.com/display/commcarepublic/Data+APIs
 * Note that HTTP requests must be performed off the main thread and that Android has some
 * annoying digest auth quirks.
 */
public class MainActivity extends AppCompatActivity {

    String domain = "test";                                 // your CCHQ domain
    String user = "testemail@gmail.com";                    // your CCHQ WEB USER email address
    String password = "12345";                              // your CCHQ WEB USER password
    String caseId = "d92c4738-g252-4e5f-b76d-a7ff9d5ddb30"; // for demo purposes only - this should be retrieved dynamicallu
    String apiVersion = "v0.4";                             // API version to use - described in documents

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button listCasesButton =  (Button) findViewById(R.id.listCases);
        listCasesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    queryCaseList(domain, user, password);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button getCaseDataButton =  (Button) findViewById(R.id.getCaseData);
        getCaseDataButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    queryCaseData(domain, user, password, apiVersion, caseId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * This function will call the Case Data API [https://confluence.dimagi.com/display/commcarepublic/Case+Data]
     * and display the results in this text view - can override this behavior in onPostExecute
     * @param domain your CCHQ domain name
     * @param username the username of your WEB USER with Admin privelidges, *not* your mobile worker
     * @param password the password for said WEB USER
     * @param apiVersion the HQ API version to use (described in docs)
     * @param caseId the Case ID to pull the data for
     * @throws IOException
     */
    public void queryCaseData(final String domain, final String username, final String password,
                              final String apiVersion, final String caseId) throws IOException {
        QueryCaseDataTask mTask = new QueryCaseDataTask() {
            @Override
            protected void onPostExecute(String result) {
                TextView statusText = (TextView) findViewById(R.id.caseListText);
                statusText.setText(result);
            }
        };
        mTask.execute(new String[]{domain, username, password, apiVersion, caseId});
    }

    private abstract class QueryCaseDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            final String domain = params[0];
            final String username = params[1];
            final String password = params[2];
            final String apiVersion = params[3];
            final String caseId = params[4];

            String responseAccumulator = "";

            String baseURL = "https://www.commcarehq.org/a/%s/api/%s/case/%s/?format=xml&properties=all&indices=all";
            String queryURL = String.format(baseURL, domain, apiVersion, caseId);

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(queryURL).openConnection();

                // JellyBean (4.1) doesn't call the authenticator, thanks Android
                // http://stackoverflow.com/questions/14550131/http-basic-authentication-issue-on-android-jelly-bean-4-1-using-httpurlconnectio
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    byte[] auth = (username + ":" + password).getBytes();
                    String basic = Base64.encodeToString(auth, Base64.NO_WRAP);
                    urlConnection.setRequestProperty("Authorization", "Basic " + basic);
                } else{
                    Authenticator.setDefault(new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password.toCharArray());
                        }
                    });
                }

                urlConnection.setUseCaches(false);
                urlConnection.connect();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader isw = new InputStreamReader(in);

                int data = isw.read();
                while (data != -1) {
                    char current = (char) data;
                    data = isw.read();
                    System.out.print(current);
                    responseAccumulator += current;
                }
            } catch (IOException e){
                e.printStackTrace();
            }
            return responseAccumulator;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    /**
     * This function will call the Case List API [https://www.commcarehq.org/a/ccqa/api/v0.3/case/?format=xml]
     * and display the results in this text view - can override this behavior in onPostExecute
     * @param domain your CCHQ domain name
     * @param username the username of your WEB USER with Admin privelidges, *not* your mobile worker
     * @param password the password for said WEB USEr
     * @throws IOException
     */
    public void queryCaseList(final String domain, final String username, final String password) throws IOException {
        QueryCaseListTask mTask = new QueryCaseListTask() {
            @Override
            protected void onPostExecute(String result) {
                TextView statusText = (TextView) findViewById(R.id.caseListText);
                statusText.setText(result);
            }
        };
        mTask.execute(new String[] {domain, username, password});
    }

    private abstract class QueryCaseListTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            final String domain = params[0];
            final String username = params[1];
            final String password = params[2];

            String responseAccumulator = "";

            String baseURL = "https://www.commcarehq.org/a/%s/api/v0.3/case/?format=xml";
            String queryURL = String.format(baseURL, domain);

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(queryURL).openConnection();

                // JellyBean (4.1) doesn't call the authenticator, thanks Android
                // http://stackoverflow.com/questions/14550131/http-basic-authentication-issue-on-android-jelly-bean-4-1-using-httpurlconnectio
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    byte[] auth = (username + ":" + password).getBytes();
                    String basic = Base64.encodeToString(auth, Base64.NO_WRAP);
                    urlConnection.setRequestProperty("Authorization", "Basic " + basic);
                } else{
                    Authenticator.setDefault(new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password.toCharArray());
                        }
                    });
                }

                urlConnection.setUseCaches(false);
                urlConnection.connect();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader isw = new InputStreamReader(in);

                int data = isw.read();
                while (data != -1) {
                    char current = (char) data;
                    data = isw.read();
                    System.out.print(current);
                    responseAccumulator += current;
                }
            } catch (IOException e){
                e.printStackTrace();
            }
            return responseAccumulator;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}
