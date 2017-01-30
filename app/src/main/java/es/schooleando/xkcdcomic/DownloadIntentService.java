package es.schooleando.xkcdcomic;

import android.app.IntentService;
import android.content.ClipData;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Scanner;

public class DownloadIntentService extends IntentService {
    private static final String TAG = DownloadIntentService.class.getSimpleName();
    private ResultReceiver mReceiver;

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    public static final int ERROR = 0;
    public static final int PROGRESS = 1;
    public static final int OK = 2;


    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra("receiver");
        Log.d(TAG, "onHandleIntent");

        // TODO Aquí hacemos la conexión y accedemos a la imagen.


        // TODO: Habrá que hacer 2 conexiones:
        //  1. Para descargar el resultado JSON para leer la URL.
        //  2. Una vez tenemos la URL descargar la imagen en la carpeta temporal.


        String url = intent.getStringExtra("url");
        Bundle bundle = new Bundle();

        File downloadFile = null;
        try {
            StringBuilder result = new StringBuilder();
            URL downloadURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) downloadURL.openConnection();
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("Error en la conexión");
            }

            InputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            conn.disconnect();
            JSONObject json = new JSONObject(result.toString());
            String imageUrl = json.getString("img");

            URL comicUrl = new URL(imageUrl);
            conn = (HttpURLConnection) comicUrl.openConnection();
            InputStream is = conn.getInputStream();
            downloadFile = File.createTempFile("imagen", ".jpg", new File("/sdcard"));
            FileOutputStream os = new FileOutputStream(downloadFile);
            downloadFile.deleteOnExit();
            byte buffer[] = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteCount);
            }
            os.close();
            is.close();

            int lenght = tryGetFileSize(comicUrl.toString());
            for (int i = 0; i < lenght; i++) {
                int progreso = ((int) ((i / (float) lenght) * 100));

                // TODO: Devolver la URI de la imagen si todo ha ido bien.
                bundle.putInt("progreso", progreso);
                mReceiver.send(PROGRESS, bundle);
            }


            String filePath = downloadFile.getPath();
            bundle.putString("filePath", filePath);
            mReceiver.send(OK, bundle);

            // TODO: Controlar los casos en los que no ha ido bien: excepciones en las conexiones, etc...

        } catch (MalformedURLException e) {
            bundle.putString("error", "Malformed URL");
            mReceiver.send(ERROR, bundle);
        } catch (SocketTimeoutException e) {
            bundle.putString("error", "Exception TimeOut");
            mReceiver.send(ERROR, bundle);
        } catch (IOException e) {
            bundle.putString("error", "Error I/O");
            mReceiver.send(ERROR, bundle);
        } catch (JSONException e) {
            bundle.putString("error", "Exception JSON");
            mReceiver.send(ERROR, bundle);
        } catch (Exception e) {
            bundle.putString("error", "Exception: " + e.getMessage());
            mReceiver.send(ERROR, bundle);
        }

     //   mReceiver.send(0, Bundle.EMPTY);  // cambiar

    }

    private int tryGetFileSize(String _url) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(_url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }

}