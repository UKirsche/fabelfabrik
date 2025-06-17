package com.fabelfabrik.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.Map;

@Path("/api/pdf")
public class PdfResource extends AbstractMediaResource {

    private static final Map<String, String> PDF_CONTENT_TYPES = Map.of(
            ".pdf", "application/pdf"
    );

    @GET
    @Path("/{pdfPath: .+}")
    public Response getPDF(@PathParam("pdfPath") String pdfPath) {
        return getMediaFile(pdfPath, "PDF");
    }

    @Override
    protected File getFileFromStorage(String filePath) {
        return fileStorageService.getPdf(filePath);
    }

    @Override
    protected Map<String, String> getContentTypeMap() {
        return PDF_CONTENT_TYPES;
    }
}