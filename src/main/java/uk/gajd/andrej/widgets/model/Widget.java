package uk.gajd.andrej.widgets.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * This is the model entity for Widget.
 *
 * @author gajduk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Widget {

    private UUID id;

    @JsonProperty("xIndex")
    private Integer xIndex;

    @JsonProperty("yIndex")
    private Integer yIndex;

    @JsonProperty("zIndex")
    private Integer zIndex;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    private LocalDateTime updateTime;
    
    public static Widget mapRowToWidget(ResultSet resultSet, int rowNum) throws SQLException {
        return Widget.builder()
                .id(UUID.fromString(resultSet.getString("id")))
                .xIndex(resultSet.getInt("xIndex"))
                .yIndex(resultSet.getInt("yIndex"))
                .zIndex(resultSet.getInt("zIndex"))
                .width(resultSet.getInt("width"))
                .height(resultSet.getInt("height"))
                .updateTime(resultSet.getTimestamp("updateTime").toLocalDateTime())
                .build();
    }
}
