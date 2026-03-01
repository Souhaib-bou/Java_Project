package Services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ResumeParserService {


    private static final String API_KEY = System.getenv("GROQ_API_KEY");
    public static Map<String, String> parse(String resumeText) {
        Map<String, String> result = new HashMap<>();

        try {
            // ✅ Properly escape the resume text for JSON
            String safeText = resumeText
                    .replace("\\", "\\\\")   // backslashes first
                    .replace("\"", "\\\"")   // double quotes
                    .replace("\r\n", "\\n")  // windows newlines
                    .replace("\n", "\\n")    // unix newlines
                    .replace("\r", "\\n")    // old mac newlines
                    .replace("\t", " ");     // tabs to spaces

            String prompt = "Extract the following fields from this document and return ONLY a valid JSON object "
                    + "with no extra text, no markdown, no backticks.\\n"
                    + "Fields to extract:\\n"
                    + "- email (string)\\n"
                    + "- phone (digits only, remove +216 prefix, spaces and dashes)\\n"
                    + "- experienceYears (integer only, just the number)\\n"
                    + "- expectedSalary (number only, no currency text)\\n"
                    + "- portfolioUrl (string, empty string if not found)\\n"
                    + "- coverLetter (string, the cover letter text)\\n"
                    + "- applicationDate (string, format YYYY-MM-DD, look for Application Date field)\\n"
                    + "- availabilityDate (string, format YYYY-MM-DD, look for Availability Date field)\\n"
                    + "\\nDocument:\\n" + safeText;

            // ✅ Build JSON body manually with proper structure
            String jsonBody = "{"
                    + "\"model\":\"llama-3.3-70b-versatile\","  // Groq's free model
                    + "\"temperature\":0.1,"
                    + "\"messages\":["
                    + "  {\"role\":\"system\",\"content\":\"You are a resume parser. Always respond with valid JSON only.\"},"
                    + "  {\"role\":\"user\",\"content\":\"" + prompt + "\"}"
                    + "]}";

            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            // ✅ Read response or error stream
            int status = conn.getResponseCode();
            InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            System.out.println("🔁 OpenAI raw response: " + response);

            if (status != 200) {
                System.err.println("❌ OpenAI error " + status + ": " + response);
                return result;
            }

            // ✅ Extract content from OpenAI response
            String raw = response.toString();
            String content = extractJsonField(raw, "content");
            content = content
                    .replace("\\n", "")
                    .replace("\\\"", "\"")
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            System.out.println("📦 Parsed content: " + content);

            result.put("email",            extractJsonField(content, "email"));
            result.put("phone",            extractJsonField(content, "phone"));
            result.put("experienceYears",  extractJsonField(content, "experienceYears"));
            result.put("expectedSalary",   extractJsonField(content, "expectedSalary"));
            result.put("portfolioUrl",     extractJsonField(content, "portfolioUrl"));
            result.put("coverLetter",      extractJsonField(content, "coverLetter"));
            result.put("applicationDate",  extractJsonField(content, "applicationDate")); // ✅ new
            result.put("availabilityDate", extractJsonField(content, "availabilityDate")); // ✅ new

            System.out.println("✅ Extracted fields: " + result);

        } catch (Exception e) {
            System.err.println("❌ Resume parse failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private static String extractJsonField(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int keyIdx = json.indexOf(search);
            if (keyIdx == -1) return "";

            int colon = json.indexOf(":", keyIdx);
            if (colon == -1) return "";

            int valueStart = colon + 1;
            while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;

            if (valueStart >= json.length()) return "";

            if (json.charAt(valueStart) == '"') {
                // String value — find closing quote (skip escaped quotes)
                int end = valueStart + 1;
                while (end < json.length()) {
                    if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                    end++;
                }
                return json.substring(valueStart + 1, end).trim();
            } else {
                // Number or boolean
                int end = valueStart;
                while (end < json.length() && ",}\n".indexOf(json.charAt(end)) == -1) end++;
                return json.substring(valueStart, end).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }
}
