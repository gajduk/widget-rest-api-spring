package uk.gajd.andrej.widgets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.WidgetRequest;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import uk.gajd.andrej.widgets.service.WidgetService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WidgetController.class)
class WidgetControllerTest {
    private static final Long DUMMY_WIDGET_ID = 123L;
    private static final Integer DUMMY_VALID_LIMIT = 20;
    private static final List<Widget> DUMMY_WIDGET_LIST = List.of(
            Widget.builder().id(DUMMY_WIDGET_ID).xIndex(0).yIndex(0).width(5).height(5).build(),
            Widget.builder().id(DUMMY_WIDGET_ID + 1).xIndex(0).yIndex(0).width(5).height(5).build()
    );

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WidgetService widgetService;

    @Test
    void create_whenBodyIsInvalid_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/widgets"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenBodyIsValid_thenReturnReturnCreated() throws Exception {
        WidgetRequest request = WidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();
        Widget createdWidget = request.toWidget(null);
        createdWidget.setId(DUMMY_WIDGET_ID);

        //mock
        given(widgetService.createWidget(request.toWidget(null))).willReturn(createdWidget);

        mockMvc.perform(post("/v1/widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(createdWidget.getId().intValue())))
                .andExpect(jsonPath("$.xIndex", is(createdWidget.getXIndex())))
                .andExpect(jsonPath("$.yIndex", is(createdWidget.getYIndex())))
                .andExpect(jsonPath("$.width", is(createdWidget.getWidth())))
                .andExpect(jsonPath("$.height", is(createdWidget.getHeight())));
    }

    @Test
    void update_whenBodyIsInvalid_thenReturnBadRequest() throws Exception {
        mockMvc.perform(put("/v1/widgets/{id}", DUMMY_WIDGET_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenIdNotProvided_thenReturnBadRequest() throws Exception {
        WidgetRequest request = WidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();

        mockMvc.perform(put("/v1/widgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void update_whenBodyIsValid_thenReturnReturnOk() throws Exception {
        WidgetRequest request = WidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();

        //mock
        given(widgetService.updateWidget(request.toWidget(DUMMY_WIDGET_ID))).willReturn(request.toWidget(DUMMY_WIDGET_ID));

        mockMvc.perform(put("/v1/widgets/{id}", DUMMY_WIDGET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(DUMMY_WIDGET_ID.intValue())));
    }

    @Test
    void delete_whenIdNotFound_thenReturnNotFound() throws Exception {
        //mock
        Mockito.doThrow(new WidgetNotFoundException("Widget not found by id")).when(widgetService).deleteWidget(DUMMY_WIDGET_ID);

        mockMvc.perform(delete("/v1/widgets/{id}", DUMMY_WIDGET_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_whenIdExists_thenReturnOk() throws Exception {
        //mock
        doNothing().when(widgetService).deleteWidget(DUMMY_WIDGET_ID);

        mockMvc.perform(delete("/v1/widgets/{id}", DUMMY_WIDGET_ID))
                .andExpect(status().isOk());
    }

    @Test
    void findById_whenIdNotFound_thenReturnNotFound() throws Exception {
        //mock
        doThrow(new WidgetNotFoundException("Widget not found by id")).when(widgetService).findWidgetById(DUMMY_WIDGET_ID);

        mockMvc.perform(get("/v1/widgets/{id}", DUMMY_WIDGET_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_whenIdExists_thenReturnWidget() throws Exception {
        Widget foundWidget = Widget.builder().id(DUMMY_WIDGET_ID).xIndex(0).yIndex(0).width(5).height(5).build();

        //mock
        given(widgetService.findWidgetById(foundWidget.getId())).willReturn(foundWidget);

        mockMvc.perform(get("/v1/widgets/{id}", DUMMY_WIDGET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(foundWidget.getId().intValue())));
    }

    @Test
    void findAll_whenLimitIsBelowZero_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/widgets?limit=-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_whenLimitIsAbove500_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/widgets?limit=501"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_whenLimitIsValid_thenReturnWidgets() throws Exception {
        //mock
        given(widgetService.findWithLimit(DUMMY_VALID_LIMIT)).willReturn(DUMMY_WIDGET_LIST);

        mockMvc.perform(get("/v1/widgets?limit=20"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(DUMMY_WIDGET_LIST)));

        verify(widgetService).findWithLimit(DUMMY_VALID_LIMIT);
    }

    @Test
    void findAll_whenCoordinatesAreNotValid_thenReturnBadRequest() throws Exception {
        RectangleCoordinates coordinates = RectangleCoordinates.builder().x0(2).y0(2).x1(1).y1(4).build();

        //mock
        given(widgetService.findWithCoordinates(coordinates, DUMMY_VALID_LIMIT)).willReturn(DUMMY_WIDGET_LIST);

        String path = "/v1/widgets?x0=" + coordinates.getX0() + "&y0=" + coordinates.getY0()
                + "&x1=" + coordinates.getX1() + "&y1=" + coordinates.getY1();
        mockMvc.perform(get(path + "&limit=20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_whenCoordinatesAreValid_thenReturnWidgets() throws Exception {
        RectangleCoordinates coordinates = RectangleCoordinates.builder().x0(1).y0(2).x1(3).y1(4).build();

        //mock
        given(widgetService.findWithCoordinates(coordinates, DUMMY_VALID_LIMIT)).willReturn(DUMMY_WIDGET_LIST);

        String path = "/v1/widgets?x0=" + coordinates.getX0() + "&y0=" + coordinates.getY0()
                + "&x1=" + coordinates.getX1() + "&y1=" + coordinates.getY1();
        mockMvc.perform(get(path + "&limit=20"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(DUMMY_WIDGET_LIST)));

        verify(widgetService, times(1)).findWithCoordinates(coordinates, DUMMY_VALID_LIMIT);
    }
}