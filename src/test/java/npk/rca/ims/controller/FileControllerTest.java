package npk.rca.ims.controller;

import npk.rca.ims.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    @WithMockUser(roles = "USER")
    void uploadFile_ShouldBeAllowedForAuthenticatedUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        given(fileStorageService.storeFile(any())).willReturn("test.txt");

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void uploadFile_ShouldBeForbiddenForUnauthenticatedUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadFile_ShouldBeAllowedPublicly() throws Exception {
        Resource resource = new ByteArrayResource("content".getBytes());
        given(fileStorageService.loadFileAsResource("test.txt")).willReturn(resource);

        mockMvc.perform(get("/api/files/download/test.txt"))
                .andExpect(status().isOk());
    }
}
