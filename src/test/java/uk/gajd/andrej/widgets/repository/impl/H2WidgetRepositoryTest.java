package uk.gajd.andrej.widgets.repository.impl;

import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import org.junit.jupiter.api.Assertions;
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
import java.util.UUID;

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
    void save_whenZIndexConflicts_thenShiftAndReturnWidget() {
        Widget widget = Widget.builder().xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();
        Widget existingWidget = Widget.builder().xIndex(1).yIndex(2).zIndex(5).width(3).height(4).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyInt())).willReturn(existingWidget, existingWidget, null);

        Widget createdWidget = h2WidgetRepository.save(null, widget);
        assertEquals(widget.getZIndex(), createdWidget.getZIndex());
    }

    @Test
    void save_whenIdExistsButNoWidgetFound_thenThrowWidgetNotFoundException() {
        UUID uuid = UUID.randomUUID();
        Widget widget = Widget.builder().id(uuid).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyInt())).willReturn(null);
        given(mockJdbcTemplate.update(anyString(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(LocalDateTime.class), anyString())).willReturn(0);

        assertThrows(WidgetNotFoundException.class, () -> h2WidgetRepository.save(uuid.toString(), widget));
    }

    @Test
    void save_whenIdExistsAndWidgetFound_thenUpdateAndReturnUpdatedWidget() {
        UUID uuid = UUID.randomUUID();
        Widget widget = Widget.builder().id(uuid).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyInt())).willReturn(null);
        given(mockJdbcTemplate.update(anyString(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(LocalDateTime.class), anyString())).willReturn(1);

        Widget updatedWidget = h2WidgetRepository.save(uuid.toString(), widget);
        assertEquals(widget.getId(), updatedWidget.getId());
        assertNotNull(updatedWidget.getUpdateTime());
    }

    @Test
    void deleteById_whenIdDoesntExist_thenThrowWidgetNotFoundException() {
        UUID uuid = UUID.randomUUID();

        // mock
        given(mockJdbcTemplate.update(H2WidgetRepository.QUERY_DELETE_WIDGET, uuid.toString())).willReturn(0);

        assertThrows(WidgetNotFoundException.class, () -> h2WidgetRepository.deleteById(uuid.toString()));
    }

    @Test
    void deleteById_whenIdExists_thenDeleteWidget() {
        UUID uuid = UUID.randomUUID();

        // mock
        given(mockJdbcTemplate.update(H2WidgetRepository.QUERY_DELETE_WIDGET, uuid.toString())).willReturn(1);

        h2WidgetRepository.deleteById(uuid.toString());

        verify(mockJdbcTemplate).update(H2WidgetRepository.QUERY_DELETE_WIDGET, uuid.toString());
    }

    @Test
    void findById_whenIdDoesntExist_thenThrowWidgetNotFoundException() {
        UUID uuid = UUID.randomUUID();

        // mock
        doThrow(EmptyResultDataAccessException.class).when(mockJdbcTemplate).queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyString());

        assertThrows(WidgetNotFoundException.class, () -> h2WidgetRepository.findById(uuid.toString()));
    }

    @Test
    void findById_whenIdExists_thenReturnFoundWidget() {
        UUID uuid = UUID.randomUUID();

        Widget widget = Widget.builder().id(uuid).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build();

        // mock
        given(mockJdbcTemplate.queryForObject(anyString(), any(BeanPropertyRowMapper.class), anyLong())).willReturn(widget);

        Assertions.assertEquals(widget, h2WidgetRepository.findById(uuid.toString()));
    }

    @Test
    void findWithLimit_whenWidgetsExist_thenReturnFoundWidgets() {
        List<Widget> widgets = List.of(
                Widget.builder().id(UUID.randomUUID()).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build(),
                Widget.builder().id(UUID.randomUUID()).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build()
        );

        // mock
        given(mockJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).willReturn(widgets);

        assertEquals(widgets, h2WidgetRepository.findWithLimit(10));
    }

    @Test
    void findWithCoordinates_whenWidgetsExist_thenReturnFoundWidgets() {
        RectangleCoordinates coordinates = RectangleCoordinates.builder().x1(1).y1(2).x2(3).y2(4).build();
        List<Widget> widgets = List.of(
                Widget.builder().id(UUID.randomUUID()).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build(),
                Widget.builder().id(UUID.randomUUID()).xIndex(10).yIndex(20).zIndex(5).width(30).height(40).build()
        );

        // mock
        given(mockJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).willReturn(widgets);

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