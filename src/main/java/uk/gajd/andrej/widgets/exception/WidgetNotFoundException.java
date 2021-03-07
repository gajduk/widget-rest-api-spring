package uk.gajd.andrej.widgets.exception;

/**
 * This is a custom runtime exception which is thrown when requested widget can't be find.
 *
 */
public class WidgetNotFoundException extends RuntimeException {
    public WidgetNotFoundException(String message) {
        super(message);
    }
}
