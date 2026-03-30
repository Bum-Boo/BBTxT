package app.model;

public enum SkippedPathKind {
    FILE("kind.file"),
    DIRECTORY("kind.directory");

    private final String messageKey;

    SkippedPathKind(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
