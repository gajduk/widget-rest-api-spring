package uk.gajd.andrej.widgets.controller;

import uk.gajd.andrej.widgets.model.CreateWidgetRequest;
import uk.gajd.andrej.widgets.model.RectangleCoordinates;
import uk.gajd.andrej.widgets.model.Widget;
import uk.gajd.andrej.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * This is the controller for Widget related operations.
 *
 * @author gajduk
 */
@RestController
@RequestMapping("/v1/widgets")
@RequiredArgsConstructor
public class WidgetController {
    private static final String DEFAULT_LIMIT = "10";

    private final WidgetService widgetService;

    @PostMapping
    public ResponseEntity<Widget> create(@Valid @RequestBody CreateWidgetRequest createWidgetRequest) {
        return new ResponseEntity<>(widgetService.createWidget(createWidgetRequest.toWidget()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Widget> update(@PathVariable("id") String id,@Valid @RequestBody CreateWidgetRequest updateWidgetRequest) {
        return new ResponseEntity<>(widgetService.updateWidget(id, updateWidgetRequest.toWidget()), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id) {
        widgetService.deleteWidget(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Widget> findById(@PathVariable("id") String id) {
        return new ResponseEntity<>(widgetService.findWidgetById(id), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<Widget>> findAll(@RequestParam(required = false, defaultValue = DEFAULT_LIMIT) Integer limit,
                                                @RequestParam(required = false) Integer x1,
                                                @RequestParam(required = false) Integer y1,
                                                @RequestParam(required = false) Integer x2,
                                                @RequestParam(required = false) Integer y2) {
        validateLimit(limit);

        // All four points need to be provided
        if (x1 != null && y1 != null && x2 != null && y2 != null) {
            RectangleCoordinates coordinates = RectangleCoordinates.builder()
                    .x1(x1).y1(y1)
                    .x2(x2).y2(y2)
                    .build();
            if (!coordinates.isValid()) {
                throw new IllegalArgumentException("x2 should be bigger than x1 & y2 should be bigger than y1.");
            }
            return new ResponseEntity<>(widgetService.findWithCoordinates(coordinates, limit), HttpStatus.OK);
        }

        return new ResponseEntity<>(widgetService.findWithLimit(limit), HttpStatus.OK);
    }

    private void validateLimit(Integer limit) {
        if(limit <= 0 || limit > 500) {
            throw new IllegalArgumentException("Limit should be between 1-500. Default is 10.");
        }
    }
}
