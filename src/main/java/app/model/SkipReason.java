package app.model;

public enum SkipReason {
    HIDDEN_EXCLUDED("skip.hiddenExcluded"),
    FILTERED_EXTENSION("skip.filteredExtension"),
    BINARY_FILE("skip.binaryFile"),
    DECODE_FAILURE("skip.decodeFailure"),
    PERMISSION_DENIED("skip.permissionDenied"),
    UNREADABLE_FILE("skip.unreadableFile"),
    UNEXPECTED_IO_ERROR("skip.unexpectedIo");

    private final String messageKey;

    SkipReason(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
