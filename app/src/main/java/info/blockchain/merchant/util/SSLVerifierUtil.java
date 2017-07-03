package info.blockchain.merchant.util;

import android.content.Context;
import android.widget.Toast;
//import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import org.thoughtcrime.ssl.pinning.util.PinningHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import info.blockchain.wallet.util.WebUtil;

// openssl s_client -showcerts -connect blockchain.info:443

public class SSLVerifierUtil {

    private static SSLVerifierUtil instance = null;
    private static Context context = null;

    private SSLVerifierUtil() { ; }

    public static SSLVerifierUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new SSLVerifierUtil();
        }

        return instance;
    }

    public boolean isValidHostname() {

        int responseCode = -1;
        boolean ret = false;

        DefaultHttpClient client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        HostnameVerifier verifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
        socketFactory.setHostnameVerifier((X509HostnameVerifier)verifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
        HttpPost httpPost = new HttpPost(WebUtil.VALIDATE_SSL_URL);
        try {

            HttpResponse response = httpClient.execute(httpPost);
            responseCode = response.getStatusLine().getStatusCode();

            return true;
        }
        catch(Exception e) {

            e.printStackTrace();

            return false;
        }

    }

    public boolean certificatePinned()   {

        try {

            // DER encoded public key:
            // 30820122300d06092a864886f70d01010105000382010f003082010a0282010100bff56f562096307165320b0f04ff30e3f7d7e7a2813a35c16bfbe549c23f2a5d0388818fc0f9326a9679322fd7a6d4a1f2c4d45129c8641f6a3e7d9175938f050352a1cf09440399a36a358a846e4b5ef43baafbcb6af9f3615a7a49aae497cfeaaeb943e0175bab546abacc60b29c9bb7f588c62ac81e21038e760f044c07fe6d8a1cba4f8b5e9835bb8eddec79d506dc47fd73030630bf1af7bd70352ced281efae1675e70a6918d98645ebc389d2169ff72a82c7ff7a6328f0cd337197d87e208d2bc8cdd21182157fcb12a6db697dbd62b76800debef8feea2da2a5e074feea56af52f4300c17892018f7584eb5d4946c10156a85746ae8eacc5ebe112df0203010001
            String[] pins                 = new String[] { "10902ad9c6fb7d84c133b8682a7e7e30a5b6fb90" };    // SHA-1 hash of DER encoded public key byte array
            URL url                       = new URL("https://blockchain.info/");
            HttpsURLConnection connection = PinningHelper.getPinnedHttpsURLConnection(context, pins, url);
            byte[] data = new byte[4096];
            connection.getInputStream().read(data);
//            Log.i("SSLVerifierUtil", "Certificate pinning success");

            return true;
        }
        catch(MalformedURLException mue)  {
            Toast.makeText(context, "Certificate pinning failed: " + mue.getMessage().toString(), Toast.LENGTH_LONG).show();
            return false;
        }
        catch(IOException ioe)  {
            Toast.makeText(context, "Certificate pinning failed: " + ioe.getMessage().toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

}
