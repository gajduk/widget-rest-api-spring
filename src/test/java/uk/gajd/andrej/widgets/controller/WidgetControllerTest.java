package uk.gajd.andrej.widgets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gajd.andrej.widgets.exception.WidgetNotFoundException;
import uk.gajd.andrej.widgets.model.CreateWidgetRequest;
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
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WidgetController.class)
class WidgetControllerTest {
    private static final Integer DUMMY_VALID_LIMIT = 20;
    private static final List<Widget> DUMMY_WIDGET_LIST = List.of(
            Widget.builder().id(UUID.randomUUID()).xIndex(0).yIndex(0).width(5).height(5).build(),
            Widget.builder().id(UUID.randomUUID()).xIndex(2).yIndex(2).width(3).height(3).build(),
            Widget.builder().id(UUID.randomUUID()).xIndex(3).yIndex(3).width(3).height(3).build()
    );
    public static final String API_PATH = "/v1/widgets";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WidgetService widgetService;

    @Test
    void create_whenBodyIsInvalid_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post(API_PATH))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenBodyIsValid_thenReturnReturnCreated() throws Exception {
        CreateWidgetRequest request = CreateWidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();
        Widget createdWidget = request.toWidget();
        UUID uuid = UUID.randomUUID();
        createdWidget.setId(uuid);

        //mock
        given(widgetService.createWidget(request.toWidget())).willReturn(createdWidget);

        mockMvc.perform(post(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(createdWidget.getId().toString())))
                .andExpect(jsonPath("$.xIndex", is(createdWidget.getXIndex())))
                .andExpect(jsonPath("$.yIndex", is(createdWidget.getYIndex())))
                .andExpect(jsonPath("$.width", is(createdWidget.getWidth())))
                .andExpect(jsonPath("$.height", is(createdWidget.getHeight())));
    }

    @Test
    void update_whenBodyIsInvalid_thenReturnBadRequest() throws Exception {
        mockMvc.perform(put(API_PATH))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenBodyIsValid_thenReturnReturnOk() throws Exception {
        CreateWidgetRequest request = CreateWidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();

        String uuid = UUID.randomUUID().toString();
        //mock
        given(widgetService.updateWidget(uuid, request.toWidget())).willReturn(request.toWidget());

        mockMvc.perform(put(API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(uuid)));
    }

    @Test
    void delete_whenIdNotFound_thenReturnNotFound() throws Exception {
        String uuid = UUID.randomUUID().toString();

        //mock
        Mockito.doThrow(new WidgetNotFoundException("Widget not found by id")).when(widgetService).deleteWidget(uuid);

        mockMvc.perform(delete("/v1/widgets/{id}", uuid))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_whenIdExists_thenReturnOk() throws Exception {
        String uuid = UUID.randomUUID().toString();

        //mock
        doNothing().when(widgetService).deleteWidget(uuid);

        mockMvc.perform(delete("/v1/widgets/{id}", uuid))
                .andExpect(status().isOk());
    }

    @Test
    void findById_whenIdNotFound_thenReturnNotFound() throws Exception {
        String uuid = UUID.randomUUID().toString();

        //mock
        doThrow(new WidgetNotFoundException("Widget not found by id")).when(widgetService).findWidgetById(uuid);

        mockMvc.perform(get("/v1/widgets/{id}", uuid))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_whenIdExists_thenReturnWidget() throws Exception {
        UUID uuid = UUID.randomUUID();

        Widget foundWidget = Widget.builder().id(uuid).xIndex(0).yIndex(0).width(5).height(5).build();

        //mock
        given(widgetService.findWidgetById(uuid.toString())).willReturn(foundWidget);

        mockMvc.perform(get("/v1/widgets/{id}", uuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(uuid.toString())));
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
        RectangleCoordinates coordinates = RectangleCoordinates.builder().x1(2).y1(2).x2(1).y2(4).build();

        //mock
        given(widgetService.findWithCoordinates(coordinates, DUMMY_VALID_LIMIT)).willReturn(DUMMY_WIDGET_LIST);

        String path = "/v1/widgets?x1=" + coordinates.getX1() + "&y1=" + coordinates.getY1()
                + "&x2=" + coordinates.getX2() + "&y2=" + coordinates.getY2();
        mockMvc.perform(get(path + "&limit=20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_whenCoordinatesAreValid_thenReturnWidgets() throws Exception {
        RectangleCoordinates coordinates = RectangleCoordinates.builder().x1(1).y1(2).x2(3).y2(4).build();

        //mock
        given(widgetService.findWithCoordinates(coordinates, DUMMY_VALID_LIMIT)).willReturn(DUMMY_WIDGET_LIST);

        String path = "/v1/widgets?x1=" + coordinates.getX1() + "&y1=" + coordinates.getY1()
                + "&x2=" + coordinates.getX2() + "&y2=" + coordinates.getY2();
        mockMvc.perform(get(path + "&limit=20"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(DUMMY_WIDGET_LIST)));

        verify(widgetService, times(1)).findWithCoordinates(coordinates, DUMMY_VALID_LIMIT);
    }
}