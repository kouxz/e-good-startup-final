package com.projeto.egoodapp.data.local;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Imports an untrusted gallery document as a bounded, metadata-free app-owned JPEG. */
final class VehiclePhotoImporter {
    static final long MAX_SOURCE_BYTES = 10L * 1024L * 1024L;
    static final long MAX_SOURCE_PIXELS = 20_000_000L;
    static final int MAX_OUTPUT_EDGE = 2048;
    private static final int JPEG_QUALITY = 85;
    private static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"));

    private VehiclePhotoImporter() {}

    static String importPhoto(Context context, Uri source, File destinationDirectory)
            throws IOException, SecurityException {
        if (source == null) throw new IOException("Selecione uma foto válida.");
        if (!destinationDirectory.isDirectory() && !destinationDirectory.mkdirs()) {
            throw new IOException("Não foi possível preparar a foto.");
        }

        File raw = File.createTempFile("vehicle-photo-source-", ".tmp", destinationDirectory);
        File destination = new File(destinationDirectory, UUID.randomUUID() + ".jpg");
        Bitmap decoded = null;
        Bitmap oriented = null;
        Bitmap flattened = null;
        try {
            copyBounded(context, source, raw);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(raw.getAbsolutePath(), bounds);
            validateBounds(bounds);

            BitmapFactory.Options decode = new BitmapFactory.Options();
            decode.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
            decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
            decoded = BitmapFactory.decodeFile(raw.getAbsolutePath(), decode);
            if (decoded == null) throw new IOException("O arquivo selecionado não é uma imagem válida.");

            oriented = applyExifOrientation(raw, decoded);
            flattened = flattenTransparency(oriented);
            try (FileOutputStream output = new FileOutputStream(destination)) {
                if (!flattened.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    throw new IOException("Não foi possível converter a foto.");
                }
            }
            if (!destination.isFile() || destination.length() == 0L) {
                throw new IOException("Não foi possível salvar a foto.");
            }
            return Uri.fromFile(destination).toString();
        } catch (IOException | RuntimeException error) {
            destination.delete();
            if (error instanceof IOException) throw (IOException) error;
            if (error instanceof SecurityException) throw (SecurityException) error;
            throw new IOException("Não foi possível processar a foto selecionada.", error);
        } finally {
            raw.delete();
            recycleDistinct(flattened, oriented, decoded);
        }
    }

    private static void copyBounded(Context context, Uri source, File raw)
            throws IOException, SecurityException {
        try (InputStream input = context.getContentResolver().openInputStream(source);
                FileOutputStream output = new FileOutputStream(raw)) {
            if (input == null) throw new IOException("Foto indisponível.");
            byte[] buffer = new byte[8192];
            long total = 0L;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_SOURCE_BYTES) {
                    throw new IOException("A foto deve ter no máximo 10 MB.");
                }
                output.write(buffer, 0, read);
            }
            if (total == 0L) throw new IOException("A foto selecionada está vazia.");
        }
    }

    private static void validateBounds(BitmapFactory.Options bounds) throws IOException {
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || bounds.outMimeType == null || !SUPPORTED_MIME_TYPES.contains(bounds.outMimeType)) {
            throw new IOException("Use uma imagem JPEG, PNG, WebP, HEIC ou HEIF.");
        }
        long pixels = (long) bounds.outWidth * (long) bounds.outHeight;
        if (pixels > MAX_SOURCE_PIXELS) {
            throw new IOException("A foto deve ter no máximo 20 megapixels.");
        }
    }

    static int sampleSize(int width, int height) {
        int sample = 1;
        int longest = Math.max(width, height);
        while ((longest + sample - 1) / sample > MAX_OUTPUT_EDGE) sample *= 2;
        return sample;
    }

    private static Bitmap applyExifOrientation(File source, Bitmap bitmap) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            orientation = new ExifInterface(source.getAbsolutePath()).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException ignored) {
            // Formats without EXIF are already displayed in their encoded orientation.
        }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90f);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap flattenTransparency(Bitmap bitmap) {
        if (!bitmap.hasAlpha()) return bitmap;
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0f, 0f, null);
        return result;
    }

    private static void recycleDistinct(Bitmap first, Bitmap second, Bitmap third) {
        if (first != null && first != second && first != third && !first.isRecycled()) first.recycle();
        if (second != null && second != third && !second.isRecycled()) second.recycle();
        if (third != null && !third.isRecycled()) third.recycle();
    }
}
