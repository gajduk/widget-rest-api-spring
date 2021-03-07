package uk.gajd.andrej.widgets.repository.impl;

import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InMemoryRepositoryTest {
    @InjectMocks
    private InMemoryRepository inMemoryRepository;

    @BeforeEach
    void setUp() {
        inMemoryRepository.clearMaps();
    }

    @Test
    void save_whenIdDoesNotExist_thenCreateAndReturnNewWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).width(30).height(40).build();

        Widget createdWidget = inMemoryRepository.save(widget);
        assertEquals(widget.getId(), createdWidget.getId());
        assertNotNull(createdWidget.getId());
        assertNotNull(createdWidget.getZIndex());
    }

    @Test
    void save_whenZIndexConflicts_thenShiftAndReturnWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();
        Widget existingWidget = Widget.builder().xIndex(1).yIndex(2).zIndex(5).width(3).height(4).build();

        Widget savedExistingWidget = inMemoryRepository.save(existingWidget);
        assertEquals(existingWidget.getZIndex(), savedExistingWidget.getZIndex());

        Widget savedWidget = inMemoryRepository.save(widget);
        assertEquals(widget.getZIndex(), savedWidget.getZIndex());
        assertEquals(savedWidget.getZIndex() + 1, savedExistingWidget.getZIndex());
    }

    @Test
    void save_whenIdExistsButWidgetNotFound_thenThrowWidgetNotFoundException() {
        Widget widget = Widget.builder().id(1L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        assertThrows(WidgetNotFoundException.class, () -> inMemoryRepository.save(widget));
    }

    @Test
    void save_whenIdExists_thenUpdateAndReturnWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        inMemoryRepository.save(widget);
        assertNotNull(widget.getId());

        widget.setZIndex(8);
        inMemoryRepository.save(widget);
        assertEquals(8, widget.getZIndex());
    }

    @Test
    void deleteById_whenIdDoesntExist_thenThrowWidgetNotFoundException() {
        assertThrows(WidgetNotFoundException.class, () -> inMemoryRepository.deleteById(1L));
    }

    @Test
    void deleteById_whenIdExists_thenDeleteWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();
        inMemoryRepository.save(widget);
        Long widgetId = widget.getId();
        assertNotNull(inMemoryRepository.findById(widgetId));

        inMemoryRepository.deleteById(widgetId);
        assertThrows(WidgetNotFoundException.class, () -> inMemoryRepository.findById(widgetId));
    }

    @Test
    void findById_whenIdDoesntExist_thenThrowWidgetNotFoundException() {
        assertThrows(WidgetNotFoundException.class, () -> inMemoryRepository.findById(1L));
    }

    @Test
    void findById_whenIdExists_thenReturnFoundWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).width(30).height(40).build();
        inMemoryRepository.save(widget);
        Long widgetId = widget.getId();

        assertEquals(widget, inMemoryRepository.findById(widgetId));
    }

    @Test
    void findWithLimit_whenWidgetsExist_thenReturnFoundWidgets() {
        IntStream.range(0, 8).forEach(i ->
                inMemoryRepository.save(Widget.builder().xIndex(10).yIndex(20).width(30).height(40).build())
        );

        assertEquals(8, inMemoryRepository.findWithLimit(8).size());
        assertEquals(8, inMemoryRepository.findWithLimit(20).size());
        assertEquals(3, inMemoryRepository.findWithLimit(3).size());
    }

    @Test
    void findWithCoordinates_whenWidgetsExist_thenReturnFoundWidgets() {
        Widget widget1 = Widget.builder().xIndex(50).yIndex(100).width(50).height(50).build();
        Widget widget2 = Widget.builder().xIndex(0).yIndex(0).width(30).height(40).build();
        Widget widget3 = Widget.builder().xIndex(100).yIndex(100).width(30).height(40).build();
        Widget widget4 = Widget.builder().xIndex(20).yIndex(20).width(80).height(180).build();
        List.of(widget1, widget2, widget3, widget4).forEach(widget -> inMemoryRepository.save(widget));

        RectangleCoordinates coordinates = RectangleCoordinates.builder().x0(0).y0(0).x1(100).y1(150).build();

        List<Widget> foundWidgets = inMemoryRepository.findWithCoordinates(coordinates, 10);
        assertEquals(2, foundWidgets.size());
        assertTrue(foundWidgets.contains(widget1));
        assertTrue(foundWidgets.contains(widget2));
        assertFalse(foundWidgets.contains(widget3));
        assertFalse(foundWidgets.contains(widget4));
    }
}