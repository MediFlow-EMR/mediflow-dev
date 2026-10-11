package com.mediflow.emr.exception;

import com.mediflow.emr.dto.VitalSignRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InputErrorResponseTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new InputController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    @RestController static class InputController {
        @PostMapping("/records") String save(@Valid @RequestBody VitalSignRequest body) { return "saved"; }
        @GetMapping("/forbidden") String forbidden() { throw new AccessDeniedException("denied"); }
    }
    @Test void malformedJsonAndInvalidValuesReturn400InsteadOf500() throws Exception {
        mvc.perform(post("/records").contentType("application/json").content("{bad json" )).andExpect(status().isBadRequest());
        mvc.perform(post("/records").contentType("application/json").content("{\"patientId\":1,\"heartRate\":999}")).andExpect(status().isBadRequest());
        mvc.perform(post("/records").contentType("application/json").content("{\"patientId\":1}")).andExpect(status().isBadRequest());
    }
    @Test void deniedOperationsReturn403InsteadOf500() throws Exception {
        mvc.perform(get("/forbidden")).andExpect(status().isForbidden());
    }
}
