package uk.gajd.andrej.widgets.repository.impl;

import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import uk.gajd.andrej.widgets.repository.WidgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This is the repository implementation to use H2 in-memory DB as datasource for operations.
 *
 * @author gajduk
 */
@Repository
@Profile("h2")
@RequiredArgsConstructor
public class H2WidgetRepository implements WidgetRepository {
    static final String QUERY_SELECT_MAX_Z_INDEX = "select max(zIndex) from widget";
    static final String QUERY_DELETE_WIDGET = "delete from widget where id=?";
    private static final String QUERY_SELECT_BY_Z_INDEX = "select * from widget where zIndex=?";
    private static final String QUERY_INSERT_WIDGET = "insert into widget (id, xIndex, yIndex, zIndex, width, height, updateTime) values(?,?,?,?,?,?,?)";
    private static final int QUERY_RESULT_SUCCESS = 1;
    private static final String QUERY_UPDATE_Z_INDEXES = "update widget set zIndex = zIndex + 1 where id=?";
    private static final String QUERY_UPDATE_WIDGET = "update widget set xIndex=?, yIndex=?, zIndex=?, width=?, height=?, updateTime=? where id = ?";
    private static final String QUERY_SELECT_BY_ID = "select * from widget where id=?";
    private static final String QUERY_SELECT_BY_LIMIT = "select * from widget order by zIndex limit ?";
    private static final String QUERY_SELECT_BY_COORDINATE_AND_LIMIT = "select * from widget " +
            "where xIndex >= ? and xIndex <= ?" +
            "order by zIndex limit ?";

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Widget save(String id, Widget widget) {
        if (widget.getZIndex() != null && findByZIndex(widget.getZIndex()).isPresent()) { // Shifting required.
            shift(widget.getZIndex());
        }

        if (widget.getZIndex() == null) {
            widget.setZIndex(getMaxZIndex() + 1);
        }

        return id == null ? saveWidget(widget) : updateWidget(id, widget);
    }

    @Override
    public void deleteById(String id) {
        if (jdbcTemplate.update(QUERY_DELETE_WIDGET, id) != QUERY_RESULT_SUCCESS) {
            throw new WidgetNotFoundException("Couldn't find widget to delete with id: " + id);
        }
    }

    @Override
    public Widget findById(String id) {
        try {
            return jdbcTemplate.queryForObject(QUERY_SELECT_BY_ID, new BeanPropertyRowMapper<>(Widget.class), id);
        } catch (EmptyResultDataAccessException e) {
            throw new WidgetNotFoundException("Couldn't find widget by id: " + id);
        }
    }

    @Override
    public List<Widget> findWithLimit(Integer limit) {
        return jdbcTemplate.query(QUERY_SELECT_BY_LIMIT,
                Widget::mapRowToWidget, limit);
    }

    @Override
    public List<Widget> findWithCoordinates(RectangleCoordinates coordinates, Integer limit) {
        return jdbcTemplate.query(QUERY_SELECT_BY_COORDINATE_AND_LIMIT,
                Widget::mapRowToWidget,
                coordinates.getX1(),
                coordinates.getX2())
                .stream()
                .filter(widget -> isInRectangle(widget, coordinates))
                .sorted((w1, w2) -> w1.getZIndex()-w2.getZIndex())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void shift(Integer zIndex) {
        List<UUID> widgetIdsToShift = new ArrayList<>();
        boolean widgetExistsAtZIndex = true;

        while (widgetExistsAtZIndex) {
            Optional<Widget> optionalWidget = findByZIndex(zIndex);
            if (optionalWidget.isEmpty()) {
                widgetExistsAtZIndex = false;
            } else {
                widgetIdsToShift.add(optionalWidget.get().getId());
                zIndex++;
            }
        }

        updateZIndexes(widgetIdsToShift);
    }

    private Widget saveWidget(Widget widget) {
        widget.setUpdateTime(LocalDateTime.now());

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(QUERY_INSERT_WIDGET);
            ps.setString(1, UUID.randomUUID().toString());
            ps.setInt(2, widget.getXIndex());
            ps.setInt(3, widget.getYIndex());
            ps.setInt(4, widget.getZIndex());
            ps.setInt(5, widget.getWidth());
            ps.setInt(6, widget.getHeight());
            ps.setTimestamp(6, Timestamp.valueOf(widget.getUpdateTime()));
            return ps;
        });

        return widget;
    }

    private Widget updateWidget(String id, Widget widget) {
        widget.setUpdateTime(LocalDateTime.now());
        int updateResult = jdbcTemplate.update(QUERY_UPDATE_WIDGET,
                widget.getXIndex(),
                widget.getYIndex(),
                widget.getZIndex(),
                widget.getWidth(),
                widget.getHeight(),
                widget.getUpdateTime(),
                id);
        if (updateResult != QUERY_RESULT_SUCCESS) {
            throw new WidgetNotFoundException("Couldn't find widget to update with id: " + widget.getId());
        }

        return widget;
    }

    private int getMaxZIndex() {
        Integer maxZIndexValue = jdbcTemplate.queryForObject(QUERY_SELECT_MAX_Z_INDEX, Integer.class);
        return maxZIndexValue != null ? maxZIndexValue : 0;
    }

    private Optional<Widget> findByZIndex(Integer zIndex) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(QUERY_SELECT_BY_Z_INDEX, new BeanPropertyRowMapper<>(Widget.class), zIndex));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private void updateZIndexes(List<UUID> widgetIds) {
        jdbcTemplate.batchUpdate(QUERY_UPDATE_Z_INDEXES, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, widgetIds.get(i).toString());
            }

            public int getBatchSize() {
                return widgetIds.size();
            }
        });
    }
}
