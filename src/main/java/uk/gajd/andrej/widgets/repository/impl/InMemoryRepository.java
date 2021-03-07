package uk.gajd.andrej.widgets.repository.impl;

import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import uk.gajd.andrej.widgets.repository.WidgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * This is the repository implementation to use in-memory ConcurrentHashMap as datasource for operations.
 * Besides the ConcurrentHashMap used to keep the widgets, there is another map(TreeMap) to keep widgetIds by their zIndex.
 * This TreeMap is basically used as an index for zIndex property.
 *
 */
@Repository
@Profile({"in-memory", "default"})
@RequiredArgsConstructor
public class InMemoryRepository implements WidgetRepository {
    private static volatile Long widgetIdCounter = 0L;

    private final Map<Long, Widget> widgetDB = new ConcurrentHashMap<>();
    private final NavigableMap<Integer, Long> zIndexDB = new ConcurrentSkipListMap<>();
    private final NavigableMap<Integer, List<Long>> xIndexDB = new ConcurrentSkipListMap<>();

    private synchronized static Long getNextWidgetId() {
        return widgetIdCounter++;
    }

    @Override
    public Widget save(Widget widget) {         // This is a create operation
        if (widget.getId() == null) {
            widget.setId(getNextWidgetId());
        } else {                                // This is an update operation
            Widget existing = widgetDB.get(widget.getId());
            if ( existing == null) {
                throw new WidgetNotFoundException("Couldn't find widget to update with id: " + widget.getId());
            }
            zIndexDB.remove(existing.getZIndex()); // Delete old z-index reference.
            xIndexDB.computeIfAbsent(existing.getXIndex(), (k) -> new CopyOnWriteArrayList<Long>())
                    .remove(widget.getId());
        }

        // This will be applicable to insert only.
        if (widget.getZIndex() == null) {
            widget.setZIndex(getMaxZIndex());
        }

        Long widgetIdAtSameZIndex = zIndexDB.get(widget.getZIndex());
        if (widgetIdAtSameZIndex != null && !widgetIdAtSameZIndex.equals(widget.getId())) {
            shift(widget);
        }
        saveWidget(widget);
        return widget;
    }

    @Override
    public void deleteById(Long id) {
        if (!widgetDB.containsKey(id)) {
            throw new WidgetNotFoundException("Couldn't find widget to delete with id: " + id);
        }

        zIndexDB.remove(widgetDB.get(id).getZIndex()); // Remove first from zIndex map
        widgetDB.remove(id); // Then, remove from widget map
    }

    @Override
    public Widget findById(Long id) {
        Widget widget = widgetDB.get(id);
        if (widget == null) {
            throw new WidgetNotFoundException("Couldn't find widget by id: " + id);
        }
        return widget;
    }

    @Override
    public List<Widget> findWithLimit(Integer limit) {
        return zIndexDB.values()
                .stream()
                .limit(limit)
                .map(widgetDB::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Widget> findWithCoordinates(RectangleCoordinates coordinates, Integer limit) {
        return  xIndexDB.subMap(coordinates.getX0(), true, coordinates.getX1(), true)
                .values()
                .stream()
                .flatMap(l -> l.stream())
                .map(widgetDB::get)
                .filter(widget -> isInRectangle(widget, coordinates))
                .sorted((w1, w2) -> w1.getZIndex()-w2.getZIndex())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void shift(Widget widget) {
        Integer endIndex = widget.getZIndex();
        for (int i = endIndex; i < zIndexDB.lastKey(); i++) {
            if (zIndexDB.containsKey(endIndex + 1)) {
                endIndex += 1;
            } else {
                break;
            }
        }

        // Move the widgets back one by one
        Integer finalEndIndex = endIndex;
        for (int i = finalEndIndex; i >= widget.getZIndex(); i--) {
            Integer newIndex = i + 1;

            Widget widgetToUpdate = widgetDB.get(zIndexDB.get(i));
            widgetToUpdate.setZIndex(newIndex);
            widgetDB.put(widgetToUpdate.getId(), widgetToUpdate);
            zIndexDB.put(newIndex, zIndexDB.get(i));
        }
    }

    private void saveWidget(Widget widget) {
        widget.setUpdateTime(LocalDateTime.now());
        widgetDB.put(widget.getId(), widget);
        zIndexDB.put(widget.getZIndex(), widget.getId());
        xIndexDB.computeIfAbsent(widget.getXIndex(), (k) -> new CopyOnWriteArrayList<Long>())
                .add(widget.getId());
    }

    private Integer getMaxZIndex() {
        return zIndexDB.isEmpty() ? 0 : zIndexDB.lastKey() + 1;
    }

    /**
     * Should only be used for testing
     */
    void _clearMaps() {
        widgetDB.clear();
        zIndexDB.clear();
        xIndexDB.clear();
    }

    private boolean isInRectangle(Widget widget, RectangleCoordinates coordinates) {
        return widget.getXIndex() >= coordinates.getX0()
                && widget.getXIndex() + widget.getWidth() <= coordinates.getX1()
                && widget.getYIndex()  >= coordinates.getY0()
                && widget.getYIndex() + widget.getHeight() <= coordinates.getY1();
    }

}
