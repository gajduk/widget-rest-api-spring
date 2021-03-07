package uk.gajd.andrej.widgets.service.impl;

import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import uk.gajd.andrej.widgets.repository.WidgetRepository;
import uk.gajd.andrej.widgets.repository.impl.H2WidgetRepository;
import uk.gajd.andrej.widgets.repository.impl.InMemoryRepository;
import uk.gajd.andrej.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * This is service implementation of widget.
 * Since we have multiple repository implementations and they have their specific operations required,
 * most of the logic resides in repository implementations {@link InMemoryRepository} & {@link H2WidgetRepository}
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WidgetServiceImpl implements WidgetService {
    private final WidgetRepository widgetRepository;

    @Override
    public Widget createWidget(Widget widget) {
        Widget createdWidget = widgetRepository.save(widget);
        log.info("Created widget with id: {}", createdWidget.getId());
        return createdWidget;
    }

    @Override
    public Widget updateWidget(Widget widget) {
        Widget updatedWidget = widgetRepository.save(widget);
        log.info("Updated widget with id: {}", updatedWidget.getId());
        return updatedWidget;
    }

    @Override
    public void deleteWidget(Long id) {
        widgetRepository.deleteById(id);
        log.info("Deleted widget with id: {}", id);
    }

    @Override
    public Widget findWidgetById(Long id) {
        Widget foundWidget = widgetRepository.findById(id);
        log.info("Found widget by id: {}", id);
        return foundWidget;
    }

    @Override
    public List<Widget> findWithLimit(Integer limit) {
        List<Widget> widgetsWithLimit = widgetRepository.findWithLimit(limit);
        log.info("Found widgets with limit: {}", limit);
        return widgetsWithLimit;
    }

    @Override
    public List<Widget> findWithCoordinates(RectangleCoordinates coordinates, Integer limit) {
        List<Widget> widgetInCoordinates = widgetRepository.findWithCoordinates(coordinates, limit);
        log.info("Found widgets with coordinates: {}, and limit: {}", coordinates, limit);
        return widgetInCoordinates;
    }
}
