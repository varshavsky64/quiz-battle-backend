package com.github.varshavsky64.quizbattle.controller.admin;

import com.github.varshavsky64.quizbattle.domain.entity.QuestionEntity;
import com.github.varshavsky64.quizbattle.domain.request.QuestionRequest;
import com.github.varshavsky64.quizbattle.domain.response.QuestionResponse;
import com.github.varshavsky64.quizbattle.service.QuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminQuestionController.class)
@DisplayName("Admin question management REST API")
class AdminQuestionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    QuestionService questionService;

    @Test
    @DisplayName("Delegates to service with correct page request")
    void list_delegatesWithCorrectPageRequest() throws Exception {
        // given
        Page<QuestionResponse> page = new PageImpl<>(List.of(), PageRequest.of(2, 15), 0);
        when(questionService.list(PageRequest.of(2, 15))).thenReturn(page);

        // when
        mockMvc.perform(get("/admin/questions").param("page", "2").param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // then
        verify(questionService).list(PageRequest.of(2, 15));
    }

    @Test
    @DisplayName("Uses page=0, size=20 when no params provided")
    void list_defaultPagination() throws Exception {
        // given
        when(questionService.list(PageRequest.of(0, 20))).thenReturn(Page.empty());

        // when
        mockMvc.perform(get("/admin/questions"))
                .andExpect(status().isOk());

        // then
        verify(questionService).list(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("Creates question and returns 201")
    void create_callsServiceAndReturns201() throws Exception {
        // given
        QuestionRequest request = buildRequest("What is 2+2?");
        QuestionResponse response = mockResponse(1L, "What is 2+2?");
        when(questionService.create(any())).thenReturn(response);

        // when
        mockMvc.perform(post("/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("What is 2+2?"));
    }

    @Test
    @DisplayName("Returns 400 when question text is blank")
    void create_returns400_whenTextBlank() throws Exception {
        // given
        QuestionRequest request = buildRequest("What is 2+2?");
        request.setText("");

        // when
        mockMvc.perform(post("/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Returns 400 when answers list is missing")
    void create_returns400_whenAnswersMissing() throws Exception {
        // given
        QuestionRequest request = new QuestionRequest();
        request.setText("Valid text?");

        // when
        mockMvc.perform(post("/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Updates question with the correct ID")
    void update_callsServiceWithCorrectId() throws Exception {
        // given
        QuestionRequest request = buildRequest("Updated?");
        QuestionResponse response = mockResponse(42L, "Updated?");
        when(questionService.update(eq(42L), any())).thenReturn(response);

        // when
        mockMvc.perform(put("/admin/questions/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated?"));

        // then
        verify(questionService).update(eq(42L), any());
    }

    @Test
    @DisplayName("Deletes question and returns 204")
    void delete_returns204() throws Exception {
        // when
        mockMvc.perform(delete("/admin/questions/7"))
                .andExpect(status().isNoContent());

        // then
        verify(questionService).delete(7L);
    }

    private QuestionRequest buildRequest(String text) {
        QuestionRequest request = new QuestionRequest();
        request.setText(text);
        request.setDifficulty((short) 1);
        request.setAnswers(List.of(
                buildAnswer("Correct", true, (short) 0),
                buildAnswer("Wrong A", false, (short) 1),
                buildAnswer("Wrong B", false, (short) 2),
                buildAnswer("Wrong C", false, (short) 3)
        ));
        return request;
    }

    private QuestionRequest.AnswerRequest buildAnswer(String text, boolean correct, short position) {
        QuestionRequest.AnswerRequest a = new QuestionRequest.AnswerRequest();
        a.setText(text);
        a.setCorrect(correct);
        a.setPosition(position);
        return a;
    }

    private QuestionResponse mockResponse(Long id, String text) {
        QuestionResponse response = new QuestionResponse(new QuestionEntity(text, (short) 1) {{
            try {
                var f = QuestionEntity.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(this, id);
            } catch (Exception e) { throw new RuntimeException(e); }
        }});
        return response;
    }
}
