package uk.gajd.andrej.widgets.model;

import lombok.Builder;
import lombok.Data;

/**
 * This is the model entity for keeping coordinates of imaginary
 * rectangle provided to find out which widgets are located in it.
 *
 * @author gajduk
 */
@Data
@Builder
public class RectangleCoordinates {
    private Integer x1;
    private Integer y1;
    private Integer x2;
    private Integer y2;

    public boolean isValid() {
        return x2 > x1 && y2 > y1;
    }
}
