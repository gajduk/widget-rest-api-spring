package uk.gajd.andrej.widgets.model;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * This is the request model for creating and updating widget,
 * All other fields besides zIndex need to be provided, otherwise the widget wouldn't be created.
 * Id will be auto-generated in DB.
 *
 */
@Data
@Builder
public class WidgetRequest {
    private Long id;

    @NotNull(message = "xIndex should be provided.")
    private Integer xIndex;

    @NotNull(message = "yIndex should be provided.")
    private Integer yIndex;

    private Integer zIndex;

    @NotNull(message = "width should be provided.")
    @Positive(message = "width should be positive.")
    private Integer width;

    @NotNull(message = "height should be provided.")
    @Positive(message = "height should be positive.")
    private Integer height;

    public Widget toWidget(Long id) {
        return Widget.builder()
                .id(id)
                .xIndex(xIndex)
                .yIndex(yIndex)
                .zIndex(zIndex)
                .width(width)
                .height(height)
                .build();
    }
}
