package it.nexxa.base64ToGallery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Objects;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

/**
 * Base64ToGallery.java
 *
 * Android implementation of the Base64ToGallery for iOS.
 * Inspirated by Joseph's "Save HTML5 Canvas Image to Gallery" plugin
 * http://jbkflex.wordpress.com/2013/06/19/save-html5-canvas-image-to-gallery-phonegap-android-plugin/
 *
 * @author Vegard Løkken <vegard@headspin.no>
 */
public class Base64ToGallery extends CordovaPlugin {

  // Consts
  public static final String EMPTY_STR = "";

  @Override
  public boolean execute(String action, JSONArray args,
      CallbackContext callbackContext) throws JSONException {

    String base64               = args.optString(0);
    String filePrefix           = args.optString(1);
    boolean mediaScannerEnabled = args.optBoolean(2);

    // isEmpty() requires API level 9
    if (base64.equals(EMPTY_STR)) {
      callbackContext.error("Missing base64 string");
    }

    // Create the bitmap from the base64 string
    byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
    Bitmap bmp           = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

    if (bmp == null) {
      callbackContext.error("The image could not be decoded");

    } else {

      // Save the image
      String imageFile = savePhoto(bmp, filePrefix);

      if (imageFile == null) {
        callbackContext.error("Error while saving image");
      }

      // Update image gallery
      if (mediaScannerEnabled) {
        scanPhoto(imageFile);
      }

      callbackContext.success(imageFile);
    }

    return true;
  }

  private String savePhoto(Bitmap bmp, String prefix) {
    String retVal = null;

    try {
      Calendar c           = Calendar.getInstance();
      String date          = EMPTY_STR
                              + c.get(Calendar.YEAR)
                              + c.get(Calendar.MONTH)
                              + c.get(Calendar.DAY_OF_MONTH)
                              + c.get(Calendar.HOUR_OF_DAY)
                              + c.get(Calendar.MINUTE)
                              + c.get(Calendar.SECOND);
      String name = prefix + date + ".png";
      OutputStream fos;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContentResolver resolver = cordova.getContext().getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
        retVal = imageUri.toString();
      } else {
        String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File image = new File(imagesDir, name + ".jpg");
        fos = new FileOutputStream(image);
        retVal = image.getAbsolutePath();
      }
      bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
      Objects.requireNonNull(fos).close();
    } catch (Exception e) {
      Log.e("Base64ToGallery", "An exception occured while saving image: " + e.toString());
    }

    return retVal;
  }

  /**
   * Invoke the system's media scanner to add your photo to the Media Provider's database,
   * making it available in the Android Gallery application and to other apps.
   */
  private void scanPhoto(String imageUri) {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    Uri contentUri         = Uri.parse(imageUri);

    mediaScanIntent.setData(contentUri);

    cordova.getActivity().sendBroadcast(mediaScanIntent);
  }
}
