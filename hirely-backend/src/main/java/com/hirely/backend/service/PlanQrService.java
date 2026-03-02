package com.hirely.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.Map;

@Service
public class PlanQrService {

    private static final SecureRandom RNG = new SecureRandom();

    public String newToken() {
        // short URL-safe token
        byte[] buf = new byte[24];
        RNG.nextBytes(buf);
        return base64Url(buf);
    }

    public byte[] qrPngBytes(String text, int sizePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos); // ZXing helper :contentReference[oaicite:1]{index=1}
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("QR generation failed: " + e.getMessage(), e);
        }
    }

    private String base64Url(byte[] b) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}