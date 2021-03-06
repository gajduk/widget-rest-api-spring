package uk.gajd.andrej.widgets.service.impl;

import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import uk.gajd.andrej.widgets.repository.WidgetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WidgetServiceImplTest {
    private static final UUID uuid = UUID.randomUUID();
    private static final Widget DUMMY_WIDGET = Widget.builder().id(uuid).xIndex(0).yIndex(0).width(5).height(5).build();
    private static final Widget DUMMY_WIDGET_WITHOUT_ID = Widget.builder().xIndex(0).yIndex(0).width(5).height(5).build();
    private static final Integer DUMMY_VALID_LIMIT = 20;

    @Mock
    private WidgetRepository mockWidgetRepository;

    @InjectMocks
    private WidgetServiceImpl widgetService;

    @Test
    void createWidget_whenWidgetIsValid_thenReturnCreatedWidget() {
        //mock
        given(mockWidgetRepository.save(null, DUMMY_WIDGET_WITHOUT_ID)).willReturn(DUMMY_WIDGET);

        Widget createdWidget = widgetService.createWidget(DUMMY_WIDGET_WITHOUT_ID);

        verify(mockWidgetRepository).save(null, DUMMY_WIDGET_WITHOUT_ID);
        assertEquals(createdWidget, DUMMY_WIDGET);
    }

    @Test
    void updateWidget_whenWidgetIsValid_thenReturnUpdatedWidget() {
        //mock
        given(mockWidgetRepository.save(uuid.toString(), DUMMY_WIDGET)).willReturn(DUMMY_WIDGET);

        Widget updatedWidget = widgetService.updateWidget(uuid.toString(), DUMMY_WIDGET);

        verify(mockWidgetRepository).save(uuid.toString(), DUMMY_WIDGET);
        assertEquals(updatedWidget, DUMMY_WIDGET);
    }

    @Test
    void deleteWidget_whenWidgetIdProvided_thenDeleteWidget() {
        //mock
        doNothing().when(mockWidgetRepository).deleteById(uuid.toString());

        widgetService.deleteWidget(uuid.toString());

        verify(mockWidgetRepository).deleteById(uuid.toString());
    }

    @Test
    void findByWidgetId_whenWidgetIdIsProvided_thenReturnWidget() {
        //mock
        given(mockWidgetRepository.findById(uuid.toString())).willReturn(DUMMY_WIDGET);

        Widget foundWidget = widgetService.findWidgetById(uuid.toString());

        verify(mockWidgetRepository).findById(uuid.toString());
        assertEquals(foundWidget, DUMMY_WIDGET);
    }

    @Test
    void findWithLimit_whenLimitIsProvided_thenReturnWidgets() {
        //mock
        given(mockWidgetRepository.findWithLimit(DUMMY_VALID_LIMIT)).willReturn(Collections.singletonList(DUMMY_WIDGET));

        List<Widget> foundWidgets = widgetService.findWithLimit(DUMMY_VALID_LIMIT);

        verify(mockWidgetRepository).findWithLimit(DUMMY_VALID_LIMIT);
        assertEquals(foundWidgets, Collections.singletonList(DUMMY_WIDGET));
    }

    @Test
    void findWithLimit_whenCoordinatesAreProvided_thenReturnWidgets() {
        RectangleCoordinates coordinates = RectangleCoordinates.builder().x1(1).y1(2).x2(3).y2(4).build();

        //mock
        given(mockWidgetRepository.findWithCoordinates(coordinates, DUMMY_VALID_LIMIT)).willReturn(Collections.singletonList(DUMMY_WIDGET));

        List<Widget> foundWidgets = widgetService.findWithCoordinates(coordinates, DUMMY_VALID_LIMIT);

        verify(mockWidgetRepository).findWithCoordinates(coordinates, DUMMY_VALID_LIMIT);
        assertEquals(foundWidgets, Collections.singletonList(DUMMY_WIDGET));
    }
}