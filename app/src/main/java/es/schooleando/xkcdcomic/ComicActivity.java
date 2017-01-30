package es.schooleando.xkcdcomic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.Random;

public class ComicActivity extends AppCompatActivity implements BgResultReceiver.Receiver {
    private BgResultReceiver mResultReceiver;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);

        mResultReceiver = new BgResultReceiver(new Handler());
        mResultReceiver.setReceiver(this);

        // Esto es gratis: al arrancar debemos cargar el cómic actual
        intent = new Intent(this, DownloadIntentService.class);
    //    intent.putExtra("url", "http://xkcd.com/info.0.json");
        intent.putExtra("url", "https://xkcd.com/info.0.json");  // sino es HTTPS no funciona
        intent.putExtra("receiver", mResultReceiver);
        startService(intent);
    }


    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // TODO: podemos recibir diferentes resultCodes del IntentService
        //      ERROR -> ha habido un problema de la conexión (Toast)
        //      PROGRESS -> nos estamos descargando la imagen (ProgressBar)
        //      OK -> nos hemos descargado la imagen correctamente. (ImageView)
        // Debeis controlar cada caso

        final ImageView imagen = (ImageView) findViewById(R.id.imageView);
        final ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);

        switch (resultCode) {
            case DownloadIntentService.ERROR:
                String error = resultData.getString("error");

                if(error!=null) {
                    Toast.makeText(ComicActivity.this, error, Toast.LENGTH_SHORT).show();
                }

                break;

            case DownloadIntentService.PROGRESS:
                int prog = resultData.getInt("progreso");
                bar.setVisibility(View.VISIBLE);
                bar.setMax(100);
                bar.setProgress(prog);
                break;


           case DownloadIntentService.OK:
                String filePath = resultData.getString("filePath");
                Bitmap bmp = BitmapFactory.decodeFile(filePath);
                if (imagen != null && bmp != null) {
                    imagen.setImageBitmap(bmp);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "error in decoding downloaded file",
                            Toast.LENGTH_SHORT).show();
                }
                bar.setIndeterminate(false);
                bar.setVisibility(View.INVISIBLE);
                break;


        }

        // TODO: Falta un callback de ImageView para hacer click en la imagen y que se descargue otro comic aleatorio.

        imagen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Random random = new Random();
                int intRandom = random.nextInt(1000);
                String urlRandom = "https://xkcd.com/"+intRandom+"/info.0.json";
                intent.putExtra("url", urlRandom);
                startService(intent);
            }
        });
    }
}