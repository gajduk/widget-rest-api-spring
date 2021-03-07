package uk.gajd.andrej.widgets.repository;

import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;

import java.util.List;

/**
 * This is the repository interface for {@link Widget}.
 *
 */
public interface WidgetRepository {
    Widget save(Widget widget);

    void deleteById(Long id);

    Widget findById(Long id);

    List<Widget> findWithLimit(Integer limit);

    List<Widget> findWithCoordinates(RectangleCoordinates coordinates, Integer limit);

}
