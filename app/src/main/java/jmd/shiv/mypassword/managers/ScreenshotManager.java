package jmd.shiv.mypassword.managers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * यह क्लास स्क्रीनशॉट इमेज के सुरक्षित संपीड़न (Compression), मेमोरी सुरक्षा (OOM रोकथाम) 
 * और BLOB बाइट्स में रूपांतरण का प्रबंधन करती है।
 */
public class ScreenshotManager {

    private static final int MAX_IMAGE_DIMENSION = 1280; // अधिकतम पिक्सेल माप (मेमोरी सुरक्षा)
    private static final int COMPRESSION_QUALITY = 80;   // 80% गुणवत्ता

    public static class ProcessedImage {
        public final byte[] imageBytes;
        public final String mimeType;

        public ProcessedImage(byte[] imageBytes, String mimeType) {
            this.imageBytes = imageBytes;
            this.mimeType = mimeType;
        }
    }

    /**
     * यूज़र द्वारा चुनी गई इमेज (Uri) को बिना मेमोरी क्रैश के संपीड़ित करके बाइट्स में बदलता है।
     *
     * @param context  एप्लीकेशन संदर्भ
     * @param imageUri चयनित इमेज का Uri
     * @return प्रोसेस की गई इमेज बाइट्स और MIME टाइप
     */
    public static ProcessedImage processAndCompressImage(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }

        ContentResolver resolver = context.getContentResolver();
        String mimeType = resolver.getType(imageUri);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        try {
            // पहला चरण: केवल इमेज का आकार (Dimensions) पढ़ना
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            try (InputStream is = resolver.openInputStream(imageUri)) {
                BitmapFactory.decodeStream(is, null, options);
            }

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            if (originalWidth <= 0 || originalHeight <= 0) {
                return null;
            }

            // सब-सैंपलिंग (inSampleSize) गणना करना ताकि OOM न हो
            int inSampleSize = 1;
            int maxDimension = Math.max(originalWidth, originalHeight);
            while (maxDimension / inSampleSize > MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2;
            }

            // दूसरा चरण: अनुकूलित आकार में इमेज डिकोड करना
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // मेमोरी कम करने के लिए

            Bitmap bitmap;
            try (InputStream is = resolver.openInputStream(imageUri)) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }

            if (bitmap == null) {
                return null;
            }

            // संपीड़न (Compression)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, baos);
            byte[] compressedBytes = baos.toByteArray();

            // मेमोरी खाली करें
            bitmap.recycle();

            return new ProcessedImage(compressedBytes, "image/jpeg");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * BLOB बाइट्स को Bitmap ऑब्जेक्ट में डिकोड करता है।
     */
    public static Bitmap decodeBlobToBitmap(byte[] blob) {
        if (blob == null || blob.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
    }
}
