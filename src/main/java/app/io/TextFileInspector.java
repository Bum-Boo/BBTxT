package app.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TextFileInspector {

    private static final int SAMPLE_BYTES = 8 * 1024;
    private static final Charset MS949 = Charset.forName("MS949");
    private static final Charset EUC_KR = Charset.forName("EUC-KR");
    private static final Set<String> UTF8_PRIMARY_EXTENSIONS = Set.of(
            "java", "kt", "kts", "py", "c", "cpp", "h", "hpp", "cs",
            "js", "ts", "jsx", "tsx", "json", "xml", "gradle",
            "html", "css", "sql", "yaml", "yml"
    );
    private static final Set<String> LEGACY_FALLBACK_EXTENSIONS = Set.of(
            "txt", "log", "md", "properties", "csv", "ini", "bat", "cmd"
    );

    public TextProbe inspect(Path file, String extension) throws IOException {
        byte[] sample = readSample(file);
        DecodingProfile profile = profileFor(extension);

        if (sample.length == 0) {
            return new TextProbe(false, "", 0, profile.allowLegacyFallback ? legacyCandidates() : utf8PrimaryCandidates());
        }

        BomInfo bomInfo = detectBom(sample);
        boolean looksUtf16Le = bomInfo.charset == null && looksLikeUtf16(sample, true);
        boolean looksUtf16Be = bomInfo.charset == null && looksLikeUtf16(sample, false);

        if (looksBinary(sample, profile.trustedTextExtension, looksUtf16Le || looksUtf16Be, bomInfo.length > 0)) {
            return new TextProbe(true, "Binary signature detected from the file header.", bomInfo.length, List.of());
        }

        return new TextProbe(false, "", bomInfo.length, buildCandidateList(bomInfo.charset, looksUtf16Le, looksUtf16Be, profile));
    }

    public BufferedReader openStrictReader(Path file, Charset charset) throws IOException {
        return openStrictReader(file, charset, 0);
    }

    public BufferedReader openStrictReader(Path file, Charset charset, int bomLength) throws IOException {
        InputStream stream = Files.newInputStream(file);
        skipBom(stream, bomLength);
        return new BufferedReader(new InputStreamReader(stream, newDecoder(charset)));
    }

    public String decodeStrict(Path file, Charset charset, int bomLength) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        int offset = Math.min(bomLength, bytes.length);
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, bytes.length - offset);
        return newDecoder(charset).decode(buffer).toString();
    }

    private CharsetDecoder newDecoder(Charset charset) {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
    }

    private byte[] readSample(Path file) throws IOException {
        int size = (int) Math.min(Files.size(file), SAMPLE_BYTES);
        if (size <= 0) {
            return new byte[0];
        }

        byte[] sample = new byte[size];
        try (InputStream input = Files.newInputStream(file)) {
            int read = input.read(sample);
            if (read <= 0) {
                return new byte[0];
            }
            if (read == size) {
                return sample;
            }

            byte[] trimmed = new byte[read];
            System.arraycopy(sample, 0, trimmed, 0, read);
            return trimmed;
        }
    }

    private void skipBom(InputStream stream, int bomLength) throws IOException {
        int remaining = Math.max(0, bomLength);
        while (remaining > 0) {
            long skipped = stream.skip(remaining);
            if (skipped <= 0) {
                if (stream.read() < 0) {
                    break;
                }
                skipped = 1;
            }
            remaining -= (int) skipped;
        }
    }

    private List<Charset> legacyCandidates() {
        return List.of(StandardCharsets.UTF_8, MS949, EUC_KR, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE);
    }

    private List<Charset> utf8PrimaryCandidates() {
        return List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE);
    }

    private List<Charset> buildCandidateList(
            Charset bomCharset,
            boolean looksUtf16Le,
            boolean looksUtf16Be,
            DecodingProfile profile
    ) {
        Set<Charset> ordered = new LinkedHashSet<>();

        if (bomCharset != null) {
            ordered.add(bomCharset);
        }
        if (looksUtf16Le) {
            ordered.add(StandardCharsets.UTF_16LE);
        }
        if (looksUtf16Be) {
            ordered.add(StandardCharsets.UTF_16BE);
        }

        ordered.add(StandardCharsets.UTF_8);

        if (profile.allowLegacyFallback) {
            ordered.add(MS949);
            ordered.add(EUC_KR);
        }

        ordered.add(StandardCharsets.UTF_16LE);
        ordered.add(StandardCharsets.UTF_16BE);
        return new ArrayList<>(ordered);
    }

    private boolean looksBinary(byte[] sample, boolean trustedTextExtension, boolean looksUtf16, boolean hasBom) {
        if (hasBom || looksUtf16) {
            return false;
        }

        int nullCount = 0;
        int controlCount = 0;
        for (byte value : sample) {
            int unsigned = value & 0xFF;
            if (unsigned == 0) {
                nullCount++;
                continue;
            }
            if (unsigned < 0x09 || (unsigned > 0x0D && unsigned < 0x20)) {
                controlCount++;
            }
        }

        if (nullCount > 0) {
            return true;
        }

        if (trustedTextExtension) {
            return false;
        }

        return controlCount > Math.max(6, sample.length / 12);
    }

    private boolean looksLikeUtf16(byte[] sample, boolean littleEndian) {
        if (sample.length < 4) {
            return false;
        }

        int zeroCount = 0;
        int inspectedPairs = 0;
        for (int index = littleEndian ? 1 : 0; index < sample.length; index += 2) {
            inspectedPairs++;
            if (sample[index] == 0) {
                zeroCount++;
            }
        }

        return inspectedPairs > 0 && zeroCount >= inspectedPairs * 0.6;
    }

    private BomInfo detectBom(byte[] sample) {
        if (sample.length >= 3
                && (sample[0] & 0xFF) == 0xEF
                && (sample[1] & 0xFF) == 0xBB
                && (sample[2] & 0xFF) == 0xBF) {
            return new BomInfo(StandardCharsets.UTF_8, 3);
        }
        if (sample.length >= 2
                && (sample[0] & 0xFF) == 0xFF
                && (sample[1] & 0xFF) == 0xFE) {
            return new BomInfo(StandardCharsets.UTF_16LE, 2);
        }
        if (sample.length >= 2
                && (sample[0] & 0xFF) == 0xFE
                && (sample[1] & 0xFF) == 0xFF) {
            return new BomInfo(StandardCharsets.UTF_16BE, 2);
        }
        return new BomInfo(null, 0);
    }

    private DecodingProfile profileFor(String extension) {
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (UTF8_PRIMARY_EXTENSIONS.contains(normalizedExtension)) {
            return new DecodingProfile(true, false);
        }
        if (LEGACY_FALLBACK_EXTENSIONS.contains(normalizedExtension)) {
            return new DecodingProfile(true, true);
        }
        return new DecodingProfile(false, true);
    }

    private record BomInfo(Charset charset, int length) {
    }

    private record DecodingProfile(boolean trustedTextExtension, boolean allowLegacyFallback) {
    }
}
