package uk.gajd.andrej.widgets.model;

import lombok.Builder;
import lombok.Data;

/**
 * This is the model entity for keeping coordinates of imaginary
 * rectangle provided to find out which widgets are located in it.
 *
 */
@Data
@Builder
public class RectangleCoordinates {
    private Integer x0;
    private Integer y0;
    private Integer x1;
    private Integer y1;

    public boolean isValid() {
        return x1 > x0 && y1 > y0;
    }
}
