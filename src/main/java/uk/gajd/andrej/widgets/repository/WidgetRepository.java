package uk.gajd.andrej.widgets.repository;

import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;

import java.util.List;

/**
 * This is the repository interface for {@link Widget}.
 *
 * @author gajduk
 */
public interface WidgetRepository {
    Widget save(String id, Widget widget);

    void deleteById(String id);

    Widget findById(String id);

    List<Widget> findWithLimit(Integer limit);

    List<Widget> findWithCoordinates(RectangleCoordinates coordinates, Integer limit);

    default boolean isInRectangle(Widget widget, RectangleCoordinates coordinates) {
        return widget.getXIndex() >= coordinates.getX1()
                && widget.getXIndex() + widget.getWidth() <= coordinates.getX2()
                && widget.getYIndex() >= coordinates.getY1()
                && widget.getYIndex() + widget.getHeight() <= coordinates.getY2();
    }
}
