package Utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class PdfTextExtractor {

    public static String extract(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            System.out.println("📄 Extracted PDF text:\n" + text); // debug
            return text.trim();
        } catch (Exception e) {
            System.err.println("❌ PDF extraction failed: " + e.getMessage());
            return "";
        }
    }
}
