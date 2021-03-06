package uk.gajd.andrej.widgets;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gajd.andrej.widgets.model.CreateWidgetRequest;
import uk.gajd.andrej.widgets.model.Widget;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MiroApplicationIntegrationTests {
    private static final String WIDGET_API_PATH = "/v1/widgets/";
    private static final String WIDGET_API_BY_ID_PATH = WIDGET_API_PATH + "{id}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUp() {
        CreateWidgetRequest createRequest = CreateWidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();
        // Create 20 items in the database, so that we know at least 20 widgets exist.
        IntStream.range(0, 20).forEach(i ->
        {
            try {
                mockMvc.perform(post(WIDGET_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                        .andExpect(status().isCreated());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    @Test
    void createWidget_whenRequestIsInvalid_thenReturnBadRequest() throws Exception {
        CreateWidgetRequest request = CreateWidgetRequest.builder().build(); // Mandatory fields are not set.

        mockMvc.perform(post(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWidget_whenZIndexNotGiven_thenReturnCreatedWidget() throws Exception {
        CreateWidgetRequest request = CreateWidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();

        mockMvc.perform(post(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.zIndex", notNullValue()));
    }

    @Test
    void createWidget_whenAllPropertiesGiven_thenReturnCreatedWidget() throws Exception {
        CreateWidgetRequest request = CreateWidgetRequest.builder().xIndex(0).yIndex(0).zIndex(3).width(5).height(5).build();

        mockMvc.perform(post(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.zIndex", is(3)));
    }

    @Test
    void updateWidget_whenRequestIsInvalid_thenReturnBadRequest() throws Exception {
        CreateWidgetRequest request = CreateWidgetRequest.builder().build(); // Mandatory fields are not set.

        mockMvc.perform(put(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWidget_whenRequestIsValid_thenReturnUpdatedWidget() throws Exception {
        CreateWidgetRequest createRequest = CreateWidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();

        MvcResult createResponse = mockMvc.perform(post(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.zIndex", notNullValue()))
                .andReturn();

        Widget widget = objectMapper.readValue(createResponse.getResponse().getContentAsString(), Widget.class);
        String id = widget.getId().toString();
        // Changes xIndex from 0 to 5.
        CreateWidgetRequest updateRequest = CreateWidgetRequest.builder().xIndex(5).yIndex(0).width(5).height(5).build();
        mockMvc.perform(put(WIDGET_API_BY_ID_PATH, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.xIndex", is(5)))
                .andReturn();

    }

    @Test
    void deleteWidget_whenIdNotFound_thenReturnNotFound() throws Exception {
        mockMvc.perform(delete(WIDGET_API_BY_ID_PATH, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWidget_whenIdFound_thenDeleteWidget() throws Exception {
        CreateWidgetRequest createRequest = CreateWidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();

        MvcResult createResponse = mockMvc.perform(post(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Widget widget = objectMapper.readValue(createResponse.getResponse().getContentAsString(), Widget.class);

        mockMvc.perform(delete(WIDGET_API_BY_ID_PATH, widget.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void findById_whenIdNotFound_thenReturnNotFound() throws Exception {
        mockMvc.perform(get(WIDGET_API_BY_ID_PATH, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_whenIdFound_thenReturnFoundWidget() throws Exception {
        CreateWidgetRequest createRequest = CreateWidgetRequest.builder().xIndex(0).yIndex(0).width(5).height(5).build();

        MvcResult createResponse = mockMvc.perform(post(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Widget createdWidget = objectMapper.readValue(createResponse.getResponse().getContentAsString(), Widget.class);

        MvcResult findByIdResponse = mockMvc.perform(get(WIDGET_API_BY_ID_PATH, createdWidget.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        Widget foundWidget = objectMapper.readValue(findByIdResponse.getResponse().getContentAsString(), Widget.class);

        assertEquals(createdWidget, foundWidget);
    }

    @Test
    void findAll_whenLimitChanges_adaptAndReturnLimitedWidgets() throws Exception {
        // When limit not defined, default(10) is taken
        mockMvc.perform(get(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));

        mockMvc.perform(get(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void findAll_whenInvalidCoordinatesGiven_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .param("x1", "10")
                .param("y1", "10")
                .param("x2", "0")
                .param("y2", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_whenCoordinatesAreValid_thenReturnFoundWidgets() throws Exception {
        CreateWidgetRequest createRequest1 = CreateWidgetRequest.builder().xIndex(1000).yIndex(1050).width(100).height(100).build();
        CreateWidgetRequest createRequest2 = CreateWidgetRequest.builder().xIndex(1050).yIndex(1100).width(100).height(100).build();
        CreateWidgetRequest createRequest3 = CreateWidgetRequest.builder().xIndex(1100).yIndex(1100).width(100).height(100).build();


        Widget widget1 = saveWidget(createRequest1);
        Widget widget2 = saveWidget(createRequest2);
        saveWidget(createRequest3);

        mockMvc.perform(get(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .param("x1", "1000")
                .param("y1", "1000")
                .param("x2", "1100")
                .param("y2", "1150"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(widget1, widget2))));
    }

    private Widget saveWidget(CreateWidgetRequest request) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(WIDGET_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Widget.class);
    }
}
