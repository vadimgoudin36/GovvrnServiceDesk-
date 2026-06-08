package ru.gov.servicedesk.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.gov.servicedesk.model.Attachment;
import ru.gov.servicedesk.repository.WebAttachmentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class AttachmentStorageService {
    private final Path uploadDirectory;
    private final WebAttachmentRepository attachmentRepository;

    public AttachmentStorageService(Path uploadDirectory, WebAttachmentRepository attachmentRepository) {
        this.uploadDirectory = uploadDirectory;
        this.attachmentRepository = attachmentRepository;
    }

    public void store(int ticketId, int authorId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        String originalName = file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;
        Files.copy(file.getInputStream(), uploadDirectory.resolve(storedName));
        attachmentRepository.create(ticketId, authorId, originalName, storedName, file.getContentType(), file.getSize());
    }

    public Path resolve(Attachment attachment) {
        return uploadDirectory.resolve(attachment.getStoredName()).normalize();
    }
}
