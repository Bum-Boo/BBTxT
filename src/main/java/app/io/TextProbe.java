package app.io;

import java.nio.charset.Charset;
import java.util.List;

public record TextProbe(
        boolean binary,
        String detail,
        int bomLength,
        List<Charset> candidateCharsets
) {
}
