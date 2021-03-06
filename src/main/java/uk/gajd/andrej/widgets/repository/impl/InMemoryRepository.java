package uk.gajd.andrej.widgets.repository.impl;

import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import uk.gajd.andrej.widgets.repository.WidgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * This is the repository implementation to use in-memory ConcurrentHashMap as datasource for operations.
 * Besides the ConcurrentHashMap used to keep the widgets, there are two additional ConcurrentSkipListMap maps:
 *  - zIndexDB - for quickly inserting and retrieving based on zIndex
 *  - xIndexDB - for quickly retrieving based on xIndex
 *
 * @author gajduk
 */
@Repository
@Profile({"in-memory", "default"})
@RequiredArgsConstructor
public class InMemoryRepository implements WidgetRepository {

    private final Map<UUID, Widget> widgetDB = new ConcurrentHashMap<>();
    private final NavigableMap<Integer, UUID> zIndexDB = new ConcurrentSkipListMap<>();
    private final NavigableMap<Integer, List<UUID>> xIndexDB = new ConcurrentSkipListMap<>();

    @Override
    public Widget save(String uuid, Widget widget) {         // This is a create operation
        if (uuid == null) {
            widget.setId(UUID.randomUUID());
        } else {                                // This is an update operation
            Widget existing = widgetDB.get(widget.getId());
            if ( existing == null) {
                throw new WidgetNotFoundException("Couldn't find widget to update with id: " + widget.getId());
            }
            zIndexDB.remove(existing.getZIndex()); // Delete old z-index reference.
            xIndexDB.computeIfAbsent(
                    existing.getXIndex() ,
                    ( x -> new CopyOnWriteArrayList<>() )
            )
            .remove(existing);
        }

        // This will be applicable to insert only.
        if (widget.getZIndex() == null) {
            widget.setZIndex(getMaxZIndex());
        }

        UUID widgetIdAtSameZIndex = zIndexDB.get(widget.getZIndex());
        if (widgetIdAtSameZIndex != null && !widgetIdAtSameZIndex.equals(widget.getId())) {
            shift(widget);
        }
        saveWidget(widget);
        return widget;
    }

    @Override
    public void deleteById(String id) {
        if (!widgetDB.containsKey(id)) {
            throw new WidgetNotFoundException("Couldn't find widget to delete with id: " + id);
        }

        zIndexDB.remove(widgetDB.get(id).getZIndex()); // Remove first from zIndex map
        widgetDB.remove(id); // Then, remove from widget map
    }

    @Override
    public Widget findById(String id) {
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
        return xIndexDB.subMap(coordinates.getX1(), true, coordinates.getX2(), true)
                .values()
                .stream()
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
        xIndexDB.computeIfAbsent(
                widget.getXIndex() ,
                ( x -> new CopyOnWriteArrayList<>() )
        )
        .add(widget.getId());
    }

    private Integer getMaxZIndex() {
        return zIndexDB.isEmpty() ? 0 : zIndexDB.lastKey() + 1;
    }

    void clearMaps() {
        widgetDB.clear();
        zIndexDB.clear();
    }
}
