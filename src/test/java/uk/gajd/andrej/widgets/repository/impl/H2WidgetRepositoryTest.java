package uk.gajd.andrej.widgets.repository.impl;

import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static uk.gajd.andrej.widgets.repository.impl.H2WidgetRepository.QUERY_DELETE_WIDGET;
import static uk.gajd.andrej.widgets.repository.impl.H2WidgetRepository.QUERY_SELECT_MAX_Z_INDEX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class H2WidgetRepositoryTest {
    @Mock
    private JdbcTemplate mockJdbcTemplate;

    @InjectMocks
    private H2WidgetRepository h2WidgetRepository;

    @Test
    void save_whenIdDoesNotExist_thenCreateAndReturnNewWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).width(30).height(40).build();

        // mock
        mockWidgetId(3L);
        given(mockJdbcTemplate.queryForObject(QUERY_SELECT_MAX_Z_INDEX, Integer.class)).willReturn(null);

        Widget createdWidget = h2WidgetRepository.save(widget);
        assertEquals(3L, createdWidget.getId());
    }

    @Test
    void save_whenZIndexConflicts_thenShiftAndReturnWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();
        Widget existingWidget = Widget.builder().xIndex(1).yIndex(2).zIndex(5).width(3).height(4).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyInt())).willReturn(existingWidget, existingWidget, null);

        Widget createdWidget = h2WidgetRepository.save(widget);
        assertEquals(widget.getZIndex(), createdWidget.getZIndex());
    }

    @Test
    void save_whenIdExistsButNoWidgetFound_thenThrowWidgetNotFoundException() {
        Widget widget = Widget.builder().id(1L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyInt())).willReturn(null);
        given(mockJdbcTemplate.update(anyString(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(LocalDateTime.class), anyLong())).willReturn(0);

        assertThrows(WidgetNotFoundException.class, () -> h2WidgetRepository.save(widget));
    }

    @Test
    void save_whenIdExistsAndWidgetFound_thenUpdateAndReturnUpdatedWidget() {
        Widget widget = Widget.builder().id(1L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyInt())).willReturn(null);
        given(mockJdbcTemplate.update(anyString(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(LocalDateTime.class), anyLong())).willReturn(1);

        Widget updatedWidget = h2WidgetRepository.save(widget);
        assertEquals(widget.getId(), updatedWidget.getId());
        assertNotNull(updatedWidget.getUpdateTime());
    }

    @Test
    void deleteById_whenIdDoesntExist_thenThrowWidgetNotFoundException() {
        Long widgetId = 5L;

        // mock
        given(mockJdbcTemplate.update(QUERY_DELETE_WIDGET, widgetId)).willReturn(0);

        assertThrows(WidgetNotFoundException.class, () -> h2WidgetRepository.deleteById(widgetId));
    }

    @Test
    void deleteById_whenIdExists_thenDeleteWidget() {
        Long widgetId = 5L;

        // mock
        given(mockJdbcTemplate.update(QUERY_DELETE_WIDGET, widgetId)).willReturn(1);

        h2WidgetRepository.deleteById(widgetId);

        verify(mockJdbcTemplate).update(QUERY_DELETE_WIDGET, widgetId);
    }

    @Test
    void findById_whenIdDoesntExist_thenThrowWidgetNotFoundException() {
        Long widgetId = 5L;

        // mock
        doThrow(EmptyResultDataAccessException.class).when(mockJdbcTemplate).queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyLong());

        assertThrows(WidgetNotFoundException.class, () -> h2WidgetRepository.findById(widgetId));
    }

    @Test
    void findById_whenIdExists_thenReturnFoundWidget() {
        Widget widget = Widget.builder().id(1L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyLong())).willReturn(widget);

        assertEquals(widget, h2WidgetRepository.findById(widget.getId()));
    }

    @Test
    void findWithLimit_whenWidgetsExist_thenReturnFoundWidgets() {
        List<Widget> widgets = List.of(
                Widget.builder().id(1L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build(),
                Widget.builder().id(2L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build()
        );

        // mock
        given(mockJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).willReturn(widgets);

        assertEquals(widgets, h2WidgetRepository.findWithLimit(10));
    }

    @Test
    void findWithCoordinates_whenWidgetsExist_thenReturnFoundWidgets() {
        RectangleCoordinates coordinates = RectangleCoordinates.builder().x0(1).y0(2).x1(3).y1(4).build();
        List<Widget> widgets = List.of(
                Widget.builder().id(1L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build(),
                Widget.builder().id(2L).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build()
        );

        // mock
        given(mockJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).willReturn(widgets);

        assertEquals(widgets, h2WidgetRepository.findWithCoordinates(coordinates, 10));
    }

    private void mockWidgetId(Long id) {
        Mockito.when(mockJdbcTemplate.update(Mockito.any(PreparedStatementCreator.class),
                Mockito.any(GeneratedKeyHolder.class))).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            ((GeneratedKeyHolder) args[1]).getKeyList().add(Map.of("", id));
            return 1;
        }).thenReturn(1);
    }
}