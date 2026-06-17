package com.katixo.studio.edit;

import com.katixo.studio.edit.EditRequests.RemoveBgRequest;
import com.katixo.studio.edit.EditRequests.UpscaleRequest;
import com.katixo.studio.job.JobIdResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/edit")
public class EditController {

    private final EditService editService;

    public EditController(EditService editService) {
        this.editService = editService;
    }

    @PostMapping("/remove-bg")
    public JobIdResponse removeBg(@Valid @RequestBody RemoveBgRequest request) {
        return new JobIdResponse(editService.submitRemoveBg(request));
    }

    @PostMapping("/upscale")
    public JobIdResponse upscale(@Valid @RequestBody UpscaleRequest request) {
        return new JobIdResponse(editService.submitUpscale(request));
    }
}
